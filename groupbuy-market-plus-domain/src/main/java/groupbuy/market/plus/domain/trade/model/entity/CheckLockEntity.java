package groupbuy.market.plus.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 锁单校验实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckLockEntity {

    /**
     * 活动ID
     */
    private Long activityId;

    /** 本次锁单商品 ID */
    private String goodsId;

    /** 档位适用的来源与渠道。 */
    private String source;
    private String channel;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 组队ID
     */
    private String teamId;

    /** 请求选择的拼团档位，参团时可省略。 */
    private Long tierId;

    /**
     * 邀请码
     */
    private String inviteId;

}
