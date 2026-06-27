package com.project.sentic.domain.message.controller;

import com.project.sentic.domain.message.dto.ChatRequest;
import com.project.sentic.domain.message.dto.ChatResponse;
import com.project.sentic.domain.message.service.MessageService;
import com.project.sentic.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Message", description = "채팅 메시지 API")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(
            summary = "채팅 대화",
            description = "사용자 메시지를 전송하고 AI 응답을 받습니다. Sliding Window(최근 10개)로 대화 맥락을 유지합니다."
    )
    @PostMapping("/{roomId}/messages/chat")
    public ApiResponse<ChatResponse> chat(
            @PathVariable Long roomId,
            @RequestBody @Valid ChatRequest request,
            @AuthenticationPrincipal Long userId) {
        ChatResponse response = messageService.chat(userId, roomId, request.getContent());
        return ApiResponse.success(response);
    }
}
