package groupbuy.market.plus.api.dto;

import lombok.Data;
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
     * 回调配置
     */
    private NotifyConfig notifyConfig;

    @Data
    public static class NotifyConfig {

        /**
         * 回调类型 1 HTTP、2 MQ
         */
        private Integer notifyType;

        /**
         * 回调地址 - HTTP
         */
        private String notifyUrl;

        /**
         * 回调主题 - MQ
         */
        private String notifyMQ;

        /**
         * 退单回调地址 - HTTP
         */
        private String refundNotifyUrl;

        /**
         * 退单回调主题 - MQ
         */
        private String refundNotifyMQ;

        /**
         * 团长退单补偿回调地址 - HTTP
         */
        private String headerRefundNotifyUrl;

        /**
         * 团长退单补偿回调地址 - MQ
         */
        private String headerRefundNotifyMQ;
    }

    // 兼容配置
    public void setHTTPNotify(String notifyUrl, String refundNotifyUrl, String headerRefundNotifyUrl) {
        NotifyConfig notifyConfig = new NotifyConfig();
        notifyConfig.setNotifyType(1);
        notifyConfig.setNotifyUrl(notifyUrl);
        notifyConfig.setRefundNotifyUrl(refundNotifyUrl);
        notifyConfig.setHeaderRefundNotifyUrl(headerRefundNotifyUrl);
        this.notifyConfig = notifyConfig;
    }

    // 兼容配置
    public void setNotifyMQ() {
        NotifyConfig notifyConfig = new NotifyConfig();
        notifyConfig.setNotifyType(2);
        this.notifyConfig = notifyConfig;
    }

}
