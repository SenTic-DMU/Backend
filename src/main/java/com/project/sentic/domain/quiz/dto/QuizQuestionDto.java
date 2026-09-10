package com.project.sentic.domain.quiz.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizQuestionDto {

    private int questionNo;          // 문제 번호 (1~5)
    private String questionType;     // FILL_BLANK, ARRANGE, MULTIPLE_CHOICE
    private String sentence;         // 문제 문장
    private String translation;      // 한국어 해석
    private List<String> options;    // 객관식 보기 (MULTIPLE_CHOICE만)
    private String answer;           // 정답 (채점용, 출제 시 프론트에 안 보냄)
    private String explanation;      // 해설 (채점용, 출제 시 프론트에 안 보냄)
}