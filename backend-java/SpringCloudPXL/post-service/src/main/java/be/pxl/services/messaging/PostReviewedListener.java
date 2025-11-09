package be.pxl.services.messaging;

import be.pxl.services.domain.PostStatus;
import be.pxl.services.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PostReviewedListener {
    private final PostRepository postRepository;

    private final Logger log = LoggerFactory.getLogger(PostReviewedListener.class);

    public PostReviewedListener(PostRepository posts) {
        this.postRepository = posts;
    }

    @RabbitListener(queues = RabbitConfig.DECISIONS_QUEUE)
    public void onReviewed(@Payload PostReviewedEvent evt) {
        log.info("Received review decision for {}: {}", evt.postId(), evt.decision());
        postRepository.findById(evt.postId()).ifPresent(post -> {
            switch (evt.decision().toUpperCase()) {
                case "APPROVED" -> post.setStatus(PostStatus.PUBLISHED);
                case "REJECTED" -> post.setStatus(PostStatus.REJECTED);
                default -> { return; }
            }
            post.setDateUpdated(LocalDateTime.now());
            postRepository.save(post);
        });
    }
}