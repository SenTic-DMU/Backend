package com.project.sentic.domain.admin.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserStatusUpdateRequest {

    private String status; // ACTIVE, INACTIVE, BANNED
}