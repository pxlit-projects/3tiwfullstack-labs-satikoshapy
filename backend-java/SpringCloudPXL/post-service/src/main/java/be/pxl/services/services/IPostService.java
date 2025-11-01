package be.pxl.services.services;

import be.pxl.services.domain.Post;
import org.springframework.stereotype.Service;

@Service
public interface IPostService {

    Post addPost(Post post);
}
