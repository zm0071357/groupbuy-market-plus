package groupbuy.market.plus.infrastructure.dao;

import groupbuy.market.plus.infrastructure.dao.po.CrowdTagsUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrowdTagsUserDao {

    /**
     * 添加人群标签明细
     * @param crowdTagsUserReq
     */
    void addCrowdTagsUser(CrowdTagsUser crowdTagsUserReq);

}
