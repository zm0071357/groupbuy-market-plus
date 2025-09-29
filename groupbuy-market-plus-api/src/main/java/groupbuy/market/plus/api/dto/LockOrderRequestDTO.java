package groupbuy.market.plus.api.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 锁单请求体
 */
@Getter
@Setter
public class LockOrderRequestDTO {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 组队ID
     */
    private String teamId;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 商品ID
     */
    private String goodsId;

    /**
     * 来源
     */
    private String source;

    /**
     * 渠道
     */
    private String channel;

    /**
     * 外部交易单号 - 后续对接蓝兔支付，由蓝兔支付生成的订单号
     */
    private String outTradeNo;

    /**
     * 回调地址
     */
    private String notifyUrl;

}

