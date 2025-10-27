package groupbuy.market.plus.trigger.job;

import groupbuy.market.plus.domain.trade.service.task.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 回调任务抽象类
 */
@Slf4j
public abstract class AbstractNotifyJob {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    protected TaskService taskService;

    /**
     * 通用执行定时任务方法
     * @param lockKey 锁
     * @param jobDesc 任务描述
     * @param jobTask 执行任务接口
     */
    protected void execNotifyJob(String lockKey, String jobDesc, JobTask jobTask) {
        // 加锁 - 部署多个应用时可能会同时执行，需要加上分布式锁，谁先抢占到谁先执行
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁
            boolean getLock = lock.tryLock(3, 30, TimeUnit.SECONDS);
            if (!getLock) {
                log.info("获取锁失败：{}，此时有其他应用在执行：{}，等待", lockKey, jobDesc);
                return;
            }
            log.info("获取锁成功：{}，定时任务 - {}开始", lockKey, jobDesc);
            Map<String, Integer> result = jobTask.execute();
            log.info("定时任务 - {}执行完成，result：{}", jobDesc, result);
        } catch (Exception e) {
            log.error("定时任务 - {}执行失败", jobDesc, e);
        } finally {
            // 检查锁是否被任何线程持有以及是否被当前线程持有 - 释放锁
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 函数式接口
     */
    @FunctionalInterface
    public interface JobTask {
        Map<String, Integer> execute() throws Exception;
    }

}
