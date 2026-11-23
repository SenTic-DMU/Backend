package com.project.sentic.domain.payment.repository;

import com.project.sentic.domain.payment.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Integer> {
    Optional<SubscriptionPlan> findByPlanName(String planName);
}