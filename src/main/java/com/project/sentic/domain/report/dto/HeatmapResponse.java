package com.project.sentic.domain.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HeatmapResponse {

    private String date;
    private int minutes;
}