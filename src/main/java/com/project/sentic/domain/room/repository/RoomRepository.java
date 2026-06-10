package com.project.sentic.domain.room.repository;

import com.project.sentic.domain.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByUserIdAndRoomTypeAndDeletedFalseOrderByLastActiveAtDesc(
            Long userId, Room.RoomType roomType
    );

    Optional<Room> findByIdAndDeletedFalse(Long id);
}
