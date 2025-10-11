package groupbuy.market.plus.domain.activity.service.trial.node;

import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.model.entity.TrialBalanceEntity;
import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.SkuVO;
import groupbuy.market.plus.domain.activity.service.AbstractGroupBuyMarketSupport;
import groupbuy.market.plus.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import groupbuy.market.plus.domain.activity.service.trial.thread.GetActivityVOThreadTask;
import groupbuy.market.plus.domain.activity.service.trial.thread.GetSkuVOThreadTask;
import groupbuy.market.plus.domain.tag.adapter.repository.TagRepository;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.*;

/**
 * 人群标签节点
 */
@Slf4j
@Service
public class TagNode extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    @Resource
    private MarketNode marketNode;

    @Resource
    private EndNode endNode;

    @Resource
    private TagRepository tagRepository;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    protected void multiThread(MarketProductEntity marketProductEntity, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        // 异步查询拼团商品
        GetSkuVOThreadTask getSkuVOThreadTask = new GetSkuVOThreadTask(marketProductEntity.getGoodsId(), activityRepository);
        FutureTask<SkuVO> skuVOFutureTask = new FutureTask<>(getSkuVOThreadTask);
        threadPoolExecutor.execute(skuVOFutureTask);

        // 异步查询拼团活动
        GetActivityVOThreadTask getActivityVOThreadTask = new GetActivityVOThreadTask(marketProductEntity.getGoodsId(), marketProductEntity.getSource(), marketProductEntity.getChannel(), activityRepository);
        FutureTask<ActivityVO> activityVOFutureTask = new FutureTask<>(getActivityVOThreadTask);
        threadPoolExecutor.execute(activityVOFutureTask);

        // 写入上下文
        dynamicContext.setSkuVO(skuVOFutureTask.get(timeout, TimeUnit.MINUTES));
        dynamicContext.setActivityVO(activityVOFutureTask.get(timeout, TimeUnit.MINUTES));

        log.info("用户ID：{}，异步线程加载数据「拼团活动、拼团商品」完成", marketProductEntity.getUserId());
    }

    @Override
    protected TrialBalanceEntity doApply(MarketProductEntity marketProductEntity, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        ActivityVO activityVO = dynamicContext.getActivityVO();
        String tagId = activityVO.getTagId();
        // 人群标签为空 - 默认都可以参与优惠试算
        if (StringUtils.isBlank(tagId)) {
            log.info("人群标签为空，所有用户可参与优惠试算");
            dynamicContext.setDiscount(true);
            return router(marketProductEntity, dynamicContext);
        }
        // 人群标签不为空 - 校验用户
        // 是否在人群标签范围内
        boolean isTagCrowdUser = tagRepository.isTagCrowdUser(tagId, marketProductEntity.getUserId());
        // 是否可见
        boolean visible = activityVO.isVisible();
        // 是否可参与
        boolean enable = activityVO.isEnable();
        // 对于人群标签内的用户
        // 同时满足可见和可参与条件时才能享用折扣
        // 同时满足不可见和不可参与条件时，则限定该人群标签用户不能享用此折扣，不在人群标签内的用户能享用折扣
        if (isTagCrowdUser) {
            dynamicContext.setDiscount(visible && enable);
        } else {
            dynamicContext.setDiscount(!visible && !enable);
        }
        return router(marketProductEntity, dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity marketProductEntity, DefaultActivityStrategyFactory.DynamicContext dynamicContext) {
        // 符合条件 - 进行优惠试算
        // 否则返回原始价格
        if (dynamicContext.isDiscount()) {
            log.info("userId：{}，满足条件，可参与优惠试算", marketProductEntity.getUserId());
            return marketNode;
        }
        log.info("userId：{}，不满足条件，不可参与优惠试算", marketProductEntity.getUserId());
        return endNode;
    }

}
