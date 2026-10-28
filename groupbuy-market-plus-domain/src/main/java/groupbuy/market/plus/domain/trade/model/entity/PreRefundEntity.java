package groupbuy.market.plus.domain.trade.model.entity;

import groupbuy.market.plus.domain.trade.model.valobj.RefundTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退单校验实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PreRefundEntity {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 外部交易单号
     */
    private String outTradeNo;

    /**
     * 来源
     */
    private String source;

    /**
     * 渠道
     */
    private String channel;

    /**
     * 是否超时退单 - 超时组队的订单将全部退单，团长退单时无需选出新团长
     */
    private boolean timeoutRefund;

}
