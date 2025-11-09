package be.pxl.services.controller;

import be.pxl.services.domain.Post;
import be.pxl.services.domain.PostStatus;
import be.pxl.services.domain.dtos.PostMapper;
import be.pxl.services.domain.dtos.PostRequest;
import be.pxl.services.domain.dtos.PostResponse;
import be.pxl.services.services.IPostService;
import jakarta.validation.Valid;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final IPostService postService;

    public PostController(IPostService postService) {
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<Post> createPost(@Valid @RequestBody PostRequest post) {
        Post saved = postService.addPost(PostMapper.toEntity(post));
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable String postId, @Valid @RequestBody PostRequest request
            , @RequestHeader("user") String user) throws ChangeSetPersister.NotFoundException {
        Post updatedPost = postService.editPost(UUID.fromString(postId), PostMapper.toEntity(request), user);
        return ResponseEntity.ok(PostMapper.toResponse(updatedPost));
    }

    @GetMapping
    public ResponseEntity<List<PostResponse>> getPublishedPosts(
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<Post> posts = postService.findPublishedPosts(content, author, from, to);
        List<PostResponse> responses = posts.stream()
                .map(PostMapper::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable String postId, @RequestHeader("user") String user) {
        Post post = postService.getPostById(UUID.fromString(postId), user);
        return ResponseEntity.ok(PostMapper.toResponse(post));
    }

    @PutMapping("/{postId}/status/{newStatus}")
    public ResponseEntity<Void> updatePostStatus(
            @PathVariable String postId,
            @PathVariable PostStatus newStatus
    ) {
        postService.updatePostStatus(UUID.fromString(postId), newStatus);
        return ResponseEntity.ok().build();
    }
}
