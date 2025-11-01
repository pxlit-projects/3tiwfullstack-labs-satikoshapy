package be.pxl.services.services;

import be.pxl.services.domain.Post;
import be.pxl.services.repository.PostRepository;
import org.springframework.stereotype.Service;

@Service
public class PostService implements IPostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public Post addPost(Post post) {
        return postRepository.save(post);
    }
}
