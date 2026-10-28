package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.aggregate.LockOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.service.lock.LockOrderServiceImpl;
import groupbuy.market.plus.domain.trade.service.lock.factory.LockOrderLinkFactory;
import groupbuy.market.plus.types.design.framework.link.multition.chain.BusinessLinkedList;
import groupbuy.market.plus.types.enums.ResponseCodeEnum;
import groupbuy.market.plus.types.exception.AppException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 锁单用户维度锁单元测试 - 同一用户同一活动的锁单请求串行执行
 */
@RunWith(MockitoJUnitRunner.class)
public class LockOrderUserLockUnitTest {

    private static final String LOCK_KEY = "groupbuy_market_plus_lock_order_user_lock_100123_user01";

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private BusinessLinkedList<CheckLockEntity, LockOrderLinkFactory.DynamicContext, CheckLockResEntity> lockOrderLink;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @InjectMocks
    private LockOrderServiceImpl lockOrderService;

    @Before
    public void setUp() {
        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
    }

    @Test
    public void test_lockHeld_reject() throws Exception {
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(false);
        try {
            lockOrder();
            Assert.fail("应抛出 E0031");
        } catch (AppException e) {
            Assert.assertEquals(ResponseCodeEnum.E0031.getCode(), e.getCode());
        }
        // 未获取到锁时不做任何校验与落库
        verify(lockOrderLink, never()).apply(any(), any());
        verify(tradeRepository, never()).lockOrder(any());
        verify(lock, never()).unlock();
    }

    @Test
    public void test_lockAcquired_checkAndSaveInsideLock() throws Exception {
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(lockOrderLink.apply(any(CheckLockEntity.class), any(LockOrderLinkFactory.DynamicContext.class)))
                .thenReturn(CheckLockResEntity.builder().isHeader(true).build());

        lockOrder();

        // 责任链校验、锁单落库都在获取锁之后、释放锁之前完成
        InOrder inOrder = inOrder(lock, lockOrderLink, tradeRepository);
        inOrder.verify(lock).tryLock(0, TimeUnit.SECONDS);
        inOrder.verify(lockOrderLink).apply(any(), any());
        inOrder.verify(tradeRepository).lockOrder(any(LockOrderAggregate.class));
        inOrder.verify(lock).unlock();
    }

    @Test
    public void test_lockOrderFail_stillUnlock() throws Exception {
        when(lock.tryLock(0, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(lockOrderLink.apply(any(CheckLockEntity.class), any(LockOrderLinkFactory.DynamicContext.class)))
                .thenThrow(new AppException(ResponseCodeEnum.E0008.getCode(), ResponseCodeEnum.E0008.getInfo()));
        try {
            lockOrder();
            Assert.fail("应抛出 E0008");
        } catch (AppException e) {
            Assert.assertEquals(ResponseCodeEnum.E0008.getCode(), e.getCode());
        }
        verify(lock).unlock();
    }

    private void lockOrder() throws Exception {
        lockOrderService.lockOrder(UserEntity.builder().userId("user01").build(),
                GroupBuyTeamEntity.builder().activityId(100123L).build(),
                OrderDetailEntity.builder().outTradeNo("OUT0001").build());
    }

}
