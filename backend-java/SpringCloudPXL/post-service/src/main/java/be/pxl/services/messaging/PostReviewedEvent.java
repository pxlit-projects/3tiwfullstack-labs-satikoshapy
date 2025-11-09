package be.pxl.services.messaging;


import java.util.UUID;


public record PostReviewedEvent(UUID postId, String decision, String remarks) {}