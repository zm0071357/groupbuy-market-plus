package groupbuy.market.plus.domain.trade.adapter.event;

import lombok.Data;

/**
 * 订单退单消息
 */
@Data
public class OrderRefundMessage {

    /**
     * 退单类型
     */
    private Integer type;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 拼团组队ID
     */
    private String teamId;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 订单ID
     */
    private String orderId;

}
