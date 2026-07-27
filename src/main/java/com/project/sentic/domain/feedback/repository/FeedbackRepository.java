package com.project.sentic.domain.feedback.repository;

import com.project.sentic.domain.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // 방의 피드백 목록 조회 (최신순)
    List<Feedback> findByRoomIdOrderByCreatedAtDesc(Long roomId);

    // 메시지별 피드백 조회
    List<Feedback> findByMessageId(Long messageId);

    // 메시지 ID 목록으로 피드백 조회
    List<Feedback> findByMessageIdIn(List<Long> messageIds);
}