package groupbuy.market.plus.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 拼团组队实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyTeamEntity {

    /**
     * 组队ID
     */
    private String teamId;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 拼团开始时间
     */
    private Date startTime;

    /**
     * 拼团结束时间
     */
    private Date endTime;

    /**
     * 拼团时长
     */
    private Integer validTime;

    /**
     * 目标数量
     */
    private Integer targetCount;

    /**
     * 完成数量
     */
    private Integer completeCount;

    /**
     * 锁单数量
     */
    private Integer lockCount;

    /**
     * 订单状态
     */
    private Integer status;

    /**
     * 回调地址
     */
    private String notifyUrl;

}
