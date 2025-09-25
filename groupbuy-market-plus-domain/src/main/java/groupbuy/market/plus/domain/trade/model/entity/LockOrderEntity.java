package groupbuy.market.plus.domain.trade.model.entity;

import groupbuy.market.plus.domain.trade.model.valobj.OrderStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 锁单订单实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LockOrderEntity {

    /**
     * 组队ID
     */
    private String teamId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 是否为团长
     */
    private Boolean isHeader;

    /**
     * 支付价格
     */
    private BigDecimal payPrice;

    /**
     * 订单状态枚举
     */
    private OrderStatusEnum orderStatusEnum;

}
