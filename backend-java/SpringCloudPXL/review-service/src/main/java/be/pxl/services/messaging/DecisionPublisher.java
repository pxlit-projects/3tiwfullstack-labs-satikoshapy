package be.pxl.services.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import static be.pxl.services.messaging.MessagingNames.POST_REVIEWED_RK;
import static be.pxl.services.messaging.MessagingNames.REVIEW_EXCHANGE;

@Component
public class DecisionPublisher {

    private final Logger log = LoggerFactory.getLogger(DecisionPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange reviewExchange;

    public DecisionPublisher(RabbitTemplate rabbitTemplate, TopicExchange reviewExchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.reviewExchange = reviewExchange;
    }

    public void publish(PostReviewedEvent evt) {
        log.info("Publishing review decision: postId={}, decision={}", evt.postId(), evt.decision());
        rabbitTemplate.convertAndSend(reviewExchange.getName(), POST_REVIEWED_RK, evt);
    }
}
