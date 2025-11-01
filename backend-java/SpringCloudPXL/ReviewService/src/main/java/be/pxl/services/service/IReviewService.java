package be.pxl.services.service;

import be.pxl.services.domain.Review;
import be.pxl.services.domain.dtos.ReviewRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface IReviewService {
    Review approvePost(UUID postId, String reviewerId);
    Review rejectPost(UUID postId, String reviewerId, Review request);
}