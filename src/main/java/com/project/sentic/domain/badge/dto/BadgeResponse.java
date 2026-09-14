package com.project.sentic.domain.badge.dto;

import com.project.sentic.domain.badge.entity.Badge;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BadgeResponse {

    private Long id;
    private String badgeCode;
    private String badgeName;
    private String badgeIcon;
    private String description;
    private String conditionText;
    private String difficulty;
    private boolean earned;       // 획득 여부
    private boolean featured;     // 대표 뱃지 여부

    public static BadgeResponse of(Badge badge, boolean earned, boolean featured) {
        return BadgeResponse.builder()
                .id(badge.getId())
                .badgeCode(badge.getBadgeCode())
                .badgeName(badge.getBadgeName())
                .badgeIcon(badge.getBadgeIcon())
                .description(badge.getDescription())
                .conditionText(badge.getConditionText())
                .difficulty(badge.getDifficulty().name())
                .earned(earned)
                .featured(featured)
                .build();
    }
}