package groupbuy.market.plus.test.unit.mq;

import groupbuy.market.plus.infrastructure.event.EventPublisher;
import org.junit.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class EventPublisherUnitTest {

    @Test
    public void enablesCorrelatedConfirmsAndReturns() throws Exception {
        Binder binder = new Binder(ConfigurationPropertySources.from(
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))));
        assertEquals("correlated", binder.bind("spring.rabbitmq.publisher-confirm-type", String.class).get());
        assertTrue(binder.bind("spring.rabbitmq.publisher-returns", Boolean.class).get());
        assertTrue(binder.bind("spring.rabbitmq.template.mandatory", Boolean.class).get());
    }

    @Test
    public void acknowledgedAndRoutedMessageSucceeds() throws Exception {
        assertTrue(publish(true, false));
    }

    @Test
    public void returnedMessageFailsEvenWithAnAck() throws Exception {
        assertFalse(publish(true, true));
    }

    @Test
    public void brokerNackFails() throws Exception {
        assertFalse(publish(false, false));
    }

    private boolean publish(boolean ack, boolean returned) throws Exception {
        EventPublisher publisher = new EventPublisher();
        RabbitTemplate template = mock(RabbitTemplate.class);
        ReflectionTestUtils.setField(publisher, "rabbitTemplate", template);
        ReflectionTestUtils.setField(publisher, "exchangeName", "trade");
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(4);
            MessagePostProcessor postProcessor = invocation.getArgument(3);
            Message message = postProcessor.postProcessMessage(new Message(new byte[0]));
            assertEquals("team_task", message.getMessageProperties().getMessageId());
            if (returned) {
                correlation.setReturned(new ReturnedMessage(message, 312, "NO_ROUTE", "trade", "completed"));
            }
            correlation.getFuture().set(new CorrelationData.Confirm(ack, null));
            return null;
        }).when(template).convertAndSend(eq("trade"), eq("completed"), eq("{}"),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        return publisher.publish("team_task", "completed", "{}");
    }
}
