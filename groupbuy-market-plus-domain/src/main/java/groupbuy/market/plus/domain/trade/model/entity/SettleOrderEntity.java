package groupbuy.market.plus.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 结算订单实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SettleOrderEntity {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 拼单组队ID
     */
    private String teamId;

    /**
     * 活动ID
     */
    private Long activityId;

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
