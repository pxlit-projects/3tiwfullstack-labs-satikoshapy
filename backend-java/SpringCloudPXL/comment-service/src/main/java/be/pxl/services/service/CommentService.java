package be.pxl.services.service;

import be.pxl.services.client.PostResponse;
import be.pxl.services.client.PostServiceClient;
import be.pxl.services.domain.Comment;
import be.pxl.services.domain.dtos.CommentMapper;
import be.pxl.services.domain.dtos.CommentResponse;
import be.pxl.services.domain.dtos.CreateCommentRequest;
import be.pxl.services.exceptions.ResourceNotFoundException;
import be.pxl.services.repository.CommentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CommentService implements ICommentService {

    private final CommentRepository commentRepository;
    private final PostServiceClient postServiceClient;

    public CommentService(CommentRepository commentRepository, PostServiceClient postServiceClient) {
        this.commentRepository = commentRepository;
        this.postServiceClient = postServiceClient;
    }

    @Override
    public CommentResponse addComment(UUID postId, String user, CreateCommentRequest req) {
        PostResponse post = getVisiblePostOrThrow(postId, user);

        Comment c = new Comment();
        c.setPostId(postId);
        c.setAuthor(user);
        c.setContent(req.content().trim());
        c.setCreatedAt(LocalDateTime.now());

        Comment saved = commentRepository.save(c);
        return CommentMapper.toResponse(saved);
    }

    @Override
    public List<CommentResponse> getAllCommentsForPost(UUID postId, String user) {
        return List.of();
    }

    private PostResponse getVisiblePostOrThrow(UUID postId, String user) {
        try {
            return postServiceClient.getPostById(postId, user);
        } catch (Exception ex) {
            throw new ResourceNotFoundException("Post not found or not visible: " + postId);
        }
    }
}
