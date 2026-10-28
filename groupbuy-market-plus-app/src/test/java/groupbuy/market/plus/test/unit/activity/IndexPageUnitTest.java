package groupbuy.market.plus.test.unit.activity;

import groupbuy.market.plus.api.dto.IndexResponseDTO;
import groupbuy.market.plus.domain.activity.adapter.repository.ActivityRepository;
import groupbuy.market.plus.domain.activity.service.IndexGroupBuyMarketServiceImpl;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamOrder;
import groupbuy.market.plus.test.unit.mapper.MapperTestSupport;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 首页展示单元测试 - 随机组队去重排除、统计口径、倒计时、个人名额补位
 */
public class IndexPageUnitTest {

    private static final String NAMESPACE_ORDER = "groupbuy.market.plus.infrastructure.dao.GroupBuyTeamOrderDao.";
    private static final String NAMESPACE_TEAM = "groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao.";

    @Test
    public void test_randomTeamSql() throws Exception {
        Configuration configuration = MapperTestSupport.parse("mybatis/mapper/group_buy_team_order_mapper.xml", "mybatis/mapper/group_buy_team_mapper.xml");
        Map<String, Object> param = new HashMap<>();
        param.put("groupBuyTeamOrderReq", new GroupBuyTeamOrder());
        param.put("limitCount", 4);
        String idSql = MapperTestSupport.sql(configuration, NAMESPACE_ORDER + "getRandomTeamIdList", param);
        Assert.assertTrue("应排除用户已参与的组队：" + idSql, idSql.contains("and team_id not in ( select team_id from group_buy_team_order where activity_id = ? and user_id = ? and status in (0, 1) )"));
        Assert.assertTrue("应按组队去重：" + idSql, idSql.contains("group by team_id order by max(id) desc"));

        Map<String, Object> teamParam = new HashMap<>();
        teamParam.put("teamIdList", Collections.singletonList("TEAM12345678"));
        String teamSql = MapperTestSupport.sql(configuration, NAMESPACE_TEAM + "getRandomTeam", teamParam);
        Assert.assertTrue("应过滤满员与过期组队：" + teamSql, teamSql.contains("where status = 0 and lock_count < target_count and end_time > now()"));
    }

    @Test
    public void test_statistics_includeCompleteRefund() throws Exception {
        Configuration configuration = MapperTestSupport.parse("mybatis/mapper/group_buy_team_mapper.xml");
        String sql = MapperTestSupport.sql(configuration, NAMESPACE_TEAM + "getTeamStatistics", new groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeam());

        Assert.assertTrue(sql, sql.contains("count(case when status in (1, 3) then 1 end) as allTeamCompleteCount"));
    }

    @Test
    public void test_countdown_remaining() {
        long now = System.currentTimeMillis();
        Date start = new Date(now - 3600_000L);
        // 剩余约 1 小时 30 分（与开始时间无关）
        String countdown = IndexResponseDTO.Team.getValidTimeCountdown(start, new Date(now + 90 * 60_000L + 500));
        Assert.assertTrue(countdown, countdown.startsWith("01:29:") || countdown.startsWith("01:30:"));
        // 超过一天显示总小时数
        Assert.assertTrue(IndexResponseDTO.Team.getValidTimeCountdown(start, new Date(now + 30 * 3600_000L + 500)).startsWith("30:"));
        Assert.assertEquals("已结束", IndexResponseDTO.Team.getValidTimeCountdown(start, new Date(now - 1000)));
    }

    @Test
    public void test_noUserTeam_randomCountIncrease() {
        ActivityRepository activityRepository = mock(ActivityRepository.class);
        // 用户没有组队时仓储返回 null
        when(activityRepository.getUserTeam(anyLong(), anyString(), anyString(), anyString(), anyString(), anyInt())).thenReturn(null);
        IndexGroupBuyMarketServiceImpl service = new IndexGroupBuyMarketServiceImpl();
        ReflectionTestUtils.setField(service, "activityRepository", activityRepository);

        service.getTeamList(100123L, "user01", "9890001", "s01", "c01", 1, 2);

        // 个人名额 1 补给随机组队：2 + 1 = 3
        verify(activityRepository).getRandomTeam(100123L, "user01", "9890001", "s01", "c01", 3);
    }

}
