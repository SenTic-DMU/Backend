package com.project.sentic.domain.faq.dto;

import com.project.sentic.domain.faq.entity.Faq;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FaqResponse {

    private Long id;
    private String question;
    private String answer;
    private int orderNum;

    public static FaqResponse from(Faq faq) {
        return FaqResponse.builder()
                .id(faq.getId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .orderNum(faq.getOrderNum())
                .build();
    }
}