package be.pxl.services.domain.dtos;

import be.pxl.services.domain.PostStatus;

import java.time.LocalTime;

public record PostResponse(
        Long id,
        String title,
        String content,
        String author,
        PostStatus status,
        LocalTime dateCreated,
        LocalTime dateUpdated
) {
}