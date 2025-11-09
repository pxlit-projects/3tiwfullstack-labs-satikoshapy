package be.pxl.services.messaging;


import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitConfig {
    public static final String REVIEW_EXCHANGE = "review.exchange";
    public static final String POST_REVIEWED_RK = "post.reviewed";
    public static final String DECISIONS_QUEUE = "review.decisions";


    @Bean TopicExchange reviewExchange() { return new TopicExchange(REVIEW_EXCHANGE); }
    @Bean Queue decisionsQueue() { return QueueBuilder.durable(DECISIONS_QUEUE).build(); }
    @Bean Binding decisionsBinding() {
        return BindingBuilder.bind(decisionsQueue()).to(reviewExchange()).with(POST_REVIEWED_RK);
    }
}