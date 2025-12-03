package be.pxl.services.exceptions;

import be.pxl.services.controller.PostController;
import be.pxl.services.services.IPostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IPostService postService;

    @Test
    void resourceNotFoundException_returns404() throws Exception {
        UUID id = UUID.randomUUID();

        when(postService.getPostById(any(UUID.class), anyString()))
                .thenThrow(new ResourceNotFoundException("Post not found with id: " + id));

        mockMvc.perform(get("/api/posts/{postId}", id.toString())
                        .header("user", "alice"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Post not found with id: " + id));
    }

    @Test
    void badRequestException_returns400() throws Exception {
        UUID id = UUID.randomUUID();

        // This assumes some controller/service path where you throw your custom BadRequestException.
        when(postService.submitForReview(any(UUID.class), anyString()))
                .thenThrow(new BadRequestException("Only DRAFT posts can be submitted"));

        mockMvc.perform(post("/api/posts/{id}/submit", id)
                        .header("user", "alice"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Only DRAFT posts can be submitted"));
    }

    @Test
    void validationException_returns400WithFieldErrors() throws Exception {
        // Send an invalid PostRequest (assuming @NotBlank on title, etc.)
        String invalidJson = """
            {
              "title": "",
              "content": ""
            }
            """;

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                // optional: check specific field key exists
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    void illegalStateException_returns403WithMessage() throws Exception {
        UUID id = UUID.randomUUID();

        when(postService.submitForReview(any(UUID.class), anyString()))
                .thenThrow(new IllegalStateException("Only the author can submit their post"));

        mockMvc.perform(post("/api/posts/{id}/submit", id)
                        .header("user", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Only the author can submit their post"));
    }
}
