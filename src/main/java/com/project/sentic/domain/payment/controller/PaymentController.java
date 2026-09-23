package com.project.sentic.domain.payment.controller;

import com.project.sentic.domain.payment.dto.PaymentConfirmRequest;
import com.project.sentic.domain.payment.dto.PaymentResponse;
import com.project.sentic.domain.payment.service.PaymentService;
import com.project.sentic.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Payment", description = "결제 API")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "토스페이먼츠 결제 승인")
    @PostMapping("/toss/confirm")
    public ApiResponse<PaymentResponse> confirmPayment(
            @AuthenticationPrincipal Long userId,
            @RequestBody PaymentConfirmRequest request) {
        return ApiResponse.success(paymentService.confirmPayment(userId, request));
    }

    @Operation(summary = "내 결제 내역 조회")
    @GetMapping("/my")
    public ApiResponse<List<PaymentResponse>> getMyPayments(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(paymentService.getMyPayments(userId));
    }

    @Operation(
            summary = "구독 해지 예약",
            description = "현재 Premium 이용기간은 유지하고, 이용기간 종료 시 구독이 해지됩니다."
    )
    @PostMapping("/subscription/cancel")
    public ApiResponse<PaymentResponse> cancelSubscription(
            @AuthenticationPrincipal Long userId
    ) {

        return ApiResponse.success(
                paymentService.cancelSubscription(userId)
        );
    }

}
