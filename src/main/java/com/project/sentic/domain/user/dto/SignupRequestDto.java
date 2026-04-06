package com.project.sentic.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/**
 * 회원가입 요청 DTO
 *
 * 클라이언트에서 회원가입할 때 보내는 데이터예요.
 * @Valid 어노테이션으로 유효성 검사를 해요.
 *
 * 요청 예시:
 * {
 *   "email": "test@email.com",
 *   "password": "password123",
 *   "nickname": "민정"
 * }
 */
@Getter
public class SignupRequestDto {

    @NotBlank(message = "이메일을 입력해주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하여야 합니다.")
    private String nickname;
}
