package groupbuy.market.plus.test.unit.trade;

import groupbuy.market.plus.domain.trade.adapter.port.TradePort;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;
import groupbuy.market.plus.domain.trade.model.valobj.NofifyStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyTypeEnum;
import groupbuy.market.plus.domain.trade.service.task.TaskServiceImpl;
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

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 回调任务服务单元测试 - 同一组队下的多个回调任务按任务ID各自更新状态；
 * 加锁覆盖「状态二次校验 → 回调 → 回调结果落库」，落库后才释放锁，防止重复回调
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskServiceImplUnitTest {

    private static final String TEAM_ID = "TEAM12345678";

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private TradePort tradePort;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @InjectMocks
    private TaskServiceImpl taskService;

    @Before
    public void setUp() throws Exception {
        lenient().when(redissonClient.getLock(anyString())).thenReturn(lock);
        lenient().when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        lenient().when(lock.isHeldByCurrentThread()).thenReturn(true);
        lenient().when(tradeRepository.getNotifyStatus(anyString())).thenReturn(0);
    }

    @Test
    public void test_sameTeamTasks_updateByTaskId() throws Exception {
        NotifyTaskEntity settleTask = buildTask(TEAM_ID + "_1", 0);
        NotifyTaskEntity refundTask = buildTask(TEAM_ID + "_2_ORDER000000000001", 0);
        when(tradeRepository.getUnNotifyOrderRefundTask()).thenReturn(Arrays.asList(settleTask, refundTask));
        when(tradePort.groupBuyNotify(settleTask)).thenReturn(NofifyStatusEnum.SUCCESS.getStatus());
        when(tradePort.groupBuyNotify(refundTask)).thenReturn(NofifyStatusEnum.FAIL.getStatus());
        when(tradeRepository.updateNotifyTaskSuccess(settleTask.getTaskId())).thenReturn(1);
        when(tradeRepository.updateNotifyTaskRetry(refundTask.getTaskId())).thenReturn(1);

        Map<String, Integer> result = taskService.execOrderRefundNotifyJob();

        // 成功的任务只更新自己，失败的任务进入重试，互不影响
        verify(tradeRepository).updateNotifyTaskSuccess(settleTask.getTaskId());
        verify(tradeRepository).updateNotifyTaskRetry(refundTask.getTaskId());
        verify(tradeRepository, never()).updateNotifyTaskSuccess(refundTask.getTaskId());
        verify(tradeRepository, never()).updateNotifyTaskSuccess(TEAM_ID);
        verify(tradeRepository, never()).updateNotifyTaskFail(anyString());
        Assert.assertEquals(Integer.valueOf(1), result.get("successCount"));
        Assert.assertEquals(Integer.valueOf(1), result.get("retryCount"));
    }

    @Test
    public void test_failOverRetryLimit_updateFailByTaskId() throws Exception {
        NotifyTaskEntity task = buildTask(TEAM_ID + "_1", 5);
        when(tradePort.groupBuyNotify(task)).thenReturn(NofifyStatusEnum.FAIL.getStatus());
        when(tradeRepository.updateNotifyTaskFail(task.getTaskId())).thenReturn(1);

        Map<String, Integer> result = taskService.execNotifyJob(task);

        verify(tradeRepository).updateNotifyTaskFail(task.getTaskId());
        verify(tradeRepository, never()).updateNotifyTaskRetry(anyString());
        Assert.assertEquals(Integer.valueOf(1), result.get("failCount"));
    }

    @Test
    public void test_skippedTask_noStatusUpdate() throws Exception {
        NotifyTaskEntity task = buildTask(TEAM_ID + "_1", 0);
        when(tradePort.groupBuyNotify(task)).thenReturn(NofifyStatusEnum.NULL.getStatus());

        taskService.execNotifyJob(task);

        verify(tradeRepository, never()).updateNotifyTaskSuccess(anyString());
        verify(tradeRepository, never()).updateNotifyTaskRetry(anyString());
        verify(tradeRepository, never()).updateNotifyTaskFail(anyString());
    }

    @Test
    public void test_statusUpdatedBeforeUnlock() throws Exception {
        NotifyTaskEntity task = buildTask(TEAM_ID + "_1", 0);
        when(tradePort.groupBuyNotify(task)).thenReturn(NofifyStatusEnum.SUCCESS.getStatus());
        when(tradeRepository.updateNotifyTaskSuccess(task.getTaskId())).thenReturn(1);

        taskService.execNotifyJob(task);

        // 按任务ID加锁、不等待；二次校验与回调结果落库都在释放锁之前完成
        verify(redissonClient).getLock(task.lockKey());
        InOrder inOrder = inOrder(lock, tradeRepository, tradePort);
        inOrder.verify(lock).tryLock(0, TimeUnit.SECONDS);
        inOrder.verify(tradeRepository).getNotifyStatus(task.getTaskId());
        inOrder.verify(tradePort).groupBuyNotify(task);
        inOrder.verify(tradeRepository).updateNotifyTaskSuccess(task.getTaskId());
        inOrder.verify(lock).unlock();
        // 只在落库后释放一次
        verify(lock, times(1)).unlock();
    }

    @Test
    public void test_retryStatusUpdatedBeforeUnlock() throws Exception {
        NotifyTaskEntity task = buildTask(TEAM_ID + "_1", 1);
        when(tradeRepository.getNotifyStatus(task.getTaskId())).thenReturn(2);
        when(tradePort.groupBuyNotify(task)).thenReturn(NofifyStatusEnum.FAIL.getStatus());
        when(tradeRepository.updateNotifyTaskRetry(task.getTaskId())).thenReturn(1);

        taskService.execNotifyJob(task);

        InOrder inOrder = inOrder(tradeRepository, lock);
        inOrder.verify(tradeRepository).updateNotifyTaskRetry(task.getTaskId());
        inOrder.verify(lock).unlock();
    }

    @Test
    public void test_lockNotAcquired_skip() throws Exception {
        NotifyTaskEntity task = buildTask(TEAM_ID + "_1", 0);
        when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(false);

        taskService.execNotifyJob(task);

        // 其他线程正在处理该任务 - 不查状态、不回调、不更新、不释放别人的锁
        verify(tradeRepository, never()).getNotifyStatus(anyString());
        verify(tradePort, never()).groupBuyNotify(any());
        verify(tradeRepository, never()).updateNotifyTaskSuccess(anyString());
        verify(tradeRepository, never()).updateNotifyTaskRetry(anyString());
        verify(lock, never()).unlock();
    }

    @Test
    public void test_alreadyFinished_skipNotify() throws Exception {
        // 已成功(1)、已失败(3)、任务不存在(null) 均不再回调
        for (Integer finishedStatus : Arrays.asList(1, 3, null)) {
            reset(tradePort);
            NotifyTaskEntity task = buildTask(TEAM_ID + "_1", 0);
            when(tradeRepository.getNotifyStatus(task.getTaskId())).thenReturn(finishedStatus);

            taskService.execNotifyJob(task);

            verify(tradePort, never()).groupBuyNotify(any());
        }
        verify(tradeRepository, never()).updateNotifyTaskSuccess(anyString());
        verify(tradeRepository, never()).updateNotifyTaskRetry(anyString());
        verify(tradeRepository, never()).updateNotifyTaskFail(anyString());
        // 跳过时同样释放锁
        verify(lock, times(3)).unlock();
    }

    @Test
    public void test_notifyThrow_releaseLock() throws Exception {
        NotifyTaskEntity task = buildTask(TEAM_ID + "_1", 0);
        when(tradePort.groupBuyNotify(task)).thenThrow(new RuntimeException("publish error"));

        try {
            taskService.execNotifyJob(task);
            Assert.fail("回调异常应向上抛出");
        } catch (RuntimeException e) {
            Assert.assertEquals("publish error", e.getMessage());
        }
        verify(lock).unlock();
    }

    @Test
    public void test_lockInterrupted_restoreInterruptAndStop() throws Exception {
        NotifyTaskEntity task1 = buildTask(TEAM_ID + "_1", 0);
        NotifyTaskEntity task2 = buildTask(TEAM_ID + "_2_ORDER000000000001", 0);
        when(tradeRepository.getUnNotifyOrderRefundTask()).thenReturn(Arrays.asList(task1, task2));
        when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException());

        taskService.execOrderRefundNotifyJob();

        // 恢复中断标记并结束本批处理，后续任务不再尝试加锁
        Assert.assertTrue("获取锁被中断时应恢复中断标记", Thread.interrupted());
        verify(lock, times(1)).tryLock(anyLong(), any(TimeUnit.class));
        verify(tradePort, never()).groupBuyNotify(any());
        verify(tradeRepository, never()).updateNotifyTaskRetry(anyString());
    }

    /**
     * 多线程并发处理同一任务（如结算后的异步回调与多轮定时补偿重叠）：只回调一次
     * 使用真实的互斥锁模拟分布式锁，回调结果落库前存在耗时，原实现在释放锁与落库之间会被其他线程再次回调
     */
    @Test
    public void test_concurrentSameTask_notifyOnce() throws Exception {
        NotifyTaskEntity task = buildTask(TEAM_ID + "_1", 0);
        ReentrantLock realLock = new ReentrantLock();
        when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenAnswer(invocation -> realLock.tryLock());
        when(lock.isHeldByCurrentThread()).thenAnswer(invocation -> realLock.isHeldByCurrentThread());
        doAnswer(invocation -> { realLock.unlock(); return null; }).when(lock).unlock();
        AtomicInteger notifyStatus = new AtomicInteger(0);
        AtomicInteger notifyTimes = new AtomicInteger(0);
        when(tradeRepository.getNotifyStatus(task.getTaskId())).thenAnswer(invocation -> notifyStatus.get());
        when(tradePort.groupBuyNotify(task)).thenAnswer(invocation -> {
            notifyTimes.incrementAndGet();
            Thread.sleep(50);
            return NofifyStatusEnum.SUCCESS.getStatus();
        });
        when(tradeRepository.updateNotifyTaskSuccess(task.getTaskId())).thenAnswer(invocation -> {
            Thread.sleep(50);
            return notifyStatus.compareAndSet(0, 1) ? 1 : 0;
        });

        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch done = new CountDownLatch(threads);
        long deadline = System.currentTimeMillis() + 300;
        try {
            // 8 个线程在 300ms 内持续重复执行，覆盖「持锁中到达」「锁释放后到达」等各种时序
            for (int i = 0; i < threads; i++) {
                executor.execute(() -> {
                    try {
                        while (System.currentTimeMillis() < deadline) {
                            taskService.execNotifyJob(task);
                            Thread.sleep(1);
                        }
                    } catch (Exception ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }
            Assert.assertTrue(done.await(30, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        Assert.assertEquals("同一任务只能回调一次", 1, notifyTimes.get());
        Assert.assertEquals(1, notifyStatus.get());
    }

    @Test
    public void test_lockKey_byTaskId() {
        NotifyTaskEntity settleTask = buildTask(TEAM_ID + "_1", 0);
        NotifyTaskEntity refundTask = buildTask(TEAM_ID + "_2_ORDER000000000001", 0);
        Assert.assertNotEquals(settleTask.lockKey(), refundTask.lockKey());
        Assert.assertTrue(settleTask.lockKey().endsWith(settleTask.getTaskId()));
    }

    private NotifyTaskEntity buildTask(String taskId, int notifyCount) {
        return NotifyTaskEntity.builder()
                .taskId(taskId)
                .teamId(TEAM_ID)
                .notifyTypeEnum(NotifyTypeEnum.HTTP)
                .notifyUrl("http://127.0.0.1/notify")
                .notifyCount(notifyCount)
                .parameterJson("{}")
                .build();
    }

}
