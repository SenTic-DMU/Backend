package com.project.sentic.domain.user.dto;

import lombok.Getter;

@Getter
public class SocialLoginRequest {

    private String accessToken;
    private String code;
}