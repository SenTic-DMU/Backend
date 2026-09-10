package com.project.sentic.domain.quiz.controller;

import com.project.sentic.domain.quiz.dto.*;
import com.project.sentic.domain.quiz.service.QuizService;
import com.project.sentic.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Quiz", description = "퀴즈 API")
@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @Operation(summary = "퀴즈 시작", description = "대화 내역에서 랜덤 5문제 출제. 무료: 빈칸채우기만, 유료: 전체 유형")
    @PostMapping("/start")
    public ApiResponse<QuizStartResponse> startQuiz(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "FREE") String plan) {
        return ApiResponse.success(quizService.startQuiz(userId, plan));
    }

    @Operation(summary = "퀴즈 답안 제출", description = "5문제 답안 제출 후 채점 결과 반환. 나가면 결과 사라짐.")
    @PostMapping("/{quizId}/submit")
    public ApiResponse<QuizResultResponse> submitQuiz(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long quizId,
            @RequestBody QuizSubmitRequest request) {
        return ApiResponse.success(quizService.submitQuiz(userId, quizId, request));
    }

    @Operation(summary = "퀴즈 기록 조회", description = "과거 퀴즈 점수/날짜 조회 (레포트용)")
    @GetMapping("/history")
    public ApiResponse<List<QuizHistoryResponse>> getQuizHistory(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(quizService.getQuizHistory(userId));
    }
}