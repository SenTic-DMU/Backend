package com.project.sentic.domain.quiz.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizResultResponse {

    private Long quizId;
    private int score;
    private int total;
    private List<QuizResultItem> results;

    @Getter
    @Builder
    public static class QuizResultItem {
        private int questionNo;
        private String questionType;
        private String sentence;       // 원래 문장
        private String translation;    // 한국어 해석
        private String answer;         // 정답
        private String userAnswer;     // 사용자 답
        private boolean correct;       // 정답 여부
        private String explanation;    // 해설 (틀린 경우)
    }
}