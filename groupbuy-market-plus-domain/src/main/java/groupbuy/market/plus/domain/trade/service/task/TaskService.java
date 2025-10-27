package groupbuy.market.plus.domain.trade.service.task;

import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;

import java.util.Map;

public interface TaskService {

    /**
     * 执行定时任务 - 拼团完成回调通知
     * @return
     */
    Map<String, Integer> execTeamSuccessNotifyJob() throws Exception;

    /**
     * 执行定时任务 - 退单回调通知
     * @return
     */
    Map<String, Integer> execOrderRefundNotifyJob() throws Exception;

    /**
     * 执行定时任务 - 团长退单补偿通知
     * @return
     */
    Map<String, Integer> execHeaderRefundNotifyJob() throws Exception;

    /**
     * 指定回调任务执行回调通知
     * @param notifyTaskEntity
     * @return
     */
    Map<String, Integer> execNotifyJob(NotifyTaskEntity notifyTaskEntity) throws Exception;
}
