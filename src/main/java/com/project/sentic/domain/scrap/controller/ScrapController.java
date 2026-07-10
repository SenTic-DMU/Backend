package com.project.sentic.domain.scrap.controller;

import com.project.sentic.domain.scrap.dto.ScrapResponse;
import com.project.sentic.domain.scrap.dto.ScrapSaveRequest;
import com.project.sentic.domain.scrap.entity.Scrap;
import com.project.sentic.domain.scrap.service.ScrapService;
import com.project.sentic.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Scrap", description = "스크랩 API")
@RestController
@RequestMapping("/api/scraps")
@RequiredArgsConstructor
public class ScrapController {

    private final ScrapService scrapService;

    @Operation(summary = "스크랩 저장")
    @PostMapping
    public ApiResponse<ScrapResponse> saveScrap(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid ScrapSaveRequest request) {
        return ApiResponse.success(scrapService.saveScrap(userId, request));
    }

    @Operation(summary = "스크랩 목록 조회 (카테고리별 또는 대화방별)")
    @GetMapping
    public ApiResponse<List<ScrapResponse>> getScraps(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Scrap.ScrapCategory category,
            @RequestParam(required = false) Long roomId) {
        return ApiResponse.success(scrapService.getScraps(userId, category, roomId));
    }

    @Operation(summary = "스크랩 삭제")
    @DeleteMapping("/{scrapId}")
    public ApiResponse<Void> deleteScrap(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long scrapId) {
        scrapService.deleteScrap(userId, scrapId);
        return ApiResponse.success(null);
    }
}
