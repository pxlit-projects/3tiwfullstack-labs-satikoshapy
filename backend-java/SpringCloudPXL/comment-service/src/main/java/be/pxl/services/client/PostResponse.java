package be.pxl.services.client;

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
    public PostResponse {
    }
}