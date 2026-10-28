package groupbuy.market.plus.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@EnableAsync
@Configuration
@EnableConfigurationProperties({ThreadPoolConfigProperties.class, NotifyThreadPoolConfigProperties.class})
public class ThreadPoolConfig {

    @Bean(name = "threadPoolExecutor")
    @ConditionalOnMissingBean(name = "threadPoolExecutor")
    public ThreadPoolExecutor threadPoolExecutor(ThreadPoolConfigProperties properties) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        // 实例化策略
        RejectedExecutionHandler handler;
        switch (properties.getPolicy()){
            case "AbortPolicy":
                handler = new ThreadPoolExecutor.AbortPolicy();
                break;
            case "DiscardPolicy":
                handler = new ThreadPoolExecutor.DiscardPolicy();
                break;
            case "DiscardOldestPolicy":
                handler = new ThreadPoolExecutor.DiscardOldestPolicy();
                break;
            case "CallerRunsPolicy":
                handler = new ThreadPoolExecutor.CallerRunsPolicy();
                break;
            default:
                handler = new ThreadPoolExecutor.AbortPolicy();
                break;
        }
        // 创建线程池
        return new ThreadPoolExecutor(
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                properties.getKeepAliveTime(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(properties.getBlockQueueSize()),
                Executors.defaultThreadFactory(),
                handler);
    }

    /**
     * 回调线程池 - 成团、退单、新团长回调专用
     * HTTP 回调单次最长可达数十秒，与试算、退单的异步查询共用线程池时，商户回调变慢会占满核心线程，导致查询排队超时
     * 拒绝策略：丢弃本次异步回调并告警 - 回调任务已持久化到 notify_task，由定时补偿任务补发；
     * 不使用 CallerRunsPolicy，避免慢回调拖慢结算、退单请求线程
     */
    @Bean(name = "notifyThreadPoolExecutor")
    @ConditionalOnMissingBean(name = "notifyThreadPoolExecutor")
    public ThreadPoolExecutor notifyThreadPoolExecutor(NotifyThreadPoolConfigProperties properties) {
        AtomicInteger threadIndex = new AtomicInteger(1);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setName("notify-pool-" + threadIndex.getAndIncrement());
            return thread;
        };
        RejectedExecutionHandler handler = (runnable, executor) ->
                log.warn("回调线程池已满，丢弃本次异步回调，由定时补偿任务补发，活跃线程数：{}，队列长度：{}",
                        executor.getActiveCount(), executor.getQueue().size());
        return new ThreadPoolExecutor(
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                properties.getKeepAliveTime(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(properties.getBlockQueueSize()),
                threadFactory,
                handler);
    }

}
