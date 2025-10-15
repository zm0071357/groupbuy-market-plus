package groupbuy.market.plus.infrastructure.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 生产者 - 发送订单支付完成消息到队列里
 * 不需要知道消息会被哪些队列接收，也不需要知道有多少消费者，因此交换机、队列、绑定的配置交给消费者
 */
@Slf4j
@Component
public class EventPublisher {

    @Value("${spring.rabbitmq.config.producer.exchange}")
    private String exchangeName;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息
     * 消费者通过路由键绑定了交换机和队列的关系，那么只需要传递交换机和路由键，交换机就能根据路由键将消息投递到对应的队列里面，等待消费者进行消费
     * @param routingKey 路由Key
     * @param message 消息
     */
    public void publish(String routingKey, String message) {
        try {
            log.info("拼团营销服务 - 发送MQ消息，交换机：{}，路由键：{}，消息：{}", exchangeName, routingKey, message);
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message, m -> {
                m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return m;
            });
        } catch (Exception e) {
            log.info("拼团营销服务 - 发送MQ消息失败，交换机：{}，路由键：{}，消息：{}", exchangeName, routingKey, message);
            throw e;
        }
    }
}
