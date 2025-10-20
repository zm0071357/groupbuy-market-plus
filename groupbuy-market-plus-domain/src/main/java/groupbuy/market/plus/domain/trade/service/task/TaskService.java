package groupbuy.market.plus.domain.trade.service.task;

import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;

import java.util.Map;

public interface TaskService {

    /**
     * 执行定时任务 - 回调通知拼团完成
     * @return
     */
    Map<String, Integer> execNotifyJob() throws Exception;

    /**
     * 指定组队ID执行回调通知拼团完成
     * @param teamId 组队ID
     * @return
     */
    Map<String, Integer> execNotifyJob(String teamId) throws Exception;

    /**
     * 指定回调任务执行回调通知拼团完成
     * @param notifyTaskEntity
     * @return
     */
    Map<String, Integer> execNotifyJob(NotifyTaskEntity notifyTaskEntity) throws Exception;
}
