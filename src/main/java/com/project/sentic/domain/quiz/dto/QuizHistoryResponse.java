package com.project.sentic.domain.quiz.dto;

import com.project.sentic.domain.quiz.entity.QuizSession;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class QuizHistoryResponse {

    private Long quizId;
    private int score;
    private int total;
    private LocalDateTime createdAt;

    public static QuizHistoryResponse from(QuizSession session) {
        return QuizHistoryResponse.builder()
                .quizId(session.getId())
                .score(session.getScore())
                .total(session.getTotal())
                .createdAt(session.getCreatedAt())
                .build();
    }
}