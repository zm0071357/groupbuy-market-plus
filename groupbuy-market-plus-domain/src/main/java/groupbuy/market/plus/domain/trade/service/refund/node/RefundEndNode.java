package groupbuy.market.plus.domain.trade.service.refund.node;

import groupbuy.market.plus.domain.trade.model.entity.PreRefundEntity;
import groupbuy.market.plus.domain.trade.model.entity.PreRefundOrderEntity;
import groupbuy.market.plus.domain.trade.model.entity.RefundResEntity;
import groupbuy.market.plus.domain.trade.model.valobj.RefundStatusEnum;
import groupbuy.market.plus.domain.trade.service.refund.AbstractRefundOrderSupport;
import groupbuy.market.plus.domain.trade.service.refund.factory.DefaultRefundStrategyFactory;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 尾节点
 */
@Slf4j
@Service
public class RefundEndNode extends AbstractRefundOrderSupport<PreRefundEntity, DefaultRefundStrategyFactory.DynamicContext, RefundResEntity> {

    @Override
    protected RefundResEntity doApply(PreRefundEntity preRefundEntity, DefaultRefundStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("退单服务 - 尾节点，用户ID：{}，外部交易单号：{}", preRefundEntity.getUserId(), preRefundEntity.getOutTradeNo());
        PreRefundOrderEntity preRefundOrderEntity = dynamicContext.getPreRefundOrderEntity();
        return RefundResEntity.builder()
                .userId(preRefundOrderEntity.getUserId())
                .orderId(preRefundOrderEntity.getOrderId())
                .teamId(preRefundOrderEntity.getTeamId())
                .refundStatusEnum(dynamicContext.getRefundStatusEnum() == null ? RefundStatusEnum.SUCCESS : dynamicContext.getRefundStatusEnum())
                .build();
    }

    @Override
    public StrategyHandler<PreRefundEntity, DefaultRefundStrategyFactory.DynamicContext, RefundResEntity> get(PreRefundEntity requestParameter, DefaultRefundStrategyFactory.DynamicContext dynamicContext) {
        return defaultStrategyHandler;
    }
}
