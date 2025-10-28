package groupbuy.market.plus.infrastructure.dao;

import groupbuy.market.plus.infrastructure.dao.po.Invite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InviteDao {

    /**
     * 获取邀请
     * @param inviteReq
     * @return
     */
    Invite getInviteByUserIdWithTeamId(Invite inviteReq);

    /**
     * 新增邀请
     * @param invite
     */
    void insert(Invite invite);

    /**
     * 根据邀请码获取邀请返利
     * @param inviteId 邀请码
     * @return
     */
    Invite getInviteByInviteId(@Param("inviteId") String inviteId);

    /**
     * 拼团返利失效
     * @param timeoutTeamIdList 超时的拼团ID集合
     */
    void inviteExpire(@Param("timeoutTeamIdList") List<String> timeoutTeamIdList);
}
