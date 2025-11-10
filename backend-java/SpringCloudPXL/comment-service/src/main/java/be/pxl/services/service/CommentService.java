package be.pxl.services.service;

import be.pxl.services.client.PostResponse;
import be.pxl.services.client.PostServiceClient;
import be.pxl.services.domain.Comment;
import be.pxl.services.domain.dtos.CommentMapper;
import be.pxl.services.domain.dtos.CommentResponse;
import be.pxl.services.domain.dtos.CreateCommentRequest;
import be.pxl.services.exceptions.ResourceNotFoundException;
import be.pxl.services.repository.CommentRepository;
import jakarta.transaction.Transactional;
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
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream().map(CommentMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId, String user) {
        Comment c = commentRepository.findById(commentId).orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));

        boolean isAuthor = user != null && user.equalsIgnoreCase(c.getAuthor());
        boolean isReviewer = user != null && user.equalsIgnoreCase("reviewer");
        if (!(isAuthor || isReviewer)) {
            throw new IllegalStateException("You are not allowed to delete this comment.");
        }
        commentRepository.delete(c);
    }

    @Override
    @Transactional
    public CommentResponse editComment(UUID id, String user, CreateCommentRequest req) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + id));
        boolean isAuthor = user != null && user.equalsIgnoreCase(comment.getAuthor());
        boolean isReviewer = user != null && user.equalsIgnoreCase("reviewer");
        if (!(isAuthor || isReviewer)) {
            throw new IllegalStateException("You are not allowed to edit this comment.");
        }
        comment.setContent(req.content().trim());
        comment.setUpdatedAt(java.time.LocalDateTime.now());
        return CommentMapper.toResponse(commentRepository.save(comment));
    }

    private PostResponse getVisiblePostOrThrow(UUID postId) {
        try {
            return postServiceClient.getPostById(postId, "comment");
        } catch (Exception ex) {
            throw new ResourceNotFoundException("Post not found or not visible: " + postId);
        }
    }
}
