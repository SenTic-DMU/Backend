package com.project.sentic.domain.payment.repository;

import com.project.sentic.domain.payment.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Integer> {
}