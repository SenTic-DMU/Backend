package com.project.sentic.domain.user.controller;

import com.project.sentic.domain.user.dto.*;
import com.project.sentic.domain.user.service.AuthService;
import com.project.sentic.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 컨트롤러
 *
 * 회원가입, 로그인, 토큰 재발급 API를 담당해요.
 *
 * POST /api/auth/signup   → 회원가입
 * POST /api/auth/login    → 로그인
 * POST /api/auth/refresh  → 토큰 재발급
 * POST /api/auth/logout   → 로그아웃
 * POST /api/auth/find-id  → 아이디 찾기
 */
@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입
     * 이메일, 비밀번호, 닉네임으로 회원가입 후 JWT 토큰 반환
     */
    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ApiResponse<TokenResponseDto> signup(@Valid @RequestBody SignupRequestDto request) {
        return ApiResponse.success(authService.signup(request));
    }

    /**
     * 로그인
     * 이메일, 비밀번호로 로그인 후 JWT 토큰 반환
     */
    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ApiResponse<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * Access Token 재발급
     * Refresh Token으로 새 Access Token 발급
     */
    @Operation(summary = "토큰 재발급")
    @PostMapping("/refresh")
    public ApiResponse<TokenResponseDto> refresh(@RequestHeader("Refresh-Token") String refreshToken) {
        return ApiResponse.success(authService.refresh(refreshToken));
    }

    /**
     * 로그아웃
     * 클라이언트에서 토큰을 삭제하는 방식으로 처리
     * 추후 Redis 도입 시 서버에서 토큰 무효화 가능
     */
    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ApiResponse<String> logout() {
        return ApiResponse.success("로그아웃 되었습니다.");
    }

    @Operation(summary = "아이디 찾기", description = "가입한 이메일을 입력하면 loginId를 반환합니다.")
    @PostMapping("/find-id")
    public ApiResponse<FindEmailResponseDto> findLoginId(@Valid @RequestBody FindEmailRequestDto request) {
        return ApiResponse.success(authService.findLoginId(request));
    }

    @Operation(summary = "비밀번호 재설정 인증코드 발송", description = "입력한 이메일로 6자리 인증코드를 발송합니다. 인증코드는 5분간 유효합니다.")
    @PostMapping("/password/reset-request")
    public ApiResponse<String> sendPasswordResetCode(@Valid @RequestBody PasswordResetRequestDto request) {
        authService.sendPasswordResetCode(request);
        return ApiResponse.success("인증코드가 발송되었습니다.");
    }

    @Operation(summary = "인증코드 확인", description = "발송된 인증코드의 유효성을 확인합니다.")
    @PostMapping("/password/verify-code")
    public ApiResponse<String> verifyCode(@Valid @RequestBody VerifyCodeRequestDto request) {
        authService.verifyCode(request);
        return ApiResponse.success("인증코드가 확인되었습니다.");
    }

    @Operation(summary = "비밀번호 재설정", description = "인증코드 검증 후 새 비밀번호로 변경합니다.")
    @PostMapping("/password/reset")
    public ApiResponse<String> resetPassword(@Valid @RequestBody PasswordResetDto request) {
        authService.resetPassword(request);
        return ApiResponse.success("비밀번호가 재설정되었습니다.");
    }

    @Operation(summary = "카카오 소셜 로그인", description = "프론트에서 발급받은 카카오 accessToken으로 로그인/자동 회원가입합니다.")
    @PostMapping("/kakao")
    public ApiResponse<TokenResponseDto> kakaoLogin(@Valid @RequestBody SocialLoginRequest request) {
        return ApiResponse.success(authService.kakaoLogin(request));
    }

    @Operation(summary = "구글 소셜 로그인", description = "프론트에서 발급받은 구글 accessToken으로 로그인/자동 회원가입합니다.")
    @PostMapping("/google")
    public ApiResponse<TokenResponseDto> googleLogin(@Valid @RequestBody SocialLoginRequest request) {
        return ApiResponse.success(authService.googleLogin(request));
    }
}
