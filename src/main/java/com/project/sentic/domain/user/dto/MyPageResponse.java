package com.project.sentic.domain.user.dto;

import com.project.sentic.domain.user.entity.User;
import com.project.sentic.domain.user.entity.UserSettings;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyPageResponse {

    // 유저 정보
    private Long id;
    private String nickname;
    private String email;
    private String profileImage;
    private String provider;
    private String role;

    // 학습 레벨
    private String defaultDifficulty;

    // 학습 통계
    private int weeklyStudyTime;  // 분 단위
    private int dailyAvgTime;     // 분 단위
    private int streakDays;       // 연속 학습일

    public static MyPageResponse of(User user, UserSettings settings) {
        return MyPageResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImage(user.getProfileImage())
                .provider(user.getProvider().name())
                .role(user.getRole().name())
                .defaultDifficulty(settings.getDefaultDifficulty().name())
                .weeklyStudyTime(settings.getWeeklyStudyTime())
                .dailyAvgTime(settings.getDailyAvgTime())
                .streakDays(settings.getStreakDays())
                .build();
    }
}