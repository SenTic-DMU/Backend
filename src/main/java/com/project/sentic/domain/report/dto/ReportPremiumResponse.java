package com.project.sentic.domain.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReportPremiumResponse {

    // 무료 레포트 포함
    private ReportSummaryResponse summary;

    // 나의 약점 TOP 3
    private List<WeaknessItem> weaknesses;

    // 실력 성장 그래프
    private GrowthData growth;

    // 자주 쓰는 표현 분석
    private List<ExpressionItem> expressions;

    // AI 월말 종합 평가
    private String aiEvaluation;

    @Getter
    @Builder
    public static class WeaknessItem {
        private int rank;           // 순위
        private String category;    // WORD, GRAMMAR, EXPRESSION
        private String pattern;     // 자주 틀리는 패턴
        private int count;          // 틀린 횟수
        private String explanation; // 설명
    }

    @Getter
    @Builder
    public static class GrowthData {
        private double lastMonthFeedbackRate;   // 지난달 피드백 비율
        private double thisMonthFeedbackRate;   // 이번달 피드백 비율
        private double lastMonthQuizAccuracy;   // 지난달 퀴즈 정답률
        private double thisMonthQuizAccuracy;   // 이번달 퀴즈 정답률
        private String improvement;             // 성장 요약
    }

    @Getter
    @Builder
    public static class ExpressionItem {
        private String myExpression;        // 내가 자주 쓰는 표현
        private int useCount;               // 사용 횟수
        private String recommendedExpression; // 추천 표현
        private String explanation;          // 설명
    }
}