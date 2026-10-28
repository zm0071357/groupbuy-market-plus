package groupbuy.market.plus.domain.trade.service.refund.strategy;

import groupbuy.market.plus.domain.trade.adapter.event.OrderRefundMessage;
import groupbuy.market.plus.domain.trade.model.entity.RefundOrderEntity;
import groupbuy.market.plus.domain.trade.model.valobj.TeamStatusEnum;

public interface RefundService {

    /**
     * 退单
     * @param refundOrderEntity
     */
    void refundOrder(RefundOrderEntity refundOrderEntity);

    /**
     * 新团长回调 - 回调任务已在退单事务内生成，这里仅异步执行回调；失败时由定时补偿任务补发
     * @param teamId 拼团组队ID
     * @param orderId 退单订单ID
     */
    void newHeaderNotify(String teamId, String orderId);

    /**
     * 恢复拼团组队库存
     * @param orderRefundMessage
     */
    void recoverTeamLockStock(OrderRefundMessage orderRefundMessage) throws Exception;

}
