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
 * 拼团组队完成 - 已支付
 */
@Slf4j
@Service("teamCompletePaidRefund")
public class TeamCompletePaidRefund extends AbstractRefundService {

    @Override
    public void refundOrder(RefundOrderEntity refundOrderEntity) {
        log.info("退单开始，类型；拼团组队完成 - 已支付，用户ID：{}，订单ID：{}", refundOrderEntity.getUserId(), refundOrderEntity.getOrderId());
        // 退单 - 拼团组队状态（失败 / 完成 - 存在退单）由数据库按最新完成量原子判断，避免并发退单时依据旧完成量判断错误
        NotifyTaskEntity notifyTaskEntity = tradeRepository.teamCompletePaidRefund(RefundOrderAggregate.buildTeamCompletePaidRefundAggregate(refundOrderEntity, -1, -1));
        // 发送MQ消息
        sendRefundNotifyMQMessage(notifyTaskEntity, "拼团组队完成 - 已支付");
    }

    @Override
    public void recoverTeamLockStock(OrderRefundMessage orderRefundMessage) {
        log.info("拼团组队已经完成，不需要恢复库存，拼团组队ID：{}", orderRefundMessage.getTeamId());
    }

}
