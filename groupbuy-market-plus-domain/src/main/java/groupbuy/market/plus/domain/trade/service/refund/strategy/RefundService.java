package groupbuy.market.plus.domain.trade.service.refund.strategy;

import groupbuy.market.plus.domain.trade.model.entity.RefundOrderEntity;

public interface RefundService {

    /**
     * 退单
     * @param refundOrderEntity
     */
    void refundOrder(RefundOrderEntity refundOrderEntity);

    /**
     * 新团长回调
     * @param newLeaderUserId 新团长用户ID
     */
    void newHeaderNotify(String newLeaderUserId);
}
