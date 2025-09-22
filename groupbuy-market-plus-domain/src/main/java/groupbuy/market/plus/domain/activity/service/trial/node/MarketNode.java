package groupbuy.market.plus.domain.activity.service.trial.node;

import com.alibaba.fastjson.JSON;
import groupbuy.market.plus.domain.activity.model.entity.MarketProductEntity;
import groupbuy.market.plus.domain.activity.model.entity.TrialBalanceEntity;
import groupbuy.market.plus.domain.activity.model.valobj.ActivityVO;
import groupbuy.market.plus.domain.activity.model.valobj.SkuVO;
import groupbuy.market.plus.domain.activity.service.AbstractGroupBuyMarketSupport;
import groupbuy.market.plus.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import groupbuy.market.plus.domain.activity.service.trial.thread.GetActivityVOThreadTask;
import groupbuy.market.plus.domain.activity.service.trial.thread.GetSkuVOThreadTask;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.*;

/**
 * 试算节点
 */
@Slf4j
@Service
public class MarketNode extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    @Resource
    private EndNode endNode;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    protected void multiThread(MarketProductEntity marketProductEntity, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        // 异步查询拼团商品
        GetSkuVOThreadTask getSkuVOThreadTask = new GetSkuVOThreadTask(marketProductEntity.getGoodId(), activityRepository);
        FutureTask<SkuVO> skuVOFutureTask = new FutureTask<>(getSkuVOThreadTask);
        threadPoolExecutor.execute(skuVOFutureTask);

        // 异步查询拼团商品
        GetActivityVOThreadTask getActivityVOThreadTask = new GetActivityVOThreadTask(marketProductEntity.getGoodId(), marketProductEntity.getSource(), marketProductEntity.getChannel(), activityRepository);
        FutureTask<ActivityVO> activityVOFutureTask = new FutureTask<>(getActivityVOThreadTask);
        threadPoolExecutor.execute(activityVOFutureTask);

        // 写入上下文
        dynamicContext.setSkuVO(skuVOFutureTask.get(timeout, TimeUnit.MINUTES));
        dynamicContext.setActivityVO(activityVOFutureTask.get(timeout, TimeUnit.MINUTES));

        log.info("用户ID：{}，异步线程加载数据「GroupBuyActivityDiscountVO、SkuVO」完成", marketProductEntity.getUserId());
    }

    @Override
    protected TrialBalanceEntity doApply(MarketProductEntity marketProductEntity, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("拼团商品试算服务-试算节点 userId：{}", marketProductEntity.getUserId());
        log.info("商品信息：{}", JSON.toJSONString(dynamicContext.getSkuVO()));
        log.info("活动信息：{}", JSON.toJSONString(dynamicContext.getActivityVO()));
        return router(marketProductEntity, dynamicContext);
    }

    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity marketProductEntity, DefaultActivityStrategyFactory.DynamicContext dynamicContext) {
        return endNode;
    }

}
