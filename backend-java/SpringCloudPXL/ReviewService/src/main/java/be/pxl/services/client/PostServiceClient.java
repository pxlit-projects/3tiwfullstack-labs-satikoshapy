package be.pxl.services.client;

import be.pxl.services.domain.dtos.PostResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "postservice", url = "http://localhost:8081")
public interface PostServiceClient {

    // http://localhost:8081/api/posts/{postId}
    @GetMapping("/api/posts/{postId}")
    PostResponse getPostById(@PathVariable("postId") UUID postId);
}
