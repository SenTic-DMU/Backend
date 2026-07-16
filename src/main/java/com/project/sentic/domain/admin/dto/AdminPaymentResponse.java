package com.project.sentic.domain.admin.dto;

import com.project.sentic.domain.payment.entity.Payment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminPaymentResponse {

    private Long id;
    private Long userId;
    private Integer planId;
    private Integer amount;
    private String status;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public static AdminPaymentResponse from(Payment payment) {
        return AdminPaymentResponse.builder()
                .id(payment.getId())
                .userId(payment.getUserId())
                .planId(payment.getPlanId())
                .amount(payment.getAmount())
                .status(payment.getStatus().name())
                .paymentMethod(payment.getPaymentMethod())
                .paidAt(payment.getPaidAt())
                .expiresAt(payment.getExpiresAt())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}