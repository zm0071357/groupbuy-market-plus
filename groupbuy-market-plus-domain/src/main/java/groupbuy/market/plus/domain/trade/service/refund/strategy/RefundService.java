package groupbuy.market.plus.domain.trade.service.refund.strategy;

import groupbuy.market.plus.domain.trade.adapter.event.OrderRefundMessage;
import groupbuy.market.plus.domain.trade.model.entity.RefundOrderEntity;

public interface RefundService {

    /**
     * 退单
     * @param refundOrderEntity
     */
    void refundOrder(RefundOrderEntity refundOrderEntity);

    /**
     * 恢复拼团组队库存
     * @param orderRefundMessage
     */
    void recoverTeamLockStock(OrderRefundMessage orderRefundMessage) throws Exception;

}
