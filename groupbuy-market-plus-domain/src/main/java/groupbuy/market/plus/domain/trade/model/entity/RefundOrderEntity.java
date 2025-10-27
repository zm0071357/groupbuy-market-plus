package groupbuy.market.plus.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退单实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundOrderEntity {

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

    /**
     * 外部交易单号
     */
    private String outTradeNo;

}
