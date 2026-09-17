package com.project.sentic.domain.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StudyStatsResponse {

    private List<DayItem> weekly;
    private int totalMinutes;
    private int avgMinutes;
    private int continuousDays;

    @Getter
    @Builder
    public static class DayItem {
        private String day;
        private int minute;
        private String date;
    }
}