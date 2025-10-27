package groupbuy.market.plus.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 退单回调实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundNotifyEntity {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 支付订单ID - 外部交易单号
     */
    private String outTradeNo;

    /**
     * 退单订单ID - 退款外部交易单号
     */
    private String outRefundNo;

    /**
     * 退款完成时间
     */
    private Date outRefundNoCompleteTime;

}
