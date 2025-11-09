package be.pxl.services.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;


@Entity
@Table(name = "post")
@Data
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;
    @Lob
    private String content;
    private String author;
    private LocalDateTime dateCreated;
    private LocalDateTime dateUpdated;
    @Enumerated(EnumType.STRING)
    private PostStatus status;

    public Post() {
    }

    public Post(String title, String content) {
        this.title = title;
        this.content = content;
        this.author = "unknown";
        this.dateCreated = LocalDateTime.now();
        this.dateUpdated = LocalDateTime.now();
        this.status = PostStatus.DRAFT;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public LocalDateTime getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(LocalDateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public PostStatus getStatus() {
        return status;
    }

    public void setStatus(PostStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return Objects.equals(getId(), post.getId()) && Objects.equals(getTitle(), post.getTitle()) && Objects.equals(getContent(), post.getContent()) && Objects.equals(getAuthor(), post.getAuthor()) && Objects.equals(getDateCreated(), post.getDateCreated()) && Objects.equals(getDateUpdated(), post.getDateUpdated()) && getStatus() == post.getStatus();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getTitle(), getContent(), getAuthor(), getDateCreated(), getDateUpdated(), getStatus());
    }
}
