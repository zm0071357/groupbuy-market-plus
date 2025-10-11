package groupbuy.market.plus.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 锁单响应体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LockOrderResponseDTO {

    /**
     * 订单ID
     */
    private String orderId;

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
     * 是否为团长 0不是、1是
     */
    private Integer isHeader;

    /**
     * 交易订单状态
     */
    private Integer tradeOrderStatus;
}
