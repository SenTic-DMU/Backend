package com.project.sentic.domain.faq.controller;

import com.project.sentic.domain.faq.dto.FaqResponse;
import com.project.sentic.domain.faq.service.FaqService;
import com.project.sentic.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "FAQ", description = "자주 묻는 질문 API")
@RestController
@RequestMapping("/api/faq")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    // 자주 묻는 질문 목록 조회
    @Operation(summary = "자주 묻는 질문 목록 조회")
    @GetMapping
    public ApiResponse<List<FaqResponse>> getFaqs() {
        return ApiResponse.success(faqService.getFaqs());
    }
}