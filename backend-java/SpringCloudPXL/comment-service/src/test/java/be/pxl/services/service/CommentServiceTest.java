package be.pxl.services.service;

import be.pxl.services.client.PostServiceClient;
import be.pxl.services.domain.Comment;
import be.pxl.services.domain.dtos.CommentResponse;
import be.pxl.services.domain.dtos.CreateCommentRequest;
import be.pxl.services.exceptions.ResourceNotFoundException;
import be.pxl.services.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostServiceClient postServiceClient;

    @InjectMocks
    private CommentService commentService;

    private UUID postId;
    private UUID commentId;

    @BeforeEach
    void setUp() {
        postId = UUID.randomUUID();
        commentId = UUID.randomUUID();
    }

    @Test
    void addComment_savesCommentWithAuthorPostIdAndTrimmedContent() {
        String user = "alice";
        CreateCommentRequest req = new CreateCommentRequest("  hello world  ");

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);

        Comment saved = new Comment();
        saved.setId(commentId);
        saved.setPostId(postId);
        saved.setAuthor(user);
        saved.setContent("hello world");
        saved.setCreatedAt(LocalDateTime.now());

        when(commentRepository.save(any(Comment.class))).thenReturn(saved);

        CommentResponse response = commentService.addComment(postId, user, req);

        verify(commentRepository).save(captor.capture());
        Comment toSave = captor.getValue();
        assertEquals(postId, toSave.getPostId());
        assertEquals(user, toSave.getAuthor());
        assertEquals("hello world", toSave.getContent());
        assertNotNull(toSave.getCreatedAt());

        assertEquals(commentId, response.id());
        assertEquals(postId, response.postId());
        assertEquals("hello world", response.content());
    }

    @Test
    void getAllCommentsForPost_callsPostVisibilityCheckAndReturnsMappedComments() {
        String user = "bob";

        // post is visible -> getPostById succeeds
        when(postServiceClient.getPostById(eq(postId), eq("internal"))).thenReturn(null);

        Comment c1 = new Comment();
        c1.setId(UUID.randomUUID());
        c1.setPostId(postId);
        c1.setAuthor("alice");
        c1.setContent("first");
        c1.setCreatedAt(LocalDateTime.now().minusMinutes(2));

        Comment c2 = new Comment();
        c2.setId(UUID.randomUUID());
        c2.setPostId(postId);
        c2.setAuthor("bob");
        c2.setContent("second");
        c2.setCreatedAt(LocalDateTime.now());

        when(commentRepository.findByPostIdOrderByCreatedAtAsc(postId))
                .thenReturn(List.of(c1, c2));

        List<CommentResponse> responses = commentService.getAllCommentsForPost(postId, user);

        verify(postServiceClient).getPostById(postId, "internal");
        verify(commentRepository).findByPostIdOrderByCreatedAtAsc(postId);

        assertEquals(2, responses.size());
        assertEquals("first", responses.get(0).content());
        assertEquals("second", responses.get(1).content());
    }

    @Test
    void getAllCommentsForPost_postNotVisible_throwsResourceNotFound() {
        when(postServiceClient.getPostById(eq(postId), eq("internal")))
                .thenThrow(new RuntimeException("Post not visible"));

        assertThrows(ResourceNotFoundException.class,
                () -> commentService.getAllCommentsForPost(postId, "any"));

        verify(commentRepository, never()).findByPostIdOrderByCreatedAtAsc(any());
    }

    @Test
    void deleteComment_authorCanDelete() {
        Comment c = new Comment();
        c.setId(commentId);
        c.setAuthor("alice");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(c));

        commentService.deleteComment(commentId, "alice");

        verify(commentRepository).delete(c);
    }

    @Test
    void deleteComment_internalCanDelete() {
        Comment c = new Comment();
        c.setId(commentId);
        c.setAuthor("alice");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(c));

        commentService.deleteComment(commentId, "internal");

        verify(commentRepository).delete(c);
    }

    @Test
    void deleteComment_notAuthorAndNotInternal_throwsIllegalState() {
        Comment c = new Comment();
        c.setId(commentId);
        c.setAuthor("alice");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(c));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> commentService.deleteComment(commentId, "bob")
        );

        assertTrue(ex.getMessage().contains("not allowed"));
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void deleteComment_commentNotFound_throwsResourceNotFound() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> commentService.deleteComment(commentId, "alice"));

        verify(commentRepository, never()).delete(any());
    }

    @Test
    void editComment_authorCanEdit() {
        Comment existing = new Comment();
        existing.setId(commentId);
        existing.setAuthor("alice");
        existing.setContent("old");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existing));
        when(commentRepository.save(any(Comment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Comment.class));

        CreateCommentRequest req = new CreateCommentRequest("  new content  ");

        CommentResponse response = commentService.editComment(commentId, "alice", req);

        assertEquals("new content", response.content());
        verify(commentRepository).save(existing);
    }

    @Test
    void editComment_internalCanEdit() {
        Comment existing = new Comment();
        existing.setId(commentId);
        existing.setAuthor("alice");
        existing.setContent("old");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existing));
        when(commentRepository.save(any(Comment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Comment.class));

        CreateCommentRequest req = new CreateCommentRequest("updated by internal");

        CommentResponse response = commentService.editComment(commentId, "internal", req);

        assertEquals("updated by internal", response.content());
        verify(commentRepository).save(existing);
    }

    @Test
    void editComment_notAuthorAndNotInternal_throwsIllegalState() {
        Comment existing = new Comment();
        existing.setId(commentId);
        existing.setAuthor("alice");
        existing.setContent("old");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(existing));

        CreateCommentRequest req = new CreateCommentRequest("new content");

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> commentService.editComment(commentId, "bob", req)
        );

        assertTrue(ex.getMessage().contains("not allowed"));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void editComment_commentNotFound_throwsResourceNotFound() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        CreateCommentRequest req = new CreateCommentRequest("whatever");

        assertThrows(ResourceNotFoundException.class,
                () -> commentService.editComment(commentId, "alice", req));
    }
}
