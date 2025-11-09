package be.pxl.services.repository;

import be.pxl.services.domain.Post;
import be.pxl.services.domain.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {

    @Query("SELECT p FROM Post p " +
            "WHERE p.status = :status " +
            "AND (:#{#contentFilter == null} IS TRUE OR LOWER(cast(p.content as string)) LIKE LOWER(CONCAT('%', :contentFilter, '%'))) " +
            "AND (:#{#authorFilter == null} IS TRUE OR LOWER(p.author) LIKE LOWER(CONCAT('%', :authorFilter, '%'))) " +
            "AND (:#{#from == null} IS TRUE OR p.dateCreated >= :from) " +
            "AND (:#{#to == null} IS TRUE OR p.dateCreated <= :to) " +
            "ORDER BY p.dateCreated DESC")
    List<Post> findByStatusAndFilters(
            @Param("status") PostStatus status,
            @Param("contentFilter") String contentFilter,
            @Param("authorFilter") String authorFilter,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}
