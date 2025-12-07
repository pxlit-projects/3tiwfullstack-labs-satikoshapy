package be.pxl.services.services;

import be.pxl.services.client.ReviewClient;
import be.pxl.services.domain.Post;
import be.pxl.services.domain.PostStatus;
import be.pxl.services.exceptions.BadRequestException;
import be.pxl.services.exceptions.ResourceNotFoundException;
import be.pxl.services.repository.PostRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
public class PostService implements IPostService {

    private final PostRepository postRepository;
    private final ReviewClient reviewClient;

    private final Logger log = LoggerFactory.getLogger(PostService.class);

    public PostService(PostRepository postRepository, ReviewClient reviewClient) {
        this.postRepository = postRepository;
        this.reviewClient = reviewClient;
    }

    @Override
    public Post addPost(Post post, String user) {
        post.setAuthor(user);
        return postRepository.save(post);
    }

    @Override
    public Post editPost(UUID postId, Post request, String author) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (!post.getAuthor().equals(author)) {
            throw new IllegalStateException("Only the author can modify this post.");
        }

        if (post.getStatus() != PostStatus.DRAFT && post.getStatus() != PostStatus.REJECTED) {
            throw new IllegalStateException("Post can only be edited when status is DRAFT or REJECTED.");
        }

        if (post.getStatus() == PostStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot edit a post that is already published.");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setDateUpdated(LocalDateTime.now());

        log.info("Updating post {} with new title '{}'", postId, request.getTitle());

        return postRepository.save(post);
    }

    @Override
    public List<Post> findPublishedPosts(String contentFilter, String authorFilter, LocalDate dateFrom, LocalDate dateTo) {
        LocalDateTime from = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime to = dateTo != null ? dateTo.atStartOfDay() : null;
        return postRepository.findByStatusAndFilters(
                PostStatus.PUBLISHED,
                contentFilter,
                authorFilter,
                from,
                to
        );
    }

    @Override
    public Post getPostById(UUID postId, String user) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        boolean isPublished = post.getStatus() == PostStatus.PUBLISHED;
        boolean isAuthor = user != null && user.equalsIgnoreCase(post.getAuthor());
        boolean isInternal = user != null && user.equalsIgnoreCase("internal");

        if (!isPublished && !(isAuthor || isInternal)) {
            log.warn("Access denied for user '{}'", user);
            throw new IllegalStateException("You are not allowed to view this post.");
        }

        return post;
    }

    @Override
    @Transactional
    public Post submitForReview(UUID id, String user) {
        Post post = postRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post not found: " + id));

        if (!post.getAuthor().equalsIgnoreCase(user)) {
            throw new IllegalStateException("Only the author can submit their post");
        }
        if (post.getStatus() != PostStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT posts can be submitted");
        }

        // mark as pending_review, then call review-service
        post.setStatus(PostStatus.PENDING_REVIEW);
        post.setDateUpdated(LocalDateTime.now());
        Post saved = postRepository.save(post);

        reviewClient.submit(new ReviewClient.SubmitReviewRequest(saved.getId(), saved.getAuthor(), saved.getTitle()));
        log.info("Post {} submitted for review by {}", id, user);
        return saved;
    }

    @Override
    @Transactional
    public void updatePostStatus(UUID postId, PostStatus newStatus) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        post.setStatus(newStatus);
        post.setDateUpdated(LocalDateTime.now());
        postRepository.save(post);
    }
}
