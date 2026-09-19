package com.project.sentic.domain.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WeakPointsResponse {

    private List<ErrorItem> words;
    private List<ErrorItem> grammar;
    private List<ErrorItem> expressions;

    @Getter
    @Builder
    public static class ErrorItem {
        private String text;
        private int count;
    }
}