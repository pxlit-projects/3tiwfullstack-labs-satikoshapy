package be.pxl.services.service;

import be.pxl.services.client.PostServiceClient;
import be.pxl.services.domain.Review;
import be.pxl.services.domain.ReviewStatus;
import be.pxl.services.domain.dtos.PostResponse;
import be.pxl.services.domain.dtos.PostStatus;
import be.pxl.services.domain.dtos.SubmitReviewRequest;
import be.pxl.services.exceptions.ResourceNotFoundException;
import be.pxl.services.messaging.DecisionPublisher;
import be.pxl.services.messaging.PostReviewedEvent;
import be.pxl.services.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ReviewService implements IReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final PostServiceClient postServiceClient;
    private final DecisionPublisher decisionPublisher;

    public ReviewService(ReviewRepository reviewRepository, PostServiceClient postServiceClient, DecisionPublisher decisionPublisher) {
        this.reviewRepository = reviewRepository;
        this.postServiceClient = postServiceClient;
        this.decisionPublisher = decisionPublisher;
    }

    @Override
    public void submit(SubmitReviewRequest req){
        logger.info("Review request received for post {}", req.postId());
    }

    @Override
    public Review approvePost(UUID postId, String reviewerId) {
        PostResponse post = getPostById(postId);
        Review review = reviewRepository.findByPostId(postId).orElse(new Review());

        if (review.getStatus() != ReviewStatus.PENDING) {
            throw new IllegalStateException("Post review is already " + review.getStatus() + ".");
        }
        review.setPostId(postId);
        review.setReviewerId(reviewerId);
        review.setStatus(ReviewStatus.APPROVED);

        Review savedReview = reviewRepository.save(review);

        decisionPublisher.publish(new PostReviewedEvent(postId, "APPROVED"));
        logger.info("Notification: Post {} was APPROVED by {}.", postId, reviewerId);

        return savedReview;
    }

    @Override
    public Review rejectPost(UUID postId, String reviewerId, Review request) {
        getPostById(postId);
        Review review = reviewRepository.findByPostId(postId).orElse(new Review());

        if (review.getStatus() != ReviewStatus.PENDING) {
            throw new IllegalStateException("Post review is already " + review.getStatus() + ".");
        }

        // 3. US9: Add rejection comment and update status
        if (request.getRejectionComment() == null || request.getRejectionComment().trim().isEmpty()) {
            throw new IllegalStateException("Rejection requires a comment.");
        }

        review.setPostId(postId);
        review.setReviewerId(reviewerId);
        review.setStatus(ReviewStatus.REJECTED);
        review.setRejectionComment(request.getRejectionComment());

        Review savedReview = reviewRepository.save(review);

        decisionPublisher.publish(new PostReviewedEvent(postId, "REJECTED"));
        logger.info("Notification: Post {} was REJECTED by {} with comment: {}", postId, reviewerId, request.getRejectionComment());

        return savedReview;
    }

    private PostResponse getPostById(UUID postId) {
        try {
            return postServiceClient.getPostById(postId, "reviewer");
        } catch (Exception e) {
            logger.warn(e.getMessage());
            throw new ResourceNotFoundException("Post not found with ID: " + postId + " in PostService.");
        }
    }
}
