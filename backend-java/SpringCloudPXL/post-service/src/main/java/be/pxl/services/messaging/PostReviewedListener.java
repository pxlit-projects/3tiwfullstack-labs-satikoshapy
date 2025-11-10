package be.pxl.services.messaging;

import be.pxl.services.domain.PostStatus;
import be.pxl.services.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static be.pxl.services.messaging.MessagingNames.DECISIONS_QUEUE;

@Component
public class PostReviewedListener {
    private final PostRepository postRepository;

    private final Logger log = LoggerFactory.getLogger(PostReviewedListener.class);

    public PostReviewedListener(PostRepository posts) {
        this.postRepository = posts;
    }

    @RabbitListener(queues = DECISIONS_QUEUE)
    public void onReviewed(PostReviewedEvent evt) {
        log.info("Received decision: postId={}, decision={}", evt.postId(), evt.decision());

        postRepository.findById(evt.postId()).ifPresentOrElse(post -> {
            String decision = evt.decision() == null ? "" : evt.decision().toUpperCase();
            switch (decision) {
                case "APPROVED" -> post.setStatus(PostStatus.PUBLISHED);
                case "REJECTED" -> post.setStatus(PostStatus.REJECTED);
                default -> {
                    log.warn("Ignoring unknown decision '{}' for post {}", evt.decision(), evt.postId());
                    return;
                }
            }
            post.setDateUpdated(LocalDateTime.now());
            postRepository.save(post);
            log.info("Post {} updated to {}", post.getId(), post.getStatus());
        }, () -> log.warn("Post {} not found; dropping decision", evt.postId()));
    }
}