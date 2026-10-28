package groupbuy.market.plus.test.unit.notify;

import groupbuy.market.plus.config.NotifyThreadPoolConfigProperties;
import groupbuy.market.plus.config.ThreadPoolConfig;
import groupbuy.market.plus.config.ThreadPoolConfigProperties;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.aggregate.RefundOrderAggregate;
import groupbuy.market.plus.domain.trade.model.aggregate.SettleOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.service.refund.strategy.impl.TeamInCompleteUnPaidRefund;
import groupbuy.market.plus.domain.trade.service.settle.SettleOrderServiceImpl;
import groupbuy.market.plus.domain.trade.service.task.TaskService;
import groupbuy.market.plus.types.design.framework.link.multition.chain.BusinessLinkedList;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 回调线程池单元测试 - 慢 IO 回调与试算、退单查询线程池隔离，队列满时丢弃由补偿任务补发
 */
public class NotifyThreadPoolUnitTest {

    @Test
    public void test_twoPools_registered() {
        new ApplicationContextRunner()
                .withUserConfiguration(ThreadPoolConfig.class)
                .run(context -> {
                    ThreadPoolExecutor queryPool = context.getBean("threadPoolExecutor", ThreadPoolExecutor.class);
                    ThreadPoolExecutor notifyPool = context.getBean("notifyThreadPoolExecutor", ThreadPoolExecutor.class);
                    Assert.assertNotSame("回调线程池应与查询线程池隔离", queryPool, notifyPool);
                    Assert.assertEquals(10, notifyPool.getCorePoolSize());
                    Assert.assertEquals(20, notifyPool.getMaximumPoolSize());
                    Assert.assertEquals(1000, notifyPool.getQueue().remainingCapacity());
                });
    }

    @Test
    public void test_notifyConfig_bindFromYml() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load("application-dev", new ClassPathResource("application-dev.yml"));
        sources.forEach(source -> environment.getPropertySources().addLast(source));
        Binder binder = Binder.get(environment);

        NotifyThreadPoolConfigProperties notify = binder.bind("thread.pool.notify.config", NotifyThreadPoolConfigProperties.class).get();
        Assert.assertEquals(Integer.valueOf(10), notify.getCorePoolSize());
        Assert.assertEquals(Integer.valueOf(20), notify.getMaxPoolSize());
        Assert.assertEquals(Long.valueOf(10), notify.getKeepAliveTime());
        Assert.assertEquals(Integer.valueOf(1000), notify.getBlockQueueSize());
        // 原查询线程池配置不受影响
        ThreadPoolConfigProperties query = binder.bind("thread.pool.executor.config", ThreadPoolConfigProperties.class).get();
        Assert.assertEquals(Integer.valueOf(20), query.getCorePoolSize());
        Assert.assertEquals("CallerRunsPolicy", query.getPolicy());
    }

    @Test
    public void test_queueFull_discardWithoutThrowAndNotRunInCaller() throws Exception {
        NotifyThreadPoolConfigProperties properties = new NotifyThreadPoolConfigProperties();
        properties.setCorePoolSize(1);
        properties.setMaxPoolSize(1);
        properties.setBlockQueueSize(1);
        ThreadPoolExecutor pool = new ThreadPoolConfig().notifyThreadPoolExecutor(properties);
        CountDownLatch block = new CountDownLatch(1);
        AtomicReference<String> workerName = new AtomicReference<>();
        AtomicReference<String> discardedRunner = new AtomicReference<>();
        try {
            pool.execute(() -> {
                workerName.set(Thread.currentThread().getName());
                try { block.await(5, TimeUnit.SECONDS); } catch (InterruptedException ignored) { }
            });
            pool.execute(() -> { });
            // 线程与队列均已满 - 不抛异常，也不在调用方线程执行（不拖慢结算、退单请求）
            pool.execute(() -> discardedRunner.set(Thread.currentThread().getName()));
        } finally {
            block.countDown();
            pool.shutdown();
            Assert.assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }
        Assert.assertNull("队列满时应丢弃，不应被执行", discardedRunner.get());
        Assert.assertTrue(workerName.get(), workerName.get().startsWith("notify-pool-"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void test_settle_notifyUseNotifyPool() throws Exception {
        TradeRepository tradeRepository = mock(TradeRepository.class);
        BusinessLinkedList settleOrderLink = mock(BusinessLinkedList.class);
        ThreadPoolExecutor notifyPool = mock(ThreadPoolExecutor.class);
        SettleOrderServiceImpl settleOrderService = new SettleOrderServiceImpl();
        ReflectionTestUtils.setField(settleOrderService, "tradeRepository", tradeRepository);
        ReflectionTestUtils.setField(settleOrderService, "taskService", mock(TaskService.class));
        ReflectionTestUtils.setField(settleOrderService, "settleOrderLink", settleOrderLink);
        ReflectionTestUtils.setField(settleOrderService, "notifyThreadPoolExecutor", notifyPool);
        Assert.assertFalse("结算不应再持有共用线程池", hasField(SettleOrderServiceImpl.class, "threadPoolExecutor"));
        when(settleOrderLink.apply(any(), any())).thenReturn(CheckSettleResEntity.builder().teamId("TEAM12345678").activityId(100123L).build());
        when(tradeRepository.settleOrder(any(SettleOrderAggregate.class))).thenReturn(NotifyTaskEntity.builder().taskId("TEAM12345678_1").teamId("TEAM12345678").build());

        settleOrderService.settleOrder(OrderPaySuccessEntity.builder().userId("user01").outTradeNo("OUT0001").outTradeNoPayTime(new Date()).build());

        verify(notifyPool).execute(any(Runnable.class));
    }

    @Test
    public void test_refund_notifyUseNotifyPool() {
        TradeRepository tradeRepository = mock(TradeRepository.class);
        ThreadPoolExecutor notifyPool = mock(ThreadPoolExecutor.class);
        TeamInCompleteUnPaidRefund refund = new TeamInCompleteUnPaidRefund();
        ReflectionTestUtils.setField(refund, "tradeRepository", tradeRepository);
        ReflectionTestUtils.setField(refund, "taskService", mock(TaskService.class));
        ReflectionTestUtils.setField(refund, "notifyThreadPoolExecutor", notifyPool);
        when(tradeRepository.teamInCompleteUnPaidRefund(any(RefundOrderAggregate.class))).thenReturn(NotifyTaskEntity.builder().taskId("TEAM12345678_2_ORDER000000000001").build());

        refund.refundOrder(RefundOrderEntity.builder().userId("user01").orderId("ORDER000000000001").teamId("TEAM12345678").build());

        verify(notifyPool).execute(any(Runnable.class));
    }

    private boolean hasField(Class<?> clazz, String name) {
        try {
            clazz.getDeclaredField(name);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

}
