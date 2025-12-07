package be.pxl.services.it;

import be.pxl.services.domain.Post;
import be.pxl.services.domain.PostStatus;
import be.pxl.services.domain.dtos.PostRequest;
import be.pxl.services.repository.PostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PostControllerIT extends AbstractMySqlIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createPost_persistsAndReturns201() throws Exception {
        PostRequest req = new PostRequest(
                "My first post",
                "Hello integration test!"
        );

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                        .header("user", "alice"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("My first post"))
                .andExpect(jsonPath("$.author").value("alice"));
    }

    @Test
    void getPostById_respectsVisibilityRules() throws Exception {
        Post post = new Post();
        post.setAuthor("alice");
        post.setTitle("Hidden draft");
        post.setContent("Draft content");
        post.setStatus(PostStatus.DRAFT);
        post.setDateCreated(LocalDateTime.now());
        Post saved = postRepository.save(post);

        // 403: another user, not internal
        mockMvc.perform(get("/api/posts/{id}", saved.getId())
                        .header("user", "bob"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Conflict"));

        // 200: author can see it
        mockMvc.perform(get("/api/posts/{id}", post.getId())
                        .header("user", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hidden draft"));

        // 200: internal user can see it
        mockMvc.perform(get("/api/posts/{id}", post.getId())
                        .header("user", "internal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hidden draft"));
    }

    @Test
    void findPublishedPosts_filtersByStatus() throws Exception {
        Post p1 = new Post();
        p1.setAuthor("alice");
        p1.setTitle("Draft");
        p1.setContent("x");
        p1.setStatus(PostStatus.DRAFT);
        p1.setDateCreated(LocalDateTime.now());

        Post p2 = new Post();
        p2.setAuthor("bob");
        p2.setTitle("Published");
        p2.setContent("y");
        p2.setStatus(PostStatus.PUBLISHED);
        p2.setDateCreated(LocalDateTime.now());

        postRepository.save(p1);
        postRepository.save(p2);

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Published"));
    }
}
