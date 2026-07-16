package com.project.sentic.domain.payment.repository;

import com.project.sentic.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 전체 결제 내역 (최신순)
    List<Payment> findAllByOrderByCreatedAtDesc();

    // 상태별 결제 내역
    List<Payment> findByStatusOrderByCreatedAtDesc(Payment.PaymentStatus status);

    // 회원별 결제 내역
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
}