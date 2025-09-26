package groupbuy.market.plus.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 结算校验实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckSettleEntity {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 来源
     */
    private String resource;

    /**
     * 渠道
     */
    private String channel;

    /**
     * 外部交易单号
     */
    private String outTradeNo;

    /**
     * 外部交易单号支付完成时间
     */
    private Date outTradeNoPayTime;

}
