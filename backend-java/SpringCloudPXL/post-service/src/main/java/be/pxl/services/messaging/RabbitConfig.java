package be.pxl.services.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static be.pxl.services.messaging.MessagingNames.*;


@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange reviewExchange() {
        return ExchangeBuilder.topicExchange(REVIEW_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue decisionsQueue() {
        return QueueBuilder.durable(DECISIONS_QUEUE).build();
    }

    @Bean
    public Binding decisionsBinding(TopicExchange reviewExchange, Queue decisionsQueue) {
        return BindingBuilder.bind(decisionsQueue).to(reviewExchange).with(POST_REVIEWED_RK);
    }

    @Bean
    public MessageConverter jackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // Optional, but ensures the listener uses the JSON converter
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        var f = new SimpleRabbitListenerContainerFactory();
        configurer.configure(f, connectionFactory);
        f.setMessageConverter(messageConverter);
        return f;
    }
}
