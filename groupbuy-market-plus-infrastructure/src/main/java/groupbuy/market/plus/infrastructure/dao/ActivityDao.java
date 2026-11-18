package groupbuy.market.plus.infrastructure.dao;

import groupbuy.market.plus.infrastructure.dao.po.Activity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ActivityDao {
    Activity getActivityById(Long activityId);

    /** 命中运营活动时锁行，与下架串行，重新确认开团时间和开关。 */
    Activity getManagedOpeningForUpdate(Long activityId);

    /** 创建已发布的档位活动。 */
    void insertManaged(Activity activity);

    /** 禁止已发布活动继续开团。 */
    int stopManagedOpening(Long activityId);
}
