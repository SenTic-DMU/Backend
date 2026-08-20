package com.project.sentic.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StudyStatsResponse {

    private int totalMinutes;
    private int avgMinutes;
    private int continuousDays;
    private List<DailyStudy> weekly;

    @Getter
    @Builder
    public static class DailyStudy {
        private String day;
        private int minute;
    }
}