package com.project.sentic.domain.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReportSummaryResponse {

    // 월별 캘린더
    private List<ReportCalendarResponse> calendar;

    // 피드백 비율
    private int totalMessages;        // 전체 메시지 수
    private int feedbackMessages;     // 피드백 받은 메시지 수
    private double feedbackRate;      // 피드백 비율 (%)

    // 퀴즈 정답률
    private int totalQuizQuestions;   // 전체 퀴즈 문제 수
    private int correctQuizAnswers;   // 맞힌 문제 수
    private double quizAccuracy;      // 정답률 (%)

    // 음성/채팅 비율
    private int totalVoiceCount;      // 총 음성 대화 수
    private int totalChatCount;       // 총 채팅 대화 수
    private double voiceRate;         // 음성 비율 (%)
    private double chatRate;          // 채팅 비율 (%)
}