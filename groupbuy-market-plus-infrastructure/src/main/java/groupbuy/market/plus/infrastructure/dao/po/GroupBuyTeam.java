package groupbuy.market.plus.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 拼团
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyTeam {

    /**
     * 自增ID
     */
    private Long id;

    /**
     * 组队ID
     */
    private String teamId;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 渠道
     */
    private String source;

    /**
     * 来源
     */
    private String channel;

    /**
     * 原始价格
     */
    private BigDecimal originalPrice;

    /**
     * 折扣价格
     */
    private BigDecimal deductionPrice;

    /**
     * 支付价格
     */
    private BigDecimal payPrice;

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
     * 状态（0-拼单中、1-完成、2-失败）
     */
    private Integer status;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 回调地址
     */
    private String notifyUrl;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}

