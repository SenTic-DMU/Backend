package com.project.sentic.domain.user.service;

import com.project.sentic.domain.badge.service.BadgeService;
import com.project.sentic.domain.user.entity.UserSettings;
import com.project.sentic.domain.user.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.project.sentic.domain.badge.entity.Badge;
import com.project.sentic.domain.badge.repository.BadgeRepository;
import com.project.sentic.domain.badge.repository.UserBadgeRepository;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingResetScheduler {

    private final UserSettingsRepository userSettingsRepository;
    private final BadgeService badgeService;
    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;

    // 매주 월요일 새벽 3시에 실행
    @Scheduled(cron = "0 0 3 * * MON")
    @Transactional
    public void processLeagueAndReset() {
        log.info("[League] 리그 승급/강등 처리 시작");

        // 리그별로 처리 (BRONZE → MASTER 순서)
        for (UserSettings.League league : UserSettings.League.values()) {
            processLeague(league);
        }

        // 전원 포인트 초기화
        int count = userSettingsRepository.resetAllWeeklyScores();
        log.info("[League] 전체 사용자 weekly_score 초기화 완료 ({}명)", count);
    }

    private void processLeague(UserSettings.League league) {
        List<UserSettings> members = userSettingsRepository
                .findByLeagueOrderByWeeklyScoreDesc(league);

        if (members.isEmpty()) return;

        int total = members.size();
        int promoteCount = (int) Math.ceil(total * 0.1);  // 상위 10% (올림)
        int demoteCount = (int) Math.ceil(total * 0.1);   // 하위 10% (올림)

        // 1위 처리 (뱃지 관련)
        UserSettings first = members.get(0);
        if (first.getWeeklyScore() > 0) {
            first.addRank1();
            first.markRank1InLeague(league);
            log.info("[League] {} 리그 1위: userId={}", league, first.getUserId());

            // 1위 뱃지 체크
            try {
                badgeService.checkAndAwardBadges(first.getUserId());
            } catch (Exception e) {
                log.warn("[Badge] 1위 뱃지 체크 실패: {}", e.getMessage());
            }
        }

        // 1위가 아닌 사용자 연속 1위 초기화
        for (int i = 1; i < members.size(); i++) {
            members.get(i).resetRank1Streak();
        }

        // 이번 주의 왕 뱃지 - 1위가 아닌 사람은 뱃지 제거
        for (int i = 1; i < members.size(); i++) {
            removeWeeklyKingBadge(members.get(i).getUserId());
        }

        // 승급 (상위 10%) - Master는 승급 없음
        if (league != UserSettings.League.MASTER) {
            UserSettings.League nextLeague = getNextLeague(league);
            for (int i = 0; i < Math.min(promoteCount, total); i++) {
                UserSettings user = members.get(i);
                if (user.getWeeklyScore() > 0) {  // 0점은 승급 안 함
                    user.updateLeague(nextLeague);
                    log.info("[League] 승급: userId={}, {} → {}", user.getUserId(), league, nextLeague);
                }
            }
        }

        // 강등 (하위 10%) - Bronze는 강등 없음
        if (league != UserSettings.League.BRONZE) {
            UserSettings.League prevLeague = getPrevLeague(league);
            for (int i = total - 1; i >= Math.max(total - demoteCount, 0); i--) {
                UserSettings user = members.get(i);
                user.updateLeague(prevLeague);
                log.info("[League] 강등: userId={}, {} → {}", user.getUserId(), league, prevLeague);
            }
        }
    }

    private UserSettings.League getNextLeague(UserSettings.League league) {
        return switch (league) {
            case BRONZE -> UserSettings.League.SILVER;
            case SILVER -> UserSettings.League.GOLD;
            case GOLD -> UserSettings.League.SAPPHIRE;
            case SAPPHIRE -> UserSettings.League.DIAMOND;
            case DIAMOND -> UserSettings.League.MASTER;
            case MASTER -> UserSettings.League.MASTER;
        };
    }

    private UserSettings.League getPrevLeague(UserSettings.League league) {
        return switch (league) {
            case MASTER -> UserSettings.League.DIAMOND;
            case DIAMOND -> UserSettings.League.SAPPHIRE;
            case SAPPHIRE -> UserSettings.League.GOLD;
            case GOLD -> UserSettings.League.SILVER;
            case SILVER -> UserSettings.League.BRONZE;
            case BRONZE -> UserSettings.League.BRONZE;
        };
    }

    private void removeWeeklyKingBadge(Long userId) {
        Badge rank1Badge = badgeRepository.findByBadgeCode("RANK_1").orElse(null);
        if (rank1Badge == null) return;

        userBadgeRepository.deleteByUserIdAndBadgeId(userId, rank1Badge.getId());
    }
}