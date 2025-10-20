package groupbuy.market.plus.domain.trade.model.entity;

import groupbuy.market.plus.domain.trade.model.valobj.NotifyTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 回调任务实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotifyTaskEntity {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 组队ID
     */
    private String teamId;

    /**
     * 回调类型
     */
    private NotifyTypeEnum notifyTypeEnum;

    /**
     * 回调地址 - HTTP
     */
    private String notifyUrl;

    /**
     * 回调主题 - MQ
     */
    private String notifyMQ;

    /**
     * 回调次数
     */
    private Integer notifyCount;

    /**
     * 参数对象
     */
    private String parameterJson;

    /**
     * 加锁
     * @return
     */
    public String lockKey() {
        return "notify_job_lock_key_" + this.teamId;
    }

}
