package com.project.sentic.domain.room.repository;

import com.project.sentic.domain.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByUserIdAndRoomTypeAndDeletedFalseOrderByLastActiveAtDesc(
            Long userId, Room.RoomType roomType
    );

    Optional<Room> findByIdAndDeletedFalse(Long id);

    List<Room> findByUserIdAndDeletedTrueOrderByDeletedAtDesc(Long userId);

    @Modifying
    @Query("DELETE FROM Room r WHERE r.deleted = true AND r.deletedAt < :expiredDate")
    int deleteAllByDeletedTrueAndDeletedAtBefore(@Param("expiredDate") LocalDateTime expiredDate);
}