package groupbuy.market.plus.infrastructure.dao;

import groupbuy.market.plus.infrastructure.dao.po.CrowdTagsJob;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CrowdTagsJobDao {

    /**
     * 查询人群标签任务
     * @param crowdTagsJobReq
     * @return
     */
    CrowdTagsJob getCrowdTagsJob(CrowdTagsJob crowdTagsJobReq);

}
