package com.project.sentic.domain.badge.controller;

import com.project.sentic.domain.badge.dto.BadgeResponse;
import com.project.sentic.domain.badge.dto.FeaturedBadgeRequest;
import com.project.sentic.domain.badge.service.BadgeService;
import com.project.sentic.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Badge", description = "뱃지 API")
@RestController
@RequestMapping("/api/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @Operation(summary = "뱃지판 조회", description = "전체 뱃지 목록 + 획득 여부 + 대표 뱃지 여부")
    @GetMapping
    public ApiResponse<List<BadgeResponse>> getBadgeBoard(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(badgeService.getBadgeBoard(userId));
    }

    @Operation(summary = "대표 뱃지 설정", description = "최대 3개까지 대표 뱃지 선택")
    @PutMapping("/featured")
    public ApiResponse<List<BadgeResponse>> setFeaturedBadges(
            @AuthenticationPrincipal Long userId,
            @RequestBody FeaturedBadgeRequest request) {
        return ApiResponse.success(badgeService.setFeaturedBadges(userId, request));
    }

    @Operation(summary = "뱃지 조건 체크", description = "조건 충족 시 자동 뱃지 부여 + 새로 획득한 뱃지 반환")
    @PostMapping("/check")
    public ApiResponse<List<BadgeResponse>> checkBadges(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(badgeService.checkAndAwardBadges(userId));
    }
}