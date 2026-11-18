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

}
