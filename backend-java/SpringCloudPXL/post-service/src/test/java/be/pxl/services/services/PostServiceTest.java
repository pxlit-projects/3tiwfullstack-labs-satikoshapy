package be.pxl.services.services;

import be.pxl.services.client.ReviewClient;
import be.pxl.services.domain.Post;
import be.pxl.services.domain.PostStatus;
import be.pxl.services.exceptions.BadRequestException;
import be.pxl.services.exceptions.ResourceNotFoundException;
import be.pxl.services.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PostServiceTest {

    private PostRepository postRepository;
    private ReviewClient reviewClient;
    private PostService postService;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        reviewClient = mock(ReviewClient.class);
        postService = new PostService(postRepository, reviewClient);
    }

    private Post buildPost(UUID id, String author, PostStatus status) {
        Post p = new Post("title", "content");
        p.setId(id);
        p.setAuthor(author);
        p.setStatus(status);
        p.setDateCreated(LocalDateTime.now().minusDays(1));
        p.setDateUpdated(LocalDateTime.now().minusHours(1));
        return p;
    }

    // addPost

    @Test
    void addPost_savesPost() {
        Post p = new Post("title", "content");
        when(postRepository.save(p)).thenReturn(p);

        Post result = postService.addPost(p, "alice");

        assertSame(p, result);
        verify(postRepository).save(p);
    }

    // editPost

    @Test
    void editPost_happyPath_updatesTitleAndContent() {
        UUID id = UUID.randomUUID();
        Post existing = buildPost(id, "alice", PostStatus.DRAFT);
        when(postRepository.findById(id)).thenReturn(Optional.of(existing));

        Post request = new Post("new title", "new content");

        Post updated = buildPost(id, "alice", PostStatus.DRAFT);
        updated.setTitle("new title");
        updated.setContent("new content");

        when(postRepository.save(any(Post.class))).thenReturn(updated);

        Post result = postService.editPost(id, request, "alice");

        assertEquals("new title", result.getTitle());
        assertEquals("new content", result.getContent());
        verify(postRepository).save(existing);
    }

    @Test
    void editPost_postNotFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(postRepository.findById(id)).thenReturn(Optional.empty());

        Post request = new Post("t", "c");

        assertThrows(ResourceNotFoundException.class,
                () -> postService.editPost(id, request, "alice"));
    }

    @Test
    void editPost_wrongAuthor_throwsIllegalState() {
        UUID id = UUID.randomUUID();
        Post existing = buildPost(id, "alice", PostStatus.DRAFT);
        when(postRepository.findById(id)).thenReturn(Optional.of(existing));

        Post request = new Post("t", "c");

        assertThrows(IllegalStateException.class,
                () -> postService.editPost(id, request, "bob"));
    }

    @Test
    void editPost_statusNotDraftOrRejected_throwsIllegalState() {
        UUID id = UUID.randomUUID();
        Post existing = buildPost(id, "alice", PostStatus.PENDING_REVIEW);
        when(postRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class,
                () -> postService.editPost(id, new Post("t", "c"), "alice"));
    }

    @Test
    void editPost_statusPublished_throwsIllegalState() {
        UUID id = UUID.randomUUID();
        Post existing = buildPost(id, "alice", PostStatus.PUBLISHED);
        when(postRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class,
                () -> postService.editPost(id, new Post("t", "c"), "alice"));
    }

    // findPublishedPosts

    @Test
    void findPublishedPosts_convertsDatesToDateTime() {
        LocalDate from = LocalDate.of(2024, 1, 1);
        LocalDate to = LocalDate.of(2024, 1, 31);

        List<Post> list = List.of(buildPost(UUID.randomUUID(), "alice", PostStatus.PUBLISHED));
        when(postRepository.findByStatusAndFilters(eq(PostStatus.PUBLISHED), any(), any(), any(), any()))
                .thenReturn(list);

        List<Post> result =
                postService.findPublishedPosts("java", "alice", from, to);

        assertEquals(1, result.size());
        verify(postRepository).findByStatusAndFilters(
                eq(PostStatus.PUBLISHED),
                eq("java"),
                eq("alice"),
                eq(from.atStartOfDay()),
                eq(to.atStartOfDay())
        );
    }

    // getPostById

    @Test
    void getPostById_published_postVisibleToAnyone() {
        UUID id = UUID.randomUUID();
        Post existing = buildPost(id, "alice", PostStatus.PUBLISHED);
        when(postRepository.findById(id)).thenReturn(Optional.of(existing));

        Post result = postService.getPostById(id, "randomUser");

        assertSame(existing, result);
    }

    @Test
    void getPostById_unpublished_visibleToAuthor() {
        UUID id = UUID.randomUUID();
        Post existing = buildPost(id, "alice", PostStatus.DRAFT);
        when(postRepository.findById(id)).thenReturn(Optional.of(existing));

        Post result = postService.getPostById(id, "alice");

        assertSame(existing, result);
    }

    @Test
    void getPostById_unpublished_visibleToInternal() {
        UUID id = UUID.randomUUID();
        Post existing = buildPost(id, "alice", PostStatus.DRAFT);
        when(postRepository.findById(id)).thenReturn(Optional.of(existing));

        Post result = postService.getPostById(id, "internal");

        assertSame(existing, result);
    }

    @Test
    void getPostById_unpublished_notVisibleToOthers_throwsIllegalState() {
        UUID id = UUID.randomUUID();
        Post existing = buildPost(id, "alice", PostStatus.DRAFT);
        when(postRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class,
                () -> postService.getPostById(id, "bob"));
    }

    @Test
    void getPostById_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(postRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> postService.getPostById(id, "alice"));
    }

    // submitForReview

    @Test
    void submitForReview_happyPath_setsPendingReview_andCallsReviewClient() {
        UUID id = UUID.randomUUID();
        Post existing = buildPost(id, "alice", PostStatus.DRAFT);
        when(postRepository.findById(id)).thenReturn(Optional.of(existing));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post saved = postService.submitForReview(id, "alice");

        assertEquals(PostStatus.PENDING_REVIEW, saved.getStatus());
        verify(postRepository).save(existing);
        verify(reviewClient).submit(any(ReviewClient.SubmitReviewRequest.class));
    }

    @Test
    void submitForReview_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(postRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> postService.submitForReview(id, "alice"));
    }

    @Test
    void submitForReview_wrongAuthor_throwsIllegalState() {
        UUID id = UUID.randomUUID();
        Post existing = buildPost(id, "alice", PostStatus.DRAFT);
        when(postRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class,
                () -> postService.submitForReview(id, "bob"));
    }

    @Test
    void submitForReview_statusNotDraft_throwsBadRequest() {
        UUID id = UUID.randomUUID();
        Post existing = buildPost(id, "alice", PostStatus.PUBLISHED);
        when(postRepository.findById(id)).thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class,
                () -> postService.submitForReview(id, "alice"));
    }

    // updatePostStatus

    @Test
    void updatePostStatus_happyPath_updatesStatusAndSaves() {
        UUID id = UUID.randomUUID();
        Post existing = buildPost(id, "alice", PostStatus.DRAFT);
        when(postRepository.findById(id)).thenReturn(Optional.of(existing));

        postService.updatePostStatus(id, PostStatus.PUBLISHED);

        assertEquals(PostStatus.PUBLISHED, existing.getStatus());
        verify(postRepository).save(existing);
    }

    @Test
    void updatePostStatus_notFound_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(postRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> postService.updatePostStatus(id, PostStatus.PUBLISHED));
    }
}
