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
     * 获取未执行回调任务集合
     * @return
     */
    List<NotifyTask> getUnNotifyTaskList();

    /**
     * 根据组队ID获取未执行回调任务
     * @param teamId 组队ID
     * @return
     */
    NotifyTask getUnNotifyTaskByTeamId(String teamId);

    /**
     * 更新回调任务状态为完成
     * @param teamId 组队ID
     * @return
     */
    int updateNotifyTaskSuccess(String teamId);

    /**
     * 更新回调任务状态为重试
     * @param teamId 组队ID
     * @return
     */
    int updateNotifyTaskRetry(String teamId);

    /**
     * 更新回调任务状态为失败
     * @param teamId 组队ID
     * @return
     */
    int updateNotifyTaskFail(String teamId);
}
