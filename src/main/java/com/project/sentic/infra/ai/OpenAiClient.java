package com.project.sentic.infra.ai;

import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import com.project.sentic.infra.ai.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class OpenAiClient {

    private final WebClient webClient;
    private final String chatModel;
    private final String sttModel;
    private final String ttsModel;

    public OpenAiClient(
            @Qualifier("openAiWebClient") WebClient webClient,
            @Value("${ai.openai.chat-model}") String chatModel,
            @Value("${ai.openai.stt-model}") String sttModel,
            @Value("${ai.openai.tts-model}") String ttsModel) {
        this.webClient = webClient;
        this.chatModel = chatModel;
        this.sttModel = sttModel;
        this.ttsModel = ttsModel;
    }

    public ChatCompletionResponse chat(List<ChatMessage> messages) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(chatModel)
                .messages(messages)
                .build();

        ChatCompletionResponse response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        res -> res.bodyToMono(String.class)
                                .doOnNext(body -> log.error("OpenAI Chat API 오류: {}", body))
                                .then(Mono.error(new CustomException(ErrorCode.AI_API_ERROR)))
                )
                .bodyToMono(ChatCompletionResponse.class)
                .block();

        if (response != null && response.getUsage() != null) {
            ChatCompletionResponse.Usage usage = response.getUsage();
            log.info("[Token Usage] prompt={}, completion={}, total={}",
                    usage.getPromptTokens(),
                    usage.getCompletionTokens(),
                    usage.getTotalTokens());
        }

        return response;
    }

    public String transcribe(MultipartFile file) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        }).contentType(MediaType.parseMediaType(
                file.getContentType() != null ? file.getContentType() : "audio/mpeg"
        ));
        bodyBuilder.part("model", sttModel);
        bodyBuilder.part("language", "en");

        SttResponse response = webClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        res -> res.bodyToMono(String.class)
                                .doOnNext(body -> log.error("Whisper STT API 오류: {}", body))
                                .then(Mono.error(new CustomException(ErrorCode.AI_API_ERROR)))
                )
                .bodyToMono(SttResponse.class)
                .block();

        return response.getText();
    }

    public byte[] textToSpeech(String text, String voice) {
        TtsRequest request = TtsRequest.builder()
                .model(ttsModel)
                .input(text)
                .voice(voice)
                .build();

        return webClient.post()
                .uri("/audio/speech")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        res -> res.bodyToMono(String.class)
                                .doOnNext(body -> log.error("TTS API 오류: {}", body))
                                .then(Mono.error(new CustomException(ErrorCode.AI_API_ERROR)))
                )
                .bodyToMono(byte[].class)
                .block();
    }
}
