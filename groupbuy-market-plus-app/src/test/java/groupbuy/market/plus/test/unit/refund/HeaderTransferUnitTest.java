package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.domain.trade.model.aggregate.RefundOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.RefundOrderEntity;
import groupbuy.market.plus.domain.trade.model.valobj.TeamStatusEnum;
import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamOrderDao;
import groupbuy.market.plus.infrastructure.dao.NotifyTaskDao;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeam;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamOrder;
import groupbuy.market.plus.infrastructure.dao.po.NotifyTask;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyTypeEnum;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyTaskTypeEnum;
import org.mockito.InOrder;
import groupbuy.market.plus.test.unit.mapper.MapperTestSupport;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 团长退单变更新团长单元测试 - 仅更新本组队订单，且与退单在同一事务内完成
 */
public class HeaderTransferUnitTest {

    private static final String TEAM_ID = "TEAM12345678";

    private static final String ORDER_ID = "ORDER000000000001";

    private NotifyTaskDao notifyTaskDao;
    private GroupBuyTeamOrderDao groupBuyTeamOrderDao;
    private GroupBuyTeamDao groupBuyTeamDao;
    private TradeRepositoryImpl tradeRepository;

    @Before
    public void setUp() {
        groupBuyTeamOrderDao = mock(GroupBuyTeamOrderDao.class);
        groupBuyTeamDao = mock(GroupBuyTeamDao.class);
        tradeRepository = new TradeRepositoryImpl();
        ReflectionTestUtils.setField(tradeRepository, "groupBuyTeamOrderDao", groupBuyTeamOrderDao);
        ReflectionTestUtils.setField(tradeRepository, "groupBuyTeamDao", groupBuyTeamDao);
        notifyTaskDao = mock(NotifyTaskDao.class);
        ReflectionTestUtils.setField(tradeRepository, "notifyTaskDao", notifyTaskDao);
        when(groupBuyTeamDao.getTeamByIdForUpdate(TEAM_ID)).thenReturn(GroupBuyTeam.builder()
                .teamId(TEAM_ID).activityId(100123L).status(TeamStatusEnum.PROGRESS.getStatus())
                .notifyType(NotifyTypeEnum.MQ.getType()).build());
        when(groupBuyTeamOrderDao.updateOrderStatusRefund(any(GroupBuyTeamOrder.class))).thenReturn(1);
        when(groupBuyTeamDao.updateTeamByTeamInCompleteUnPaidRefund(any(GroupBuyTeam.class))).thenReturn(1);
        when(groupBuyTeamDao.updateTeamByTeamInCompletePaidRefund(any(GroupBuyTeam.class))).thenReturn(1);
    }

    @Test
    public void test_sql_onlySameTeam() throws Exception {
        Configuration configuration = MapperTestSupport.parse("mybatis/mapper/group_buy_team_order_mapper.xml");
        Map<String, Object> param = new HashMap<>();
        param.put("teamId", TEAM_ID);
        param.put("userId", "user02");
        String sql = MapperTestSupport.sql(configuration, "groupbuy.market.plus.infrastructure.dao.GroupBuyTeamOrderDao.updateUserIsHeader", param);

        Assert.assertTrue(sql, sql.contains("where team_id = ? and user_id = ? and status in (0, 1) and is_header = 0"));
    }

