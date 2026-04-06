package com.project.sentic.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * 로그인 요청 DTO
 *
 * 클라이언트에서 로그인할 때 보내는 데이터예요.
 *
 * 요청 예시:
 * {
 *   "email": "test@email.com",
 *   "password": "password123"
 * }
 */
@Getter
public class LoginRequestDto {

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;
}
