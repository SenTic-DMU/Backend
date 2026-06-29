package com.project.sentic.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserSettingsUpdateRequest {

    private Boolean notificationEnabled;
    private String defaultMode;
    private String defaultDifficulty;
}