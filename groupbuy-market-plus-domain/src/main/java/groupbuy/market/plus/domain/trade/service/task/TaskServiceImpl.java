package groupbuy.market.plus.domain.trade.service.task;

import groupbuy.market.plus.domain.trade.adapter.port.TradePort;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;
import groupbuy.market.plus.domain.trade.model.valobj.NofifyStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Slf4j
@Service
public class TaskServiceImpl implements TaskService {

    @Resource
    private TradeRepository tradeRepository;

    @Resource
    private TradePort tradePort;

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
            log.info("开始执行回调任务，组队ID：{}，任务ID：{}", notifyTaskEntity.getTeamId(), notifyTaskEntity.getTaskId());
            // 执行
            String status = tradePort.groupBuyNotify(notifyTaskEntity);
            // 执行成功
            if (status.equals(NofifyStatusEnum.SUCCESS.getStatus())) {
                // 更新数据库状态
                int updateCount = tradeRepository.updateNotifyTaskSuccess(notifyTaskEntity.getTaskId());
                if (updateCount == 1) {
                    successCount ++;
                    log.info("回调任务执行成功，组队ID：{}，任务ID：{}", notifyTaskEntity.getTeamId(), notifyTaskEntity.getTaskId());
                }
            } else if (status.equals(NofifyStatusEnum.FAIL.getStatus())) {
                // 执行失败
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
        }
        Map<String, Integer> resultMap = new HashMap<>();
        resultMap.put("size", notifyTaskEntityList.size());
        resultMap.put("successCount", successCount);
        resultMap.put("failCount", failCount);
        resultMap.put("retryCount", retryCount);
        return resultMap;
    }

}
