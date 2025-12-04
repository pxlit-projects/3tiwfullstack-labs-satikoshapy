package be.pxl.services.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static be.pxl.services.messaging.MessagingNames.POST_REVIEWED_RK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecisionPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private TopicExchange reviewExchange;

    @InjectMocks
    private DecisionPublisher decisionPublisher;

    @Test
    void publish_sendsEventToExchangeWithRoutingKey() {
        PostReviewedEvent event = new PostReviewedEvent(
                java.util.UUID.randomUUID(),
                "APPROVED"
        );

        when(reviewExchange.getName()).thenReturn("review.exchange");

        decisionPublisher.publish(event);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        verify(rabbitTemplate).convertAndSend(
                eq("review.exchange"),
                eq(POST_REVIEWED_RK),
                payloadCaptor.capture()
        );

        PostReviewedEvent sent = (PostReviewedEvent) payloadCaptor.getValue();
        assertEquals(event.postId(), sent.postId());
        assertEquals(event.decision(), sent.decision());
    }
}
