package com.project.sentic.domain.badge.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class FeaturedBadgeRequest {

    private List<Long> badgeIds;  // 대표 뱃지로 설정할 뱃지 ID (최대 3개)
}