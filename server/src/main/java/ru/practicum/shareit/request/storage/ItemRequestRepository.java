package ru.practicum.shareit.request.storage;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.request.model.ItemRequest;

import java.util.List;

@Repository
public interface ItemRequestRepository extends JpaRepository<ItemRequest, Long> {
    List<ItemRequest> findByRequesterIdOrderByCreatedDesc(Long requesterId);

    @Query("SELECT ir FROM ItemRequest ir WHERE ir.requester.id != :userId ORDER BY ir.created DESC")
    List<ItemRequest> findAllByOtherUsers(@Param("userId") Long userId, Pageable pageable);

    List<ItemRequest> findByRequesterIdNotOrderByCreatedDesc(Long userId, Pageable pageable);
}