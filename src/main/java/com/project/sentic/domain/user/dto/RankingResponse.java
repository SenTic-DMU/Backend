package com.project.sentic.domain.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RankingResponse {

    private int rank;
    private Long userId;
    private String nickname;
    private int weeklyScore;
}