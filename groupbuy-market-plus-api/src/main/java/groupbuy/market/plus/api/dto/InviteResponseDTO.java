package groupbuy.market.plus.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 生成邀请码响应体
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InviteResponseDTO {

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
}
