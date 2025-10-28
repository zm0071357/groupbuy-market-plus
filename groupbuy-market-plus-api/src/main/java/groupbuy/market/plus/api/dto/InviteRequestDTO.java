package groupbuy.market.plus.api.dto;

import lombok.Getter;

/**
 * 生成邀请码请求体
 */
@Getter
public class InviteRequestDTO {

    /**
     * 邀请人ID
     */
    private String userId;

    /**
     * 拼团组队ID
     */
    private String teamId;

}
