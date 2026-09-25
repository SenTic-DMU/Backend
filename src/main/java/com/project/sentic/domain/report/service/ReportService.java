package com.project.sentic.domain.report.service;

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

import java.time.*;
import java.time.temporal.TemporalAdjusters;
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

    // 1. 주간 학습 통계
    public StudyStatsResponse getStudyStats(Long userId, LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        List<DailyStudyLog> logs = dailyStudyLogRepository
                .findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(userId, monday, sunday);

        List<StudyStatsResponse.DayItem> weekly = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            String dayName = day.getDayOfWeek().name().substring(0, 3);

            int minutes = logs.stream()
                    .filter(l -> l.getStudyDate().equals(day))
                    .mapToInt(DailyStudyLog::getStudyMinutes)
                    .sum();

            weekly.add(StudyStatsResponse.DayItem.builder()
                    .day(dayName)
                    .minute(minutes)
                    .date(day.toString())
                    .build());
        }

        int totalMinutes = weekly.stream().mapToInt(StudyStatsResponse.DayItem::getMinute).sum();
        int avgMinutes = totalMinutes / 7;

        UserSettings settings = userSettingsRepository.findByUserId(userId).orElse(null);
        int continuousDays = settings != null ? settings.getStreakDays() : 0;

        return StudyStatsResponse.builder()
                .weekly(weekly)
                .totalMinutes(totalMinutes)
                .avgMinutes(avgMinutes)
                .continuousDays(continuousDays)
                .build();
    }

    // 2. 퀴즈 정답률 (누적)
    public QuizStatsResponse getQuizStats(Long userId) {
        List<QuizSession> sessions = quizSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);

        int totalAttempted = sessions.stream()
                .filter(s -> s.getScore() != null)
                .mapToInt(QuizSession::getTotal)
                .sum();
        int totalCorrect = sessions.stream()
                .filter(s -> s.getScore() != null)
                .mapToInt(QuizSession::getScore)
                .sum();
        double accuracy = totalAttempted > 0 ? (double) totalCorrect / totalAttempted * 100 : 0;

        return QuizStatsResponse.builder()
                .totalAttempted(totalAttempted)
                .totalCorrect(totalCorrect)
                .accuracy(Math.round(accuracy * 10) / 10.0)
                .build();
    }

    // 3. 피드백 비율 (누적)
    public FeedbackStatsResponse getFeedbackStats(Long userId) {
        List<Long> roomIds = messageRepository.findDistinctRoomIdsByUserId(userId);
        if (roomIds.isEmpty()) {
            return FeedbackStatsResponse.builder()
                    .totalUtterances(0).cleanUtterances(0).cleanRatio(0).build();
        }

        List<Message> userMessages = messageRepository.findByRoomIdIn(roomIds).stream()
                .filter(m -> m.getSenderType() == Message.SenderType.USER)
                .collect(Collectors.toList());

        int totalUtterances = userMessages.size();

        List<Long> messageIds = userMessages.stream()
                .map(Message::getId)
                .collect(Collectors.toList());

        Set<Long> feedbackMessageIds = messageIds.isEmpty()
                ? Set.of()
                : feedbackRepository.findByMessageIdIn(messageIds).stream()
                  .filter(f -> f.getWordErrors() != null || f.getGrammarErrors() != null || f.getExpressionErrors() != null)
                  .map(Feedback::getMessageId)
                  .collect(Collectors.toSet());

        int cleanUtterances = totalUtterances - feedbackMessageIds.size();
        double cleanRatio = totalUtterances > 0 ? (double) cleanUtterances / totalUtterances * 100 : 0;

        return FeedbackStatsResponse.builder()
                .totalUtterances(totalUtterances)
                .cleanUtterances(cleanUtterances)
                .cleanRatio(Math.round(cleanRatio * 10) / 10.0)
                .build();
    }

    // 4. 약점 TOP 3 (주간) - GPT 분석
    public WeakPointsResponse getWeakPoints(Long userId, LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        List<Long> roomIds = messageRepository.findDistinctRoomIdsByUserId(userId);
        if (roomIds.isEmpty()) {
            return WeakPointsResponse.builder().weakPoints(List.of()).build();
        }

        List<Message> weekMessages = messageRepository.findByRoomIdIn(roomIds).stream()
                .filter(m -> m.getSenderType() == Message.SenderType.USER)
                .filter(m -> m.getCreatedAt() != null
                        && !m.getCreatedAt().toLocalDate().isBefore(monday)
                        && !m.getCreatedAt().toLocalDate().isAfter(sunday))
                .collect(Collectors.toList());

        List<Long> messageIds = weekMessages.stream().map(Message::getId).collect(Collectors.toList());
        if (messageIds.isEmpty()) {
            return WeakPointsResponse.builder().weakPoints(List.of()).build();
        }

        List<Feedback> feedbacks = feedbackRepository.findByMessageIdIn(messageIds);
        if (feedbacks.isEmpty()) {
            return WeakPointsResponse.builder().weakPoints(List.of()).build();
        }

        // 모든 오류를 모아서 GPT에게 분석 요청
        StringBuilder errorSummary = new StringBuilder();
        for (Feedback f : feedbacks) {
            appendErrors(errorSummary, f.getWordErrors(), "단어");
            appendErrors(errorSummary, f.getGrammarErrors(), "문법");
            appendErrors(errorSummary, f.getExpressionErrors(), "표현");
        }

        String prompt = """
            아래는 영어 학습자가 이번 주에 받은 피드백 목록입니다.
            이 데이터를 분석해서 학습자의 약점 TOP 3를 알려주세요.
            
            단어/문법/표현으로 나누지 말고, 전체적으로 어떤 부분이 약한지 문장으로 설명해주세요.
            예시: "전치사 사용이 약해요. at/in/on 구분이 자주 틀려요."
            
            피드백 목록:
            %s
            
            JSON 배열로만 응답해주세요:
            [{"rank": 1, "description": "약점 설명", "count": 관련오류횟수}]
            최대 3개만. 한국어로 작성.
            """.formatted(errorSummary.toString());

        try {
            String result = openAiService.chatWithSystem(
                    "You are an English learning analyst. Reply with JSON array only.",
                    prompt
            );
            String cleaned = result.replace("```json", "").replace("```", "").trim();
            JsonNode nodes = objectMapper.readTree(cleaned);

            List<WeakPointsResponse.WeakPointItem> weakPoints = new ArrayList<>();
            if (nodes.isArray()) {
                for (JsonNode node : nodes) {
                    weakPoints.add(WeakPointsResponse.WeakPointItem.builder()
                            .rank(node.get("rank").asInt())
                            .description(node.get("description").asText())
                            .count(node.get("count").asInt())
                            .build());
                }
            }

            return WeakPointsResponse.builder().weakPoints(weakPoints).build();
        } catch (Exception e) {
            log.error("[Report] 약점 분석 실패: {}", e.getMessage());
            return WeakPointsResponse.builder().weakPoints(List.of()).build();
        }
    }

    private void appendErrors(StringBuilder sb, String errorsJson, String category) {
        if (errorsJson == null || errorsJson.isBlank()) return;
        try {
            JsonNode nodes = objectMapper.readTree(errorsJson);
            if (nodes.isArray()) {
                for (JsonNode node : nodes) {
                    String original = node.has("original") ? node.get("original").asText() : "";
                    String corrected = node.has("corrected") ? node.get("corrected").asText()
                            : node.has("suggested") ? node.get("suggested").asText() : "";
                    String explanation = node.has("explanation") ? node.get("explanation").asText() : "";
                    sb.append(category).append(": ").append(original)
                            .append(" → ").append(corrected)
                            .append(" (").append(explanation).append(")\n");
                }
            }
        } catch (Exception e) {
            log.warn("[Report] 에러 파싱 실패: {}", e.getMessage());
        }
    }

    // 5. 성장 그래프 (최근 8주)
    public List<GrowthTrendResponse> getGrowthTrend(Long userId) {
        List<GrowthTrendResponse> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        List<Long> roomIds = messageRepository.findDistinctRoomIdsByUserId(userId);

        for (int i = 7; i >= 0; i--) {
            LocalDate weekStart = today.minusWeeks(i).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate weekEnd = weekStart.plusDays(6);

            // 퀴즈 정답률
            List<QuizSession> weekQuizzes = quizSessionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                    .filter(s -> s.getScore() != null
                            && !s.getCreatedAt().toLocalDate().isBefore(weekStart)
                            && !s.getCreatedAt().toLocalDate().isAfter(weekEnd))
                    .collect(Collectors.toList());

            int quizTotal = weekQuizzes.stream().mapToInt(QuizSession::getTotal).sum();
            int quizCorrect = weekQuizzes.stream().mapToInt(QuizSession::getScore).sum();
            double quizAccuracy = quizTotal > 0 ? (double) quizCorrect / quizTotal * 100 : 0;

            // 피드백 비율
            double feedbackCleanRatio = 0;
            if (!roomIds.isEmpty()) {
                List<Message> weekUserMessages = messageRepository.findByRoomIdIn(roomIds).stream()
                        .filter(m -> m.getSenderType() == Message.SenderType.USER)
                        .filter(m -> m.getCreatedAt() != null
                                && !m.getCreatedAt().toLocalDate().isBefore(weekStart)
                                && !m.getCreatedAt().toLocalDate().isAfter(weekEnd))
                        .collect(Collectors.toList());

                if (!weekUserMessages.isEmpty()) {
                    List<Long> msgIds = weekUserMessages.stream().map(Message::getId).collect(Collectors.toList());
                    long feedbackCount = feedbackRepository.findByMessageIdIn(msgIds).stream()
                            .filter(f -> f.getWordErrors() != null || f.getGrammarErrors() != null || f.getExpressionErrors() != null)
                            .count();
                    feedbackCleanRatio = (double) (weekUserMessages.size() - feedbackCount) / weekUserMessages.size() * 100;
                }
            }

            trend.add(GrowthTrendResponse.builder()
                    .weekStart(weekStart.toString())
                    .quizAccuracy(Math.round(quizAccuracy * 10) / 10.0)
                    .feedbackCleanRatio(Math.round(feedbackCleanRatio * 10) / 10.0)
                    .build());
        }

        return trend;
    }

    // 6. AI 월말 종합평가 (유료)
    public MonthlyReviewResponse getMonthlyReview(Long userId, String month) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!settings.isPremium()) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        String[] parts = month.split("-");
        int year = Integer.parseInt(parts[0]);
        int mon = Integer.parseInt(parts[1]);

        YearMonth current = YearMonth.now();
        if (YearMonth.of(year, mon).equals(current)) {
            return MonthlyReviewResponse.builder()
                    .month(month).summary(null).generatedAt(null).build();
        }

        String summary = generateMonthlyAiEvaluation(userId, year, mon);

        return MonthlyReviewResponse.builder()
                .month(month)
                .summary(summary)
                .generatedAt(LocalDateTime.now().toString())
                .build();
    }

    private String generateMonthlyAiEvaluation(Long userId, int year, int month) {
        List<Long> roomIds = messageRepository.findDistinctRoomIdsByUserId(userId);
        int totalMessages = 0;
        int feedbackCount = 0;

        if (!roomIds.isEmpty()) {
            List<Message> monthMessages = messageRepository.findByRoomIdIn(roomIds).stream()
                    .filter(m -> m.getSenderType() == Message.SenderType.USER)
                    .filter(m -> m.getCreatedAt() != null
                            && m.getCreatedAt().getYear() == year
                            && m.getCreatedAt().getMonthValue() == month)
                    .collect(Collectors.toList());

            totalMessages = monthMessages.size();
            List<Long> msgIds = monthMessages.stream().map(Message::getId).collect(Collectors.toList());
            if (!msgIds.isEmpty()) {
                feedbackCount = (int) feedbackRepository.findByMessageIdIn(msgIds).size();
            }
        }

        List<QuizSession> quizzes = quizSessionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(s -> s.getScore() != null
                        && s.getCreatedAt().getYear() == year
                        && s.getCreatedAt().getMonthValue() == month)
                .collect(Collectors.toList());

        int quizTotal = quizzes.stream().mapToInt(QuizSession::getTotal).sum();
        int quizCorrect = quizzes.stream().mapToInt(QuizSession::getScore).sum();

        String prompt = """
                당신은 영어 학습 코치입니다. 아래 학습 데이터를 보고 따뜻하고 구체적인 월말 평가를 한국어로 작성해주세요.
                
                %d년 %d월 학습 데이터:
                - 전체 대화 수: %d회
                - 피드백 받은 횟수: %d회
                - 퀴즈 응시: %d문제, 정답: %d문제
                
                평가 형식:
                1. 이번 달 전체 평가 (2-3문장)
                2. 잘한 점 (1-2문장)
                3. 개선할 점 (1-2문장)
                4. 다음 달 추천 학습 방향 (1-2문장)
                """.formatted(year, month, totalMessages, feedbackCount, quizTotal, quizCorrect);

        try {
            return openAiService.chatWithSystem(
                    "You are a warm and encouraging English learning coach. Reply in Korean only.",
                    prompt
            );
        } catch (Exception e) {
            log.error("[Report] AI 평가 생성 실패: {}", e.getMessage());
            return null;
        }
    }

    // 7. 자주 쓰는 표현 (주간)
    public List<FrequentExpressionResponse> getFrequentExpressions(Long userId, LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        List<Long> roomIds = messageRepository.findDistinctRoomIdsByUserId(userId);
        if (roomIds.isEmpty()) return List.of();

        List<String> userTexts = messageRepository.findByRoomIdIn(roomIds).stream()
                .filter(m -> m.getSenderType() == Message.SenderType.USER)
                .filter(m -> m.getContentText() != null && !m.getContentText().isBlank())
                .filter(m -> m.getCreatedAt() != null
                        && !m.getCreatedAt().toLocalDate().isBefore(monday)
                        && !m.getCreatedAt().toLocalDate().isAfter(sunday))
                .map(Message::getContentText)
                .collect(Collectors.toList());

        if (userTexts.isEmpty()) return List.of();

        String allTexts = String.join("\n", userTexts);
        String prompt = """
                아래는 영어 학습자가 이번 주에 사용한 문장들입니다.
                자주 반복되는 표현이나 패턴을 최대 5개 추출해주세요.
                
                학습자 문장들:
                %s
                
                JSON 배열로만 응답해주세요:
                [{"expression": "자주 쓰는 표현", "count": 사용횟수}]
                """.formatted(allTexts);

        try {
            String result = openAiService.chatWithSystem(
                    "You are an English expression analyzer. Reply with JSON array only.",
                    prompt
            );
            String cleaned = result.replace("```json", "").replace("```", "").trim();
            JsonNode nodes = objectMapper.readTree(cleaned);

            List<FrequentExpressionResponse> expressions = new ArrayList<>();
            if (nodes.isArray()) {
                for (JsonNode node : nodes) {
                    expressions.add(FrequentExpressionResponse.builder()
                            .expression(node.get("expression").asText())
                            .count(node.get("count").asInt())
                            .build());
                }
            }
            return expressions;
        } catch (Exception e) {
            log.error("[Report] 표현 분석 실패: {}", e.getMessage());
            return List.of();
        }
    }

    // 8. 음성/채팅 비율 (주간)
    public ModeRatioResponse getModeRatio(Long userId, LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        List<DailyStudyLog> logs = dailyStudyLogRepository
                .findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(userId, monday, sunday);

        int voiceMinutes = logs.stream().mapToInt(DailyStudyLog::getVoiceCount).sum();
        int chatMinutes = logs.stream().mapToInt(DailyStudyLog::getChatCount).sum();

        return ModeRatioResponse.builder()
                .voiceMinutes(voiceMinutes)
                .chatMinutes(chatMinutes)
                .build();
    }

    // 9. 학습 히트맵
    public List<HeatmapResponse> getStudyHeatmap(Long userId, int months) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months);

        List<DailyStudyLog> logs = dailyStudyLogRepository
                .findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(userId, startDate, endDate);

        return logs.stream()
                .map(l -> HeatmapResponse.builder()
                        .date(l.getStudyDate().toString())
                        .minutes(l.getStudyMinutes())
                        .build())
                .collect(Collectors.toList());
    }

    // 일별 학습 기록 저장
    @Transactional
    public void logStudy(Long userId, String roomType) {
        LocalDate today = LocalDate.now();

        DailyStudyLog studyLog = dailyStudyLogRepository.findByUserIdAndStudyDate(userId, today)
                .orElseGet(() -> dailyStudyLogRepository.save(DailyStudyLog.builder()
                        .userId(userId)
                        .studyDate(today)
                        .build()));

        if ("VOICE".equals(roomType)) {
            studyLog.incrementVoiceCount();
        } else {
            studyLog.incrementChatCount();
        }
    }

    // 학습 시간 기록
    @Transactional
    public void logStudyMinutes(Long userId, int minutes) {
        LocalDate today = LocalDate.now();

        DailyStudyLog studyLog = dailyStudyLogRepository.findByUserIdAndStudyDate(userId, today)
                .orElseGet(() -> dailyStudyLogRepository.save(DailyStudyLog.builder()
                        .userId(userId)
                        .studyDate(today)
                        .build()));

        studyLog.addStudyMinutes(minutes);
    }
}