    @Test
    public void transfersCurrentHeaderAndPersistsNotificationInLockedRefund() throws Exception {
        when(groupBuyTeamOrderDao.getOrderHeaderFlag(TEAM_ID, ORDER_ID)).thenReturn(1);
        when(groupBuyTeamOrderDao.getNewHeaderUserId(TEAM_ID)).thenReturn("user02");
        when(groupBuyTeamOrderDao.updateUserIsHeader(TEAM_ID, "user02")).thenReturn(1);

        tradeRepository.teamInCompleteUnPaidRefund(RefundOrderAggregate.buildTeamInCompleteUnPaidRefundAggregate(buildRefundOrder(false), -1));

        InOrder writes = inOrder(groupBuyTeamDao, groupBuyTeamOrderDao, notifyTaskDao);
        writes.verify(groupBuyTeamDao).getTeamByIdForUpdate(TEAM_ID);
        writes.verify(groupBuyTeamOrderDao).updateOrderStatusRefund(any());
        writes.verify(groupBuyTeamOrderDao).getOrderHeaderFlag(TEAM_ID, ORDER_ID);
        writes.verify(groupBuyTeamOrderDao).updateUserIsHeader(TEAM_ID, "user02");
        writes.verify(notifyTaskDao).insert(argThat(task -> hasType(task, NotifyTaskTypeEnum.NEW_HEADER)));
        writes.verify(notifyTaskDao).insert(argThat(task -> hasType(task, NotifyTaskTypeEnum.TEAM_INCOMPLETE_UNPAID_REFUND)));
        Method method = TradeRepositoryImpl.class.getMethod("teamInCompleteUnPaidRefund", RefundOrderAggregate.class);
        Assert.assertNotNull(method.getAnnotation(org.springframework.transaction.annotation.Transactional.class));
    }

    @Test
    public void failedHeaderNotificationInsertAbortsRefund() {
        when(groupBuyTeamOrderDao.getOrderHeaderFlag(TEAM_ID, ORDER_ID)).thenReturn(1);
        when(groupBuyTeamOrderDao.getNewHeaderUserId(TEAM_ID)).thenReturn("user02");
        when(groupBuyTeamOrderDao.updateUserIsHeader(TEAM_ID, "user02")).thenReturn(1);
        doThrow(new IllegalStateException("notification insert failed")).when(notifyTaskDao)
                .insert(argThat(task -> hasType(task, NotifyTaskTypeEnum.NEW_HEADER)));

        Assert.assertThrows(IllegalStateException.class, () -> tradeRepository.teamInCompletePaidRefund(
                RefundOrderAggregate.buildTeamInCompletePaidRefundAggregate(buildRefundOrder(false), -1, -1)));
        verify(notifyTaskDao, never()).insert(argThat(task -> hasType(task, NotifyTaskTypeEnum.TEAM_INCOMPLETE_PAID_REFUND)));
    }

    @Test
    public void timeoutRefundDoesNotTransferHeader() {
        tradeRepository.teamInCompleteUnPaidRefund(RefundOrderAggregate.buildTeamInCompleteUnPaidRefundAggregate(buildRefundOrder(true), -1));

        verify(groupBuyTeamOrderDao, never()).getOrderHeaderFlag(anyString(), anyString());
        verify(notifyTaskDao, never()).insert(argThat(task -> hasType(task, NotifyTaskTypeEnum.NEW_HEADER)));
    }

    @Test
    public void keepsOptionalHeaderNotificationForHttpMerchant() {
        when(groupBuyTeamDao.getTeamByIdForUpdate(TEAM_ID)).thenReturn(GroupBuyTeam.builder()
                .teamId(TEAM_ID).notifyType(NotifyTypeEnum.HTTP.getType()).build());
        when(groupBuyTeamOrderDao.getOrderHeaderFlag(TEAM_ID, ORDER_ID)).thenReturn(1);
        when(groupBuyTeamOrderDao.getNewHeaderUserId(TEAM_ID)).thenReturn("user02");
        when(groupBuyTeamOrderDao.updateUserIsHeader(TEAM_ID, "user02")).thenReturn(1);

        tradeRepository.teamInCompleteUnPaidRefund(RefundOrderAggregate.buildTeamInCompleteUnPaidRefundAggregate(buildRefundOrder(false), -1));

        verify(groupBuyTeamOrderDao).updateUserIsHeader(TEAM_ID, "user02");
        verify(notifyTaskDao, never()).insert(argThat(task -> hasType(task, NotifyTaskTypeEnum.NEW_HEADER)));
    }

    private boolean hasType(NotifyTask task, NotifyTaskTypeEnum type) {
        return task != null && type.getType().equals(task.getTaskType());
    }

    private RefundOrderEntity buildRefundOrder(boolean timeout) {
        return RefundOrderEntity.builder().userId("user01").orderId(ORDER_ID).teamId(TEAM_ID)
                .activityId(100123L).outTradeNo("OUT0001").timeoutRefund(timeout).build();
    }
}
