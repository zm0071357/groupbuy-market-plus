package groupbuy.market.plus.domain.trade.service.refund.strategy;

import groupbuy.market.plus.domain.trade.adapter.event.OrderRefundMessage;
import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.model.entity.RefundOrderEntity;
import groupbuy.market.plus.domain.trade.model.valobj.TeamStatusEnum;

public interface RefundService {

    /**
     * 退单
     * @param refundOrderEntity
     */
    void refundOrder(RefundOrderEntity refundOrderEntity);

    /**
     * 新团长回调
     * @param newLeaderUserId 新团长ID
     * @param orderId 订单ID
     * @param groupBuyTeamEntity 拼团组队信息
     */
    void newHeaderNotify(String newLeaderUserId, String orderId, GroupBuyTeamEntity groupBuyTeamEntity);

    /**
     * 恢复拼团组队库存
     * @param orderRefundMessage
     */
    void recoverTeamLockStock(OrderRefundMessage orderRefundMessage) throws Exception;

}
