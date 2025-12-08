package be.pxl.services.service;

import be.pxl.services.client.PostServiceClient;
import be.pxl.services.domain.Comment;
import be.pxl.services.domain.dtos.CommentMapper;
import be.pxl.services.domain.dtos.CommentResponse;
import be.pxl.services.domain.dtos.CreateCommentRequest;
import be.pxl.services.exceptions.ResourceNotFoundException;
import be.pxl.services.repository.CommentRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CommentService implements ICommentService {

    Logger Logger = LoggerFactory.getLogger(CommentService.class);

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
        Logger.info("Getting comments for user " + user);
        getVisiblePostOrThrow(postId);
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream().map(CommentMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId, String user) {
        Comment c = commentRepository.findById(commentId).orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));

        boolean isAuthor = user != null && user.equalsIgnoreCase(c.getAuthor());
        boolean isInternal = user != null && user.equalsIgnoreCase("internal");
        if (!(isAuthor || isInternal)) {
            Logger.error("Action not allowed for this user");
            throw new IllegalStateException("You are not allowed to delete this comment.");
        }
        Logger.info("Deleting comment " + commentId);
        commentRepository.delete(c);
    }

    @Override
    @Transactional
    public CommentResponse editComment(UUID id, String user, CreateCommentRequest req) {
        Comment comment = commentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + id));
        boolean isAuthor = user != null && user.equalsIgnoreCase(comment.getAuthor());
        boolean isInternal = user != null && user.equalsIgnoreCase("internal");
        if (!(isAuthor || isInternal)) {
            throw new IllegalStateException("You are not allowed to edit this comment.");
        }
        comment.setContent(req.content().trim());
        comment.setUpdatedAt(java.time.LocalDateTime.now());
        Logger.info("Successfully edited comemnt: " + comment);
        return CommentMapper.toResponse(commentRepository.save(comment));
    }

    private void getVisiblePostOrThrow(UUID postId) {
        try {
            Logger.info("Trying to fetch visible post: " + postId);
            postServiceClient.getPostById(postId, "internal");
        } catch (Exception ex) {
            Logger.error("Error while fetching visible post: " + postId, ex);
            throw new ResourceNotFoundException("Post not found or not visible: " + postId);
        }
    }
}
