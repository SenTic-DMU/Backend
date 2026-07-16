package com.project.sentic.domain.payment.service;

import com.project.sentic.domain.payment.dto.PaymentConfirmRequest;
import com.project.sentic.domain.payment.dto.PaymentResponse;
import com.project.sentic.domain.payment.entity.Payment;
import com.project.sentic.domain.payment.entity.SubscriptionPlan;
import com.project.sentic.domain.payment.repository.PaymentRepository;
import com.project.sentic.domain.payment.repository.SubscriptionPlanRepository;
import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import com.project.sentic.infra.toss.TossConfirmResponse;
import com.project.sentic.infra.toss.TossPaymentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final TossPaymentClient tossPaymentClient;

    // 결제 승인 실패 시 FAILED 레코드를 커밋해야 하므로 CustomException은 롤백하지 않음
    @Transactional(noRollbackFor = CustomException.class)
    public PaymentResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getPlanId())
                .orElseThrow(() -> new CustomException(ErrorCode.PLAN_NOT_FOUND));

        if (!plan.getPrice().equals(request.getAmount())) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        LocalDateTime now = LocalDateTime.now();

        // Free 플랜: 토스 API 호출 없이 바로 저장
        if (plan.getId() == 1) {
            Payment payment = Payment.builder()
                    .userId(userId)
                    .planId(plan.getId())
                    .amount(plan.getPrice())
                    .status(Payment.PaymentStatus.SUCCESS)
                    .paymentMethod("FREE")
                    .paidAt(now)
                    .expiresAt(now.plusDays(plan.getDurationDays()))
                    .build();
            return PaymentResponse.from(paymentRepository.save(payment));
        }

        // 유료 플랜: 토스페이먼츠 승인 API 호출
        try {
            TossConfirmResponse tossResponse = tossPaymentClient.confirm(
                    request.getPaymentKey(), request.getOrderId(), request.getAmount());

            Payment payment = Payment.builder()
                    .userId(userId)
                    .planId(plan.getId())
                    .amount(plan.getPrice())
                    .status(Payment.PaymentStatus.SUCCESS)
                    .paymentMethod(tossResponse.getMethod())
                    .externalPaymentId(request.getPaymentKey())
                    .paidAt(now)
                    .expiresAt(now.plusDays(plan.getDurationDays()))
                    .build();
            return PaymentResponse.from(paymentRepository.save(payment));

        } catch (Exception e) {
            log.error("결제 승인 실패 - userId: {}, paymentKey: {}", userId, request.getPaymentKey(), e);

            Payment failedPayment = Payment.builder()
                    .userId(userId)
                    .planId(plan.getId())
                    .amount(request.getAmount())
                    .status(Payment.PaymentStatus.FAILED)
                    .externalPaymentId(request.getPaymentKey())
                    .build();
            paymentRepository.save(failedPayment);

            throw new CustomException(ErrorCode.PAYMENT_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PaymentResponse::from)
                .collect(Collectors.toList());
    }
}
