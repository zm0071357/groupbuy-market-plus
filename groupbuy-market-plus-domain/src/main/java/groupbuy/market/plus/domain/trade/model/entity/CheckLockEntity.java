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

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 组队ID
     */
    private String teamId;

    /**
     * 邀请码
     */
    private String inviteId;

    /**
     * 商品ID - 参团时校验与组队商品一致
     */
    private String goodsId;

    /**
     * 来源 - 参团时校验与组队来源一致
     */
    private String source;

    /**
     * 渠道 - 参团时校验与组队渠道一致
     */
    private String channel;

}
