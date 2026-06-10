package com.project.sentic.infra.ai;

import com.project.sentic.global.common.ApiResponse;
import com.project.sentic.infra.ai.dto.ChatCompletionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "AI Test", description = "AI 연동 선행 검증용 엔드포인트")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiTestController {

    private final OpenAiService openAiService;

    @Operation(summary = "GPT-4o 단순 호출 테스트")
    @PostMapping("/test/chat")
    public ApiResponse<String> testChat(@Valid @RequestBody ChatTestRequest request) {
        String reply = openAiService.chat(request.getMessage());
        return ApiResponse.success(reply);
    }

    @Operation(summary = "GPT-4o 시스템 프롬프트 + 대화 테스트")
    @PostMapping("/test/chat-with-system")
    public ApiResponse<ChatCompletionResponse> testChatWithSystem(@Valid @RequestBody SystemChatTestRequest request) {
        ChatCompletionResponse response = openAiService.chatWithHistory(
                request.getSystemPrompt(),
                java.util.List.of(),
                request.getMessage()
        );
        return ApiResponse.success(response);
    }

    @Operation(summary = "Whisper STT 테스트", description = "음성 파일(mp3/wav)을 텍스트로 변환합니다.")
    @PostMapping(value = "/test/stt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> testStt(@RequestParam("file") MultipartFile file) {
        String text = openAiService.transcribe(file);
        return ApiResponse.success(text);
    }

    @Operation(summary = "TTS 테스트", description = "텍스트를 음성(mp3)으로 변환하여 바이트로 반환합니다. voice: alloy/echo/fable/onyx/nova/shimmer")
    @PostMapping("/test/tts")
    public ResponseEntity<byte[]> testTts(@Valid @RequestBody TtsTestRequest request) {
        byte[] audio = openAiService.textToSpeech(request.getText(), request.getVoice());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"speech.mp3\"")
                .body(audio);
    }

    @Getter
    @NoArgsConstructor
    static class ChatTestRequest {
        @NotBlank
        private String message;
    }

    @Getter
    @NoArgsConstructor
    static class SystemChatTestRequest {
        @NotBlank
        private String systemPrompt;
        @NotBlank
        private String message;
    }

    @Getter
    @NoArgsConstructor
    static class TtsTestRequest {
        @NotBlank
        private String text;
        private String voice = "alloy";  // alloy | echo | fable | onyx | nova | shimmer
    }
}
