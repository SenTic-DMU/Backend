package com.project.sentic.domain.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GrowthTrendResponse {

    private String weekStart;
    private double quizAccuracy;
    private double feedbackCleanRatio;
}