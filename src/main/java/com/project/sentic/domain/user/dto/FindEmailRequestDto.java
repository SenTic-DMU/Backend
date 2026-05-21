package com.project.sentic.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FindEmailRequestDto {

    @NotBlank(message = "닉네임을 입력해주세요.")
    private String nickname;
}
