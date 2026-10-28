package groupbuy.market.plus.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 死信配置
 * 消费重试耗尽后被拒绝的消息，经死信交换机按「死信队列名」路由到各自的死信队列，便于排查与人工补偿
 * 死信交换机、死信队列、绑定关系由 RabbitAdmin 启动时自动声明
 */
@Configuration
public class RabbitMQDeadLetterConfig {

    @Value("${spring.rabbitmq.config.dead_letter.exchange}")
    private String deadLetterExchange;

    @Value("${spring.rabbitmq.config.consumer.topic_order_refund.dead_letter_queue}")
    private String orderRefundDeadLetterQueue;

    @Value("${spring.rabbitmq.config.consumer.topic_order_refund_success.dead_letter_queue}")
    private String orderRefundSuccessDeadLetterQueue;

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(deadLetterExchange, true, false);
    }

    @Bean
    public Queue orderRefundDeadLetterQueue() {
        return new Queue(orderRefundDeadLetterQueue, true);
    }

    @Bean
    public Queue orderRefundSuccessDeadLetterQueue() {
        return new Queue(orderRefundSuccessDeadLetterQueue, true);
    }

    @Bean
    public Binding orderRefundDeadLetterBinding() {
        return BindingBuilder.bind(orderRefundDeadLetterQueue()).to(deadLetterExchange()).with(orderRefundDeadLetterQueue);
    }

    @Bean
    public Binding orderRefundSuccessDeadLetterBinding() {
        return BindingBuilder.bind(orderRefundSuccessDeadLetterQueue()).to(deadLetterExchange()).with(orderRefundSuccessDeadLetterQueue);
    }

}
