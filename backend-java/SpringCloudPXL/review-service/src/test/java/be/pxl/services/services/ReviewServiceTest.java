package be.pxl.services.services;

import be.pxl.services.client.PostServiceClient;
import be.pxl.services.domain.Review;
import be.pxl.services.domain.ReviewStatus;
import be.pxl.services.domain.dtos.PostResponse;
import be.pxl.services.domain.dtos.SubmitReviewRequest;
import be.pxl.services.exceptions.ResourceNotFoundException;
import be.pxl.services.messaging.DecisionPublisher;
import be.pxl.services.messaging.PostReviewedEvent;
import be.pxl.services.repository.ReviewRepository;
import be.pxl.services.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private PostServiceClient postServiceClient;

    @Mock
    private DecisionPublisher decisionPublisher;

    @InjectMocks
    private ReviewService reviewService;

    private UUID postId;
    private String reviewerId;

    @BeforeEach
    void setUp() {
        postId = UUID.randomUUID();
        reviewerId = "editor_mock_id";
    }

    @Test
    void submit_logsOnly_doesNotThrow() {
        SubmitReviewRequest req = new SubmitReviewRequest(postId, "author", "title");

        assertDoesNotThrow(() -> reviewService.submit(req));

        // no repository or publisher interaction
        verifyNoInteractions(reviewRepository, decisionPublisher);
    }

    @Test
    void approvePost_happyPath_savesReviewAndPublishesEvent() {
        // Arrange
        PostResponse dummyPost = mock(PostResponse.class);
        when(postServiceClient.getPostById(eq(postId), eq("internal"))).thenReturn(dummyPost);

        Review existing = new Review();
        existing.setStatus(ReviewStatus.PENDING);
        when(reviewRepository.findByPostId(postId)).thenReturn(Optional.of(existing));

        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Review.class));

        ArgumentCaptor<PostReviewedEvent> eventCaptor = ArgumentCaptor.forClass(PostReviewedEvent.class);

        // Act
        Review result = reviewService.approvePost(postId, reviewerId);

        // Assert
        assertEquals(postId, result.getPostId());
        assertEquals(reviewerId, result.getReviewerId());
        assertEquals(ReviewStatus.APPROVED, result.getStatus());

        verify(reviewRepository).save(result);
        verify(decisionPublisher).publish(eventCaptor.capture());

        PostReviewedEvent evt = eventCaptor.getValue();
        assertEquals(postId, evt.postId());
        assertEquals("APPROVED", evt.decision());
    }

    @Test
    void approvePost_nonPendingReview_throwsIllegalState() {
        PostResponse dummyPost = mock(PostResponse.class);
        when(postServiceClient.getPostById(eq(postId), eq("internal"))).thenReturn(dummyPost);

        Review existing = new Review();
        existing.setStatus(ReviewStatus.APPROVED); // already approved
        when(reviewRepository.findByPostId(postId)).thenReturn(Optional.of(existing));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> reviewService.approvePost(postId, reviewerId)
        );

        assertTrue(ex.getMessage().contains("already"));

        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(decisionPublisher);
    }

    @Test
    void approvePost_postNotFoundInPostService_throwsResourceNotFound() {
        when(postServiceClient.getPostById(eq(postId), eq("internal")))
                .thenThrow(new RuntimeException("PostService down"));

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> reviewService.approvePost(postId, reviewerId)
        );

        assertTrue(ex.getMessage().contains(postId.toString()));
        verifyNoInteractions(reviewRepository, decisionPublisher);
    }

    @Test
    void rejectPost_happyPath_savesReviewAndPublishesEvent() {
        // Arrange
        PostResponse dummyPost = mock(PostResponse.class);
        when(postServiceClient.getPostById(eq(postId), eq("internal"))).thenReturn(dummyPost);

        Review existing = new Review();
        existing.setStatus(ReviewStatus.PENDING);
        when(reviewRepository.findByPostId(postId)).thenReturn(Optional.of(existing));

        Review request = new Review();
        request.setRejectionComment("Too many typos");

        when(reviewRepository.save(any(Review.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Review.class));

        ArgumentCaptor<PostReviewedEvent> eventCaptor = ArgumentCaptor.forClass(PostReviewedEvent.class);

        // Act
        Review result = reviewService.rejectPost(postId, reviewerId, request);

        // Assert
        assertEquals(postId, result.getPostId());
        assertEquals(reviewerId, result.getReviewerId());
        assertEquals(ReviewStatus.REJECTED, result.getStatus());
        assertEquals("Too many typos", result.getRejectionComment());

        verify(reviewRepository).save(result);
        verify(decisionPublisher).publish(eventCaptor.capture());

        PostReviewedEvent evt = eventCaptor.getValue();
        assertEquals(postId, evt.postId());
        assertEquals("REJECTED", evt.decision());
    }

    @Test
    void rejectPost_withoutComment_throwsIllegalState() {
        PostResponse dummyPost = mock(PostResponse.class);
        when(postServiceClient.getPostById(eq(postId), eq("internal"))).thenReturn(dummyPost);

        Review existing = new Review();
        existing.setStatus(ReviewStatus.PENDING);
        when(reviewRepository.findByPostId(postId)).thenReturn(Optional.of(existing));

        Review request = new Review(); // no comment

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> reviewService.rejectPost(postId, reviewerId, request)
        );

        assertTrue(ex.getMessage().contains("requires a comment"));

        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(decisionPublisher);
    }

    @Test
    void rejectPost_nonPendingReview_throwsIllegalState() {
        PostResponse dummyPost = mock(PostResponse.class);
        when(postServiceClient.getPostById(eq(postId), eq("internal"))).thenReturn(dummyPost);

        Review existing = new Review();
        existing.setStatus(ReviewStatus.APPROVED);
        when(reviewRepository.findByPostId(postId)).thenReturn(Optional.of(existing));

        Review request = new Review();
        request.setRejectionComment("Bad post");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> reviewService.rejectPost(postId, reviewerId, request)
        );

        assertTrue(ex.getMessage().contains("already"));

        verify(reviewRepository, never()).save(any());
        verifyNoInteractions(decisionPublisher);
    }

    @Test
    void rejectPost_postNotFoundInPostService_throwsResourceNotFound() {
        when(postServiceClient.getPostById(eq(postId), eq("internal")))
                .thenThrow(new RuntimeException("Post not found"));

        Review request = new Review();
        request.setRejectionComment("Irrelevant");

        assertThrows(
                ResourceNotFoundException.class,
                () -> reviewService.rejectPost(postId, reviewerId, request)
        );

        verifyNoInteractions(reviewRepository, decisionPublisher);
    }
}
