package be.pxl.services.controller;

import be.pxl.services.domain.Post;
import be.pxl.services.domain.PostStatus;
import be.pxl.services.domain.dtos.PostRequest;
import be.pxl.services.services.IPostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IPostService postService;

    @Autowired
    private ObjectMapper objectMapper;

    private Post buildPost(UUID id, String author, PostStatus status) {
        Post p = new Post("title", "content");
        p.setId(id);
        p.setAuthor(author);
        p.setStatus(status);
        p.setDateCreated(LocalDateTime.now().minusDays(1));
        p.setDateUpdated(LocalDateTime.now().minusHours(1));
        return p;
    }

    @Test
    void createPost_returnsCreated() throws Exception {
        PostRequest req = new PostRequest("title", "content");
        Post saved = buildPost(UUID.randomUUID(), "unknown", PostStatus.DRAFT);

        when(postService.addPost(any(Post.class), any())).thenReturn(saved);

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.title").value("title"))
                .andExpect(jsonPath("$.content").value("content"));

        verify(postService).addPost(any(Post.class), any());
    }

    @Test
    void updatePost_callsServiceWithUserHeader() throws Exception {
        UUID id = UUID.randomUUID();
        PostRequest req = new PostRequest("new title", "new content");
        Post updated = buildPost(id, "alice", PostStatus.DRAFT);
        updated.setTitle("new title");
        updated.setContent("new content");

        when(postService.editPost(eq(id), any(Post.class), eq("alice")))
                .thenReturn(updated);

        mockMvc.perform(put("/api/posts/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("user", "alice")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("new title"))
                .andExpect(jsonPath("$.content").value("new content"));

        verify(postService).editPost(eq(id), any(Post.class), eq("alice"));
    }

    @Test
    void getPublishedPosts_withFilters_callsServiceAndReturnsList() throws Exception {
        Post p1 = buildPost(UUID.randomUUID(), "alice", PostStatus.PUBLISHED);
        Post p2 = buildPost(UUID.randomUUID(), "bob", PostStatus.PUBLISHED);

        when(postService.findPublishedPosts(eq("java"), eq("alice"),
                any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/api/posts")
                        .param("content", "java")
                        .param("author", "alice")
                        .param("from", "2024-01-01")
                        .param("to", "2024-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(p1.getId().toString()))
                .andExpect(jsonPath("$[1].id").value(p2.getId().toString()));

        verify(postService).findPublishedPosts(eq("java"), eq("alice"),
                any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void getPostById_usesUserHeader() throws Exception {
        UUID id = UUID.randomUUID();
        Post post = buildPost(id, "alice", PostStatus.PUBLISHED);

        when(postService.getPostById(id, "bob")).thenReturn(post);

        mockMvc.perform(get("/api/posts/{id}", id)
                        .header("user", "bob"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.author").value("alice"));

        verify(postService).getPostById(id, "bob");
    }

    @Test
    void updatePostStatus_ok() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/posts/{id}/status/{status}", id, "PUBLISHED"))
                .andExpect(status().isOk());

        verify(postService).updatePostStatus(id, PostStatus.PUBLISHED);
    }

    @Test
    void submitForReview_usesUserHeader() throws Exception {
        UUID id = UUID.randomUUID();
        Post post = buildPost(id, "alice", PostStatus.PENDING_REVIEW);
        when(postService.submitForReview(id, "alice")).thenReturn(post);

        mockMvc.perform(post("/api/posts/{id}/submit", id)
                        .header("user", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("PENDING_REVIEW"));

        verify(postService).submitForReview(id, "alice");
    }
}
