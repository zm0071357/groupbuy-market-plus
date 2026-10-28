package groupbuy.market.plus.domain.trade.service.task;

import groupbuy.market.plus.domain.trade.adapter.port.TradePort;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;
import groupbuy.market.plus.domain.trade.model.valobj.NofifyStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TaskServiceImpl implements TaskService {

    /**
     * 回调任务状态 - 初始、重试（notify_task.notify_status），仅这两种状态需要执行回调
     */
    private static final Integer NOTIFY_STATUS_INIT = 0;
    private static final Integer NOTIFY_STATUS_RETRY = 2;

    @Resource
    private TradeRepository tradeRepository;

    @Resource
    private TradePort tradePort;

    @Resource
    private RedissonClient redissonClient;

    @Override
    public Map<String, Integer> execTeamSuccessNotifyJob() throws Exception {
        // 查询拼团完成回调通知任务集合
        List<NotifyTaskEntity> notifyTaskEntityList = tradeRepository.getUnNotifyTeamSuccessTask();
        return start(notifyTaskEntityList);
    }

    @Override
    public Map<String, Integer> execOrderRefundNotifyJob() throws Exception {
        // 查询退单回调通知任务集合
        List<NotifyTaskEntity> notifyTaskEntityList = tradeRepository.getUnNotifyOrderRefundTask();
        return start(notifyTaskEntityList);
    }

    @Override
    public Map<String, Integer> execHeaderRefundNotifyJob() throws Exception {
        // 查询团长退单补偿回调通知任务集合
        List<NotifyTaskEntity> notifyTaskEntityList = tradeRepository.getUnNotifyHeaderRefundTask();
        return start(notifyTaskEntityList);
    }

    @Override
    public Map<String, Integer> execNotifyJob(NotifyTaskEntity notifyTaskEntity) throws Exception {
        return start(Collections.singletonList(notifyTaskEntity));
    }

    /**
     * 执行回调任务
     * @param notifyTaskEntityList 回调任务列表
     * @return
     */
    private Map<String, Integer> start(List<NotifyTaskEntity> notifyTaskEntityList) throws Exception {
        int successCount = 0;   // 回调任务成功数量
        int failCount = 0;      // 回调任务失败数量
        int retryCount = 0;     // 回调任务重试数量
        // 执行回调任务
        if (notifyTaskEntityList == null) {
            log.info("暂无需要执行的回调任务");
            return null;
        }
        for (NotifyTaskEntity notifyTaskEntity : notifyTaskEntityList) {
            // 回调状态按任务ID更新、按任务ID加锁，缺少任务ID时无法正确更新状态，跳过避免无限重复回调
            if (StringUtils.isBlank(notifyTaskEntity.getTaskId())) {
                log.error("回调任务缺少任务ID，跳过执行，组队ID：{}", notifyTaskEntity.getTeamId());
                continue;
            }
            // 分布式锁 - 防止重复回调：锁覆盖「状态二次校验 → 回调 → 回调结果落库」全过程，
            // 状态落库后才释放锁，其他线程拿到锁时必然读到最新状态（如结算后的异步回调与定时补偿同时拿到该任务）
            // 不等待：未获取到锁说明该任务正在被其他线程处理，跳过，由下一轮补偿任务处理
            RLock lock = redissonClient.getLock(notifyTaskEntity.lockKey());
            try {
                if (!lock.tryLock(0, TimeUnit.SECONDS)) {
                    log.info("回调任务正在被其他线程处理，跳过本次回调，任务ID：{}", notifyTaskEntity.getTaskId());
                    continue;
                }
            } catch (InterruptedException e) {
                // 等待锁被中断 - 恢复中断标记并结束本批处理，剩余任务由下一轮补偿任务处理
                log.error("回调任务获取锁被中断，结束本批处理，任务ID：{}", notifyTaskEntity.getTaskId(), e);
                Thread.currentThread().interrupt();
                break;
            }
            try {
                // 二次校验 - 任务可能已被其他线程回调成功或标记失败，仅初始、重试状态需要回调
                Integer notifyStatus = tradeRepository.getNotifyStatus(notifyTaskEntity.getTaskId());
                if (!NOTIFY_STATUS_INIT.equals(notifyStatus) && !NOTIFY_STATUS_RETRY.equals(notifyStatus)) {
                    log.info("回调任务无需执行，跳过本次回调，任务ID：{}，当前状态：{}", notifyTaskEntity.getTaskId(), notifyStatus);
                    continue;
                }
                log.info("开始执行回调任务，组队ID：{}，任务ID：{}", notifyTaskEntity.getTeamId(), notifyTaskEntity.getTaskId());
                // 执行
                String status = tradePort.groupBuyNotify(notifyTaskEntity);
                // 执行成功
                if (NofifyStatusEnum.SUCCESS.getStatus().equals(status)) {
                    // 更新数据库状态
                    int updateCount = tradeRepository.updateNotifyTaskSuccess(notifyTaskEntity.getTaskId());
                    if (updateCount == 1) {
                        successCount ++;
                        log.info("回调任务执行成功，组队ID：{}，任务ID：{}", notifyTaskEntity.getTeamId(), notifyTaskEntity.getTaskId());
                    }
                } else if (!NofifyStatusEnum.NULL.getStatus().equals(status)) {
                    // 执行失败 - 除成功、空执行外的任何结果均按失败处理，累加回调次数直至标记失败
                    // 回调次数小于5次 - 更新为重试
                    if (notifyTaskEntity.getNotifyCount() < 5) {
                        int updateCount = tradeRepository.updateNotifyTaskRetry(notifyTaskEntity.getTaskId());
                        if (updateCount == 1) {
                            retryCount ++;
                            log.info("回调任务执行失败，可重试，组队ID：{}，任务ID：{}", notifyTaskEntity.getTeamId(), notifyTaskEntity.getTaskId());
                        }
                    } else {
                        // 失败次数大于5次 - 更新为失败
                        int updateCount = tradeRepository.updateNotifyTaskFail(notifyTaskEntity.getTaskId());
                        if (updateCount == 1) {
                            failCount ++;
                            log.info("回调任务执行失败，不可重试，组队ID：{}，任务ID：{}", notifyTaskEntity.getTeamId(), notifyTaskEntity.getTaskId());
                        }
                    }
                }
            } finally {
                // 回调结果落库后释放锁
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
        Map<String, Integer> resultMap = new HashMap<>();
        resultMap.put("size", notifyTaskEntityList.size());
        resultMap.put("successCount", successCount);
        resultMap.put("failCount", failCount);
        resultMap.put("retryCount", retryCount);
        return resultMap;
    }

}
