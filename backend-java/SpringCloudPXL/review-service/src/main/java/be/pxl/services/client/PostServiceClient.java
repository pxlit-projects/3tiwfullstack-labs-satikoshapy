package be.pxl.services.client;

import be.pxl.services.domain.dtos.PostResponse;
import be.pxl.services.domain.dtos.PostStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "post-service", url = "http://localhost:8081", path = "/api/posts")
public interface PostServiceClient {

    @GetMapping("/{postId}")
    PostResponse getPostById(@PathVariable("postId") UUID postId, @RequestHeader("user") String user);

    @PutMapping("/{postId}/status/{newStatus}")
    void updatePostStatus(@PathVariable("postId") UUID postId, @PathVariable("newStatus") PostStatus newStatus);
}
