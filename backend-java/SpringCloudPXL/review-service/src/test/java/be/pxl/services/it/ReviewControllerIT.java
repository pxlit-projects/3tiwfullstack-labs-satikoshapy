package be.pxl.services.it;

import be.pxl.services.ReviewServiceApplication;
import be.pxl.services.client.PostServiceClient;
import be.pxl.services.domain.Review;
import be.pxl.services.domain.ReviewStatus;
import be.pxl.services.domain.dtos.PostResponse;
import be.pxl.services.domain.dtos.ReviewRequest;
import be.pxl.services.exceptions.GlobalExceptionHandler;
import be.pxl.services.messaging.DecisionPublisher;
import be.pxl.services.repository.ReviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ReviewServiceApplication.class)
@AutoConfigureMockMvc
@Import(GlobalExceptionHandler.class)
class ReviewControllerIT {

    private static final MySQLContainer<?> MYSQL_CONTAINER;

    static {
        MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.3.0")
                .withDatabaseName("reviewdb")
                .withUsername("test")
                .withPassword("test");
        MYSQL_CONTAINER.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // external collaborators are mocked
    @MockBean
    private PostServiceClient postServiceClient;

    @MockBean
    private DecisionPublisher decisionPublisher;

    @BeforeEach
    void cleanDb() {
        reviewRepository.deleteAll();
    }

    @Test
    void approvePost_persistsReviewAndPublishesEvent_IT() throws Exception {
        // Arrange
        UUID postId = UUID.randomUUID();

        // There must be an existing PENDING review, otherwise ervice throws
        Review pending = new Review();
        pending.setPostId(postId);
        pending.setStatus(ReviewStatus.PENDING);
        pending.setReviewerId("someone");
        reviewRepository.save(pending);

        // Stub PostServiceClient – only needs to return sometihng, content not used
        PostResponse stubPost = mock(PostResponse.class);
        when(postServiceClient.getPostById(eq(postId), eq("internal")))
                .thenReturn(stubPost);

        // Act
        mockMvc.perform(post("/api/reviews/{postId}/approve", postId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").value(postId.toString()))
                .andExpect(jsonPath("$.reviewerId").value("editor_mock_id"))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Assert DB
        Review saved = reviewRepository.findByPostId(postId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(saved.getReviewerId()).isEqualTo("editor_mock_id");

        // Assert messaging
        verify(decisionPublisher).publish(argThat(evt ->
                evt.postId().equals(postId) && "APPROVED".equals(evt.decision())
        ));
    }

    @Test
    void rejectPost_persistsReviewWithCommentAndPublishesEvent_IT() throws Exception {
        // Arrange
        UUID postId = UUID.randomUUID();

        Review pending = new Review();
        pending.setPostId(postId);
        pending.setStatus(ReviewStatus.PENDING);
        pending.setReviewerId("someone");
        reviewRepository.save(pending);

        PostResponse stubPost = mock(PostResponse.class);
        when(postServiceClient.getPostById(eq(postId), eq("internal")))
                .thenReturn(stubPost);

        ReviewRequest request = new ReviewRequest("Too many typos");

        // Act
        mockMvc.perform(post("/api/reviews/{postId}/reject", postId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").value(postId.toString()))
                .andExpect(jsonPath("$.reviewerId").value("editor_mock_id"))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionComment").value("Too many typos"));

        // Assert DB
        Review saved = reviewRepository.findByPostId(postId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReviewStatus.REJECTED);
        assertThat(saved.getRejectionComment()).isEqualTo("Too many typos");

        // Assert messaging
        verify(decisionPublisher).publish(argThat(evt ->
                evt.postId().equals(postId) && "REJECTED".equals(evt.decision())
        ));
    }

    @Test
    void approvePost_postServiceFails_returns404AndNoReviewChanged_IT() throws Exception {
        // Arrange
        UUID postId = UUID.randomUUID();

        Review pending = new Review();
        pending.setPostId(postId);
        pending.setStatus(ReviewStatus.PENDING);
        pending.setReviewerId("someone");
        reviewRepository.save(pending);

        when(postServiceClient.getPostById(eq(postId), eq("internal")))
                .thenThrow(new RuntimeException("Post service down"));

        // Act + Assert
        mockMvc.perform(post("/api/reviews/{postId}/approve", postId.toString()))
                .andExpect(status().isNotFound());

        // DB should still have pending review, unchanged
        Review saved = reviewRepository.findByPostId(postId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReviewStatus.PENDING);

        // No event sent
        verifyNoInteractions(decisionPublisher);
    }
}
