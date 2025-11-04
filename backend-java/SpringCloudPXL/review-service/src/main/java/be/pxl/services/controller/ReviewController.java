package be.pxl.services.controller;

import be.pxl.services.domain.Review;
import be.pxl.services.domain.dtos.ReviewMapper;
import be.pxl.services.domain.dtos.ReviewRequest;
import be.pxl.services.service.IReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final IReviewService reviewService;

    public ReviewController(IReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/{postId}/approve")
    public ResponseEntity<Review> approvePost(@PathVariable String postId) {
        String reviewerId = "editor_mock_id"; // AuthUtils.getCurrentUserIdentifier();

        Review review = reviewService.approvePost(UUID.fromString(postId), reviewerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }

    @PostMapping("/{postId}/reject")
    public ResponseEntity<Review> rejectPost(
            @PathVariable String postId,
            @Valid @RequestBody ReviewRequest request) {

        String reviewerId = "editor_mock_id"; // AuthUtils.getCurrentUserIdentifier();

        Review review = reviewService.rejectPost(UUID.fromString(postId), reviewerId, ReviewMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(review);
    }
}