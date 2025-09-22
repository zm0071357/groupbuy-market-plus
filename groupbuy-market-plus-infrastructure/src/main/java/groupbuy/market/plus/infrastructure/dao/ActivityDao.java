package groupbuy.market.plus.infrastructure.dao;

import groupbuy.market.plus.infrastructure.dao.po.Activity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ActivityDao {
    Activity getActivityById(Long activityId);
}
