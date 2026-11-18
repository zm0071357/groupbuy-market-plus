package groupbuy.market.plus.test.unit.mapper;

import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao;
import groupbuy.market.plus.infrastructure.dao.SCSkuActivityDao;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeam;
import groupbuy.market.plus.infrastructure.dao.po.SCSkuActivity;
import org.apache.ibatis.session.Configuration;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/** 验证持久化映射读写档位，并继续按组队目标人数更新锁单量和成团状态。 */
public class TieredGroupBuyMapperUnitTest {

    @Test
    public void persistsTierAndUsesTeamTargetForStockAndSettlement() throws Exception {
        Configuration configuration = MapperTestSupport.parse("mybatis/mapper/sc_sku_activity_mapper.xml",
                "mybatis/mapper/group_buy_team_mapper.xml");
        String activityMapper = SCSkuActivityDao.class.getName() + ".";
        String teamMapper = GroupBuyTeamDao.class.getName() + ".";

        String findActivity = MapperTestSupport.sql(configuration, activityMapper + "getSCSkuActivityByIdWithSC",
                new SCSkuActivity());
        assertTrue(findActivity.contains("tiers_json"));
        String findTeam = MapperTestSupport.sql(configuration, teamMapper + "getTeamById", "TEAM001");
        assertTrue(findTeam.contains("tier_id, source, channel, original_price, pay_price, target_count"));
        assertTrue(configuration.getMappedStatement(teamMapper + "insert").getBoundSql(new GroupBuyTeam())
                .getParameterMappings().stream().anyMatch(mapping -> "tierId".equals(mapping.getProperty())));

        assertTrue(MapperTestSupport.sql(configuration, teamMapper + "updateLockCount", "TEAM001")
                .contains("lock_count < target_count"));
        assertTrue(MapperTestSupport.sql(configuration, teamMapper + "updateTeamStatusComplete", "TEAM001")
                .contains("complete_count = target_count"));
    }
}
