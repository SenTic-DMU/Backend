package com.project.sentic.domain.scrap.dto;

import com.project.sentic.domain.scrap.entity.Scrap;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ScrapResponse {

    private Long scrapId;
    private Long feedbackId;
    private Long roomId;            // ← 추가
    private String expression;
    private String context;
    private Scrap.ScrapCategory category;
    private LocalDateTime createdAt;

    public static ScrapResponse from(Scrap scrap) {
        return ScrapResponse.builder()
                .scrapId(scrap.getId())
                .feedbackId(scrap.getFeedbackId())
                .roomId(scrap.getRoomId())      // ← 추가
                .expression(scrap.getExpression())
                .context(scrap.getContext())
                .category(scrap.getCategory())
                .createdAt(scrap.getCreatedAt())
                .build();
    }
}