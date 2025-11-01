package be.pxl.services.domain.dtos;

import be.pxl.services.domain.Review;

public final class ReviewMapper {

    private ReviewMapper() {
    }

    public static Review toEntity(ReviewRequest request) {
        return new Review(
                request.comment()
        );
    }
}
