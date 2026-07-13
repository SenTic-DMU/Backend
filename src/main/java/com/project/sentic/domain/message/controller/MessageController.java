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
import com.project.sentic.domain.message.dto.VoiceResponse;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import com.project.sentic.domain.message.dto.MessageResponse;

import java.util.List;

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

    @Operation(
            summary = "음성 대화",
            description = "음성 파일을 업로드하면 STT → GPT → TTS → S3 저장 후 URL로 응답합니다."
    )
    @PostMapping(value = "/{roomId}/messages/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<VoiceResponse> voice(
            @PathVariable Long roomId,
            @RequestParam("file") MultipartFile audioFile,
            @AuthenticationPrincipal Long userId) {
        VoiceResponse response = messageService.voice(userId, roomId, audioFile);
        return ApiResponse.success(response);
    }

    @Operation(summary = "대화 기록 조회")
    @GetMapping("/{roomId}/messages")
    public ApiResponse<List<MessageResponse>> getMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(messageService.getMessages(userId, roomId));
    }

    @Operation(
            summary = "방 입장",
            description = "방 입장 시 AI가 먼저 말합니다. 첫 대화면 인사, 마지막이 AI면 재생, 마지막이 USER면 응답 생성."
    )
    @PostMapping("/{roomId}/enter")
    public ApiResponse<VoiceResponse> enterRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(messageService.enterRoom(userId, roomId));
    }
}
