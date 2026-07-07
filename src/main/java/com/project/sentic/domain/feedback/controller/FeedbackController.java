package com.project.sentic.domain.feedback.controller;

import com.project.sentic.domain.feedback.dto.FeedbackResponse;
import com.project.sentic.domain.feedback.service.FeedbackService;
import com.project.sentic.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Feedback", description = "피드백 API")
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "방의 피드백 목록 조회")
    @GetMapping("/{roomId}/feedbacks")
    public ApiResponse<List<FeedbackResponse>> getFeedbacks(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long roomId) {
        return ApiResponse.success(feedbackService.getFeedbacks(userId, roomId));
    }
}