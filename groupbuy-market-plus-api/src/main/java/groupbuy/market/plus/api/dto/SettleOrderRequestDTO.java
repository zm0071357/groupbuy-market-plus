package groupbuy.market.plus.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * 结算请求体
 */
@Getter
@Setter
public class SettleOrderRequestDTO {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 外部交易单号
     */
    private String outTradeNo;

    /**
     * 外部交易单号支付完成时间
     */
    private Date outTradeNoPayTime;

    /**
     * 来源
     */
    private String source;

    /**
     * 渠道
     */
    private String channel;


}
