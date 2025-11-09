package be.pxl.services.services;

import be.pxl.services.domain.Post;
import be.pxl.services.domain.PostStatus;
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

    private final Logger log = LoggerFactory.getLogger(PostService.class);

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public Post addPost(Post post) {
        return postRepository.save(post);
    }

    @Override
    public Post editPost(UUID postId, Post request) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

//        if (!post.getAuthor().equals(author)) {
//            throw new IllegalStateException("Only the author can modify this post.");
//        }

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
        Post post = postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));

        if (post.getStatus() != PostStatus.PUBLISHED &&
                !post.getAuthor().equalsIgnoreCase(user)) {
            log.warn("Not Allowed");
            throw new IllegalStateException("You are not allowed to view this post.");
        }
        return post;
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
