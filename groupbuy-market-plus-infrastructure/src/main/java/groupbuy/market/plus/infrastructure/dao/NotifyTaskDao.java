package groupbuy.market.plus.infrastructure.dao;

import groupbuy.market.plus.infrastructure.dao.po.NotifyTask;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NotifyTaskDao {

    /**
     * 新增拼团回调任务
     * @param notifyTask
     */
    void insert(NotifyTask notifyTask);

    /**
     * 获取未完成的拼团完成回调通知任务集合
     * @return
     */
    List<NotifyTask> getUnNotifyTeamSuccessTaskList();

    /**
     * 更新回调任务状态为完成
     * @param taskId 回调任务ID
     * @return
     */
    int updateNotifyTaskSuccess(String taskId);

    /**
     * 更新回调任务状态为重试
     * @param taskId 回调任务ID
     * @return
     */
    int updateNotifyTaskRetry(String taskId);

    /**
     * 更新回调任务状态为失败
     * @param taskId 回调任务ID
     * @return
     */
    int updateNotifyTaskFail(String taskId);

    /**
     * 查询回调任务状态
     * @param taskId 回调任务ID
     * @return 回调状态，任务不存在时返回null
     */
    Integer getNotifyStatusByTaskId(String taskId);

    /**
     * 获取未完成的退单回调通知任务集合
     * @return
     */
    List<NotifyTask> getUnNotifyOrderRefundTaskList();

    /**
     * 获取未完成的团长退单补偿回调通知任务集合
     * @return
     */
    List<NotifyTask> getUnNotifyHeaderRefundTaskList();
}
