package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.domain.trade.model.aggregate.RefundOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.RefundOrderEntity;
import groupbuy.market.plus.domain.trade.model.valobj.OrderStatusEnum;
import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamOrderDao;
import groupbuy.market.plus.infrastructure.dao.NotifyTaskDao;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeam;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamOrder;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 退单仓储单元测试 - 更新订单状态时使用真实用户ID，并按选择策略时的原状态更新
 */
@RunWith(MockitoJUnitRunner.class)
public class RefundOrderRepositoryUnitTest {

    private static final String USER_ID = "user01";
    private static final String ORDER_ID = "ORDER000000000001";

    @Mock
    private GroupBuyTeamDao groupBuyTeamDao;

    @Mock
    private GroupBuyTeamOrderDao groupBuyTeamOrderDao;

    @Mock
    private NotifyTaskDao notifyTaskDao;

    @InjectMocks
    private TradeRepositoryImpl tradeRepository;

    private final RefundOrderEntity refundOrderEntity = RefundOrderEntity.builder()
            .userId(USER_ID)
            .orderId(ORDER_ID)
            .teamId("TEAM12345678")
            .activityId(100123L)
            .outTradeNo("OUT0001")
            .build();

    @Test
    public void test_teamInCompleteUnPaidRefund_useUserId() {
        when(groupBuyTeamOrderDao.updateOrderStatusRefund(any(GroupBuyTeamOrder.class))).thenReturn(1);
        when(groupBuyTeamDao.updateTeamByTeamInCompleteUnPaidRefund(any(GroupBuyTeam.class))).thenReturn(1);

        tradeRepository.teamInCompleteUnPaidRefund(RefundOrderAggregate.buildTeamInCompleteUnPaidRefundAggregate(refundOrderEntity, -1));

        assertRefundOrderReq(OrderStatusEnum.CREATE);
    }

    @Test
    public void test_teamInCompletePaidRefund_expectPaidStatus() {
        when(groupBuyTeamOrderDao.updateOrderStatusRefund(any(GroupBuyTeamOrder.class))).thenReturn(1);
        when(groupBuyTeamDao.updateTeamByTeamInCompletePaidRefund(any(GroupBuyTeam.class))).thenReturn(1);

        tradeRepository.teamInCompletePaidRefund(RefundOrderAggregate.buildTeamInCompletePaidRefundAggregate(refundOrderEntity, -1, -1));

        assertRefundOrderReq(OrderStatusEnum.COMPLETE);
    }

    @Test
    public void test_teamInCompleteUnPaidRefund_orderPaidConcurrently_rollback() {
        // 选策略时订单为已创建，事务执行前被并发结算为已支付 - 按原状态更新命中 0 行
        when(groupBuyTeamOrderDao.updateOrderStatusRefund(any(GroupBuyTeamOrder.class))).thenReturn(0);

        try {
            tradeRepository.teamInCompleteUnPaidRefund(RefundOrderAggregate.buildTeamInCompleteUnPaidRefundAggregate(refundOrderEntity, -1));
            Assert.fail("订单原状态已变更时应抛出异常回滚");
        } catch (AppException e) {
            Assert.assertEquals(ResponseCodeEnum.UPDATE_ZERO.getCode(), e.getCode());
        }

        // 不得只扣锁单量，也不得生成未支付退单回调任务
        verify(groupBuyTeamDao, never()).updateTeamByTeamInCompleteUnPaidRefund(any(GroupBuyTeam.class));
        verifyNoInteractions(notifyTaskDao);
    }

    @Test
    public void test_teamCompletePaidRefund_useUserId() {
        when(groupBuyTeamOrderDao.updateOrderStatusRefund(any(GroupBuyTeamOrder.class))).thenReturn(1);
        when(groupBuyTeamDao.updateTeamByTeamCompletePaidRefund(any(GroupBuyTeam.class))).thenReturn(1);

        tradeRepository.teamCompletePaidRefund(RefundOrderAggregate.buildTeamCompletePaidRefundAggregate(refundOrderEntity, -1, -1));

        assertRefundOrderReq(OrderStatusEnum.COMPLETE);
        // 组队状态交由 SQL 判断，不再由调用方传入
        ArgumentCaptor<GroupBuyTeam> teamCaptor = ArgumentCaptor.forClass(GroupBuyTeam.class);
        verify(groupBuyTeamDao).updateTeamByTeamCompletePaidRefund(teamCaptor.capture());
        Assert.assertNull(teamCaptor.getValue().getStatus());
        Assert.assertEquals(Integer.valueOf(-1), teamCaptor.getValue().getCompleteCount());
    }

    private void assertRefundOrderReq(OrderStatusEnum expectStatus) {
        ArgumentCaptor<GroupBuyTeamOrder> captor = ArgumentCaptor.forClass(GroupBuyTeamOrder.class);
        verify(groupBuyTeamOrderDao).updateOrderStatusRefund(captor.capture());
        Assert.assertEquals("更新订单状态应使用用户ID，而不是订单ID", USER_ID, captor.getValue().getUserId());
        Assert.assertEquals(ORDER_ID, captor.getValue().getOrderId());
        Assert.assertEquals("应按选择退单策略时的原状态更新", expectStatus.getCode(), captor.getValue().getStatus());
    }

}
