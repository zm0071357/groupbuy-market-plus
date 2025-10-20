package groupbuy.market.plus.domain.trade.service.refund.strategy.impl;

import groupbuy.market.plus.domain.trade.adapter.event.OrderRefundMessage;
import groupbuy.market.plus.domain.trade.model.aggregate.RefundOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;
import groupbuy.market.plus.domain.trade.model.entity.RefundOrderEntity;
import groupbuy.market.plus.domain.trade.service.refund.strategy.AbstractRefundService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 退单
 * 拼团组队未完成 - 已支付
 */
@Slf4j
@Service("teamInCompletePaidRefund")
public class TeamInCompletePaidRefund extends AbstractRefundService {

    @Override
    public void refundOrder(RefundOrderEntity refundOrderEntity) {
        log.info("退单开始，类型；拼团组队未完成 - 已支付，用户ID：{}，订单ID：{}", refundOrderEntity.getUserId(), refundOrderEntity.getOrderId());
        // 退单
        NotifyTaskEntity notifyTaskEntity = tradeRepository.teamInCompletePaidRefund(RefundOrderAggregate.buildTeamInCompletePaidRefundAggregate(refundOrderEntity, -1, -1));
        // 发送MQ消息
        sendRefundNotifyMQMessage(notifyTaskEntity, "拼团组队未完成 - 已支付");
    }

    @Override
    public void recoverTeamLockStock(OrderRefundMessage orderRefundMessage) throws Exception {
        doReverseStock(orderRefundMessage, "拼团组队未完成 - 已支付");
    }

}
