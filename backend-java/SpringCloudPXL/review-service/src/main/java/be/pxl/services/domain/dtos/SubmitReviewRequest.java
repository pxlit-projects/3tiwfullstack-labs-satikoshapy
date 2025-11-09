package be.pxl.services.domain.dtos;

import java.util.UUID;

public record SubmitReviewRequest(UUID postId, String author, String title) {}
