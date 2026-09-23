package com.project.sentic.domain.report.dto;

import com.project.sentic.domain.report.entity.DailyStudyLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ReportCalendarResponse {

    private LocalDate date;
    private int studyMinutes;
    private int voiceCount;
    private int chatCount;

    public static ReportCalendarResponse from(DailyStudyLog log) {
        return ReportCalendarResponse.builder()
                .date(log.getStudyDate())
                .studyMinutes(log.getStudyMinutes())
                .voiceCount(log.getVoiceCount())
                .chatCount(log.getChatCount())
                .build();
    }
}