package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.aggregate.RefundOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.model.entity.PreRefundEntity;
import groupbuy.market.plus.domain.trade.model.entity.PreRefundOrderEntity;
import groupbuy.market.plus.domain.trade.model.entity.RefundOrderEntity;
import groupbuy.market.plus.domain.trade.model.valobj.OrderStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.TeamStatusEnum;
import groupbuy.market.plus.domain.trade.service.refund.factory.DefaultRefundStrategyFactory;
import groupbuy.market.plus.domain.trade.service.refund.node.RefundUserNode;
import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamOrderDao;
import groupbuy.market.plus.infrastructure.dao.NotifyTaskDao;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeam;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamOrder;
import groupbuy.market.plus.test.unit.mapper.MapperTestSupport;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import org.apache.ibatis.session.Configuration;
import groupbuy.market.plus.infrastructure.dao.po.NotifyTask;
import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyConfigVO;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyTypeEnum;
import org.mockito.ArgumentCaptor;
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

    private GroupBuyTeamOrderDao groupBuyTeamOrderDao;
    private GroupBuyTeamDao groupBuyTeamDao;
    private NotifyTaskDao notifyTaskDao;
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
    public void test_unPaidRefund_transferHeaderInRefund() {
        when(groupBuyTeamOrderDao.updateUserIsHeader(TEAM_ID, "user02")).thenReturn(1);

        tradeRepository.teamInCompleteUnPaidRefund(RefundOrderAggregate.buildTeamInCompleteUnPaidRefundAggregate(buildRefundOrder("user02"), -1));

        verify(groupBuyTeamOrderDao).updateUserIsHeader(TEAM_ID, "user02");
    }

    @Test
    public void test_paidRefund_transferFail_throwToRollback() {
        // 新团长更新失败（如候选人已退单）时抛异常，使退单事务整体回滚
        when(groupBuyTeamOrderDao.updateUserIsHeader(TEAM_ID, "user02")).thenReturn(0);
        try {
            tradeRepository.teamInCompletePaidRefund(RefundOrderAggregate.buildTeamInCompletePaidRefundAggregate(buildRefundOrder("user02"), -1, -1));
            Assert.fail("应抛出 UPDATE_ZERO");
        } catch (AppException e) {
            Assert.assertEquals(ResponseCodeEnum.UPDATE_ZERO.getCode(), e.getCode());
        }
    }

    @Test
    public void test_headerRefund_newHeaderTaskInRefundTx() {
        when(groupBuyTeamOrderDao.updateUserIsHeader(TEAM_ID, "user02")).thenReturn(1);
        RefundOrderEntity refundOrder = buildRefundOrder("user02");
        refundOrder.setGroupBuyTeamEntity(buildTeam(NotifyTypeEnum.MQ, null));

        tradeRepository.teamInCompletePaidRefund(RefundOrderAggregate.buildTeamInCompletePaidRefundAggregate(refundOrder, -1, -1));

        // 新团长回调任务与退单回调任务在同一个退单方法（同一事务）内写入
        ArgumentCaptor<NotifyTask> captor = ArgumentCaptor.forClass(NotifyTask.class);
        verify(notifyTaskDao, times(2)).insert(captor.capture());
        NotifyTask newHeaderTask = captor.getAllValues().stream()
                .filter(task -> Integer.valueOf(5).equals(task.getTaskType())).findFirst().orElse(null);
        Assert.assertNotNull("应生成新团长回调任务", newHeaderTask);
        Assert.assertEquals(TEAM_ID + "_5_ORDER000000000001", newHeaderTask.getTaskId());
        Assert.assertTrue(newHeaderTask.getParameterJson(), newHeaderTask.getParameterJson().contains("\"userId\":\"user02\""));
    }

    @Test
    public void test_headerRefund_httpWithoutUrl_noNewHeaderTask() {
        when(groupBuyTeamOrderDao.updateUserIsHeader(TEAM_ID, "user02")).thenReturn(1);
        RefundOrderEntity refundOrder = buildRefundOrder("user02");
        refundOrder.setGroupBuyTeamEntity(buildTeam(NotifyTypeEnum.HTTP, null));

        tradeRepository.teamInCompleteUnPaidRefund(RefundOrderAggregate.buildTeamInCompleteUnPaidRefundAggregate(refundOrder, -1));

        // HTTP 方式未配置团长退单补偿回调地址 - 商户不接收该通知，只生成退单回调任务
        ArgumentCaptor<NotifyTask> captor = ArgumentCaptor.forClass(NotifyTask.class);
        verify(notifyTaskDao, times(1)).insert(captor.capture());
        Assert.assertNotEquals(Integer.valueOf(5), captor.getValue().getTaskType());
    }

    @Test
    public void test_transferFail_noNewHeaderTask() {
        when(groupBuyTeamOrderDao.updateUserIsHeader(TEAM_ID, "user02")).thenReturn(0);
        RefundOrderEntity refundOrder = buildRefundOrder("user02");
        refundOrder.setGroupBuyTeamEntity(buildTeam(NotifyTypeEnum.MQ, null));
        try {
            tradeRepository.teamInCompleteUnPaidRefund(RefundOrderAggregate.buildTeamInCompleteUnPaidRefundAggregate(refundOrder, -1));
            Assert.fail("应抛出 UPDATE_ZERO");
        } catch (AppException e) {
            Assert.assertEquals(ResponseCodeEnum.UPDATE_ZERO.getCode(), e.getCode());
        }
        verify(notifyTaskDao, never()).insert(any(NotifyTask.class));
    }

    @Test
    public void test_getNewHeaderNotifyTask_byTaskId() {
        when(notifyTaskDao.getNotifyTaskByTaskId(TEAM_ID + "_5_ORDER000000000001")).thenReturn(NotifyTask.builder()
                .taskId(TEAM_ID + "_5_ORDER000000000001").teamId(TEAM_ID).notifyType(NotifyTypeEnum.MQ.getType())
                .notifyMQ("topic.header_refund").notifyCount(0).parameterJson("{}").build());

        NotifyTaskEntity task = tradeRepository.getNewHeaderNotifyTask(TEAM_ID, "ORDER000000000001");

        Assert.assertEquals(TEAM_ID + "_5_ORDER000000000001", task.getTaskId());
        Assert.assertEquals(NotifyTypeEnum.MQ, task.getNotifyTypeEnum());
        Assert.assertEquals("topic.header_refund", task.getNotifyMQ());
        Assert.assertNull(tradeRepository.getNewHeaderNotifyTask(TEAM_ID, "ORDER000000000002"));
    }

    @Test
    public void test_memberRefund_noTransfer() {
        tradeRepository.teamInCompleteUnPaidRefund(RefundOrderAggregate.buildTeamInCompleteUnPaidRefundAggregate(buildRefundOrder(null), -1));

        verify(groupBuyTeamOrderDao, never()).updateUserIsHeader(anyString(), anyString());
    }

    @Test
    public void test_refundTransactional() throws Exception {
        // 新团长变更依赖退单方法上的事务注解
        for (String name : new String[]{"teamInCompleteUnPaidRefund", "teamInCompletePaidRefund"}) {
            Method method = TradeRepositoryImpl.class.getMethod(name, RefundOrderAggregate.class);
            Assert.assertNotNull(name + " 应有 @Transactional", method.getAnnotation(org.springframework.transaction.annotation.Transactional.class));
        }
    }

    @Test
    public void test_refundUserNode_onlyQueryCandidate() throws Exception {
        TradeRepository repository = mock(TradeRepository.class);
        when(repository.queryNewHeaderUserId(TEAM_ID)).thenReturn("user02");
        RefundUserNode node = new RefundUserNode();
        ReflectionTestUtils.setField(node, "tradeRepository", repository);
        DefaultRefundStrategyFactory.DynamicContext context = new DefaultRefundStrategyFactory.DynamicContext();
        context.setPreRefundOrderEntity(PreRefundOrderEntity.builder().isHeader(true).orderStatusEnum(OrderStatusEnum.CREATE).build());
        context.setGroupBuyTeamEntity(GroupBuyTeamEntity.builder().teamId(TEAM_ID).teamStatusEnum(TeamStatusEnum.PROGRESS).build());

        Method doApply = RefundUserNode.class.getDeclaredMethod("doApply", PreRefundEntity.class, DefaultRefundStrategyFactory.DynamicContext.class);
        doApply.setAccessible(true);
        try {
            doApply.invoke(node, PreRefundEntity.builder().userId("user01").build(), context);
        } catch (java.lang.reflect.InvocationTargetException ignored) {
            // 路由到下一节点时未注入依赖，忽略；只关心候选人已写入上下文
        }

        Assert.assertEquals("user02", context.getNewLeaderUserId());
        verify(repository).queryNewHeaderUserId(TEAM_ID);
    }

    private GroupBuyTeamEntity buildTeam(NotifyTypeEnum notifyTypeEnum, String headerRefundNotifyUrl) {
        return GroupBuyTeamEntity.builder()
                .teamId(TEAM_ID)
                .activityId(100123L)
                .teamStatusEnum(TeamStatusEnum.PROGRESS)
                .notifyConfigVO(NotifyConfigVO.builder().notifyTypeEnum(notifyTypeEnum).headerRefundNotifyUrl(headerRefundNotifyUrl).build())
                .build();
    }

    private RefundOrderEntity buildRefundOrder(String newHeaderUserId) {
        return RefundOrderEntity.builder()
                .userId("user01")
                .orderId("ORDER000000000001")
                .teamId(TEAM_ID)
                .activityId(100123L)
                .outTradeNo("OUT0001")
                .newHeaderUserId(newHeaderUserId)
                .build();
    }

}
