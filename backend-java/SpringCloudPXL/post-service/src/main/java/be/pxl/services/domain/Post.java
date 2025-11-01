package be.pxl.services.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.Objects;


@Entity
@Table(name = "post")
@Data
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String title;
    private String content;
    private String author;
    private LocalTime dateCreated;
    private LocalTime dateUpdated;
    private PostStatus status;

    public Post(String title, String content) {
        this.title = title;
        this.content = content;
        this.author = "unknown";
        this.dateCreated = LocalTime.now();
        this.dateUpdated = LocalTime.now();
        this.status = PostStatus.CONCEPT;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public LocalTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public LocalTime getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(LocalTime dateUpdated) {
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
