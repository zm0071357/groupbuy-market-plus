package groupbuy.market.plus.infrastructure.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 发布交易回调消息并等待 RabbitMQ 确认。
 */
@Slf4j
@Component
public class EventPublisher {

    @Value("${spring.rabbitmq.config.producer.exchange}")
    private String exchangeName;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /** 按任务标识发布持久化消息，确认已路由后返回成功。 */
    public boolean publish(String taskId, String routingKey, String message)
            throws InterruptedException, ExecutionException, TimeoutException {
        CorrelationData correlation = new CorrelationData();
        rabbitTemplate.convertAndSend(exchangeName, routingKey, message, outgoing -> {
            outgoing.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            outgoing.getMessageProperties().setMessageId(taskId);
            return outgoing;
        }, correlation);

        // 不可路由的消息也可能收到 ACK，关联返回先于发布确认完成。
        CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
        boolean delivered = confirm.isAck() && correlation.getReturned() == null;
        if (!delivered) {
            log.warn("MQ 通知未投递，任务ID：{}，路由键：{}，确认原因：{}，是否退回：{}",
                    taskId, routingKey, confirm.getReason(), correlation.getReturned() != null);
        }
        return delivered;
    }
}
