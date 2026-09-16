package com.project.sentic.domain.user.dto;

import com.project.sentic.domain.badge.dto.BadgeResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LeagueResponse {

    private String league;
    private int myRank;
    private int myScore;
    private int totalMembers;
    private List<LeagueMemberItem> members;

    @Getter
    @Builder
    public static class LeagueMemberItem {
        private int rank;
        private Long userId;
        private String nickname;
        private int weeklyScore;
        private List<BadgeResponse> featuredBadges;
    }
}