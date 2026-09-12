package com.project.sentic.domain.quiz.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizStartResponse {

    private Long quizId;
    private int total;
    private List<QuizQuestionItem> questions;

    @Getter
    @Builder
    public static class QuizQuestionItem {
        private int questionNo;
        private String questionType;
        private String sentence;         // 빈칸이 뚫린 문장 or 섞인 단어
        private String translation;      // 한국어 해석
        private List<String> options;    // 객관식 보기 (해당 시)
    }
}