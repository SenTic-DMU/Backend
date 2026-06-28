package com.project.sentic.domain.message.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VoiceResponse {
    private String aiText;    // AI 응답 텍스트
    private String audioUrl;  // S3 업로드된 음성 URL
}