package be.pxl.services.domain.dtos;

import be.pxl.services.domain.PostStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostResponse(
        UUID id,
        String title,
        String content,
        String author,
        PostStatus status,
        LocalDateTime dateCreated,
        LocalDateTime dateUpdated
) {
}