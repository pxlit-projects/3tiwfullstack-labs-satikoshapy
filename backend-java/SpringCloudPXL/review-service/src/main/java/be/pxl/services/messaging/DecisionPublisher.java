package be.pxl.services.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class DecisionPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange reviewExchange;

    public DecisionPublisher(RabbitTemplate rabbitTemplate, TopicExchange reviewExchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.reviewExchange = reviewExchange;
    }

    public void publish(PostReviewedEvent evt) {
        rabbitTemplate.convertAndSend(reviewExchange.getName(),
                RabbitConfig.POST_REVIEWED_RK, evt);
    }
}
