package com.project.sentic.domain.message.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final OpenAiService openAiService;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    @Transactional
    public ChatResponse chat(Long userId, Long roomId, String content) {
        Room room = roomRepository.findByIdAndDeletedFalse(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ROOM_ACCESS_DENIED);
        }

        String systemPrompt = buildSystemPrompt(room);

        // Sliding Window: 최근 10개 메시지를 시간순으로 정렬
        List<Message> recent = messageRepository.findTop10ByRoomIdOrderBySequenceNoDesc(roomId);
        Collections.reverse(recent);

        List<ChatMessage> history = recent.stream()
                .map(m -> m.getSenderType() == Message.SenderType.USER
                        ? ChatMessage.user(m.getContentText())
                        : ChatMessage.assistant(m.getContentText()))
                .toList();

        String aiContent = openAiService.chatWithHistory(systemPrompt, history, content).getContent();

        int nextSeq = messageRepository.findMaxSequenceNoByRoomId(roomId) + 1;
        messageRepository.save(Message.builder()
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

        return new ChatResponse(aiContent);
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
}
