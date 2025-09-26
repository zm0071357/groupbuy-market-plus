package groupbuy.market.plus.domain.trade.model.entity;

import groupbuy.market.plus.domain.trade.model.valobj.OrderStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

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
     * 外部交易单号-确保外部调用唯一幂等
     */
    private String outTradeNo;

    /**
     * 外部交易单号支付完成时间
     */
    private Date outTradeNoPayTime;

    /**
     * 订单状态枚举
     */
    private OrderStatusEnum orderStatusEnum;

}
