package be.pxl.services.controller;

import be.pxl.services.domain.Post;
import be.pxl.services.domain.dtos.PostMapper;
import be.pxl.services.domain.dtos.PostRequest;
import be.pxl.services.services.IPostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
