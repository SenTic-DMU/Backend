package com.project.sentic.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * 로그인 요청 DTO
 *
 * 요청 예시:
 * {
 *   "loginId": "myid123",
 *   "password": "password123"
 * }
 */
@Getter
public class LoginRequestDto {

    @NotBlank(message = "아이디를 입력해주세요.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;
}
