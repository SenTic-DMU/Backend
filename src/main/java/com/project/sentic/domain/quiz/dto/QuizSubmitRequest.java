package com.project.sentic.domain.quiz.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class QuizSubmitRequest {

    private List<AnswerItem> answers;

    @Getter
    @NoArgsConstructor
    public static class AnswerItem {
        private int questionNo;
        private String userAnswer;
    }
}