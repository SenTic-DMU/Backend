package com.project.sentic.domain.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FrequentExpressionResponse {

    private String expression;
    private int count;
}