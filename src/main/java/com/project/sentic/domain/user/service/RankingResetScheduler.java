package com.project.sentic.domain.user.service;

import com.project.sentic.domain.user.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingResetScheduler {

    private final UserSettingsRepository userSettingsRepository;

    // 매주 월요일 새벽 3시에 초기화
    @Scheduled(cron = "0 0 3 * * MON")
    @Transactional
    public void resetWeeklyScores() {
        int count = userSettingsRepository.resetAllWeeklyScores();
        log.info("[Ranking] 전체 사용자 weekly_score 초기화 완료 ({}명)", count);
    }
}