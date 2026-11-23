package com.project.sentic.domain.payment.service;

import com.project.sentic.domain.payment.entity.SubscriptionPlan;
import com.project.sentic.domain.payment.repository.SubscriptionPlanRepository;
import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanLimitService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    private SubscriptionPlan getFreePlan() {
        return subscriptionPlanRepository.findByPlanName("Free")
                .orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    public int getRoomLimit() {
        Integer limit = getFreePlan().getRoomLimit();
        return limit != null ? limit : 3;
    }

    public int getDailyMessageLimit() {
        Integer limit = getFreePlan().getDailyMessageLimit();
        return limit != null ? limit : 30;
    }
}