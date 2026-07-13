package com.project.sentic.domain.message.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.sentic.domain.feedback.dto.FeedbackResponse;
import com.project.sentic.domain.feedback.service.FeedbackService;
import com.project.sentic.domain.message.dto.ChatResponse;
import com.project.sentic.domain.message.entity.Message;
import com.project.sentic.domain.message.repository.MessageRepository;
import com.project.sentic.domain.room.entity.Room;
import com.project.sentic.domain.room.repository.RoomRepository;
import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import com.project.sentic.infra.ai.OpenAiService;
import com.project.sentic.infra.ai.PromptBuilder;
import com.project.sentic.infra.ai.dto.CharacterInfo;
import com.project.sentic.infra.ai.dto.ChatMessage;
import com.project.sentic.infra.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.sentic.domain.message.dto.VoiceResponse;
import org.springframework.web.multipart.MultipartFile;
import com.project.sentic.domain.message.dto.MessageResponse;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final OpenAiService openAiService;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final S3Service s3Service;
    private final FeedbackService feedbackService;

    @Transactional
    public ChatResponse chat(Long userId, Long roomId, String content) {
        Room room = roomRepository.findByIdAndDeletedFalse(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ROOM_ACCESS_DENIED);
        }

        String systemPrompt = buildSystemPrompt(room);

        List<Message> recent = messageRepository.findTop10ByRoomIdOrderBySequenceNoDesc(roomId);
        Collections.reverse(recent);

        List<ChatMessage> history = recent.stream()
                .map(m -> m.getSenderType() == Message.SenderType.USER
                        ? ChatMessage.user(m.getContentText())
                        : ChatMessage.assistant(m.getContentText()))
                .toList();

        String aiContent = openAiService.chatWithHistory(systemPrompt, history, content).getContent();

        int nextSeq = messageRepository.findMaxSequenceNoByRoomId(roomId) + 1;

        // 유저 메시지 저장
        Message userMessage = messageRepository.save(Message.builder()
                .roomId(roomId)
                .senderType(Message.SenderType.USER)
                .contentText(content)
                .sequenceNo(nextSeq)
                .build());

        messageRepository.save(Message.builder()
                .roomId(roomId)
                .senderType(Message.SenderType.AI)
                .contentText(aiContent)
                .sequenceNo(nextSeq + 1)
                .build());

        room.updateMemoryBank(extractMemoryBank(room.getMemoryBank(), content, aiContent));
        room.updateLastActiveAt();

        // 피드백 생성 (실패해도 대화는 정상 진행)
        FeedbackResponse feedback = null;
        try {
            feedback = feedbackService.generateFeedback(
                    roomId,
                    userMessage.getId(),
                    content,
                    room.getDifficulty().name()
            );
        } catch (Exception e) {
            log.warn("[Feedback] 피드백 생성 실패: {}", e.getMessage());
        }

        return new ChatResponse(aiContent, feedback);
    }

    @Transactional
    public VoiceResponse voice(Long userId, Long roomId, MultipartFile audioFile) {
        Room room = roomRepository.findByIdAndDeletedFalse(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ROOM_ACCESS_DENIED);
        }

        // 1. STT: 음성 → 텍스트
        String userText = openAiService.transcribe(audioFile);

        // 2. 시스템 프롬프트 빌드
        String systemPrompt = buildSystemPrompt(room);

        // 3. Sliding Window: 최근 10개 메시지
        List<Message> recent = messageRepository.findTop10ByRoomIdOrderBySequenceNoDesc(roomId);
        Collections.reverse(recent);

        List<ChatMessage> history = recent.stream()
                .map(m -> m.getSenderType() == Message.SenderType.USER
                        ? ChatMessage.user(m.getContentText())
                        : ChatMessage.assistant(m.getContentText()))
                .toList();

        // 4. GPT-4o 응답 생성
        String aiContent = openAiService.chatWithHistory(systemPrompt, history, userText).getContent();

        // 5. 유저 메시지 저장
        int nextSeq = messageRepository.findMaxSequenceNoByRoomId(roomId) + 1;
        Message userMessage = messageRepository.save(Message.builder()
                .roomId(roomId)
                .senderType(Message.SenderType.USER)
                .contentText(userText)
                .sequenceNo(nextSeq)
                .build());

        messageRepository.save(Message.builder()
                .roomId(roomId)
                .senderType(Message.SenderType.AI)
                .contentText(aiContent)
                .sequenceNo(nextSeq + 1)
                .build());

        // 6. Memory Bank 업데이트
        room.updateMemoryBank(extractMemoryBank(room.getMemoryBank(), userText, aiContent));
        room.updateLastActiveAt();

        // 7. TTS: AI 텍스트 → 음성
        byte[] audioData = openAiService.textToSpeech(aiContent, "alloy");

        // 8. S3 업로드
        String audioUrl = s3Service.uploadAudio(audioData, "voice/" + roomId);

        // 9. 피드백 생성 (실패해도 대화는 정상 진행)
        FeedbackResponse feedback = null;
        try {
            feedback = feedbackService.generateFeedback(
                    roomId,
                    userMessage.getId(),
                    userText,
                    room.getDifficulty().name()
            );
        } catch (Exception e) {
            log.warn("[Feedback] 피드백 생성 실패: {}", e.getMessage());
        }

        return new VoiceResponse(aiContent, audioUrl, feedback);
    }

    private String buildSystemPrompt(Room room) {
        List<CharacterInfo> characters = parseCharacters(room.getCharacters());
        PromptBuilder.PromptContext ctx = new PromptBuilder.PromptContext(
                room.getRoomName(),
                room.getSituation(),
                characters,
                room.getDifficulty().name(),
                room.getMemoryBank()
        );
        return promptBuilder.build(ctx);
    }

    private String extractMemoryBank(String currentMemoryBank, String userMessage, String aiMessage) {
        String current = (currentMemoryBank != null && !currentMemoryBank.isBlank())
                ? currentMemoryBank : "{}";

        String metaPrompt = """
                You are a memory manager for a language learning chat app.
                Extract only factual, reusable information worth remembering across sessions
                (e.g. user's name, order details, unresolved tasks, preferences).
                Ignore small talk. Keep it concise.
                Return a single-line JSON object with a "facts" array.
                If nothing new to remember, return the current memory unchanged.

                Current memory: %s

                New exchange:
                User: %s
                AI: %s

                Updated memory (JSON only, no explanation):""".formatted(current, userMessage, aiMessage);

        String response = openAiService.chatWithSystem("You are a concise memory manager. Reply with JSON only.", metaPrompt);
        try {
            objectMapper.readTree(response);
            return response;
        } catch (JsonProcessingException e) {
            return currentMemoryBank;
        }
    }

    private List<CharacterInfo> parseCharacters(String json) {
        if (json == null || json.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    // 대화 기록 조회
    public List<MessageResponse> getMessages(Long userId, Long roomId) {
        Room room = roomRepository.findByIdAndDeletedFalse(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ROOM_ACCESS_DENIED);
        }

        return messageRepository.findByRoomIdOrderBySequenceNoAsc(roomId)
                .stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public VoiceResponse enterRoom(Long userId, Long roomId) {
        Room room = roomRepository.findByIdAndDeletedFalse(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ROOM_ACCESS_DENIED);
        }

        String systemPrompt = buildSystemPrompt(room);

        // 최근 메시지 확인
        List<Message> recent = messageRepository.findTop10ByRoomIdOrderBySequenceNoDesc(roomId);

        String aiContent;

        if (recent.isEmpty()) {
            // Case 1: 첫 대화 → AI 인사말 생성
            aiContent = openAiService.chatWithHistory(
                    systemPrompt,
                    List.of(),
                    "Start the conversation. Greet the user naturally according to the situation."
            ).getContent();
        } else {
            Message lastMessage = recent.get(0); // 가장 최신 메시지

            if (lastMessage.getSenderType() == Message.SenderType.AI) {
                // Case 2: 마지막이 AI → 마지막 메시지 재사용
                aiContent = lastMessage.getContentText();
            } else {
                // Case 3: 마지막이 USER → AI가 응답 생성
                Collections.reverse(recent);
                List<ChatMessage> history = recent.stream()
                        .map(m -> m.getSenderType() == Message.SenderType.USER
                                ? ChatMessage.user(m.getContentText())
                                : ChatMessage.assistant(m.getContentText()))
                        .toList();

                aiContent = openAiService.chatWithHistory(
                        systemPrompt,
                        history,
                        "Continue the conversation naturally."
                ).getContent();
            }
        }

        // Case 1, 3인 경우에만 AI 메시지 저장
        if (recent.isEmpty() || recent.get(0).getSenderType() == Message.SenderType.USER) {
            int nextSeq = messageRepository.findMaxSequenceNoByRoomId(roomId) + 1;
            messageRepository.save(Message.builder()
                    .roomId(roomId)
                    .senderType(Message.SenderType.AI)
                    .contentText(aiContent)
                    .sequenceNo(nextSeq)
                    .build());
        }

        room.updateLastActiveAt();

        // TTS: AI 텍스트 → 음성
        byte[] audioData = openAiService.textToSpeech(aiContent, "alloy");

        // S3 업로드
        String audioUrl = s3Service.uploadAudio(audioData, "voice/" + roomId);

        return new VoiceResponse(aiContent, audioUrl, null);
    }
}