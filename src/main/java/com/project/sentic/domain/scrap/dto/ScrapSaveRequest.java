package com.project.sentic.domain.scrap.dto;

import com.project.sentic.domain.scrap.entity.Scrap;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ScrapSaveRequest {

    private Long feedbackId;

    @NotBlank(message = "표현을 입력해주세요.")
    private String expression;

    private String context;

    @NotNull(message = "카테고리를 선택해주세요.")
    private Scrap.ScrapCategory category;
}
