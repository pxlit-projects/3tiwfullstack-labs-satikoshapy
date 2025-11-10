package be.pxl.services.domain.dtos;

import java.util.UUID;

public record CommentResponse(UUID id, UUID postId, String content, String author,
                              java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {}
