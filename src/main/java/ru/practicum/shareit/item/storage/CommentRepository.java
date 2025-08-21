package ru.practicum.shareit.item.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Comment;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.item.id = :itemId")
    List<Comment> findAllByItemIdWithAuthor(@Param("itemId") Long itemId);

    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.item.id IN :itemIds")
    List<Comment> findAllByItemIdInWithAuthors(@Param("itemIds") List<Long> itemIds);

    List<Comment> findAllByItemId(Long itemId);

    List<Comment> findAllByItemIdIn(List<Long> itemIds);
}