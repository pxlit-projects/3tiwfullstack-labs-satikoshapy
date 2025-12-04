package be.pxl.services.controller;

import be.pxl.services.domain.dtos.CommentResponse;
import be.pxl.services.domain.dtos.CreateCommentRequest;
import be.pxl.services.exceptions.GlobalExceptionHandler;
import be.pxl.services.service.ICommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
@Import(GlobalExceptionHandler.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ICommentService commentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void addComment_returns201AndDelegatesToService() throws Exception {
        UUID postId = UUID.randomUUID();

        CreateCommentRequest req = new CreateCommentRequest("hello");
        CommentResponse resp = new CommentResponse(
                UUID.randomUUID(),
                postId,
                "hello",
                "alice",
                LocalDateTime.now(),
                null
        );

        when(commentService.addComment(eq(postId), eq("alice"), any(CreateCommentRequest.class)))
                .thenReturn(resp);

        mockMvc.perform(post("/api/comments/posts/{postId}/comment", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("user", "alice")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").value(postId.toString()))
                .andExpect(jsonPath("$.author").value("alice"))
                .andExpect(jsonPath("$.content").value("hello"));

        ArgumentCaptor<CreateCommentRequest> captor = ArgumentCaptor.forClass(CreateCommentRequest.class);
        verify(commentService).addComment(eq(postId), eq("alice"), captor.capture());
        assertEquals("hello", captor.getValue().content());
    }

    @Test
    void listComments_returns200AndList() throws Exception {
        UUID postId = UUID.randomUUID();

        CommentResponse c1 = new CommentResponse(
                UUID.randomUUID(),
                postId,
                "first",
                "alice",
                LocalDateTime.now().minusMinutes(2),
                null
        );
        CommentResponse c2 = new CommentResponse(
                UUID.randomUUID(),
                postId,
                "second",
                "bob",
                LocalDateTime.now(),
                null
        );

        when(commentService.getAllCommentsForPost(postId, "bob"))
                .thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/comments/posts/{postId}", postId)
                        .header("user", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("first"))
                .andExpect(jsonPath("$[1].content").value("second"));
    }

    @Test
    void deleteComment_returns204() throws Exception {
        UUID commentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/comments/{commentId}", commentId)
                        .header("user", "alice"))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(commentId, "alice");
    }

    @Test
    void editComment_returns200AndBody() throws Exception {
        UUID commentId = UUID.randomUUID();

        CreateCommentRequest req = new CreateCommentRequest("updated");
        CommentResponse resp = new CommentResponse(
                commentId,
                UUID.randomUUID(),
                "alice",
                "updated",
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now()
        );

        when(commentService.editComment(eq(commentId), eq("alice"), any(CreateCommentRequest.class)))
                .thenReturn(resp);

        mockMvc.perform(put("/api/comments/{commentId}", commentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("user", "alice")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
