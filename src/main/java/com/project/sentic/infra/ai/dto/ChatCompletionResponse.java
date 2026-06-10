package com.project.sentic.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class ChatCompletionResponse {

    private String id;
    private List<Choice> choices;
    private Usage usage;

    public String getContent() {
        if (choices == null || choices.isEmpty()) return null;
        return choices.get(0).getMessage().getContent();
    }

    @Getter
    @NoArgsConstructor
    public static class Choice {
        private ChatMessage message;

        @JsonProperty("finish_reason")
        private String finishReason;

        private int index;
    }

    @Getter
    @NoArgsConstructor
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;

        @JsonProperty("completion_tokens")
        private int completionTokens;

        @JsonProperty("total_tokens")
        private int totalTokens;
    }
}
