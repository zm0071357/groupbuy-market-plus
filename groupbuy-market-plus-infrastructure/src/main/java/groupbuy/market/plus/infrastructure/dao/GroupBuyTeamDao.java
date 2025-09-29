package groupbuy.market.plus.infrastructure.dao;

import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

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

    /**
     * 根据组队ID获取组队
     * @param teamId 组队ID
     * @return
     */
    GroupBuyTeam getTeamById(String teamId);

    /**
     * 更新拼团组队进度 - 完成数量+1
     * @param teamId 组队ID
     * @return
     */
    Integer updateTeamAddCompleteCount(String teamId);

    /**
     * 更新拼团组队状态为完成
     * @param teamId 组队ID
     * @return
     */
    Integer updateTeamStatusComplete(String teamId);

    /**
     * 查询拼团组队ID集合
     * @param groupBuyTeamReq
     * @return
     */
    List<String> getTeamIdList(GroupBuyTeam groupBuyTeamReq);

    /**
     * 查询完成拼团组队数量
     * @param teamIdList 组队ID集合
     * @return
     */
    Integer getAllTeamCompleteCount(List<String> teamIdList);

    /**
     * 查询参与拼团人数
     * @param teamIdList 组队ID集合
     * @return
     */
    Integer getAllTeamUserCount(List<String> teamIdList);

    /**
     * 根据组队ID集合查询用户组队信息
     * @param teamIdList 组队ID集合
     * @param limitCount 数量
     * @return
     */
    List<GroupBuyTeam> getUserTeam(@Param("teamIdList") List<String> teamIdList, @Param("limitCount") Integer limitCount);

    /**
     * 根据组队ID集合查询随机组队信息
     * @param teamIdList 组队ID集合
     * @return
     */
    List<GroupBuyTeam> getRandomTeam(List<String> teamIdList);
}
