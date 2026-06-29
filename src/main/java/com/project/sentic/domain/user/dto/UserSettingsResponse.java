package com.project.sentic.domain.user.dto;

import com.project.sentic.domain.user.entity.UserSettings;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSettingsResponse {

    private String defaultMode;
    private String defaultDifficulty;
    private boolean subtitleEnabled;
    private boolean notificationEnabled;

    public static UserSettingsResponse from(UserSettings settings) {
        return UserSettingsResponse.builder()
                .defaultMode(settings.getDefaultMode().name())
                .defaultDifficulty(settings.getDefaultDifficulty().name())
                .subtitleEnabled(settings.isSubtitleEnabled())
                .notificationEnabled(settings.isNotificationEnabled())
                .build();
    }
}