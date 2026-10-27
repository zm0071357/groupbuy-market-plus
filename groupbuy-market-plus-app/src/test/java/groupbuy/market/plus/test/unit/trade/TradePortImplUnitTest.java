package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;
import groupbuy.market.plus.domain.trade.model.valobj.NofifyStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyTypeEnum;
import groupbuy.market.plus.infrastructure.adapter.port.TradePortImpl;
import groupbuy.market.plus.infrastructure.dao.NotifyTaskDao;
import groupbuy.market.plus.infrastructure.event.EventPublisher;
import groupbuy.market.plus.infrastructure.gateway.NotifyTaskServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 回调端口单元测试 - 防重复回调
 */
@RunWith(MockitoJUnitRunner.class)
public class TradePortImplUnitTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private NotifyTaskDao notifyTaskDao;

    @Mock
    private NotifyTaskServiceImpl notifyTaskServiceImpl;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private TradePortImpl tradePort;

    private final NotifyTaskEntity task = NotifyTaskEntity.builder()
            .taskId("TEAM12345678_1")
            .teamId("TEAM12345678")
            .notifyTypeEnum(NotifyTypeEnum.HTTP)
            .notifyUrl("http://127.0.0.1/notify")
            .notifyCount(0)
            .parameterJson("{}")
            .build();

    @Before
    public void setUp() throws Exception {
        when(redissonClient.getLock(task.lockKey())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    }

    @Test
    public void test_alreadySuccess_skipNotify() throws Exception {
        when(notifyTaskDao.getNotifyStatusByTaskId(task.getTaskId())).thenReturn(1);

        String status = tradePort.groupBuyNotify(task);

        Assert.assertEquals(NofifyStatusEnum.NULL.getStatus(), status);
        verify(notifyTaskServiceImpl, never()).notify(anyString(), anyString());
        verify(lock).unlock();
    }

    @Test
    public void test_notSuccess_doNotify() throws Exception {
        when(notifyTaskDao.getNotifyStatusByTaskId(task.getTaskId())).thenReturn(2);
        when(notifyTaskServiceImpl.notify(task.getNotifyUrl(), task.getParameterJson())).thenReturn(NofifyStatusEnum.SUCCESS.getStatus());

        String status = tradePort.groupBuyNotify(task);

        Assert.assertEquals(NofifyStatusEnum.SUCCESS.getStatus(), status);
        verify(notifyTaskServiceImpl).notify(task.getNotifyUrl(), task.getParameterJson());
    }

    /**
     * 回调本身抛异常时返回失败，且不能给当前线程设置中断标记
     */
    @Test
    public void test_notifyException_notInterruptThread() throws Exception {
        when(notifyTaskDao.getNotifyStatusByTaskId(task.getTaskId())).thenReturn(0);
        when(notifyTaskServiceImpl.notify(task.getNotifyUrl(), task.getParameterJson())).thenThrow(new RuntimeException("connect timed out"));

        String status = tradePort.groupBuyNotify(task);

        Assert.assertEquals(NofifyStatusEnum.FAIL.getStatus(), status);
        Assert.assertFalse("回调异常不应设置线程中断标记", Thread.interrupted());
    }

    /**
     * 等待锁被中断时返回失败，并恢复中断标记
     */
    @Test
    public void test_lockInterrupted_restoreInterrupt() throws Exception {
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());

        String status = tradePort.groupBuyNotify(task);

        Assert.assertEquals(NofifyStatusEnum.FAIL.getStatus(), status);
        // Thread.interrupted() 会同时清除中断标记，避免影响后续测试
        Assert.assertTrue("等待锁被中断时应恢复中断标记", Thread.interrupted());
    }

}
