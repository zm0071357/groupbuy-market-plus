package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.domain.trade.model.aggregate.RefundOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.RefundOrderEntity;
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
 * 退单仓储单元测试 - 更新订单状态时使用真实用户ID，并按退单策略传入期望的原状态
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

        // 未支付退单 - 原状态必须为已创建(0)
        assertRefundOrderReq(0);
    }

    @Test
    public void test_teamInCompletePaidRefund_expectPaidStatus() {
        when(groupBuyTeamOrderDao.updateOrderStatusRefund(any(GroupBuyTeamOrder.class))).thenReturn(1);
        when(groupBuyTeamDao.updateTeamByTeamInCompletePaidRefund(any(GroupBuyTeam.class))).thenReturn(1);

        tradeRepository.teamInCompletePaidRefund(RefundOrderAggregate.buildTeamInCompletePaidRefundAggregate(refundOrderEntity, -1, -1));

        // 已支付退单 - 原状态必须为已支付(1)
        assertRefundOrderReq(1);
    }

    @Test
    public void test_teamInCompleteUnPaidRefund_orderPaidConcurrently_rollback() {
        // 结算并发提交后订单已变为已支付，按未支付策略退单更新 0 行，抛异常回滚，不再扣减组队锁单量与生成回调任务
        when(groupBuyTeamOrderDao.updateOrderStatusRefund(any(GroupBuyTeamOrder.class))).thenReturn(0);

        try {
            tradeRepository.teamInCompleteUnPaidRefund(RefundOrderAggregate.buildTeamInCompleteUnPaidRefundAggregate(refundOrderEntity, -1));
            Assert.fail("原状态已变化时应抛出异常");
        } catch (AppException e) {
            Assert.assertEquals(ResponseCodeEnum.UPDATE_ZERO.getCode(), e.getCode());
        }
        verify(groupBuyTeamDao, never()).updateTeamByTeamInCompleteUnPaidRefund(any(GroupBuyTeam.class));
        verify(notifyTaskDao, never()).insert(any());
    }

    @Test
    public void test_teamCompletePaidRefund_useUserId() {
        when(groupBuyTeamOrderDao.updateOrderStatusRefund(any(GroupBuyTeamOrder.class))).thenReturn(1);
        when(groupBuyTeamDao.updateTeamByTeamCompletePaidRefund(any(GroupBuyTeam.class))).thenReturn(1);

        tradeRepository.teamCompletePaidRefund(RefundOrderAggregate.buildTeamCompletePaidRefundAggregate(refundOrderEntity, -1, -1));

        // 已支付退单 - 原状态必须为已支付(1)
        assertRefundOrderReq(1);
        // 组队状态交由 SQL 判断，不再由调用方传入
        ArgumentCaptor<GroupBuyTeam> teamCaptor = ArgumentCaptor.forClass(GroupBuyTeam.class);
        verify(groupBuyTeamDao).updateTeamByTeamCompletePaidRefund(teamCaptor.capture());
        Assert.assertNull(teamCaptor.getValue().getStatus());
        Assert.assertEquals(Integer.valueOf(-1), teamCaptor.getValue().getCompleteCount());
    }

    private void assertRefundOrderReq(Integer expectStatus) {
        ArgumentCaptor<GroupBuyTeamOrder> captor = ArgumentCaptor.forClass(GroupBuyTeamOrder.class);
        verify(groupBuyTeamOrderDao).updateOrderStatusRefund(captor.capture());
        Assert.assertEquals("更新订单状态应使用用户ID，而不是订单ID", USER_ID, captor.getValue().getUserId());
        Assert.assertEquals(ORDER_ID, captor.getValue().getOrderId());
        Assert.assertEquals(expectStatus, captor.getValue().getStatus());
    }

}
