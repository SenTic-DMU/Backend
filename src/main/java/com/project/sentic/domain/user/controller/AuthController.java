package com.project.sentic.domain.user.controller;

import com.project.sentic.domain.user.dto.LoginRequestDto;
import com.project.sentic.domain.user.dto.SignupRequestDto;
import com.project.sentic.domain.user.dto.TokenResponseDto;
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
 * POST /api/auth/signup  → 회원가입
 * POST /api/auth/login   → 로그인
 * POST /api/auth/refresh → 토큰 재발급
 * POST /api/auth/logout  → 로그아웃
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
}
