package com.project.sentic.domain.feedback.dto;

import com.project.sentic.domain.feedback.entity.Feedback;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FeedbackResponse {

    private Long id;
    private Long roomId;
    private Long messageId;
    private String wordErrors;
    private String grammarErrors;
    private String expressionErrors;
    private String perfectSentence;
    private LocalDateTime createdAt;

    public static FeedbackResponse from(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .roomId(feedback.getRoomId())
                .messageId(feedback.getMessageId())
                .wordErrors(feedback.getWordErrors())
                .grammarErrors(feedback.getGrammarErrors())
                .expressionErrors(feedback.getExpressionErrors())
                .perfectSentence(feedback.getPerfectSentence())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}