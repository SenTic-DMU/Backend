package com.project.sentic.domain.user.controller;

import com.project.sentic.domain.report.dto.*;
import com.project.sentic.domain.report.service.ReportService;
import com.project.sentic.domain.user.dto.*;
import com.project.sentic.domain.user.service.UserService;
import com.project.sentic.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ReportService reportService;

    // ── 마이페이지 ──────────────────────────────────

    @Operation(summary = "마이페이지 조회")
    @GetMapping("/me")
    public ApiResponse<MyPageResponse> getMyPage(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getMyPage(userId));
    }

    @Operation(summary = "회원 탈퇴", description = "LOCAL 계정은 비밀번호 확인 필수. 소셜 계정은 password 필드 불필요.")
    @DeleteMapping("/me")
    public ApiResponse<String> withdraw(
            @AuthenticationPrincipal Long userId,
            @RequestBody WithdrawRequestDto request) {
        userService.withdraw(userId, request);
        return ApiResponse.success("회원 탈퇴가 완료되었습니다.");
    }

    @Operation(summary = "학습 레벨 변경")
    @PutMapping("/me/level")
    public ApiResponse<UserSettingsResponse> updateLevel(
            @AuthenticationPrincipal Long userId,
            @RequestParam String difficulty) {
        return ApiResponse.success(userService.updateLevel(userId, difficulty));
    }

    @Operation(summary = "설정 조회")
    @GetMapping("/me/settings")
    public ApiResponse<UserSettingsResponse> getSettings(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getSettings(userId));
    }

    @Operation(summary = "설정 변경")
    @PutMapping("/me/settings")
    public ApiResponse<UserSettingsResponse> updateSettings(
            @AuthenticationPrincipal Long userId,
            @RequestBody UserSettingsUpdateRequest request) {
        return ApiResponse.success(userService.updateSettings(userId, request));
    }

    @Operation(summary = "앱 접속 시작 (학습 시간 기록 시작)")
    @PostMapping("/me/session/start")
    public ApiResponse<Void> startSession(
            @AuthenticationPrincipal Long userId) {
        userService.startSession(userId);
        return ApiResponse.success(null);
    }

    @Operation(summary = "앱 접속 종료 (학습 시간 기록 종료)")
    @PostMapping("/me/session/end")
    public ApiResponse<Void> endSession(
            @AuthenticationPrincipal Long userId) {
        userService.endSession(userId);
        return ApiResponse.success(null);
    }

    // ── 랭킹 ──────────────────────────────────

    @Operation(summary = "이번 주 전체 랭킹 조회")
    @GetMapping("/ranking")
    public ApiResponse<List<RankingResponse>> getRanking() {
        return ApiResponse.success(userService.getRanking());
    }

    @Operation(summary = "내 순위 조회")
    @GetMapping("/ranking/me")
    public ApiResponse<RankingResponse> getMyRanking(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getMyRanking(userId));
    }

    // ── 리그 ──────────────────────────────────

    @Operation(summary = "내 리그 + 리그 내 순위 조회")
    @GetMapping("/league")
    public ApiResponse<LeagueResponse> getMyLeague(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getMyLeague(userId));
    }

    // ── 학습 레포트 ──────────────────────────────────

    @Operation(summary = "주간 학습 통계", description = "date 파라미터로 해당 주 기준 조회. 생략 시 이번 주.")
    @GetMapping("/study-stats")
    public ApiResponse<StudyStatsResponse> getStudyStats(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String date) {
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        return ApiResponse.success(reportService.getStudyStats(userId, targetDate));
    }

    @Operation(summary = "퀴즈 정답률 (전체 누적)")
    @GetMapping("/quiz-stats")
    public ApiResponse<QuizStatsResponse> getQuizStats(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(reportService.getQuizStats(userId));
    }

    @Operation(summary = "피드백 비율 (전체 누적)")
    @GetMapping("/feedback-stats")
    public ApiResponse<FeedbackStatsResponse> getFeedbackStats(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(reportService.getFeedbackStats(userId));
    }

    @Operation(summary = "나의 약점 TOP 3", description = "date 파라미터로 해당 주 기준 조회. 생략 시 이번 주.")
    @GetMapping("/weak-points")
    public ApiResponse<WeakPointsResponse> getWeakPoints(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String date) {
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        return ApiResponse.success(reportService.getWeakPoints(userId, targetDate));
    }

    @Operation(summary = "실력 성장 그래프 (최근 8주)")
    @GetMapping("/growth-trend")
    public ApiResponse<List<GrowthTrendResponse>> getGrowthTrend(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(reportService.getGrowthTrend(userId));
    }

    @Operation(summary = "AI 월말 종합평가", description = "month 파라미터: YYYY-MM 형식. 유료 회원만 가능.")
    @GetMapping("/monthly-review")
    public ApiResponse<MonthlyReviewResponse> getMonthlyReview(
            @AuthenticationPrincipal Long userId,
            @RequestParam String month) {
        return ApiResponse.success(reportService.getMonthlyReview(userId, month));
    }

    @Operation(summary = "자주 쓰는 표현 분석", description = "date 파라미터로 해당 주 기준 조회. 생략 시 이번 주.")
    @GetMapping("/frequent-expressions")
    public ApiResponse<List<FrequentExpressionResponse>> getFrequentExpressions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String date) {
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        return ApiResponse.success(reportService.getFrequentExpressions(userId, targetDate));
    }

    @Operation(summary = "음성/채팅 학습 비율", description = "date 파라미터로 해당 주 기준 조회. 생략 시 이번 주.")
    @GetMapping("/mode-ratio")
    public ApiResponse<ModeRatioResponse> getModeRatio(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String date) {
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        return ApiResponse.success(reportService.getModeRatio(userId, targetDate));
    }

    @Operation(summary = "학습 히트맵", description = "최근 N개월간 날짜별 학습 시간. 기본 6개월.")
    @GetMapping("/study-heatmap")
    public ApiResponse<List<HeatmapResponse>> getStudyHeatmap(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "6") int months) {
        return ApiResponse.success(reportService.getStudyHeatmap(userId, months));
    }
}