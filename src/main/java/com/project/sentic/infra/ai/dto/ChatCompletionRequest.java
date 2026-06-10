package com.project.sentic.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {

    private String model;
    private List<ChatMessage> messages;

    @JsonProperty("max_tokens")
    private Integer maxTokens;
}
