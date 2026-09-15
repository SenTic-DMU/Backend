package com.project.sentic.domain.user.service;

import com.project.sentic.domain.badge.service.BadgeService;
import com.project.sentic.domain.user.entity.UserSettings;
import com.project.sentic.domain.user.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingResetScheduler {

    private final UserSettingsRepository userSettingsRepository;
    private final BadgeService badgeService;

    // 매주 월요일 새벽 3시에 초기화
    @Scheduled(cron = "0 0 3 * * MON")
    @Transactional
    public void resetWeeklyScores() {
        // 1위 확인 후 뱃지 처리
        List<UserSettings> allSettings = userSettingsRepository.findAllByOrderByWeeklyScoreDesc();
        if (!allSettings.isEmpty() && allSettings.get(0).getWeeklyScore() > 0) {
            UserSettings first = allSettings.get(0);
            first.addRank1();
            log.info("[Ranking] 이번 주 1위: userId={}", first.getUserId());

            // 1위가 아닌 사용자 연속 1위 초기화
            for (int i = 1; i < allSettings.size(); i++) {
                allSettings.get(i).resetRank1Streak();
            }

            // 뱃지 체크
            try {
                badgeService.checkAndAwardBadges(first.getUserId());
            } catch (Exception e) {
                log.warn("[Badge] 1위 뱃지 체크 실패: {}", e.getMessage());
            }
        }

        // 점수 초기화
        int count = userSettingsRepository.resetAllWeeklyScores();
        log.info("[Ranking] 전체 사용자 weekly_score 초기화 완료 ({}명)", count);
    }
}