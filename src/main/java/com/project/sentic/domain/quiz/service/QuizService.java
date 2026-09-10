package com.project.sentic.domain.quiz.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.sentic.domain.message.entity.Message;
import com.project.sentic.domain.message.repository.MessageRepository;
import com.project.sentic.domain.quiz.dto.*;
import com.project.sentic.domain.quiz.entity.QuizSession;
import com.project.sentic.domain.quiz.repository.QuizSessionRepository;
import com.project.sentic.domain.user.repository.UserSettingsRepository;
import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import com.project.sentic.infra.ai.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    private final QuizSessionRepository quizSessionRepository;
    private final MessageRepository messageRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
    private final UserSettingsRepository userSettingsRepository;

    // 퀴즈 문제 임시 저장 (quizId → 문제 목록)
    // 나가면 사라지는 일회성이라 DB 대신 메모리 사용
    private final Map<Long, List<QuizQuestionDto>> quizCache = new ConcurrentHashMap<>();

    // 퀴즈 시작 (5문제 출제)
    @Transactional
    public QuizStartResponse startQuiz(Long userId, String userPlan) {
        // 1. 사용자의 모든 대화에서 메시지 가져오기
        List<Message> allMessages = messageRepository.findByRoomIdIn(
                getRoomIdsForUser(userId)
        );

        if (allMessages.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 2. 텍스트가 있는 메시지만 필터링
        List<String> sentences = allMessages.stream()
                .filter(m -> m.getContentText() != null && !m.getContentText().isBlank())
                .map(Message::getContentText)
                .collect(Collectors.toList());

        // 3. 랜덤으로 5개 선택
        Collections.shuffle(sentences);
        List<String> selected = sentences.stream()
                .limit(5)
                .collect(Collectors.toList());

        // 4. GPT에게 문제 생성 요청
        String questionTypes = getQuestionTypes(userPlan);
        List<QuizQuestionDto> questions = generateQuestions(selected, questionTypes);

        // 5. 퀴즈 세션 DB 저장
        QuizSession session = QuizSession.builder()
                .userId(userId)
                .build();
        QuizSession saved = quizSessionRepository.save(session);

        // 6. 문제를 메모리에 임시 저장
        quizCache.put(saved.getId(), questions);

        // 7. 응답 (정답/해설 제외)
        List<QuizStartResponse.QuizQuestionItem> items = questions.stream()
                .map(q -> QuizStartResponse.QuizQuestionItem.builder()
                        .questionNo(q.getQuestionNo())
                        .questionType(q.getQuestionType())
                        .sentence(q.getSentence())
                        .translation(q.getTranslation())
                        .options(q.getOptions())
                        .build())
                .collect(Collectors.toList());

        return QuizStartResponse.builder()
                .quizId(saved.getId())
                .total(questions.size())
                .questions(items)
                .build();
    }

    // 퀴즈 답안 제출 + 채점
    @Transactional
    public QuizResultResponse submitQuiz(Long userId, Long quizId, QuizSubmitRequest request) {
        QuizSession session = quizSessionRepository.findById(quizId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        if (!session.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 메모리에서 문제 가져오기
        List<QuizQuestionDto> questions = quizCache.get(quizId);
        if (questions == null) {
            throw new CustomException(ErrorCode.NOT_FOUND);
        }

        // 채점
        Map<Integer, String> userAnswers = request.getAnswers().stream()
                .collect(Collectors.toMap(
                        QuizSubmitRequest.AnswerItem::getQuestionNo,
                        QuizSubmitRequest.AnswerItem::getUserAnswer
                ));

        int score = 0;
        List<QuizResultResponse.QuizResultItem> results = new ArrayList<>();

        for (QuizQuestionDto q : questions) {
            String userAnswer = userAnswers.getOrDefault(q.getQuestionNo(), "");
            boolean correct = q.getAnswer().trim().equalsIgnoreCase(userAnswer.trim());
            if (correct) score++;

            results.add(QuizResultResponse.QuizResultItem.builder()
                    .questionNo(q.getQuestionNo())
                    .questionType(q.getQuestionType())
                    .sentence(q.getSentence())
                    .translation(q.getTranslation())
                    .answer(q.getAnswer())
                    .userAnswer(userAnswer)
                    .correct(correct)
                    .explanation(correct ? null : q.getExplanation())
                    .build());
        }

        // 점수 저장
        session.updateScore(score);

        // 랭킹 점수 +1점 * 맞힌 개수
        int finalScore = score;

        userSettingsRepository.findByUserId(userId)
                .ifPresent(settings -> settings.addQuizScore(finalScore));

        // 메모리에서 삭제
        quizCache.remove(quizId);

        return QuizResultResponse.builder()
                .quizId(quizId)
                .score(score)
                .total(session.getTotal())
                .results(results)
                .build();
    }

    // 퀴즈 기록 조회
    public List<QuizHistoryResponse> getQuizHistory(Long userId) {
        return quizSessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(QuizHistoryResponse::from)
                .collect(Collectors.toList());
    }

    // 요금제별 퀴즈 유형
    private String getQuestionTypes(String userPlan) {
        if (userPlan == null || userPlan.equalsIgnoreCase("FREE")) {
            return "FILL_BLANK only";
        }
        return "FILL_BLANK, ARRANGE, MULTIPLE_CHOICE (mix randomly)";
    }

    // 사용자의 모든 방 ID 가져오기
    private List<Long> getRoomIdsForUser(Long userId) {
        // MessageRepository에서 userId로 roomId 목록 조회 필요
        return messageRepository.findDistinctRoomIdsByUserId(userId);
    }

    // GPT에게 문제 생성 요청
    private List<QuizQuestionDto> generateQuestions(List<String> sentences, String questionTypes) {
        String prompt = buildQuizPrompt(sentences, questionTypes);

        try {
            String result = openAiService.chatWithSystem(
                    "You are an English quiz generator. Reply with JSON only. No explanation.",
                    prompt
            );
            log.info("[Quiz] GPT 응답: {}", result);

            String cleaned = result.replace("```json", "").replace("```", "").trim();
            return objectMapper.readValue(cleaned, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("[Quiz] 퀴즈 생성 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.AI_API_ERROR);
        }
    }

    // 퀴즈 프롬프트 생성
    private String buildQuizPrompt(List<String> sentences, String questionTypes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sentences.size(); i++) {
            sb.append((i + 1)).append(". ").append(sentences.get(i)).append("\n");
        }

        return """
                Generate 5 English quiz questions from these sentences:
                
                %s
                
                Question types to use: %s
                
                Rules:
                1. Return a JSON array of 5 objects
                2. Each object must have:
                   - questionNo (1-5)
                   - questionType ("FILL_BLANK" or "ARRANGE" or "MULTIPLE_CHOICE")
                   - sentence (the quiz question)
                     - FILL_BLANK: replace one key word with "_____"
                     - ARRANGE: shuffle the words randomly, separated by " / "
                     - MULTIPLE_CHOICE: show the Korean translation and 4 English options
                   - translation (Korean translation of the original sentence)
                   - answer (correct answer)
                     - FILL_BLANK: the missing word
                     - ARRANGE: the correct full sentence
                     - MULTIPLE_CHOICE: the correct option text
                   - options (array of 4 strings, MULTIPLE_CHOICE only, null for others)
                   - explanation (Korean explanation of why this is the answer)
                3. All explanations must be in Korean.
                4. Return JSON array only. No markdown, no extra text.
                """.formatted(sb.toString(), questionTypes);
    }
}