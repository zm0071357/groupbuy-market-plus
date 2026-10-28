package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.domain.trade.service.invite.InviteService;
import groupbuy.market.plus.domain.trade.service.refund.RefundOrderService;
import groupbuy.market.plus.domain.trade.service.task.TaskService;
import groupbuy.market.plus.trigger.job.TeamTimeOutRefundJob;
import groupbuy.market.plus.trigger.job.notify.TeamSuccessNotifyJob;
import org.junit.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 定时任务分布式锁单元测试 - 不指定租约时间，由看门狗续期，任务执行时间较长时锁不会提前过期
 */
public class JobLockWatchdogUnitTest {

    @Test
    public void test_notifyJob_lockWithWatchdog() throws Exception {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isLocked()).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        TaskService taskService = mock(TaskService.class);
        TeamSuccessNotifyJob job = new TeamSuccessNotifyJob();
        ReflectionTestUtils.setField(job, "redissonClient", redissonClient);
        ReflectionTestUtils.setField(job, "taskService", taskService);

        job.exec();

        verify(lock).tryLock(3, TimeUnit.SECONDS);
        verify(lock, never()).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
        verify(taskService).execTeamSuccessNotifyJob();
        verify(lock).unlock();
    }

    @Test
    public void test_timeoutRefundJob_lockWithWatchdog() throws Exception {
        RedissonClient redissonClient = mock(RedissonClient.class);
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        RefundOrderService refundOrderService = mock(RefundOrderService.class);
        when(refundOrderService.teamTimeoutRefund()).thenReturn(Collections.emptyMap());
        TeamTimeOutRefundJob job = new TeamTimeOutRefundJob();
        ReflectionTestUtils.setField(job, "redissonClient", redissonClient);
        ReflectionTestUtils.setField(job, "refundOrderService", refundOrderService);
        ReflectionTestUtils.setField(job, "inviteService", mock(InviteService.class));

        job.exec();

        verify(lock).tryLock(3, TimeUnit.SECONDS);
        verify(lock, never()).tryLock(anyLong(), anyLong(), any(TimeUnit.class));
        verify(refundOrderService).teamTimeoutRefund();
    }

}
