package groupbuy.market.plus.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 回调任务
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotifyTask {

    /**
     * 自增ID
     */
    private Long id;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 拼团组队ID
     */
    private String teamId;

    /**
     * 回调接口
     */
    private String notifyUrl;

    /**
     * 回调次数
     */
    private Integer notifyCount;

    /**
     * 回调状态 0初始、1完成、2重试、3失败
     */
    private Integer notifyStatus;

    /**
     * 参数对象
     */
    private String parameterJson;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
