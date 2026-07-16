package com.project.sentic.domain.admin.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminFaqRequest {

    private String question;
    private String answer;
}