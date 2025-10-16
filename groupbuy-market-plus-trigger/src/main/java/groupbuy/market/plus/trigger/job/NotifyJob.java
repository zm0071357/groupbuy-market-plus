package groupbuy.market.plus.trigger.job;

import groupbuy.market.plus.domain.trade.service.settle.SettleOrderService;
import groupbuy.market.plus.types.common.GroupBuyConstants;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class NotifyJob {

    @Resource
    private SettleOrderService settleOrderService;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 定时任务 - 执行回调通知拼团完成
     */
    @Scheduled(cron = "0/10 * * * * ?")
    public void exec() {
        // 加锁 - 部署多个应用时可能会同时执行，需要加上分布式锁，谁先抢占到谁先执行
        RLock lock = redissonClient.getLock(GroupBuyConstants.Lock);
        try {
            // 尝试获取锁
            boolean getLock = lock.tryLock(3, 30, TimeUnit.SECONDS);
            if (!getLock) {
                log.info("获取锁失败：{}，此时有其他应用在执行回调通知拼团完成任务，等待", GroupBuyConstants.Lock);
                return;
            }
            log.info("获取锁成功：{}，定时任务 - 执行回调通知开始", GroupBuyConstants.Lock);
            Map<String, Integer> result = settleOrderService.execNotifyJob();
            log.info("定时任务 - 执行回调通知拼团完成 result:{}", result);
        } catch (Exception e) {
            log.error("定时任务 - 执行回调通知拼团完成 失败", e);
        } finally {
            // 检查锁是否被任何线程持有以及是否被当前线程持有 - 释放锁
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
