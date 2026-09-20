package com.project.sentic.domain.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeedbackStatsResponse {

    private int totalUtterances;
    private int cleanUtterances;
    private double cleanRatio;
}