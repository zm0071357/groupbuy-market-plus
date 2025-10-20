package groupbuy.market.plus.domain.trade.service.refund.strategy;

import com.alibaba.fastjson.JSON;
import groupbuy.market.plus.domain.trade.adapter.event.OrderRefundMessage;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.domain.trade.model.entity.GroupBuyTeamEntity;
import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;
import groupbuy.market.plus.domain.trade.service.lock.factory.LockOrderLinkFactory;
import groupbuy.market.plus.domain.trade.service.task.TaskService;
import groupbuy.market.plus.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 退单抽象类
 */
@Slf4j
@Service
public abstract class AbstractRefundService implements RefundService {

    @Resource
    protected TradeRepository tradeRepository;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private TaskService taskService;

    /**
     * 通用发送退单MQ消息
     * @param notifyTaskEntity 回调任务
     * @param refundType 退单类型
     */
    protected void sendRefundNotifyMQMessage(NotifyTaskEntity notifyTaskEntity, String refundType) {
        if (null != notifyTaskEntity) {
            threadPoolExecutor.execute(() -> {
                Map<String, Integer> notifyResultMap = null;
                try {
                    notifyResultMap = taskService.execNotifyJob(notifyTaskEntity);
                    log.info("退单回调通知 - 退单类型：{}，结果:{}", refundType, JSON.toJSONString(notifyResultMap));
                } catch (Exception e) {
                    log.error("退单回调通知失败 - 退单类型：{}，结果:{}", refundType, JSON.toJSONString(notifyResultMap), e);
                    throw new AppException(e.getMessage());
                }
            });
        }
    }

    /**
     * 通用库存恢复
     * @param orderRefundMessage 订单退单消息
     * @param refundType 退单类型
     * @throws Exception
     */
    protected void doReverseStock(OrderRefundMessage orderRefundMessage, String refundType) throws Exception {
        log.info("恢复拼团组队库存 - 退单类型：{}，用户ID：{}，活动ID：{}，拼团组队ID：{}", refundType, orderRefundMessage.getUserId(), orderRefundMessage.getActivityId(), orderRefundMessage.getTeamId());
        // 恢复库存key
        String recoveryTeamStockKey = LockOrderLinkFactory.getTeamStockRecoverKey(orderRefundMessage.getActivityId(), orderRefundMessage.getTeamId());
        // 恢复库存
        tradeRepository.recoverTeamStock(recoveryTeamStockKey);
    }

    @Override
    public void newHeaderNotify(String newLeaderUserId, String orderId, GroupBuyTeamEntity groupBuyTeamEntity) {
        // 回调任务
        NotifyTaskEntity notifyTaskEntity = tradeRepository.newHeaderNotify(newLeaderUserId, orderId, groupBuyTeamEntity);
        if (null != notifyTaskEntity) {
            threadPoolExecutor.execute(() -> {
                Map<String, Integer> notifyResultMap = null;
                try {
                    notifyResultMap = taskService.execNotifyJob(notifyTaskEntity);
                    log.info("新团长回调通知 - 新团长ID：{}，结果:{}", newLeaderUserId, JSON.toJSONString(notifyResultMap));
                } catch (Exception e) {
                    log.error("新团长回调通知失败 - 新团长ID：{}，结果:{}", newLeaderUserId, JSON.toJSONString(notifyResultMap), e);
                    throw new AppException(e.getMessage());
                }
            });
        }
    }

}
