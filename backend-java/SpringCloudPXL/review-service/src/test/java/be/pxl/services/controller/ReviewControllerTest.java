package be.pxl.services.controller;

import be.pxl.services.domain.Review;
import be.pxl.services.domain.ReviewStatus;
import be.pxl.services.domain.dtos.SubmitReviewRequest;
import be.pxl.services.exceptions.GlobalExceptionHandler;
import be.pxl.services.service.IReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import(GlobalExceptionHandler.class) // if you re-use the same handler here
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IReviewService reviewService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void submit_returns202AndDelegatesToService() throws Exception {
        UUID postId = UUID.randomUUID();
        SubmitReviewRequest req = new SubmitReviewRequest(postId, "alice", "My nice post");

        ArgumentCaptor<SubmitReviewRequest> captor = ArgumentCaptor.forClass(SubmitReviewRequest.class);

        mockMvc.perform(post("/api/reviews/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted());

        verify(reviewService).submit(captor.capture());
        assertEquals(postId, captor.getValue().postId());
        assertEquals("alice", captor.getValue().author());
        assertEquals("My nice post", captor.getValue().title());
    }

    @Test
    void approvePost_returns201AndReviewBody() throws Exception {
        UUID postId = UUID.randomUUID();

        Review review = new Review();
        review.setPostId(postId);
        review.setReviewerId("editor_mock_id");
        review.setStatus(ReviewStatus.APPROVED);

        when(reviewService.approvePost(eq(postId), eq("editor_mock_id")))
                .thenReturn(review);

        mockMvc.perform(post("/api/reviews/{postId}/approve", postId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").value(postId.toString()))
                .andExpect(jsonPath("$.reviewerId").value("editor_mock_id"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void rejectPost_returns201AndReviewBody() throws Exception {
        UUID postId = UUID.randomUUID();

        Review review = new Review();
        review.setPostId(postId);
        review.setReviewerId("editor_mock_id");
        review.setStatus(ReviewStatus.REJECTED);
        review.setRejectionComment("Too many typos");

        when(reviewService.rejectPost(eq(postId), eq("editor_mock_id"), any(Review.class)))
                .thenReturn(review);

        String requestJson = """
            {
              "rejectionComment": "Too many typos"
            }
            """;

        mockMvc.perform(post("/api/reviews/{postId}/reject", postId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").value(postId.toString()))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionComment").value("Too many typos"));
    }
}
