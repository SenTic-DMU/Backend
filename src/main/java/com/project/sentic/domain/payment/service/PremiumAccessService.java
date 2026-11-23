
package com.project.sentic.domain.payment.service;

import com.project.sentic.domain.payment.entity.Payment;
import com.project.sentic.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PremiumAccessService {

    private final PaymentRepository paymentRepository;

    public boolean isPremium(Long userId) {
        if (userId == null) {
            return false;
        }

        return paymentRepository
                .findFirstByUserIdAndStatusAndExpiresAtAfterAndPlanIdNotOrderByPaidAtDesc(
                        userId,
                        Payment.PaymentStatus.SUCCESS,
                        LocalDateTime.now(),
                        1
                )
                .isPresent();
    }
}
