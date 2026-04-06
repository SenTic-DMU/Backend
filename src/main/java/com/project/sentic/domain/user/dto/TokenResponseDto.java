package com.project.sentic.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 토큰 응답 DTO
 *
 * 로그인/회원가입 성공 시 클라이언트에게 반환하는 토큰 정보예요.
 *
 * 응답 예시:
 * {
 *   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
 *   "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
 * }
 *
 * 프론트에서 accessToken은 API 호출 시 헤더에 담아서 보내요.
 * Authorization: Bearer {accessToken}
 */
@Getter
@AllArgsConstructor
public class TokenResponseDto {

    private String accessToken;
    private String refreshToken;
}
