package groupbuy.market.plus.api.dto;

import lombok.Getter;

/**
 * 退单请求体
 */
@Getter
public class RefundOrderRequestDTO {

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

}
