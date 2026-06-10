package com.project.sentic.domain.message.repository;

import com.project.sentic.domain.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findTop10ByRoomIdOrderBySequenceNoDesc(Long roomId);

    @Query("SELECT COALESCE(MAX(m.sequenceNo), 0) FROM Message m WHERE m.roomId = :roomId")
    int findMaxSequenceNoByRoomId(@Param("roomId") Long roomId);
}
