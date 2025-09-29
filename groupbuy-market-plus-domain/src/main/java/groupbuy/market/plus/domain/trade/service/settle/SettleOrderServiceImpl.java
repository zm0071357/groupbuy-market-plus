package groupbuy.market.plus.domain.trade.service.settle;

import groupbuy.market.plus.domain.trade.adapter.port.TradePort;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.aggregate.SettleOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.model.valobj.NofifyStatusEnum;
import groupbuy.market.plus.domain.trade.service.settle.factory.SettleOrderLinkFactory;
import groupbuy.market.plus.types.design.framework.link.multition.chain.BusinessLinkedList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SettleOrderServiceImpl implements SettleOrderService{

    @Resource
    private TradeRepository tradeRepository;

    @Resource
    private TradePort tradePort;

    @Resource
    private BusinessLinkedList<CheckSettleEntity, SettleOrderLinkFactory.DynamicContext, CheckSettleResEntity> settleOrderLink;

    @Override
    public SettleOrderEntity settleOrder(OrderPaySuccessEntity orderPaySuccessEntity) throws Exception {
        // 责任链过滤
        CheckSettleEntity checkSettleEntity = CheckSettleEntity.builder()
                .userId(orderPaySuccessEntity.getUserId())
                .resource(orderPaySuccessEntity.getSource())
                .channel(orderPaySuccessEntity.getChannel())
                .outTradeNo(orderPaySuccessEntity.getOutTradeNo())
                .outTradeNoPayTime(orderPaySuccessEntity.getOutTradeNoPayTime())
                .build();
        CheckSettleResEntity checkSettleResEntity = settleOrderLink.apply(checkSettleEntity, new SettleOrderLinkFactory.DynamicContext());
        SettleOrderAggregate settleOrderAggregate = SettleOrderAggregate.builder()
                .userEntity(UserEntity.builder().userId(orderPaySuccessEntity.getUserId()).build())
                .groupBuyTeamEntity(GroupBuyTeamEntity.builder()
                        .teamId(checkSettleResEntity.getTeamId())
                        .activityId(checkSettleResEntity.getActivityId())
                        .targetCount(checkSettleResEntity.getTargetCount())
                        .completeCount(checkSettleResEntity.getCompleteCount())
                        .lockCount(checkSettleResEntity.getLockCount())
                        .status(checkSettleResEntity.getStatus().getCode())
                        .startTime(checkSettleResEntity.getStartTime())
                        .endTime(checkSettleResEntity.getEndTime())
                        .build())
                .orderPaySuccessEntity(orderPaySuccessEntity)
                .build();
        // 结算
        SettleOrderEntity settleOrderEntity = tradeRepository.settleOrder(settleOrderAggregate);

        // 拼团完成 - 进行回调
        if (settleOrderEntity.getIsComplete()) {
            log.info("拼团完成，可进行回调，组队ID：{}", settleOrderEntity.getTeamId());
            execNotifyJob(settleOrderEntity.getTeamId());
        }
        return settleOrderEntity;
    }

    @Override
    public Map<String, Integer> execNotifyJob() throws Exception {
        // 查询未执行回调任务集合
        List<NotifyTaskEntity> notifyTaskEntityList = tradeRepository.getUnNotifyTask();
        return start(notifyTaskEntityList);
    }

    @Override
    public Map<String, Integer> execNotifyJob(String teamId) throws Exception {
        // 查询未执行回调任务
        List<NotifyTaskEntity> notifyTaskEntityList = tradeRepository.getUnNotifyTask(teamId);
        return start(notifyTaskEntityList);
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
            log.info("开始执行回调任务，组队ID：{}", notifyTaskEntity.getTeamId());
            // 执行
            String status = tradePort.groupBuyNotify(notifyTaskEntity);
            // 执行成功
            if (status.equals(NofifyStatusEnum.SUCCESS.getStatus())) {
                // 更新数据库状态
                int updateCount = tradeRepository.updateNotifyTaskSuccess(notifyTaskEntity.getTeamId());
                if (updateCount == 1) {
                    successCount ++;
                    log.info("回调任务执行成功，组队ID：{}", notifyTaskEntity.getTeamId());
                }
            } else if (status.equals(NofifyStatusEnum.FAIL.getStatus())) {
                // 执行失败
                // 回调次数小于5次 - 更新为重试
                if (notifyTaskEntity.getNotifyCount() < 5) {
                    int updateCount = tradeRepository.updateNotifyTaskRetry(notifyTaskEntity.getTeamId());
                    if (updateCount == 1) {
                        retryCount ++;
                        log.info("回调任务执行失败，可重试，组队ID：{}", notifyTaskEntity.getTeamId());
                    }
                } else {
                    // 失败次数大于5次 - 更新为失败
                    int updateCount = tradeRepository.updateNotifyTaskFail(notifyTaskEntity.getTeamId());
                    if (updateCount == 1) {
                        failCount ++;
                        log.info("回调任务执行失败，不可重试，组队ID：{}", notifyTaskEntity.getTeamId());
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
