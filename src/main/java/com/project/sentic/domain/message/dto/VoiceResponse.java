package com.project.sentic.domain.message.dto;

import com.project.sentic.domain.feedback.dto.FeedbackResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VoiceResponse {
    private String aiText;
    private String audioUrl;
    private FeedbackResponse feedback; // null이면 피드백 없음
}