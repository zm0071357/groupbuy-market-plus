package groupbuy.market.plus.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 锁单校验结果
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckLockResEntity {

    /**
     * 是否为团长
     */
    private Boolean isHeader;

    /** 参团时已校验的组队，开团时为空。 */
    private GroupBuyTeamEntity joinedTeam;

    /**
     * 恢复Key - 用于锁单失败恢复可用位置
     */
    private String teamStockRecoverKey;

    /**
     * 邀请人ID
     */
    private String inviteUserId;

}
