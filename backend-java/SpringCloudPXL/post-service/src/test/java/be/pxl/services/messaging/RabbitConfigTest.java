package be.pxl.services.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static be.pxl.services.messaging.MessagingNames.*;
import static org.assertj.core.api.Assertions.assertThat;

class RabbitConfigTest {

    private final RabbitConfig config = new RabbitConfig();

    @Test
    void reviewExchange_hasCorrectNameAndIsDurable() {
        TopicExchange exchange = config.reviewExchange();

        assertThat(exchange.getName()).isEqualTo(REVIEW_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
    }

    @Test
    void decisionsQueue_hasCorrectNameAndIsDurable() {
        Queue queue = config.decisionsQueue();

        assertThat(queue.getName()).isEqualTo(DECISIONS_QUEUE);
        assertThat(queue.isDurable()).isTrue();
    }

    @Test
    void decisionsBinding_bindsQueueToExchangeWithCorrectRoutingKey() {
        TopicExchange exchange = config.reviewExchange();
        Queue queue = config.decisionsQueue();

        Binding binding = config.decisionsBinding(exchange, queue);

        assertThat(binding.getExchange()).isEqualTo(REVIEW_EXCHANGE);
        assertThat(binding.getDestination()).isEqualTo(DECISIONS_QUEUE);
        assertThat(binding.getRoutingKey()).isEqualTo(POST_REVIEWED_RK);
    }

    @Test
    void jackson2MessageConverter_isJacksonConverter() {
        MessageConverter converter = config.jackson2MessageConverter();
        assertThat(converter).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
