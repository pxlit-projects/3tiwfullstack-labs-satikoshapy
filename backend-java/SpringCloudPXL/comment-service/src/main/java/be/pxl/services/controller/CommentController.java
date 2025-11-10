package be.pxl.services.controller;

import be.pxl.services.domain.dtos.CommentResponse;
import be.pxl.services.domain.dtos.CreateCommentRequest;
import be.pxl.services.service.CommentService;
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

    public CommentController(CommentService service) {
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
}
