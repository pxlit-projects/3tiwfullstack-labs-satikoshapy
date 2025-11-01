package be.pxl.services.domain.dtos;

import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @Size(max = 500, message = "Comment must be less than 500 characters.")
        String comment
) {}
