package com.project.sentic.domain.user.controller;

import com.project.sentic.domain.user.dto.MyPageResponse;
import com.project.sentic.domain.user.dto.UserSettingsResponse;
import com.project.sentic.domain.user.dto.UserSettingsUpdateRequest;
import com.project.sentic.domain.user.dto.WithdrawRequestDto;
import com.project.sentic.domain.user.service.UserService;
import com.project.sentic.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 회원 탈퇴 (기존 유지)
    @Operation(summary = "회원 탈퇴", description = "LOCAL 계정은 비밀번호 확인 필수. 소셜 계정은 password 필드 불필요.")
    @DeleteMapping("/me")
    public ApiResponse<String> withdraw(
            @AuthenticationPrincipal Long userId,
            @RequestBody WithdrawRequestDto request) {
        userService.withdraw(userId, request);
        return ApiResponse.success("회원 탈퇴가 완료되었습니다.");
    }

    // 마이페이지 조회
    @Operation(summary = "마이페이지 조회")
    @GetMapping("/me")
    public ApiResponse<MyPageResponse> getMyPage(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getMyPage(userId));
    }

    // 학습 레벨 변경
    @Operation(summary = "학습 레벨 변경")
    @PutMapping("/me/level")
    public ApiResponse<UserSettingsResponse> updateLevel(
            @AuthenticationPrincipal Long userId,
            @RequestParam String difficulty) {
        return ApiResponse.success(userService.updateLevel(userId, difficulty));
    }

    // 설정 조회
    @Operation(summary = "설정 조회")
    @GetMapping("/me/settings")
    public ApiResponse<UserSettingsResponse> getSettings(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getSettings(userId));
    }

    // 설정 변경
    @Operation(summary = "설정 변경")
    @PutMapping("/me/settings")
    public ApiResponse<UserSettingsResponse> updateSettings(
            @AuthenticationPrincipal Long userId,
            @RequestBody UserSettingsUpdateRequest request) {
        return ApiResponse.success(userService.updateSettings(userId, request));
    }

    // 앱 접속 시작
    @Operation(summary = "앱 접속 시작 (학습 시간 기록 시작)")
    @PostMapping("/me/session/start")
    public ApiResponse<Void> startSession(
            @AuthenticationPrincipal Long userId) {
        userService.startSession(userId);
        return ApiResponse.success(null);
    }

    // 앱 접속 종료
    @Operation(summary = "앱 접속 종료 (학습 시간 기록 종료)")
    @PostMapping("/me/session/end")
    public ApiResponse<Void> endSession(
            @AuthenticationPrincipal Long userId) {
        userService.endSession(userId);
        return ApiResponse.success(null);
    }
}