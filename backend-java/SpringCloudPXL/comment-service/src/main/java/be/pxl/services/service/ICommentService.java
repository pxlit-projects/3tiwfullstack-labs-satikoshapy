package be.pxl.services.service;

import be.pxl.services.domain.dtos.CommentResponse;
import be.pxl.services.domain.dtos.CreateCommentRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface ICommentService {
    CommentResponse addComment(UUID postId, String user, @Valid CreateCommentRequest req);

    List<CommentResponse> getAllCommentsForPost(UUID postId, String user);

    void deleteComment(UUID commentId, String user);

    CommentResponse editComment(UUID commentId, String user, CreateCommentRequest req);
}
