package com.project.sentic.domain.report.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ModeRatioResponse {

    private int voiceMinutes;
    private int chatMinutes;
}