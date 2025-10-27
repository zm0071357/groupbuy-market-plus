package groupbuy.market.plus.domain.trade.service.refund.node;

import groupbuy.market.plus.domain.trade.model.aggregate.RefundThreadTaskAggregate;
import groupbuy.market.plus.domain.trade.model.entity.PreRefundEntity;
import groupbuy.market.plus.domain.trade.model.entity.RefundResEntity;
import groupbuy.market.plus.domain.trade.model.entity.PreRefundOrderEntity;
import groupbuy.market.plus.domain.trade.model.valobj.OrderStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.RefundStatusEnum;
import groupbuy.market.plus.domain.trade.service.refund.AbstractRefundOrderSupport;
import groupbuy.market.plus.domain.trade.service.refund.factory.DefaultRefundStrategyFactory;
import groupbuy.market.plus.domain.trade.service.refund.thread.GetAggregateThreadTask;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 重复退单节点
 */
@Slf4j
@Service
public class RefundRepeatNode extends AbstractRefundOrderSupport<PreRefundEntity, DefaultRefundStrategyFactory.DynamicContext, RefundResEntity> {

    @Resource
    private RefundUserNode refundUserNode;

    @Resource
    private RefundEndNode refundEndNode;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    protected void multiThread(PreRefundEntity preRefundEntity, DefaultRefundStrategyFactory.DynamicContext dynamicContext) throws Exception {
        // 异步查询退单所需数据聚合
        GetAggregateThreadTask getAggregateThreadTask = new GetAggregateThreadTask(preRefundEntity, tradeRepository);
        FutureTask<RefundThreadTaskAggregate> refundThreadTaskResAggregateFutureTask = new FutureTask<>(getAggregateThreadTask);
        threadPoolExecutor.execute(refundThreadTaskResAggregateFutureTask);
        RefundThreadTaskAggregate refundThreadTaskAggregate = refundThreadTaskResAggregateFutureTask.get(timeout, TimeUnit.MINUTES);

        // 写入上下文
        dynamicContext.setPreRefundOrderEntity(refundThreadTaskAggregate.getPreRefundOrderEntity());
        dynamicContext.setGroupBuyTeamEntity(refundThreadTaskAggregate.getGroupBuyTeamEntity());

        log.info("退单服务 - 重复退单节点，用户ID：{}，异步线程加载数据「预退单订单、拼团组队」完成", preRefundEntity.getUserId());
    }

    @Override
    protected RefundResEntity doApply(PreRefundEntity preRefundEntity, DefaultRefundStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("退单服务 - 重复退单节点，用户ID：{}，外部交易单号：{}", preRefundEntity.getUserId(), preRefundEntity.getOutTradeNo());
        PreRefundOrderEntity preRefundOrderEntity = dynamicContext.getPreRefundOrderEntity();
        if (preRefundOrderEntity.getOrderStatusEnum().equals(OrderStatusEnum.REFUND)) {
            log.info("退单服务 - 重复退单节点，用户已退单，用户ID：{}，外部交易单号：{}", preRefundEntity.getUserId(), preRefundEntity.getOutTradeNo());
            dynamicContext.setRefundStatusEnum(RefundStatusEnum.REPEAT);
        }
        return router(preRefundEntity, dynamicContext);
    }

    @Override
    public StrategyHandler<PreRefundEntity, DefaultRefundStrategyFactory.DynamicContext, RefundResEntity> get(PreRefundEntity requestParameter, DefaultRefundStrategyFactory.DynamicContext dynamicContext) {
        // 已经退单 - 直接到尾节点
        if (RefundStatusEnum.REPEAT.equals(dynamicContext.getRefundStatusEnum())) {
            return refundEndNode;
        }
        return refundUserNode;
    }
}
