package groupbuy.market.plus.domain.trade.adapter.event;

import lombok.Data;

import java.util.Date;

/**
 * 订单退款成功消息
 */
@Data
public class OrderRefundSuccessMessage {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 支付订单ID
     */
    private String payOrderId;

    /**
     * 退单订单ID
     */
    private String refundOrderId;

    /**
     * 订单退款完成时间
     */
    private Date refundTime;
}
