package be.pxl.services.messaging;

import be.pxl.services.domain.Post;
import be.pxl.services.domain.PostStatus;
import be.pxl.services.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostReviewedListenerTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostReviewedListener listener;

    @Test
    void onReviewed_approved_updatesPostToPublished() {
        // Arrange
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setStatus(PostStatus.PENDING_REVIEW);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        PostReviewedEvent event = new PostReviewedEvent(postId, "APPROVED");

        // Act
        listener.onReviewed(event);

        // Assert
        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());

        Post saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(PostStatus.PUBLISHED);
        assertThat(saved.getDateUpdated()).isNotNull();
    }

    @Test
    void onReviewed_rejected_updatesPostToRejected() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setStatus(PostStatus.PENDING_REVIEW);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        PostReviewedEvent event = new PostReviewedEvent(postId, "REJECTED");

        listener.onReviewed(event);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        Post saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(PostStatus.REJECTED);
    }

    @Test
    void onReviewed_unknownDecision_doesNotSave() {
        UUID postId = UUID.randomUUID();
        Post post = new Post();
        post.setId(postId);
        post.setStatus(PostStatus.PENDING_REVIEW);

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        PostReviewedEvent event = new PostReviewedEvent(postId, "SOMETHING_ELSE");

        listener.onReviewed(event);

        verify(postRepository, never()).save(any());
    }

    @Test
    void onReviewed_postNotFound_logsAndDoesNothing() {
        UUID postId = UUID.randomUUID();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        PostReviewedEvent event = new PostReviewedEvent(postId, "APPROVED");

        listener.onReviewed(event);

        verify(postRepository, never()).save(any());
    }
}
