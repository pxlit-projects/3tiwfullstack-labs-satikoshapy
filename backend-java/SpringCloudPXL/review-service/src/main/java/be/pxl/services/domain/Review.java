package be.pxl.services.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "review")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID postId;
    private String reviewerId;
    @Enumerated(EnumType.STRING)
    private ReviewStatus status;
    private String rejectionComment;
    private LocalDateTime reviewDate;

    public Review() {
        this.status = ReviewStatus.PENDING;
        this.reviewDate = LocalDateTime.now();
    }

    public Review(String rejectionComment) {
        this.rejectionComment = rejectionComment;
        this.status = ReviewStatus.REJECTED;
        this.reviewDate = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPostId() {
        return postId;
    }

    public void setPostId(UUID postId) {
        this.postId = postId;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(ReviewStatus status) {
        this.status = status;
    }

    public String getRejectionComment() {
        return rejectionComment;
    }

    public void setRejectionComment(String rejectionComment) {
        this.rejectionComment = rejectionComment;
    }

    public LocalDateTime getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(LocalDateTime reviewDate) {
        this.reviewDate = reviewDate;
    }
}
