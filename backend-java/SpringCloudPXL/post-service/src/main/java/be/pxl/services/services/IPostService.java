package be.pxl.services.services;

import be.pxl.services.domain.Post;
import be.pxl.services.domain.dtos.PostRequest;
import jakarta.validation.Valid;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public interface IPostService {

    Post addPost(Post post);

    Post editPost(UUID postId, Post request) throws ChangeSetPersister.NotFoundException;

    List<Post> findPublishedPosts(String content, String author, LocalDate dateFrom, LocalDate dateTo);
}
