package be.pxl.services.it;

import be.pxl.services.PostServiceApplication;
import be.pxl.services.domain.Post;
import be.pxl.services.domain.PostStatus;
import be.pxl.services.messaging.PostReviewedEvent;
import be.pxl.services.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDateTime;
import java.util.UUID;

import static be.pxl.services.messaging.MessagingNames.POST_REVIEWED_RK;
import static be.pxl.services.messaging.MessagingNames.REVIEW_EXCHANGE;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PostServiceApplication.class)
class PostReviewedListenerIT {

    private static final RabbitMQContainer rabbit =
            new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management"));

    static {
        rabbit.start();
    }

    // 3) Register dynamic properties using the *already started* container
    @DynamicPropertySource
    static void rabbitProps(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PostRepository postRepository;

    @Test
    void listener_updatesPostStatusOnApprovedEvent() throws Exception {
        // Arrange: create a post in DB with DRAFT status
        Post post = new Post();
        post.setTitle("Test");
        post.setContent("Content");
        post.setAuthor("alice");
        post.setStatus(PostStatus.DRAFT);
        post.setDateCreated(LocalDateTime.now());
        post = postRepository.save(post);

        UUID postId = post.getId();

        // Act: send APPROVED event via RabbitMQ
        PostReviewedEvent evt = new PostReviewedEvent(postId, "APPROVED");
        rabbitTemplate.convertAndSend(REVIEW_EXCHANGE, POST_REVIEWED_RK, evt);

        // Slep a bit for listener to process
        Thread.sleep(500);

        // Assert: post status changed to PUBLISHED
        Post updated = postRepository.findById(postId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PostStatus.PUBLISHED);
    }
}
