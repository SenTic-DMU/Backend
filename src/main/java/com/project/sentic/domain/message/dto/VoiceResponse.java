package com.project.sentic.domain.message.dto;

import com.project.sentic.domain.feedback.dto.FeedbackResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VoiceResponse {
    private String userText;
    private String aiText;
    private String audioUrl;
    private FeedbackResponse feedback;
}