package be.pxl.services.messaging;

import java.io.Serializable;
import java.util.UUID;
public record PostReviewedEvent(UUID postId, String decision) implements Serializable {
    private static final long serialVersionUID = 1L;
}

