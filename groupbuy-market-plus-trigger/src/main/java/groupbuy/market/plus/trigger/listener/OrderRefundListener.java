package groupbuy.market.plus.trigger.listener;

import com.alibaba.fastjson.JSON;
import groupbuy.market.plus.domain.trade.adapter.event.OrderRefundMessage;
import groupbuy.market.plus.domain.trade.service.refund.RefundOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 消费者 - 接收订单退单队列的消息进行消费
 */
@Slf4j
@Component
public class OrderRefundListener {

    @Resource
    private RefundOrderService refundOrderService;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "${spring.rabbitmq.config.producer.topic_order_refund.queue}"),
                    exchange = @Exchange(value = "${spring.rabbitmq.config.producer.exchange}", type = ExchangeTypes.TOPIC),
                    key = "${spring.rabbitmq.config.producer.topic_order_refund.routing_key}"
            )
    )
    public void listener(String message) {
        log.info("监听订单退单队列 - 接收到消息：{}，进行处理", message);
        OrderRefundMessage orderRefundMessage = JSON.parseObject(message, OrderRefundMessage.class);
        try {
            // 恢复锁单量
            refundOrderService.recoverTeamLockStock(orderRefundMessage);
        } catch (Exception e) {
            log.info("监听订单退单队列 - 消息处理失败：{}", message, e);
            // 抛异常，MQ会重试
            throw new RuntimeException(e);
        }
    }
}
