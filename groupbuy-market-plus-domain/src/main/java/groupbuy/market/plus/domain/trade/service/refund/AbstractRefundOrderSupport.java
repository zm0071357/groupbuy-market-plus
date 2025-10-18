package groupbuy.market.plus.domain.trade.service.refund;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.types.design.framework.tree.multithread.AbstractMultiThreadStrategyRouter;

import javax.annotation.Resource;

/**
 * 退单策略路由
 * @param <CheckRefundEntity>
 * @param <DynamicContext>
 * @param <CheckRefundResEntity>
 */
public abstract class AbstractRefundOrderSupport<CheckRefundEntity, DynamicContext, CheckRefundResEntity> extends AbstractMultiThreadStrategyRouter<CheckRefundEntity, DynamicContext, CheckRefundResEntity> {

    // 超时时间
    protected long timeout = 500;

    @Resource
    protected TradeRepository tradeRepository;

    @Override
    protected void multiThread(CheckRefundEntity requestParameter, DynamicContext dynamicContext) throws Exception {

    }
}
