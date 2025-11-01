package be.pxl.services.domain.dtos;

import be.pxl.services.domain.Post;

public final class PostMapper {

    private PostMapper() {
    }

    public static Post toEntity(PostRequest request) {
        return new Post(
                request.title(),
                request.content()
        );
    }

    public static PostResponse toResponse(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor(),
                post.getStatus(),
                post.getDateCreated(),
                post.getDateUpdated()
        );
    }
}