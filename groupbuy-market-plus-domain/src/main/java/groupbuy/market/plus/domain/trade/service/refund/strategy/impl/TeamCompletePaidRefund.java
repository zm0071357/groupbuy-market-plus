package groupbuy.market.plus.domain.trade.service.refund.strategy.impl;

import groupbuy.market.plus.domain.trade.adapter.event.OrderRefundMessage;
import groupbuy.market.plus.domain.trade.model.aggregate.RefundOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;
import groupbuy.market.plus.domain.trade.model.entity.RefundOrderEntity;
import groupbuy.market.plus.domain.trade.model.valobj.TeamStatusEnum;
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
        // 判断拼团状态
        GroupBuyTeamEntity groupBuyTeamEntity = tradeRepository.getTeamById(refundOrderEntity.getTeamId());
        Integer completeCount = groupBuyTeamEntity.getCompleteCount();
        TeamStatusEnum teamStatusEnum = completeCount == 1 ? TeamStatusEnum.FAIL : TeamStatusEnum.COMPLETE_REFUND;
        // 退单
        NotifyTaskEntity notifyTaskEntity = tradeRepository.teamCompletePaidRefund(RefundOrderAggregate.buildTeamCompletePaidRefundAggregate(refundOrderEntity, -1, -1, teamStatusEnum));
        // 发送MQ消息
        sendRefundNotifyMQMessage(notifyTaskEntity, "拼团组队完成 - 已支付");
    }

    @Override
    public void recoverTeamLockStock(OrderRefundMessage orderRefundMessage) {
        log.info("拼团组队已经完成，不需要恢复库存，拼团组队ID：{}", orderRefundMessage.getTeamId());
    }

}
