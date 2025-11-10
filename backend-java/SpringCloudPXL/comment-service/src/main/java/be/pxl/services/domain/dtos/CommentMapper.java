package be.pxl.services.domain.dtos;

import be.pxl.services.domain.Comment;

public class CommentMapper {

    private CommentMapper() {}

    public static CommentResponse toResponse(Comment saved) {
        return new CommentResponse(saved.getId(), saved.getPostId(), saved.getContent(), saved.getAuthor(), saved.getCreatedAt(), saved.getUpdatedAt());
    }
}
