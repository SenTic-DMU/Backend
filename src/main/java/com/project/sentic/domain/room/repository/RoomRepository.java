package com.project.sentic.domain.room.repository;

import com.project.sentic.domain.room.entity.Room;
import com.project.sentic.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // 유저의 방 목록 조회 (타입별 필터 + 소프트 딜리트 제외)
    List<Room> findByUserAndRoomTypeAndDeletedFalseOrderByLastMessageAtDesc(
            User user, Room.RoomType roomType
    );

    // 방 단건 조회 (소프트 딜리트 제외)
    Optional<Room> findByIdAndDeletedFalse(Long id);
}