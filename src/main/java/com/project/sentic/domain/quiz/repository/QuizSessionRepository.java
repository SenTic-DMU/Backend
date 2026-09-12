package com.project.sentic.domain.quiz.repository;

import com.project.sentic.domain.quiz.entity.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

    // 퀴즈 기록 조회 (최신순)
    List<QuizSession> findByUserIdOrderByCreatedAtDesc(Long userId);
}