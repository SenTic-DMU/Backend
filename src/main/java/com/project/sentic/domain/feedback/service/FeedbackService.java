package com.project.sentic.domain.feedback.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.sentic.domain.feedback.dto.FeedbackResponse;
import com.project.sentic.domain.feedback.entity.Feedback;
import com.project.sentic.domain.feedback.repository.FeedbackRepository;
import com.project.sentic.domain.room.entity.Room;
import com.project.sentic.domain.room.repository.RoomRepository;
import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import com.project.sentic.infra.ai.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final RoomRepository roomRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    // 피드백 생성 (메시지 전송 시 호출)
    @Transactional
    public FeedbackResponse generateFeedback(Long roomId, Long messageId, String userMessage, String difficulty) {
        // GPT에게 피드백 요청
        String feedbackJson = requestFeedbackFromGpt(userMessage, difficulty);

        // null이면 피드백 불필요 → 저장 안 함
        if (feedbackJson == null || feedbackJson.isBlank() || feedbackJson.equals("null")) {
            return null;
        }

        // JSON 파싱
        FeedbackResult result = parseFeedbackResult(feedbackJson);
        if (result == null) return null;

        // 피드백 저장
        Feedback feedback = Feedback.builder()
                .roomId(roomId)
                .messageId(messageId)
                .wordErrors(result.wordErrors())
                .grammarErrors(result.grammarErrors())
                .expressionErrors(result.expressionErrors())
                .perfectSentence(result.perfectSentence())
                .build();

        return FeedbackResponse.from(feedbackRepository.save(feedback));
    }

    // 방의 피드백 목록 조회
    public List<FeedbackResponse> getFeedbacks(Long userId, Long roomId) {
        Room room = roomRepository.findByIdAndDeletedFalse(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.ROOM_ACCESS_DENIED);
        }

        return feedbackRepository.findByRoomIdOrderByCreatedAtDesc(roomId)
                .stream()
                .map(FeedbackResponse::from)
                .collect(Collectors.toList());
    }

    // GPT에게 피드백 요청
    private String requestFeedbackFromGpt(String userMessage, String difficulty) {
        String prompt = buildFeedbackPrompt(userMessage, difficulty);
        try {
            return openAiService.chatWithSystem(
                    "You are an English language feedback system. Reply with JSON only. No explanation.",
                    prompt
            );
        } catch (Exception e) {
            log.error("[Feedback] GPT 피드백 생성 실패: {}", e.getMessage());
            return null;
        }
    }

    // 피드백 프롬프트 생성
    private String buildFeedbackPrompt(String userMessage, String difficulty) {
        String levelGuide = switch (difficulty.toUpperCase()) {
            case "BEGINNER" -> "Only point out errors that completely block communication. Ignore minor mistakes.";
            case "ADVANCED" -> "Point out all errors including subtle unnatural expressions and word choices.";
            default -> "Point out clear grammar mistakes and suggest more natural expressions.";
        };

        return """
            Analyze the following English sentence and provide feedback.
            
            Difficulty level: %s
            Level guideline: %s
            
            User message: "%s"
            
            Rules:
            1. If the sentence has NO errors worth correcting at this level, return exactly: null
            2. If feedback is needed, return JSON in this exact format:
            {
              "wordErrors": [{"original": "...", "corrected": "...", "explanation": "...", "startIndex": 0, "endIndex": 0}],
              "grammarErrors": [{"original": "...", "corrected": "...", "explanation": "...", "startIndex": 0, "endIndex": 0}],
              "expressionErrors": [{"original": "...", "suggested": "...", "explanation": "...", "startIndex": 0, "endIndex": 0}],
              "perfectSentence": "..."
            }
            3. Empty arrays [] if no errors in that category.
            4. Return JSON only. No markdown, no explanation.
            5. All "explanation" values MUST be written in natural, polite Korean so that a Korean user can easily understand them.
            """.formatted(difficulty, levelGuide, userMessage);
    }

    // GPT 응답 JSON 파싱
    private FeedbackResult parseFeedbackResult(String json) {
        try {
            String cleaned = json.replace("```json", "").replace("```", "").trim();
            var node = objectMapper.readTree(cleaned);
            return new FeedbackResult(
                    node.has("wordErrors") ? node.get("wordErrors").toString() : null,
                    node.has("grammarErrors") ? node.get("grammarErrors").toString() : null,
                    node.has("expressionErrors") ? node.get("expressionErrors").toString() : null,
                    node.has("perfectSentence") ? node.get("perfectSentence").asText() : null
            );
        } catch (Exception e) {
            log.error("[Feedback] JSON 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private record FeedbackResult(
            String wordErrors,
            String grammarErrors,
            String expressionErrors,
            String perfectSentence
    ) {}
}