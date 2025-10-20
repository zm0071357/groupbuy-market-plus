package groupbuy.market.plus.domain.trade.service.refund;

import groupbuy.market.plus.domain.trade.adapter.event.OrderRefundMessage;
import groupbuy.market.plus.domain.trade.model.entity.*;

public interface RefundOrderService {

    /**
     * 退单
     * @param preRefundEntity
     * @return
     */
    RefundResEntity refundOrder(PreRefundEntity preRefundEntity) throws Exception;

    /**
     * 恢复锁单量
     * @param orderRefundMessage
     */
    void recoverTeamLockStock(OrderRefundMessage orderRefundMessage) throws Exception;
}
