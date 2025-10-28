package groupbuy.market.plus.domain.trade.service.invite;

import groupbuy.market.plus.domain.trade.model.entity.InviteEntity;

public interface InviteService {

    /**
     * 生成唯一邀请码
     * @param userId 邀请人ID
     * @param teamId 拼团组队ID
     * @return
     */
    InviteEntity invite(String userId, String teamId) throws Exception;

    /**
     * 邀请返利失效
     */
    void inviteExpire();
}
