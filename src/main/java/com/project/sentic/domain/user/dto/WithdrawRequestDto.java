package com.project.sentic.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WithdrawRequestDto {

    // LOCAL 유저만 필수 — 소셜 로그인 유저는 비밀번호 없음
    private String password;
}
