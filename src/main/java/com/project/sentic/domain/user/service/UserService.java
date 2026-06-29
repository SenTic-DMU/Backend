package com.project.sentic.domain.user.service;

import com.project.sentic.domain.user.dto.MyPageResponse;
import com.project.sentic.domain.user.dto.UserSettingsResponse;
import com.project.sentic.domain.user.dto.UserSettingsUpdateRequest;
import com.project.sentic.domain.user.dto.WithdrawRequestDto;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
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