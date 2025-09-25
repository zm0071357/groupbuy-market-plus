package groupbuy.market.plus.infrastructure.dao;

import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeam;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupBuyTeamDao {

    /**
     * 新增拼团组队
     * @param groupBuyTeam
     */
    void insert(GroupBuyTeam groupBuyTeam);

    /**
     * 更新锁单量
     * @param teamId 组队ID
     * @return
     */
    Integer updateLockCount(String teamId);

    /**
     * 根据组队ID获取组队进度
     * @param teamId 组队ID
     * @return
     */
    GroupBuyTeam getTeamProgress(String teamId);
}
