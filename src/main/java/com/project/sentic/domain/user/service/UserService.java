package com.project.sentic.domain.user.service;

import com.project.sentic.domain.message.repository.MessageRepository;
import com.project.sentic.domain.user.dto.*;
import com.project.sentic.domain.user.entity.User;
import com.project.sentic.domain.user.entity.UserSettings;
import com.project.sentic.domain.user.repository.UserRepository;
import com.project.sentic.domain.user.repository.UserSettingsRepository;
import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원 탈퇴 (기존 코드 유지)
    @Transactional
    public void withdraw(Long userId, WithdrawRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != User.Status.ACTIVE) {
            throw new CustomException(ErrorCode.INACTIVE_USER);
        }

        if (user.getProvider() == User.Provider.LOCAL) {
            if (request.getPassword() == null
                    || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new CustomException(ErrorCode.INVALID_PASSWORD);
            }
        }

        user.deactivate();
    }

    // 마이페이지 조회
    public MyPageResponse getMyPage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return MyPageResponse.of(user, settings);
    }

    // 학습 통계 조회
    public StudyStatsResponse getStudyStats(Long userId) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<StudyStatsResponse.DailyStudy> weekly = List.of(
                StudyStatsResponse.DailyStudy.builder().day("Mon").minute(settings.getMonMinutes()).build(),
                StudyStatsResponse.DailyStudy.builder().day("Tue").minute(settings.getTueMinutes()).build(),
                StudyStatsResponse.DailyStudy.builder().day("Wed").minute(settings.getWedMinutes()).build(),
                StudyStatsResponse.DailyStudy.builder().day("Thu").minute(settings.getThuMinutes()).build(),
                StudyStatsResponse.DailyStudy.builder().day("Fri").minute(settings.getFriMinutes()).build(),
                StudyStatsResponse.DailyStudy.builder().day("Sat").minute(settings.getSatMinutes()).build(),
                StudyStatsResponse.DailyStudy.builder().day("Sun").minute(settings.getSunMinutes()).build()
        );

        int totalMinutes = settings.getWeeklyStudyTime();
        int avgMinutes = totalMinutes > 0 ? totalMinutes / 7 : 0;

        return StudyStatsResponse.builder()
                .totalMinutes(totalMinutes)
                .avgMinutes(avgMinutes)
                .continuousDays(settings.getStreakDays())
                .weekly(weekly)
                .build();
    }

    // 학습 레벨 변경
    @Transactional
    public UserSettingsResponse updateLevel(Long userId, String difficulty) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        settings.updateDefaultDifficulty(
                UserSettings.DefaultDifficulty.valueOf(difficulty)
        );

        return UserSettingsResponse.from(settings);
    }

    // 설정 조회
    public UserSettingsResponse getSettings(Long userId) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return UserSettingsResponse.from(settings);
    }

    // 설정 변경
    @Transactional
    public UserSettingsResponse updateSettings(Long userId, UserSettingsUpdateRequest request) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (request.getNotificationEnabled() != null) {
            settings.updateNotificationEnabled(request.getNotificationEnabled());
        }
        if (request.getDefaultMode() != null) {
            settings.updateDefaultMode(
                    UserSettings.DefaultMode.valueOf(request.getDefaultMode())
            );
        }
        if (request.getDefaultDifficulty() != null) {
            settings.updateDefaultDifficulty(
                    UserSettings.DefaultDifficulty.valueOf(request.getDefaultDifficulty())
            );
        }

        return UserSettingsResponse.from(settings);
    }

    // 앱 접속 시작
    @Transactional
    public void startSession(Long userId) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        settings.startSession();
    }

    // 앱 접속 종료
    @Transactional
    public void endSession(Long userId) {
        UserSettings settings = userSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        settings.endSession();
    }

    // 전체 랭킹 조회
    public List<RankingResponse> getRanking() {
        List<UserSettings> allSettings = userSettingsRepository.findAllByOrderByWeeklyScoreDesc();

        List<RankingResponse> ranking = new ArrayList<>();
        for (int i = 0; i < allSettings.size(); i++) {
            UserSettings s = allSettings.get(i);
            User user = userRepository.findById(s.getUserId()).orElse(null);
            if (user == null) continue;

            ranking.add(RankingResponse.builder()
                    .rank(i + 1)
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .weeklyScore(s.getWeeklyScore())
                    .build());
        }
        return ranking;
    }

    // 내 순위 조회
    public RankingResponse getMyRanking(Long userId) {
        List<UserSettings> allSettings = userSettingsRepository.findAllByOrderByWeeklyScoreDesc();

        for (int i = 0; i < allSettings.size(); i++) {
            UserSettings s = allSettings.get(i);
            if (s.getUserId().equals(userId)) {
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                return RankingResponse.builder()
                        .rank(i + 1)
                        .userId(user.getId())
                        .nickname(user.getNickname())
                        .weeklyScore(s.getWeeklyScore())
                        .build();
            }
        }
        throw new CustomException(ErrorCode.USER_NOT_FOUND);
    }
}