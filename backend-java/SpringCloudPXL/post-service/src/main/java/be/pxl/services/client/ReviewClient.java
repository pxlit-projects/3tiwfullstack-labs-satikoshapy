package be.pxl.services.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(name = "review-service", path = "/api/reviews")
public interface ReviewClient {
    @PostMapping("/submit")
    void submit(@RequestBody SubmitReviewRequest request);

    record SubmitReviewRequest(java.util.UUID postId, String author, String title) {}
}