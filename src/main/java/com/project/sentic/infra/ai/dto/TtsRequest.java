package com.project.sentic.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TtsRequest {
    private String model;
    private String input;
    private String voice;
}
