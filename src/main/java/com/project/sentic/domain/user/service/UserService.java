package com.project.sentic.domain.user.service;

import com.project.sentic.domain.message.entity.Message;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);

        // 최근 7일 메시지 조회
        List<Message> recentMessages = messageRepository
                .findByRoomUserIdAndCreatedAtAfter(userId, weekStart.atStartOfDay());

        // 요일별 학습 시간 계산 (메시지 1개당 1분으로 계산)
        Map<DayOfWeek, Integer> minutesByDay = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            minutesByDay.put(day, 0);
        }

        for (Message message : recentMessages) {
            if (message.getSenderType() == Message.SenderType.USER) {
                DayOfWeek day = message.getCreatedAt().getDayOfWeek();
                minutesByDay.put(day, minutesByDay.get(day) + 1);
            }
        }

        // weekly 리스트 생성
        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};

        List<StudyStatsResponse.DailyStudy> weekly = new ArrayList<>();
        int totalMinutes = 0;

        for (int i = 0; i < 7; i++) {
            int minutes = minutesByDay.get(days[i]);
            totalMinutes += minutes;
            weekly.add(StudyStatsResponse.DailyStudy.builder()
                    .day(dayNames[i])
                    .minute(minutes)
                    .build());
        }

        int avgMinutes = totalMinutes > 0 ? totalMinutes / 7 : 0;

        // 연속 학습일 계산
        int continuousDays = calculateContinuousDays(userId, today);

        return StudyStatsResponse.builder()
                .totalMinutes(totalMinutes)
                .avgMinutes(avgMinutes)
                .continuousDays(continuousDays)
                .weekly(weekly)
                .build();
    }

    private int calculateContinuousDays(Long userId, LocalDate today) {
        int days = 0;
        LocalDate date = today;

        while (true) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(23, 59, 59);
            boolean hasMessage = messageRepository
                    .existsByRoomUserIdAndCreatedAtBetween(userId, start, end);
            if (!hasMessage) break;
            days++;
            date = date.minusDays(1);
        }
        return days;
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
}