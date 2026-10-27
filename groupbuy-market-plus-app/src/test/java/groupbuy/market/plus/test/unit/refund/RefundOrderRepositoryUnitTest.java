package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.domain.trade.model.aggregate.RefundOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.RefundOrderEntity;
import groupbuy.market.plus.infrastructure.adapter.repository.TradeRepositoryImpl;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamDao;
import groupbuy.market.plus.infrastructure.dao.GroupBuyTeamOrderDao;
import groupbuy.market.plus.infrastructure.dao.NotifyTaskDao;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeam;
import groupbuy.market.plus.infrastructure.dao.po.GroupBuyTeamOrder;
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
 * 退单仓储单元测试 - 更新订单状态时使用真实用户ID
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

        assertRefundOrderReq();
    }

    @Test
    public void test_teamCompletePaidRefund_useUserId() {
        when(groupBuyTeamOrderDao.updateOrderStatusRefund(any(GroupBuyTeamOrder.class))).thenReturn(1);
        when(groupBuyTeamDao.updateTeamByTeamCompletePaidRefund(any(GroupBuyTeam.class))).thenReturn(1);

        tradeRepository.teamCompletePaidRefund(RefundOrderAggregate.buildTeamCompletePaidRefundAggregate(refundOrderEntity, -1, -1));

        assertRefundOrderReq();
        // 组队状态交由 SQL 判断，不再由调用方传入
        ArgumentCaptor<GroupBuyTeam> teamCaptor = ArgumentCaptor.forClass(GroupBuyTeam.class);
        verify(groupBuyTeamDao).updateTeamByTeamCompletePaidRefund(teamCaptor.capture());
        Assert.assertNull(teamCaptor.getValue().getStatus());
        Assert.assertEquals(Integer.valueOf(-1), teamCaptor.getValue().getCompleteCount());
    }

    private void assertRefundOrderReq() {
        ArgumentCaptor<GroupBuyTeamOrder> captor = ArgumentCaptor.forClass(GroupBuyTeamOrder.class);
        verify(groupBuyTeamOrderDao).updateOrderStatusRefund(captor.capture());
        Assert.assertEquals("更新订单状态应使用用户ID，而不是订单ID", USER_ID, captor.getValue().getUserId());
        Assert.assertEquals(ORDER_ID, captor.getValue().getOrderId());
    }

}
