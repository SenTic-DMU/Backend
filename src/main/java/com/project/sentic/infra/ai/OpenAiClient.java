package com.project.sentic.infra.ai;

import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import com.project.sentic.infra.ai.dto.ChatCompletionRequest;
import com.project.sentic.infra.ai.dto.ChatCompletionResponse;
import com.project.sentic.infra.ai.dto.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class OpenAiClient {

    private final WebClient webClient;
    private final String chatModel;

    public OpenAiClient(
            @Qualifier("openAiWebClient") WebClient webClient,
            @Value("${ai.openai.chat-model}") String chatModel) {
        this.webClient = webClient;
        this.chatModel = chatModel;
    }

    public ChatCompletionResponse chat(List<ChatMessage> messages) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(chatModel)
                .messages(messages)
                .build();

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .doOnNext(body -> log.error("OpenAI API 오류: {}", body))
                                .then(Mono.error(new CustomException(ErrorCode.AI_API_ERROR)))
                )
                .bodyToMono(ChatCompletionResponse.class)
                .block();
    }
}
