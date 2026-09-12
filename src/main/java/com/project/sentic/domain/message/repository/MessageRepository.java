package com.project.sentic.domain.message.repository;

import com.project.sentic.domain.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.project.sentic.domain.room.entity.Room;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findTop10ByRoomIdOrderBySequenceNoDesc(Long roomId);

    @Query("SELECT COALESCE(MAX(m.sequenceNo), 0) FROM Message m WHERE m.roomId = :roomId")
    int findMaxSequenceNoByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT m FROM Message m JOIN Room r ON m.roomId = r.id WHERE r.userId = :userId AND m.createdAt > :after")
    List<Message> findByRoomUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("after") LocalDateTime after);

    @Query("SELECT COUNT(m) > 0 FROM Message m JOIN Room r ON m.roomId = r.id WHERE r.userId = :userId AND m.createdAt BETWEEN :start AND :end")
    boolean existsByRoomUserIdAndCreatedAtBetween(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 대화 기록 조회 (시간순 정렬)
    List<Message> findByRoomIdOrderBySequenceNoAsc(Long roomId);

    // 사용자의 모든 방 ID 조회
    @Query("SELECT DISTINCT m.roomId FROM Message m WHERE m.roomId IN " +
            "(SELECT r.id FROM Room r WHERE r.userId = :userId AND r.deleted = false)")
    List<Long> findDistinctRoomIdsByUserId(@Param("userId") Long userId);

    // 여러 방의 메시지 조회
    List<Message> findByRoomIdIn(List<Long> roomIds);
}