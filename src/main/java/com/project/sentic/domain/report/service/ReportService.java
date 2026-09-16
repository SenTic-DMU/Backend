package com.project.sentic.domain.report.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.sentic.domain.feedback.entity.Feedback;
import com.project.sentic.domain.feedback.repository.FeedbackRepository;
import com.project.sentic.domain.message.entity.Message;
import com.project.sentic.domain.message.repository.MessageRepository;
import com.project.sentic.domain.quiz.entity.QuizSession;
import com.project.sentic.domain.quiz.repository.QuizSessionRepository;
import com.project.sentic.domain.report.dto.*;
import com.project.sentic.domain.report.entity.DailyStudyLog;
import com.project.sentic.domain.report.repository.DailyStudyLogRepository;
import com.project.sentic.domain.user.entity.UserSettings;
import com.project.sentic.domain.user.repository.UserSettingsRepository;
import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import com.project.sentic.infra.ai.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final DailyStudyLogRepository dailyStudyLogRepository;
    private final MessageRepository messageRepository;
    private final FeedbackRepository feedbackRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    // 무료 레포트 조회
    public ReportSummaryResponse getSummaryReport(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 월별 캘린더
        List<ReportCalendarResponse> calendar = dailyStudyLogRepository
                .findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(userId, startDate, endDate)
                .stream()
                .map(ReportCalendarResponse::from)
                .collect(Collectors.toList());

        // 전체 메시지 조회
        List<Long> roomIds = messageRepository.findDistinctRoomIdsByUserId(userId);
        List<Message> allMessages = roomIds.isEmpty()
                ? List.of()
                : messageRepository.findByRoomIdIn(roomIds);

        List<Message> userMessages = allMessages.stream()
                .filter(m -> m.getSenderType() == Message.SenderType.USER)
                .collect(Collectors.toList());

        // 피드백 비율
        List<Long> messageIds = userMessages.stream()
                .map(Message::getId)
                .collect(Collectors.toList());

        int totalMessages = userMessages.size();
        int feedbackMessages = messageIds.isEmpty()
                ? 0
                : (int) feedbackRepository.findByMessageIdIn(messageIds).stream()
                .filter(f -> f.getWordErrors() != null || f.getGrammarErrors() != null || f.getExpressionErrors() != null)
                .count();
        double feedbackRate = totalMessages > 0 ? (double) feedbackMessages / totalMessages * 100 : 0;

        // 퀴즈 정답률
        List<QuizSession> quizSessions = quizSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        int totalQuizQuestions = quizSessions.stream()
                .filter(s -> s.getScore() != null)
                .mapToInt(QuizSession::getTotal)
                .sum();
        int correctQuizAnswers = quizSessions.stream()
                .filter(s -> s.getScore() != null)
                .mapToInt(QuizSession::getScore)
                .sum();
        double quizAccuracy = totalQuizQuestions > 0 ? (double) correctQuizAnswers / totalQuizQuestions * 100 : 0;

        // 음성/채팅 비율
        int totalVoiceCount = calendar.stream().mapToInt(ReportCalendarResponse::getVoiceCount).sum();
        int totalChatCount = calendar.stream().mapToInt(ReportCalendarResponse::getChatCount).sum();
        int totalCount = totalVoiceCount + totalChatCount;
        double voiceRate = totalCount > 0 ? (double) totalVoiceCount / totalCount * 100 : 0;
        double chatRate = totalCount > 0 ? (double) totalChatCount / totalCount * 100 : 0;

        return ReportSummaryResponse.builder()
                .calendar(calendar)
                .totalMessages(totalMessages)
                .feedbackMessages(feedbackMessages)
                .feedbackRate(Math.round(feedbackRate * 10) / 10.0)
                .totalQuizQuestions(totalQuizQuestions)
                .correctQuizAnswers(correctQuizAnswers)
                .quizAccuracy(Math.round(quizAccuracy * 10) / 10.0)
                .totalVoiceCount(totalVoiceCount)
                .totalChatCount(totalChatCount)
                .voiceRate(Math.round(voiceRate * 10) / 10.0)
                .chatRate(Math.round(chatRate * 10) / 10.0)
                .build();
    }

    // 유료 레포트 조회
    public ReportPremiumResponse getPremiumReport(Long userId, int year, int month) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!settings.isPremium()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 무료 레포트 포함
        ReportSummaryResponse summary = getSummaryReport(userId, year, month);

        // 나의 약점 TOP 3
        List<ReportPremiumResponse.WeaknessItem> weaknesses = analyzeWeaknesses(userId);

        // 실력 성장 그래프
        ReportPremiumResponse.GrowthData growth = analyzeGrowth(userId, year, month);

        // 자주 쓰는 표현 분석
        List<ReportPremiumResponse.ExpressionItem> expressions = analyzeExpressions(userId);

        // AI 월말 종합 평가
        String aiEvaluation = generateAiEvaluation(userId, summary, weaknesses);

        return ReportPremiumResponse.builder()
                .summary(summary)
                .weaknesses(weaknesses)
                .growth(growth)
                .expressions(expressions)
                .aiEvaluation(aiEvaluation)
                .build();
    }

    // 나의 약점 TOP 3
    private List<ReportPremiumResponse.WeaknessItem> analyzeWeaknesses(Long userId) {
        List<Long> roomIds = messageRepository.findDistinctRoomIdsByUserId(userId);
        if (roomIds.isEmpty()) return List.of();

        List<Message> userMessages = messageRepository.findByRoomIdIn(roomIds).stream()
                .filter(m -> m.getSenderType() == Message.SenderType.USER)
                .collect(Collectors.toList());

        List<Long> messageIds = userMessages.stream()
                .map(Message::getId)
                .collect(Collectors.toList());

        if (messageIds.isEmpty()) return List.of();

        List<Feedback> feedbacks = feedbackRepository.findByMessageIdIn(messageIds);

        Map<String, Integer> errorCounts = new HashMap<>();
        Map<String, String> errorExamples = new HashMap<>();

        for (Feedback f : feedbacks) {
            countErrors(f.getWordErrors(), "단어", errorCounts, errorExamples);
            countErrors(f.getGrammarErrors(), "문법", errorCounts, errorExamples);
            countErrors(f.getExpressionErrors(), "표현", errorCounts, errorExamples);
        }

        List<ReportPremiumResponse.WeaknessItem> weaknesses = new ArrayList<>();
        int rank = 1;

        List<Map.Entry<String, Integer>> sorted = errorCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());

        for (Map.Entry<String, Integer> entry : sorted) {
            weaknesses.add(ReportPremiumResponse.WeaknessItem.builder()
                    .rank(rank++)
                    .category(entry.getKey())
                    .pattern(errorExamples.getOrDefault(entry.getKey(), ""))
                    .count(entry.getValue())
                    .explanation(entry.getKey() + " 오류가 " + entry.getValue() + "회 발생했어요.")
                    .build());
        }

        return weaknesses;
    }

    private void countErrors(String errorsJson, String category,
                             Map<String, Integer> counts, Map<String, String> examples) {
        if (errorsJson == null || errorsJson.isBlank()) return;
        try {
            JsonNode nodes = objectMapper.readTree(errorsJson);
            if (nodes.isArray()) {
                counts.merge(category, nodes.size(), Integer::sum);
                if (!examples.containsKey(category) && nodes.size() > 0) {
                    JsonNode first = nodes.get(0);
                    String original = first.has("original") ? first.get("original").asText() : "";
                    examples.put(category, original);
                }
            }
        } catch (Exception e) {
            log.warn("[Report] 에러 파싱 실패: {}", e.getMessage());
        }
    }

    // 실력 성장 그래프
    private ReportPremiumResponse.GrowthData analyzeGrowth(Long userId, int year, int month) {
        double thisMonthFeedbackRate = calculateMonthlyFeedbackRate(userId, year, month);

        int prevMonth = month == 1 ? 12 : month - 1;
        int prevYear = month == 1 ? year - 1 : year;
        double lastMonthFeedbackRate = calculateMonthlyFeedbackRate(userId, prevYear, prevMonth);

        double thisMonthQuizAccuracy = calculateMonthlyQuizAccuracy(userId, year, month);
        double lastMonthQuizAccuracy = calculateMonthlyQuizAccuracy(userId, prevYear, prevMonth);

        String improvement;
        if (thisMonthFeedbackRate < lastMonthFeedbackRate) {
            improvement = "피드백 비율이 줄었어요! 실력이 성장하고 있어요!";
        } else if (thisMonthQuizAccuracy > lastMonthQuizAccuracy) {
            improvement = "퀴즈 정답률이 올랐어요! 꾸준히 발전하고 있어요!";
        } else {
            improvement = "꾸준히 학습하고 있어요! 조금만 더 힘내봐요!";
        }

        return ReportPremiumResponse.GrowthData.builder()
                .lastMonthFeedbackRate(Math.round(lastMonthFeedbackRate * 10) / 10.0)
                .thisMonthFeedbackRate(Math.round(thisMonthFeedbackRate * 10) / 10.0)
                .lastMonthQuizAccuracy(Math.round(lastMonthQuizAccuracy * 10) / 10.0)
                .thisMonthQuizAccuracy(Math.round(thisMonthQuizAccuracy * 10) / 10.0)
                .improvement(improvement)
                .build();
    }

    private double calculateMonthlyFeedbackRate(Long userId, int year, int month) {
        // 간단히 전체 피드백 비율 반환 (월별 세분화는 추후 개선)
        List<Long> roomIds = messageRepository.findDistinctRoomIdsByUserId(userId);
        if (roomIds.isEmpty()) return 0;

        List<Message> userMessages = messageRepository.findByRoomIdIn(roomIds).stream()
                .filter(m -> m.getSenderType() == Message.SenderType.USER)
                .filter(m -> m.getCreatedAt() != null
                        && m.getCreatedAt().getYear() == year
                        && m.getCreatedAt().getMonthValue() == month)
                .collect(Collectors.toList());

        if (userMessages.isEmpty()) return 0;

        List<Long> messageIds = userMessages.stream().map(Message::getId).collect(Collectors.toList());
        long feedbackCount = feedbackRepository.findByMessageIdIn(messageIds).size();

        return (double) feedbackCount / userMessages.size() * 100;
    }

    private double calculateMonthlyQuizAccuracy(Long userId, int year, int month) {
        List<QuizSession> sessions = quizSessionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(s -> s.getScore() != null
                        && s.getCreatedAt().getYear() == year
                        && s.getCreatedAt().getMonthValue() == month)
                .collect(Collectors.toList());

        int total = sessions.stream().mapToInt(QuizSession::getTotal).sum();
        int correct = sessions.stream().mapToInt(QuizSession::getScore).sum();

        return total > 0 ? (double) correct / total * 100 : 0;
    }

    // 자주 쓰는 표현 분석
    private List<ReportPremiumResponse.ExpressionItem> analyzeExpressions(Long userId) {
        List<Long> roomIds = messageRepository.findDistinctRoomIdsByUserId(userId);
        if (roomIds.isEmpty()) return List.of();

        List<Message> userMessages = messageRepository.findByRoomIdIn(roomIds).stream()
                .filter(m -> m.getSenderType() == Message.SenderType.USER)
                .filter(m -> m.getContentText() != null)
                .collect(Collectors.toList());

        List<Long> messageIds = userMessages.stream().map(Message::getId).collect(Collectors.toList());
        if (messageIds.isEmpty()) return List.of();

        List<Feedback> feedbacks = feedbackRepository.findByMessageIdIn(messageIds);

        Map<String, ReportPremiumResponse.ExpressionItem> expressionMap = new LinkedHashMap<>();

        for (Feedback f : feedbacks) {
            if (f.getExpressionErrors() == null || f.getExpressionErrors().isBlank()) continue;
            try {
                JsonNode nodes = objectMapper.readTree(f.getExpressionErrors());
                if (nodes.isArray()) {
                    for (JsonNode node : nodes) {
                        String original = node.has("original") ? node.get("original").asText() : "";
                        String suggested = node.has("suggested") ? node.get("suggested").asText() : "";
                        String explanation = node.has("explanation") ? node.get("explanation").asText() : "";

                        expressionMap.merge(original,
                                ReportPremiumResponse.ExpressionItem.builder()
                                        .myExpression(original)
                                        .useCount(1)
                                        .recommendedExpression(suggested)
                                        .explanation(explanation)
                                        .build(),
                                (existing, newItem) -> ReportPremiumResponse.ExpressionItem.builder()
                                        .myExpression(existing.getMyExpression())
                                        .useCount(existing.getUseCount() + 1)
                                        .recommendedExpression(existing.getRecommendedExpression())
                                        .explanation(existing.getExplanation())
                                        .build()
                        );
                    }
                }
            } catch (Exception e) {
                log.warn("[Report] 표현 분석 실패: {}", e.getMessage());
            }
        }

        return expressionMap.values().stream()
                .sorted((a, b) -> b.getUseCount() - a.getUseCount())
                .limit(5)
                .collect(Collectors.toList());
    }

    // AI 월말 종합 평가
    private String generateAiEvaluation(Long userId,
                                        ReportSummaryResponse summary,
                                        List<ReportPremiumResponse.WeaknessItem> weaknesses) {
        String prompt = """
                당신은 영어 학습 코치입니다. 아래 학습 데이터를 보고 학생에게 따뜻하고 구체적인 월말 평가를 한국어로 작성해주세요.
                
                학습 데이터:
                - 전체 대화 수: %d회
                - 피드백 받은 비율: %.1f%%
                - 퀴즈 정답률: %.1f%%
                - 음성 대화 비율: %.1f%%
                - 채팅 대화 비율: %.1f%%
                - 주요 약점: %s
                
                평가 형식:
                1. 이번 달 전체 평가 (2-3문장)
                2. 잘한 점 (1-2문장)
                3. 개선할 점 (1-2문장)
                4. 다음 달 추천 학습 방향 (1-2문장)
                
                따뜻하고 격려하는 톤으로 작성해주세요.
                """.formatted(
                summary.getTotalMessages(),
                summary.getFeedbackRate(),
                summary.getQuizAccuracy(),
                summary.getVoiceRate(),
                summary.getChatRate(),
                weaknesses.stream()
                        .map(w -> w.getCategory() + " " + w.getCount() + "회")
                        .collect(Collectors.joining(", "))
        );

        try {
            return openAiService.chatWithSystem(
                    "You are a warm and encouraging English learning coach. Reply in Korean only.",
                    prompt
            );
        } catch (Exception e) {
            log.error("[Report] AI 평가 생성 실패: {}", e.getMessage());
            return "AI 평가를 생성할 수 없습니다. 나중에 다시 시도해주세요.";
        }
    }

    // 일별 학습 기록 저장 (MessageService에서 호출)
    @Transactional
    public void logStudy(Long userId, String roomType) {
        LocalDate today = LocalDate.now();

        DailyStudyLog log = dailyStudyLogRepository.findByUserIdAndStudyDate(userId, today)
                .orElseGet(() -> dailyStudyLogRepository.save(DailyStudyLog.builder()
                        .userId(userId)
                        .studyDate(today)
                        .build()));

        if ("VOICE".equals(roomType)) {
            log.incrementVoiceCount();
        } else {
            log.incrementChatCount();
        }
    }

    // 학습 시간 기록 (세션 종료 시 호출)
    @Transactional
    public void logStudyMinutes(Long userId, int minutes) {
        LocalDate today = LocalDate.now();

        DailyStudyLog log = dailyStudyLogRepository.findByUserIdAndStudyDate(userId, today)
                .orElseGet(() -> dailyStudyLogRepository.save(DailyStudyLog.builder()
                        .userId(userId)
                        .studyDate(today)
                        .build()));

        log.addStudyMinutes(minutes);
    }
}