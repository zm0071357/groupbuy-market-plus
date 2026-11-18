package groupbuy.market.plus.test.unit.mapper;

import groupbuy.market.plus.infrastructure.dao.ActivityAdminDao;
import groupbuy.market.plus.infrastructure.dao.ActivityDao;
import groupbuy.market.plus.infrastructure.dao.po.SCSkuActivity;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertTrue;

/** 验证发布路由保护及下架锁的 SQL 约束。 */
public class ActivityAdminMapperUnitTest {
    @Test
    public void protectsExistingGroupsAndSerializesOpeningAgainstOffline() throws Exception {
        Configuration cfg = MapperTestSupport.parse("mybatis/mapper/activity_admin_mapper.xml",
                "mybatis/mapper/sku_mapper.xml", "mybatis/mapper/activity_mapper.xml");
        String admin = ActivityAdminDao.class.getName() + ".";
        String activity = ActivityDao.class.getName() + ".";
        assertTrue(MapperTestSupport.sql(cfg, admin + "product", Map.of("lock", true)).endsWith("for update"));
        assertTrue(MapperTestSupport.sql(cfg, admin + "replaceInactiveMapping", new SCSkuActivity())
                .contains("t.status = 0 and t.end_time >= now()"));
        assertTrue(MapperTestSupport.sql(cfg, activity + "getManagedOpeningForUpdate", 1L).endsWith("for update"));
        assertTrue(MapperTestSupport.sql(cfg, activity + "stopManagedOpening", 1L).contains("opening_enabled = 1"));
    }
}
