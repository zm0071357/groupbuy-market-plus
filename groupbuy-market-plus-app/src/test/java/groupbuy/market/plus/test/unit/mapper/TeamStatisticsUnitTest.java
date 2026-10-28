package groupbuy.market.plus.test.unit.mapper;

import groupbuy.market.plus.domain.activity.model.valobj.StatisticsVO;
import groupbuy.market.plus.infrastructure.adapter.repository.ActivityRepositoryImpl;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeam;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamStatistics;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 首页组队统计单元测试 - 单条聚合 SQL，无数据返回 0
 */
public class TeamStatisticsUnitTest {

    @Test
    public void test_sql_singleAggregate() throws Exception {
        Configuration configuration = MapperTestSupport.parse("mybatis/mapper/group_buy_team_mapper.xml");
        GroupBuyTeam param = new GroupBuyTeam();
        param.setActivityId(100123L);
        param.setGoodsId("9890001");
        String sql = MapperTestSupport.sql(configuration, "groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao.getTeamStatistics", param);

        Assert.assertTrue(sql, sql.contains("where activity_id = ? and goods_id = ?"));
        Assert.assertFalse("不应再使用 IN 列表", sql.contains(" in "));
        Assert.assertTrue(sql, sql.contains("coalesce(sum(case when status = 1 then lock_count else 0 end), 0) as allTeamUserCount"));
        // 旧的三条语句已移除
        Assert.assertFalse(configuration.hasStatement("groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao.getTeamIdList"));
    }

    @Test
    public void test_repository_mapStatistics() {
        GroupBuyTeamDao groupBuyTeamDao = mock(GroupBuyTeamDao.class);
        when(groupBuyTeamDao.getTeamStatistics(any(GroupBuyTeam.class))).thenReturn(new GroupBuyTeamStatistics(5, 2, 6));
        ActivityRepositoryImpl activityRepository = new ActivityRepositoryImpl();
        ReflectionTestUtils.setField(activityRepository, "groupBuyTeamDao", groupBuyTeamDao);

        StatisticsVO statisticsVO = activityRepository.getStatistics(100123L, "9890001");

        Assert.assertEquals(Integer.valueOf(5), statisticsVO.getAllTeamCount());
        Assert.assertEquals(Integer.valueOf(2), statisticsVO.getAllTeamCompleteCount());
        Assert.assertEquals(Integer.valueOf(6), statisticsVO.getAllTeamUserCount());
    }

}
