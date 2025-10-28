package groupbuy.market.plus.domain.trade.model.entity;

import groupbuy.market.plus.domain.trade.model.valobj.InviteStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 邀请返利实体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InviteEntity {

    /**
     * 邀请人ID
     */
    private String inviteUserId;

    /**
     * 拼团组队ID
     */
    private String teamId;

    /**
     * 邀请码
     */
    private String inviteId;

    /**
     * 邀请码开始时间
     */
    private Date startTime;

    /**
     * 邀请码失效时间
     */
    private Date endTime;

    /**
     * 邀请码状态枚举
     */
    private InviteStatusEnum inviteStatusEnum;

}
