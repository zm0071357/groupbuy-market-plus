package groupbuy.market.plus.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 回调设置值对象
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotifyConfigVO {

    /**
     * 回调类型枚举
     */
    private NotifyTypeEnum notifyTypeEnum;

    /**
     * 成团回调地址 - HTTP
     */
    private String notifyUrl;

    /**
     * 成团回调主题 - MQ
     */
    private String notifyMQ;

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
