package be.pxl.services.messaging;


import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String REVIEW_EXCHANGE = "review.exchange";
    public static final String POST_REVIEWED_RK = "post.reviewed";
    @Bean
    TopicExchange reviewExchange(){ return new
            TopicExchange(REVIEW_EXCHANGE); }
}

