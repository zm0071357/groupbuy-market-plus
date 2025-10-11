package groupbuy.market.plus.domain.trade.model.entity;

import groupbuy.market.plus.domain.trade.model.valobj.TeamStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 结算校验结果
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckSettleResEntity {

    /**
     * 组队ID
     */
    private String teamId;

    /**
     *  活动ID
     */
    private Long activityId;

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
     * 拼团组队状态（0-拼单中、1-完成、2-失败）
     */
    private TeamStatusEnum status;

    /**
     * 拼团开始时间
     */
    private Date startTime;

    /**
     * 拼团结束时间
     */
    private Date endTime;

    /**
     * 回调地址
     */
    private String notifyUrl;
}
