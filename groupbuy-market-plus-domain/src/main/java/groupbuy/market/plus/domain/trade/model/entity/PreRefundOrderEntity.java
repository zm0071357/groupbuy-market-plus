package groupbuy.market.plus.domain.trade.model.entity;

import groupbuy.market.plus.domain.trade.model.valobj.OrderStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 预退单订单实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PreRefundOrderEntity {

    /**
     * 用户ID
     */
    private String userId;

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
     * 订单状态枚举
     */
    private OrderStatusEnum orderStatusEnum;

}
