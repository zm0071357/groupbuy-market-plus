package groupbuy.market.plus.domain.trade.service.settle;

import com.alibaba.fastjson.JSON;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.aggregate.SettleOrderAggregate;
import groupbuy.market.plus.domain.trade.model.entity.*;
import groupbuy.market.plus.domain.trade.service.task.TaskService;
import groupbuy.market.plus.domain.trade.service.settle.factory.SettleOrderLinkFactory;
import groupbuy.market.plus.types.design.framework.link.multition.chain.BusinessLinkedList;
import groupbuy.market.plus.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
public class SettleOrderServiceImpl implements SettleOrderService{

    @Resource
    private TradeRepository tradeRepository;

    @Resource
    private TaskService taskService;

    @Resource
    private BusinessLinkedList<CheckSettleEntity, SettleOrderLinkFactory.DynamicContext, CheckSettleResEntity> settleOrderLink;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public SettleOrderEntity settleOrder(OrderPaySuccessEntity orderPaySuccessEntity) throws Exception {
        // 责任链过滤
        CheckSettleEntity checkSettleEntity = CheckSettleEntity.builder()
                .userId(orderPaySuccessEntity.getUserId())
                .source(orderPaySuccessEntity.getSource())
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
                        .teamStatusEnum(checkSettleResEntity.getTeamStatusEnum())
                        .startTime(checkSettleResEntity.getStartTime())
                        .endTime(checkSettleResEntity.getEndTime())
                        .notifyConfigVO(checkSettleResEntity.getNotifyConfigVO())
                        .build())
                .orderPaySuccessEntity(orderPaySuccessEntity)
                .build();
        // 结算
        NotifyTaskEntity notifyTaskEntity = tradeRepository.settleOrder(settleOrderAggregate);

        // 拼团完成 - 进行回调
        if (notifyTaskEntity != null) {
            log.info("拼团完成，异步执行回调任务，组队ID：{}", notifyTaskEntity.getTeamId());
            // 异步执行回调任务
            threadPoolExecutor.execute(() -> {
                Map<String, Integer> notifyResultMap = null;
                try {
                    notifyResultMap = taskService.execNotifyJob(notifyTaskEntity);
                    log.info("回调拼团完成通知完成：{}", JSON.toJSONString(notifyResultMap));
                } catch (Exception e) {
                    log.error("回调拼团完成通知失败：{}", JSON.toJSONString(notifyResultMap), e);
                    throw new AppException(e.getMessage());
                }
            });
        }

        return SettleOrderEntity.builder()
                .userId(orderPaySuccessEntity.getUserId())
                .teamId(checkSettleResEntity.getTeamId())
                .activityId(checkSettleResEntity.getActivityId())
                .outTradeNo(orderPaySuccessEntity.getOutTradeNo())
                .outTradeNoPayTime(orderPaySuccessEntity.getOutTradeNoPayTime())
                .source(orderPaySuccessEntity.getSource())
                .channel(orderPaySuccessEntity.getChannel())
                .isComplete(notifyTaskEntity != null)
                .build();
    }

}
