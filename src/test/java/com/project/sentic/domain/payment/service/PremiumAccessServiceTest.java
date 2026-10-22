package com.project.sentic.domain.payment.service;

import com.project.sentic.domain.payment.entity.Payment;
import com.project.sentic.domain.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PremiumAccessServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PremiumAccessService premiumAccessService;

    @Test
    void 사용자ID가_없으면_무료로_판단한다() {
        assertFalse(premiumAccessService.isPremium(null));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void 유효한_결제기록이_없으면_무료로_판단한다() {
        when(paymentRepository
                .findFirstByUserIdAndStatusAndExpiresAtAfterAndPlanIdNotOrderByPaidAtDesc(
                        eq(1L),
                        eq(Payment.PaymentStatus.SUCCESS),
                        any(LocalDateTime.class),
                        eq(1)
                ))
                .thenReturn(Optional.empty());

        assertFalse(premiumAccessService.isPremium(1L));
    }

    @Test
    void 유효한_결제기록이_있으면_프리미엄으로_판단한다() {
        when(paymentRepository
                .findFirstByUserIdAndStatusAndExpiresAtAfterAndPlanIdNotOrderByPaidAtDesc(
                        eq(1L),
                        eq(Payment.PaymentStatus.SUCCESS),
                        any(LocalDateTime.class),
                        eq(1)
                ))
                .thenReturn(Optional.of(mock(Payment.class)));

        assertTrue(premiumAccessService.isPremium(1L));
    }
}