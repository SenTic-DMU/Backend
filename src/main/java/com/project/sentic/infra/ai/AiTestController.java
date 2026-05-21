package com.project.sentic.infra.ai;

import com.project.sentic.global.common.ApiResponse;
import com.project.sentic.infra.ai.dto.ChatCompletionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Test", description = "AI 연동 선행 검증용 엔드포인트")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiTestController {

    private final OpenAiService openAiService;

    @Operation(summary = "GPT-4o 단순 호출 테스트")
    @PostMapping("/test/chat")
    public ApiResponse<String> testChat(@RequestBody ChatTestRequest request) {
        String reply = openAiService.chat(request.getMessage());
        return ApiResponse.success(reply);
    }

    @Operation(summary = "GPT-4o 시스템 프롬프트 + 대화 테스트")
    @PostMapping("/test/chat-with-system")
    public ApiResponse<ChatCompletionResponse> testChatWithSystem(@RequestBody SystemChatTestRequest request) {
        ChatCompletionResponse response = openAiService.chatWithHistory(
                request.getSystemPrompt(),
                java.util.List.of(),
                request.getMessage()
        );
        return ApiResponse.success(response);
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
}
