package groupbuy.market.plus.trigger.job;

import groupbuy.market.plus.domain.trade.service.invite.InviteService;
import groupbuy.market.plus.domain.trade.service.refund.RefundOrderService;
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
public class TeamTimeOutRefundJob {

    @Resource
    private RefundOrderService refundOrderService;

    @Resource
    private InviteService inviteService;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 定时任务 - 拼团超时进行退单
     */
    @Scheduled(cron = "* 0/15 * * * ? ")
    public void exec() {
        // 加锁 - 部署多个应用时可能会同时执行，需要加上分布式锁，谁先抢占到谁先执行
        RLock lock = redissonClient.getLock(GroupBuyConstants.TeamTimeoutRefundJobLock);
        try {
            // 尝试获取锁
            boolean getLock = lock.tryLock(3, 30, TimeUnit.SECONDS);
            if (!getLock) {
                log.info("获取锁失败：{}，此时有其他应用在执行拼团超时进行退单任务，等待", GroupBuyConstants.TeamTimeoutRefundJobLock);
                return;
            }
            log.info("获取锁成功：{}，定时任务 - 执行拼团超时失败进行退单任务开始", GroupBuyConstants.TeamTimeoutRefundJobLock);
            // 拼团超时失败进行退单
            Map<String, Integer> resultMap = refundOrderService.teamTimeoutRefund();
            // 将邀请返利标记为不可用
            inviteService.inviteExpire();
            log.info("定时任务 - 执行拼团超时进行退单任务完成，result：{}", resultMap);
        } catch (Exception e) {
            log.error("定时任务 - 执行拼团超时进行退单任务失败", e);
        } finally {
            // 检查锁是否被任何线程持有以及是否被当前线程持有 - 释放锁
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
