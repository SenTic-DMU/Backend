package com.project.sentic.domain.badge.service;

import com.project.sentic.domain.badge.dto.BadgeResponse;
import com.project.sentic.domain.badge.dto.FeaturedBadgeRequest;
import com.project.sentic.domain.badge.entity.Badge;
import com.project.sentic.domain.badge.entity.UserBadge;
import com.project.sentic.domain.badge.repository.BadgeRepository;
import com.project.sentic.domain.badge.repository.UserBadgeRepository;
import com.project.sentic.domain.message.repository.MessageRepository;
import com.project.sentic.domain.quiz.repository.QuizSessionRepository;
import com.project.sentic.domain.user.entity.UserSettings;
import com.project.sentic.domain.user.repository.UserSettingsRepository;
import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final MessageRepository messageRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final UserSettingsRepository userSettingsRepository;

    // 뱃지판 조회 (전체 뱃지 + 획득 여부 + 대표 뱃지 여부)
    public List<BadgeResponse> getBadgeBoard(Long userId) {
        List<Badge> allBadges = badgeRepository.findAll();
        List<UserBadge> userBadges = userBadgeRepository.findByUserId(userId);

        Set<Long> earnedBadgeIds = userBadges.stream()
                .map(UserBadge::getBadgeId)
                .collect(Collectors.toSet());

        Set<Long> featuredBadgeIds = userBadges.stream()
                .filter(UserBadge::isFeatured)
                .map(UserBadge::getBadgeId)
                .collect(Collectors.toSet());

        return allBadges.stream()
                .map(badge -> BadgeResponse.of(
                        badge,
                        earnedBadgeIds.contains(badge.getId()),
                        featuredBadgeIds.contains(badge.getId())
                ))
                .collect(Collectors.toList());
    }

    // 대표 뱃지 설정 (최대 3개)
    @Transactional
    public List<BadgeResponse> setFeaturedBadges(Long userId, FeaturedBadgeRequest request) {
        if (request.getBadgeIds().size() > 3) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        List<UserBadge> userBadges = userBadgeRepository.findByUserId(userId);

        // 기존 대표 뱃지 모두 해제
        userBadges.forEach(ub -> ub.setFeatured(false));

        // 선택한 뱃지 대표로 설정
        for (UserBadge ub : userBadges) {
            if (request.getBadgeIds().contains(ub.getBadgeId())) {
                ub.setFeatured(true);
            }
        }

        return getBadgeBoard(userId);
    }

    // 대표 뱃지 조회 (랭킹에서 사용)
    public List<BadgeResponse> getFeaturedBadges(Long userId) {
        List<UserBadge> featuredBadges = userBadgeRepository.findByUserIdAndFeaturedTrue(userId);
        List<Badge> allBadges = badgeRepository.findAll();

        Map<Long, Badge> badgeMap = allBadges.stream()
                .collect(Collectors.toMap(Badge::getId, b -> b));

        return featuredBadges.stream()
                .map(ub -> BadgeResponse.of(badgeMap.get(ub.getBadgeId()), true, true))
                .collect(Collectors.toList());
    }

    // 뱃지 조건 체크 + 자동 부여
    @Transactional
    public List<BadgeResponse> checkAndAwardBadges(Long userId) {
        List<Badge> allBadges = badgeRepository.findAll();
        List<BadgeResponse> newBadges = new ArrayList<>();

        for (Badge badge : allBadges) {
            // 이미 획득한 뱃지는 스킵
            if (userBadgeRepository.existsByUserIdAndBadgeId(userId, badge.getId())) {
                continue;
            }

            // 조건 충족 여부 확인
            if (checkCondition(userId, badge.getBadgeCode())) {
                userBadgeRepository.save(UserBadge.builder()
                        .userId(userId)
                        .badgeId(badge.getId())
                        .build());

                newBadges.add(BadgeResponse.of(badge, true, false));
                log.info("[Badge] 뱃지 획득! userId: {}, badge: {}", userId, badge.getBadgeName());

                // 뱃지 수집가, 뱃지 마스터 재체크
                checkMetaBadges(userId);
            }
        }

        return newBadges;
    }

    // 개별 뱃지 조건 확인
    private boolean checkCondition(Long userId, String badgeCode) {
        return switch (badgeCode) {
            // 초급
            case "CHAT_100" -> getChatCount(userId) >= 100;
            case "QUIZ_50" -> getQuizCorrectCount(userId) >= 50;
            case "STREAK_7" -> getStreakDays(userId) >= 7;

            // 중급
            case "CHAT_200" -> getChatCount(userId) >= 200;
            case "QUIZ_150" -> getQuizCorrectCount(userId) >= 150;
            case "STUDY_3H" -> getStudyMinutes(userId) >= 180;
            case "RANK_1" -> getRank1Streak(userId) >= 1;
            case "BADGE_7" -> userBadgeRepository.countByUserId(userId) >= 7;

            // 고급
            case "STREAK_30" -> getStreakDays(userId) >= 30;
            case "STUDY_10H" -> getStudyMinutes(userId) >= 600;
            case "RANK_1_STREAK_5" -> getRank1Streak(userId) >= 5;
            case "LEAGUE_ALL_RANK_1" -> isAllLeagueRank1(userId);

            // 특별
            case "BADGE_ALL" -> userBadgeRepository.countByUserId(userId) >= 12;

            default -> false;
        };
    }

    // 메타 뱃지 (뱃지 수집가, 뱃지 마스터) 재체크
    private void checkMetaBadges(Long userId) {
        long badgeCount = userBadgeRepository.countByUserId(userId);

        // 뱃지 수집가 (7개)
        Badge badge7 = badgeRepository.findByBadgeCode("BADGE_7").orElse(null);
        if (badge7 != null && badgeCount >= 7
                && !userBadgeRepository.existsByUserIdAndBadgeId(userId, badge7.getId())) {
            userBadgeRepository.save(UserBadge.builder()
                    .userId(userId)
                    .badgeId(badge7.getId())
                    .build());
            log.info("[Badge] 뱃지 수집가 획득! userId: {}", userId);
        }

        // 뱃지 마스터 (12개)
        Badge badgeAll = badgeRepository.findByBadgeCode("BADGE_ALL").orElse(null);
        if (badgeAll != null && badgeCount >= 12
                && !userBadgeRepository.existsByUserIdAndBadgeId(userId, badgeAll.getId())) {
            userBadgeRepository.save(UserBadge.builder()
                    .userId(userId)
                    .badgeId(badgeAll.getId())
                    .build());
            log.info("[Badge] 뱃지 마스터 획득! userId: {}", userId);
        }
    }

    // ── 데이터 조회 메서드 ──────────────────────────

    private long getChatCount(Long userId) {
        List<Long> roomIds = messageRepository.findDistinctRoomIdsByUserId(userId);
        if (roomIds.isEmpty()) return 0;
        return messageRepository.findByRoomIdIn(roomIds).stream()
                .filter(m -> m.getSenderType().name().equals("USER"))
                .count();
    }

    private long getQuizCorrectCount(Long userId) {
        return quizSessionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(s -> s.getScore() != null)
                .mapToInt(s -> s.getScore())
                .sum();
    }

    private int getStreakDays(Long userId) {
        return userSettingsRepository.findByUserId(userId)
                .map(UserSettings::getStreakDays)
                .orElse(0);
    }

    private int getStudyMinutes(Long userId) {
        return userSettingsRepository.findByUserId(userId)
                .map(UserSettings::getWeeklyStudyTime)
                .orElse(0);
    }

    private int getRank1Total(Long userId) {
        return userSettingsRepository.findByUserId(userId)
                .map(UserSettings::getRank1Total)
                .orElse(0);
    }

    private int getRank1Streak(Long userId) {
        return userSettingsRepository.findByUserId(userId)
                .map(UserSettings::getRank1Streak)
                .orElse(0);
    }

    private boolean isAllLeagueRank1(Long userId) {
        return userSettingsRepository.findByUserId(userId)
                .map(UserSettings::isAllLeagueRank1)
                .orElse(false);
    }
}