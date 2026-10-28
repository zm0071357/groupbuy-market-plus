package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.PreRefundEntity;
import groupbuy.market.plus.domain.trade.model.entity.RefundResEntity;
import groupbuy.market.plus.domain.trade.model.valobj.RefundStatusEnum;
import groupbuy.market.plus.domain.trade.service.refund.RefundOrderServiceImpl;
import groupbuy.market.plus.domain.trade.service.refund.factory.DefaultRefundStrategyFactory;
import groupbuy.market.plus.types.design.framework.tree.StrategyHandler;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import groupbuy.market.plus.types.common.GroupBuyConstants;
import groupbuy.market.plus.domain.trade.service.invite.InviteServiceImpl;
import groupbuy.market.plus.test.unit.mapper.MapperTestSupport;
import org.apache.ibatis.session.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 拼团超时退单单元测试 - 单条退单失败不影响后续订单
 */
@RunWith(MockitoJUnitRunner.class)
public class TeamTimeoutRefundUnitTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private DefaultRefundStrategyFactory defaultRefundStrategyFactory;

    @Mock
    private StrategyHandler<PreRefundEntity, DefaultRefundStrategyFactory.DynamicContext, RefundResEntity> strategyHandler;

    @InjectMocks
    private RefundOrderServiceImpl refundOrderService;

    @Test
    public void test_oneRefundFail_continueOthers() throws Exception {
        PreRefundEntity order1 = buildOrder("OUT0001");
        PreRefundEntity order2 = buildOrder("OUT0002");
        PreRefundEntity order3 = buildOrder("OUT0003");
        List<String> teamIdList = Collections.singletonList("TEAM12345678");
        when(tradeRepository.getTimeoutTeamIdList(null, GroupBuyConstants.TimeoutTeamPageSize)).thenReturn(teamIdList);
        when(tradeRepository.getTimeoutOrderList(teamIdList)).thenReturn(Arrays.asList(order1, order2, order3));
        when(defaultRefundStrategyFactory.strategyHandler()).thenReturn(strategyHandler);
        RefundResEntity success = RefundResEntity.builder().refundStatusEnum(RefundStatusEnum.SUCCESS).build();
        when(strategyHandler.apply(eq(order1), any())).thenReturn(success);
        when(strategyHandler.apply(eq(order2), any())).thenThrow(new AppException(ResponseCodeEnum.UPDATE_ZERO.getCode(), ResponseCodeEnum.UPDATE_ZERO.getInfo()));
        when(strategyHandler.apply(eq(order3), any())).thenReturn(success);

        Map<String, Integer> result = refundOrderService.teamTimeoutRefund();

        // 第 2 条失败后第 3 条仍被处理
        verify(strategyHandler).apply(eq(order3), any());
        Assert.assertEquals(Integer.valueOf(3), result.get("size"));
        Assert.assertEquals(Integer.valueOf(2), result.get("successCount"));
        Assert.assertEquals(Integer.valueOf(1), result.get("failCount"));
    }

    @Test
    public void test_timeoutTeams_pageByTeamIdCursor() throws Exception {
        int pageSize = GroupBuyConstants.TimeoutTeamPageSize;
        // 第一页满页，第二页不足一页 - 共查询两次，第二次以第一页最后一个组队ID为游标
        List<String> page1 = new ArrayList<>();
        for (int i = 0; i < pageSize; i++) {
            page1.add(String.format("TEAM%08d", i));
        }
        List<String> page2 = Collections.singletonList("TEAM99999999");
        when(tradeRepository.getTimeoutTeamIdList(null, pageSize)).thenReturn(page1);
        when(tradeRepository.getTimeoutTeamIdList(page1.get(pageSize - 1), pageSize)).thenReturn(page2);
        PreRefundEntity order1 = buildOrder("OUT0001");
        PreRefundEntity order2 = buildOrder("OUT0002");
        when(tradeRepository.getTimeoutOrderList(page1)).thenReturn(Collections.singletonList(order1));
        when(tradeRepository.getTimeoutOrderList(page2)).thenReturn(Collections.singletonList(order2));
        when(defaultRefundStrategyFactory.strategyHandler()).thenReturn(strategyHandler);
        when(strategyHandler.apply(any(), any())).thenReturn(RefundResEntity.builder().refundStatusEnum(RefundStatusEnum.SUCCESS).build());

        Map<String, Integer> result = refundOrderService.teamTimeoutRefund();

        // 两页的订单均被处理，统计按全部分页累加；不足一页后不再查询
        verify(tradeRepository, times(2)).getTimeoutTeamIdList(any(), eq(pageSize));
        verify(strategyHandler).apply(eq(order1), any());
        verify(strategyHandler).apply(eq(order2), any());
        Assert.assertEquals(Integer.valueOf(2), result.get("size"));
        Assert.assertEquals(Integer.valueOf(2), result.get("successCount"));
    }

    @Test
    public void test_noTimeoutTeam_emptyResult() throws Exception {
        when(tradeRepository.getTimeoutTeamIdList(null, GroupBuyConstants.TimeoutTeamPageSize)).thenReturn(Collections.emptyList());

        Map<String, Integer> result = refundOrderService.teamTimeoutRefund();

        verify(tradeRepository, never()).getTimeoutOrderList(anyList());
        Assert.assertEquals(Integer.valueOf(0), result.get("size"));
    }

    @Test
    public void test_inviteExpire_pageByTeamIdCursor() {
        int pageSize = GroupBuyConstants.TimeoutTeamPageSize;
        List<String> page1 = new ArrayList<>();
        for (int i = 0; i < pageSize; i++) {
            page1.add(String.format("TEAM%08d", i));
        }
        List<String> page2 = Collections.singletonList("TEAM99999999");
        when(tradeRepository.getTimeoutTeamIdList(null, pageSize)).thenReturn(page1);
        when(tradeRepository.getTimeoutTeamIdList(page1.get(pageSize - 1), pageSize)).thenReturn(page2);
        InviteServiceImpl inviteService = new InviteServiceImpl();
        ReflectionTestUtils.setField(inviteService, "tradeRepository", tradeRepository);

        inviteService.inviteExpire();

        verify(tradeRepository).inviteExpire(page1);
        verify(tradeRepository).inviteExpire(page2);
    }

    @Test
    public void test_timeoutTeamSql_cursorAndLimit() throws Exception {
        Configuration configuration = MapperTestSupport.parse("mybatis/mapper/group_buy_team_mapper.xml");
        Map<String, Object> first = new HashMap<>();
        first.put("lastTeamId", null);
        first.put("limit", 200);
        String firstSql = MapperTestSupport.sql(configuration, "groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao.getTimeoutTeamIdList", first);
        Assert.assertTrue(firstSql, firstSql.contains("where now() > end_time and status = 0 order by team_id limit ?"));

        Map<String, Object> next = new HashMap<>();
        next.put("lastTeamId", "TEAM00000199");
        next.put("limit", 200);
        String nextSql = MapperTestSupport.sql(configuration, "groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao.getTimeoutTeamIdList", next);
        Assert.assertTrue(nextSql, nextSql.contains("and team_id > ? order by team_id limit ?"));
    }

    private PreRefundEntity buildOrder(String outTradeNo) {
        return PreRefundEntity.builder()
                .userId("user01")
                .outTradeNo(outTradeNo)
                .source("s01")
                .channel("c01")
                .build();
    }

}
