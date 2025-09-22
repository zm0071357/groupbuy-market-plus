package groupbuy.market.plus.domain.activity.service;

import groupbuy.market.plus.domain.activity.adapter.repository.ActivityRepository;
import groupbuy.market.plus.types.design.framework.tree.AbstractStrategyRouter;
import groupbuy.market.plus.types.design.framework.tree.multithread.AbstractMultiThreadStrategyRouter;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 营销商品-策略路由
 * @param <MarketProductEntity>
 * @param <DynamicContext>
 * @param <TrialBalanceEntity>
 */
public abstract class AbstractGroupBuyMarketSupport<MarketProductEntity, DynamicContext, TrialBalanceEntity>
        extends AbstractMultiThreadStrategyRouter<MarketProductEntity, DynamicContext, TrialBalanceEntity> {

    // 超时时间
    protected long timeout = 500;

    // 活动仓储定义在抽象类中 - 每个节点都能直接使用，不需要重复引入
    @Resource
    protected ActivityRepository activityRepository;

    @Override
    protected void multiThread(MarketProductEntity requestParameter, DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        // 空实现 - 需要进行异步加载的子类手动做对应实现
        // 其他不需要的子类就不用实现，只需要实现处理节点的方法
    }

}
