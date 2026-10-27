package groupbuy.market.plus.test.unit.refund;

import groupbuy.market.plus.domain.trade.adapter.event.OrderRefundMessage;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.ActivityEntity;
import groupbuy.market.plus.domain.trade.service.lock.factory.LockOrderLinkFactory;
import groupbuy.market.plus.domain.trade.service.refund.strategy.impl.TeamInCompletePaidRefund;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 退单恢复组队库存单元测试 - 按订单幂等恢复
 */
@RunWith(MockitoJUnitRunner.class)
public class RecoverTeamStockUnitTest {

    private static final Long ACTIVITY_ID = 100123L;
    private static final String TEAM_ID = "TEAM12345678";
    private static final String ORDER_ID = "ORDER000000000001";

    @Mock
    private TradeRepository tradeRepository;

    @InjectMocks
    private TeamInCompletePaidRefund refundService;

    private final OrderRefundMessage message = new OrderRefundMessage();

    private final String recoverKey = LockOrderLinkFactory.getTeamStockRecoverKey(ACTIVITY_ID, TEAM_ID);

    @Before
    public void setUp() {
        message.setActivityId(ACTIVITY_ID);
        message.setTeamId(TEAM_ID);
        message.setOrderId(ORDER_ID);
        message.setUserId("user01");
        ActivityEntity activityEntity = new ActivityEntity();
        activityEntity.setValidTime(15);
        when(tradeRepository.getActivityById(ACTIVITY_ID)).thenReturn(activityEntity);
    }

    @Test
    public void test_recover_byOrderIdWithValidTime() throws Exception {
        when(tradeRepository.recoverTeamStockOnce(recoverKey, ORDER_ID, 15)).thenReturn(1L);

        refundService.recoverTeamLockStock(message);

        verify(tradeRepository).recoverTeamStockOnce(recoverKey, ORDER_ID, 15);
        // 不再调用无幂等的恢复方法
        verify(tradeRepository, never()).recoverTeamStock(anyString());
    }

    @Test
    public void test_repeatMessage_notThrow() throws Exception {
        // 第二次投递时仓储返回 null 表示已恢复过，应正常结束、不抛异常（否则 MQ 会无限重试）
        when(tradeRepository.recoverTeamStockOnce(recoverKey, ORDER_ID, 15)).thenReturn(1L).thenReturn(null);

        refundService.recoverTeamLockStock(message);
        refundService.recoverTeamLockStock(message);

        verify(tradeRepository, times(2)).recoverTeamStockOnce(recoverKey, ORDER_ID, 15);
    }

}
