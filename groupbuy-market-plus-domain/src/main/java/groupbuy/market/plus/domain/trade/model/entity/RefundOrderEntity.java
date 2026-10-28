package groupbuy.market.plus.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退单实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundOrderEntity {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 拼团组队ID
     */
    private String teamId;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 外部交易单号
     */
    private String outTradeNo;

    /**
     * 新团长用户ID - 拼团中的团长退单时由退单成员节点选出，为空表示无需变更团长
     */
    private String newHeaderUserId;

    /**
     * 拼团组队 - 团长退单变更新团长时，用于在退单事务内生成新团长回调任务（回调方式、团长退单补偿回调配置、组队状态）
     */
    private GroupBuyTeamEntity groupBuyTeamEntity;

}
