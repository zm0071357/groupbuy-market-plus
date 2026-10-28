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
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
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
    private TrialEndNode trialEndNode;

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
        dynamicContext.setSkuVO(skuVOFutureTask.get(timeout, TimeUnit.MILLISECONDS));
        dynamicContext.setActivityVO(activityVOFutureTask.get(timeout, TimeUnit.MILLISECONDS));

        log.info("用户ID：{}，异步线程加载数据「拼团活动、拼团商品」完成", marketProductEntity.getUserId());
    }

    @Override
    protected TrialBalanceEntity doApply(MarketProductEntity marketProductEntity, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        ActivityVO activityVO = dynamicContext.getActivityVO();
        // 商品未配置拼团活动或商品不存在 - 与错误节点保持一致，返回不存在拼团营销配置
        if (activityVO == null || dynamicContext.getSkuVO() == null) {
            log.info("拼团商品试算服务-标签节点，不存在拼团营销配置，用户ID：{}，商品ID：{}", marketProductEntity.getUserId(), marketProductEntity.getGoodsId());
            throw new AppException(ResponseCodeEnum.E0002.getCode(), ResponseCodeEnum.E0002.getInfo());
        }
        String tagId = activityVO.getTagId();
        // 人群标签为空 - 默认都可以参与优惠试算
        if (StringUtils.isBlank(tagId)) {
            log.info("人群标签为空，所有用户可参与优惠试算");
            dynamicContext.setDiscount(true);
            return router(marketProductEntity, dynamicContext);
        }
        // 人群范围配置错误（仅支持白名单 2,2、黑名单 1,1）- 无法确定优惠对象，所有用户均不享受优惠，
        // 避免如黑名单漏配范围时反而只给人群内用户优惠
        if (!activityVO.isValidTagScope()) {
            log.error("活动人群范围配置错误，仅支持白名单 {}、黑名单 {}，所有用户不可享用优惠，活动ID：{}，tagId：{}，tagScope：{}",
                    ActivityVO.TAG_SCOPE_WHITE_LIST, ActivityVO.TAG_SCOPE_BLACK_LIST, activityVO.getActivityId(), tagId, activityVO.getTagScope());
            dynamicContext.setDiscount(false);
            return router(marketProductEntity, dynamicContext);
        }
        // 人群标签数据不存在 - 无法判断用户是否在人群内，不可享用优惠（白名单、黑名单两种配置均拒绝）
        if (!tagRepository.isTagCrowdExists(tagId)) {
            log.warn("人群标签数据不存在，不可参与优惠试算，tagId：{}，用户ID：{}", tagId, marketProductEntity.getUserId());
            dynamicContext.setDiscount(false);
            return router(marketProductEntity, dynamicContext);
        }
        // 人群标签只决定能否享受优惠，不限制参与拼团 - 不享受优惠的用户按原价拼团
        // 白名单：人群内用户享受优惠；黑名单：人群外用户享受优惠
        boolean isTagCrowdUser = tagRepository.isTagCrowdUser(tagId, marketProductEntity.getUserId());
        dynamicContext.setDiscount(activityVO.isTagCrowdDiscount(isTagCrowdUser));
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
        return trialEndNode;
    }

}
