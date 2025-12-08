package be.pxl.services.controller;

import be.pxl.services.domain.dtos.CommentResponse;
import be.pxl.services.domain.dtos.CreateCommentRequest;
import be.pxl.services.service.ICommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final ICommentService service;

    public CommentController(ICommentService service) {
        this.service = service;
    }

    @PostMapping("/posts/{postId}/comment")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(@PathVariable UUID postId,
                               @RequestHeader("user") String user,
                               @Valid @RequestBody CreateCommentRequest req) {
        return service.addComment(postId, user, req);
    }

    @GetMapping("/posts/{postId}")
    public List<CommentResponse> list(@PathVariable UUID postId,
                                      @RequestHeader("user") String user) {
        return service.getAllCommentsForPost(postId, user);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String commentId,
                       @RequestHeader("user") String user) {
        service.deleteComment(UUID.fromString(commentId), user);
    }

    @PutMapping("/{commentId}")
    @ResponseStatus(HttpStatus.OK)
    public CommentResponse editComment(@PathVariable  String commentId, @RequestHeader("user") String user, @Valid @RequestBody CreateCommentRequest req) {
        return service.editComment(UUID.fromString(commentId), user, req);
    }
}
