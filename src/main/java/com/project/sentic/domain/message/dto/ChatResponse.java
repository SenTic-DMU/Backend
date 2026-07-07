package com.project.sentic.domain.message.dto;

import com.project.sentic.domain.feedback.dto.FeedbackResponse;

public record ChatResponse(
        String content,
        FeedbackResponse feedback  // null이면 피드백 없음
) {}