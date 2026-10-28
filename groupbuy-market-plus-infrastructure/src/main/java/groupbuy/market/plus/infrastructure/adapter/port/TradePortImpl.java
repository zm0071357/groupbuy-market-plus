package groupbuy.market.plus.infrastructure.adapter.port;

import groupbuy.market.plus.domain.trade.adapter.port.TradePort;
import groupbuy.market.plus.domain.trade.model.entity.NotifyTaskEntity;
import groupbuy.market.plus.domain.trade.model.valobj.NofifyStatusEnum;
import groupbuy.market.plus.domain.trade.model.valobj.NotifyTypeEnum;
import groupbuy.market.plus.infrastructure.event.EventPublisher;
import groupbuy.market.plus.infrastructure.gateway.NotifyTaskServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class TradePortImpl implements TradePort {

    @Resource
    private NotifyTaskServiceImpl notifyTaskServiceImpl;

    @Resource
    private EventPublisher eventPublisher;

    /**
     * 执行回调 - 只负责按回调类型发送通知
     * 防重复回调的加锁、状态二次校验与回调结果落库由调用方（TaskServiceImpl）在同一把锁内完成
     */
    @Override
    public String groupBuyNotify(NotifyTaskEntity notifyTaskEntity) throws Exception {
        try {
            // 根据不同回调类型进行回调
            if (notifyTaskEntity.getNotifyTypeEnum().equals(NotifyTypeEnum.HTTP)) {
                log.info("开始执行回调任务，回调类型：HTTP，组队ID：{}", notifyTaskEntity.getTeamId());
                return notifyTaskServiceImpl.notify(notifyTaskEntity.getNotifyUrl(), notifyTaskEntity.getParameterJson());
            } else {
                log.info("开始执行回调任务，回调类型：MQ，组队ID：{}", notifyTaskEntity.getTeamId());
                eventPublisher.publish(notifyTaskEntity.getNotifyMQ(), notifyTaskEntity.getParameterJson());
                return NofifyStatusEnum.SUCCESS.getStatus();
            }
        } catch (Exception e) {
            // 回调本身异常（如HTTP请求失败）不能设置中断标记，否则同批后续任务获取锁时会直接抛出中断异常
            log.error("回调任务执行异常，任务ID：{}", notifyTaskEntity.getTaskId(), e);
            return NofifyStatusEnum.FAIL.getStatus();
        }
    }

}
