package groupbuy.market.plus.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 邀请
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Invite {

    /**
     * 自增ID
     */
    private Long id;

    /**
     * 邀请人ID
     */
    private String inviteUserId;

    /**
     * 拼团组队ID
     */
    private String teamId;

    /**
     * 唯一邀请码
     */
    private String inviteId;

    /**
     * 邀请成功人数
     */
    private Integer inviteSuccessCount;

    /**
     * 邀请码开始时间
     */
    private Date startTime;

    /**
     * 邀请码失效时间
     */
    private Date endTime;

    /**
     * 邀请码状态 - 0 可用、1 不可用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

}
