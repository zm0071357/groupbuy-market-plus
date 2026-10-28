package groupbuy.market.plus.test.unit.mq;

import groupbuy.market.plus.config.RabbitMQDeadLetterConfig;
import groupbuy.market.plus.domain.trade.adapter.repository.TradeRepository;
import groupbuy.market.plus.trigger.listener.OrderRefundListener;
import groupbuy.market.plus.trigger.listener.OrderRefundSuccessListener;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.Argument;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * MQ 消费重试与死信单元测试
 */
public class DeadLetterUnitTest {

    @Test
    public void test_listener_retryAndNoRequeue() throws Exception {
        StandardEnvironment environment = loadYml();
        Binder binder = Binder.get(environment);

        Assert.assertEquals(Boolean.FALSE, binder.bind("spring.rabbitmq.listener.simple.default-requeue-rejected", Boolean.class).get());
        Assert.assertEquals(Boolean.TRUE, binder.bind("spring.rabbitmq.listener.simple.retry.enabled", Boolean.class).get());
        Assert.assertEquals(Integer.valueOf(3), binder.bind("spring.rabbitmq.listener.simple.retry.max-attempts", Integer.class).get());
    }

    @Test
    public void test_consumerQueues_deadLetterArguments() throws Exception {
        StandardEnvironment environment = loadYml();
        assertDeadLetter(environment, OrderRefundListener.class, "groupbuy_market_plus_topic_order_refund_dlq");
        assertDeadLetter(environment, OrderRefundSuccessListener.class, "groupbuy_market_plus_order_refund_success_dlq");
    }

    @Test
    public void test_deadLetterExchangeAndQueues_declared() {
        RabbitMQDeadLetterConfig config = new RabbitMQDeadLetterConfig();
        ReflectionTestUtils.setField(config, "deadLetterExchange", "groupbuy_market_plus_dlx_exchange");
        ReflectionTestUtils.setField(config, "orderRefundDeadLetterQueue", "groupbuy_market_plus_topic_order_refund_dlq");
        ReflectionTestUtils.setField(config, "orderRefundSuccessDeadLetterQueue", "groupbuy_market_plus_order_refund_success_dlq");

        DirectExchange exchange = config.deadLetterExchange();
        Queue queue = config.orderRefundDeadLetterQueue();
        Binding binding = config.orderRefundDeadLetterBinding();
        Assert.assertTrue(exchange.isDurable());
        Assert.assertTrue(queue.isDurable());
        Assert.assertEquals("groupbuy_market_plus_dlx_exchange", binding.getExchange());
        Assert.assertEquals("groupbuy_market_plus_topic_order_refund_dlq", binding.getDestination());
        Assert.assertEquals("groupbuy_market_plus_topic_order_refund_dlq", binding.getRoutingKey());
        Assert.assertEquals("groupbuy_market_plus_order_refund_success_dlq", config.orderRefundSuccessDeadLetterBinding().getRoutingKey());
    }

    @Test
    public void test_refundSuccess_unmatchedOrder_noException() throws Exception {
        TradeRepository tradeRepository = mock(TradeRepository.class);
        when(tradeRepository.updateRefundNoAndRefundTime(anyString(), anyString(), anyString(), any(Date.class))).thenReturn(false);
        OrderRefundSuccessListener listener = new OrderRefundSuccessListener();
        ReflectionTestUtils.setField(listener, "tradeRepository", tradeRepository);

        // 非拼团订单的退款消息：正常消费，不抛异常
        listener.listener("{\"userId\":\"user01\",\"payOrderId\":\"OUT0001\",\"refundOrderId\":\"R0001\",\"refundTime\":\"2026-10-28 10:00:00\"}");
    }

    private void assertDeadLetter(StandardEnvironment environment, Class<?> listenerClass, String expectedRoutingKey) throws Exception {
        RabbitListener rabbitListener = listenerClass.getMethod("listener", String.class).getAnnotation(RabbitListener.class);
        Map<String, String> arguments = new HashMap<>();
        for (Argument argument : rabbitListener.bindings()[0].value().arguments()) {
            arguments.put(argument.name(), environment.resolveRequiredPlaceholders(argument.value()));
        }
        Assert.assertEquals("groupbuy_market_plus_dlx_exchange", arguments.get("x-dead-letter-exchange"));
        Assert.assertEquals(expectedRoutingKey, arguments.get("x-dead-letter-routing-key"));
    }

    private StandardEnvironment loadYml() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load("application-dev", new ClassPathResource("application-dev.yml"));
        sources.forEach(source -> environment.getPropertySources().addLast(source));
        return environment;
    }

}
