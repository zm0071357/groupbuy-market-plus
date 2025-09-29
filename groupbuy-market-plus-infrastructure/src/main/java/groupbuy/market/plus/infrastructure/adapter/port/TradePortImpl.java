package groupbuy.market.plus.infrastructure.adapter.port;

import groupbuy.market.plus.domain.trade.adapter.port.TradePort;
import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;
import groupbuy.market.plus.domain.trade.model.valobj.NofifyStatusEnum;
import groupbuy.market.plus.infrastructure.gateway.NotifyTaskServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TradePortImpl implements TradePort {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private NotifyTaskServiceImpl notifyTaskServiceImpl;

    @Override
    public String groupBuyNotify(NotifyTaskEntity notifyTaskEntity) throws Exception {
        // 分布式锁 - 防止重复回调
        RLock lock = redissonClient.getLock(notifyTaskEntity.lockKey());
        try {
            // 抢到锁 - 执行
            if (lock.tryLock(3, 0, TimeUnit.SECONDS)) {
                try {
                    log.info("开始执行回调任务，组队ID：{}", notifyTaskEntity.getTeamId());
                    return notifyTaskServiceImpl.notify(notifyTaskEntity.getNotifyUrl(), notifyTaskEntity.getParameterJson());
                } finally {
                    // 释放锁
                    log.info("回调任务执行结束，释放锁，组队ID：{}", notifyTaskEntity.getTeamId());
                    lock.unlock();
                }
            }
            // 未抢到锁 - 不执行返回空结果
            return NofifyStatusEnum.NULL.getStatus();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            return NofifyStatusEnum.FAIL.getStatus();
        }
    }

}

