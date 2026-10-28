package groupbuy.market.plus.trigger.listener;

import com.alibaba.fastjson.JSON;
import groupbuy.market.plus.domain.trade.adapter.event.OrderRefundSuccessMessage;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 消费者 - 接收订单退款完成队列的消息进行消费
 */
@Slf4j
@Component
public class OrderRefundSuccessListener {

    @Resource
    private TradeRepository tradeRepository;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "${spring.rabbitmq.config.consumer.topic_order_refund_success.queue}", arguments = {
                            // 重试耗尽后转入死信队列，便于人工补偿
                            @Argument(name = "x-dead-letter-exchange", value = "${spring.rabbitmq.config.dead_letter.exchange}"),
                            @Argument(name = "x-dead-letter-routing-key", value = "${spring.rabbitmq.config.consumer.topic_order_refund_success.dead_letter_queue}")
                    }),
                    exchange = @Exchange(value = "${spring.rabbitmq.config.consumer.topic_order_refund_success.exchange}", type = ExchangeTypes.TOPIC),
                    key = "${spring.rabbitmq.config.consumer.topic_order_refund_success.routing_key}"
            )
    )
    public void listener(String message) throws Exception {
        try {
            log.info("拼团营销服务 - 监听订单退款完成队列 - 接收到消息：{}，进行处理", message);
            // 反序列化
            OrderRefundSuccessMessage orderRefundSuccessMessage = JSON.parseObject(message, OrderRefundSuccessMessage.class);
            // 更新订单的退款外部单号和退款完成时间
            boolean updated = tradeRepository.updateRefundNoAndRefundTime(orderRefundSuccessMessage.getUserId(), orderRefundSuccessMessage.getPayOrderId(),
                    orderRefundSuccessMessage.getRefundOrderId(), orderRefundSuccessMessage.getRefundTime());
            // 退款消息来自外部支付平台，可能不属于拼团订单 - 匹配不到订单时正常消费，不抛异常，避免无效重试
            if (!updated) {
                log.warn("拼团营销服务 - 监听订单退款完成队列 - 未匹配到拼团订单，忽略消息：{}", message);
            }
        } catch (Exception e) {
            log.error("拼团营销服务 - 监听订单退款完成队列 - 消息处理失败：{}", message, e);
            throw e;
        }
    }
}
