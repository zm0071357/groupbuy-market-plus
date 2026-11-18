package groupbuy.market.plus.domain.trade.model.valobj;

import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;

/** 退单事务提交后可立即发送的两类通知，其中团长变更通知可以为空。 */
public record RefundNotifyTasks(NotifyTaskEntity refund, NotifyTaskEntity headerChanged) {
}
