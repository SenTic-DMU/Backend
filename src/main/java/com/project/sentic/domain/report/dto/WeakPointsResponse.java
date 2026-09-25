package com.project.sentic.domain.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WeakPointsResponse {

    private List<WeakPointItem> weakPoints;

    @Getter
    @Builder
    public static class WeakPointItem {
        private int rank;
        private String description;
        private int count;
    }
}