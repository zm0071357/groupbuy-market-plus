package groupbuy.market.plus.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 校验邀请实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckInviteEntity {

    /**
     * 邀请人ID
     */
    private String inviteUserId;

    /**
     * 拼团组队ID
     */
    private String teamId;

}
