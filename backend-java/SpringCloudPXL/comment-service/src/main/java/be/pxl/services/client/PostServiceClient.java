package be.pxl.services.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "post-service", path = "/api/posts")
public interface PostServiceClient {
    @GetMapping("/{postId}")
    PostResponse getPostById(@PathVariable("postId") UUID postId,
                             @RequestHeader("user") String user);
}