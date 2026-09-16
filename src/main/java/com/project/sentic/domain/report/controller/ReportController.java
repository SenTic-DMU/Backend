package com.project.sentic.domain.report.controller;

import com.project.sentic.domain.report.dto.ReportPremiumResponse;
import com.project.sentic.domain.report.dto.ReportSummaryResponse;
import com.project.sentic.domain.report.service.ReportService;
import com.project.sentic.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Report", description = "학습 레포트 API")
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "무료 레포트 조회", description = "월별 캘린더, 피드백 비율, 퀴즈 정답률, 음성/채팅 비율")
    @GetMapping("/summary")
    public ApiResponse<ReportSummaryResponse> getSummaryReport(
            @AuthenticationPrincipal Long userId,
            @RequestParam int year,
            @RequestParam int month) {
        return ApiResponse.success(reportService.getSummaryReport(userId, year, month));
    }

    @Operation(summary = "유료 레포트 조회", description = "무료 포함 + 약점 TOP3, 실력 성장, AI 평가, 표현 분석")
    @GetMapping("/premium")
    public ApiResponse<ReportPremiumResponse> getPremiumReport(
            @AuthenticationPrincipal Long userId,
            @RequestParam int year,
            @RequestParam int month) {
        return ApiResponse.success(reportService.getPremiumReport(userId, year, month));
    }
}