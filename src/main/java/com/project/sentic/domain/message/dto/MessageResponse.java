package com.project.sentic.domain.message.dto;

import com.project.sentic.domain.feedback.dto.FeedbackResponse;
import com.project.sentic.domain.message.entity.Message;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MessageResponse {

    private Long id;
    private String senderType;
    private String contentText;
    private String audioUrl;
    private int sequenceNo;
    private LocalDateTime createdAt;
    private FeedbackResponse feedback;  // 추가

    public static MessageResponse of(Message message, FeedbackResponse feedback) {
        return MessageResponse.builder()
                .id(message.getId())
                .senderType(message.getSenderType().name())
                .contentText(message.getContentText())
                .audioUrl(message.getAudioUrl())
                .sequenceNo(message.getSequenceNo())
                .createdAt(message.getCreatedAt())
                .feedback(feedback)
                .build();
    }
}