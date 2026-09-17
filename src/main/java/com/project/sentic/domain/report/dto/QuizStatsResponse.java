package com.project.sentic.domain.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizStatsResponse {

    private int totalAttempted;
    private int totalCorrect;
    private double accuracy;
}