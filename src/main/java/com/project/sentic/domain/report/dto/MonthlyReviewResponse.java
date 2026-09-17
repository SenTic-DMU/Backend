package com.project.sentic.domain.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonthlyReviewResponse {

    private String month;
    private String summary;
    private String generatedAt;
}