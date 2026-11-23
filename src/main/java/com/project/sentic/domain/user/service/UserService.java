package com.project.sentic.domain.user.service;

import com.project.sentic.domain.report.service.ReportService;
import com.project.sentic.domain.user.dto.*;
import com.project.sentic.domain.user.entity.User;
import com.project.sentic.domain.user.entity.UserSettings;
import com.project.sentic.domain.user.repository.UserRepository;
import com.project.sentic.domain.user.repository.UserSettingsRepository;
import com.project.sentic.domain.badge.service.BadgeService;
import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.project.sentic.infra.s3.S3Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final BadgeService badgeService;
    private final ReportService reportService;
    private final S3Service s3Service;

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

        return MyPageResponse.of(
                user,
                settings,
                s3Service.getProfileImageUrl(user.getProfileImage())
        );
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

        int minutes = settings.endSession();

        // 학습 시간 기록
        if (minutes > 0) {
            reportService.logStudyMinutes(userId, minutes);
        }
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


    // 내 리그 조회 (기존 호출 호환)
    public LeagueResponse getMyLeague(Long userId) {
        return getMyLeague(userId, null);
    }

    // 리그 및 리그 내 순위 조회
    public LeagueResponse getMyLeague(Long userId, String tier) {

        UserSettings mySettings = userSettingsRepository
                .findByUserId(userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND));

        // 기본값은 내 리그
        var targetLeague = mySettings.getLeague();

        // tier가 전달되면 해당 리그로 변경
        if (tier != null) {
            if (tier.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "tier 값이 비어 있습니다."
                );
            }

            try {
                targetLeague = Enum.valueOf(
                        targetLeague.getDeclaringClass(),
                        tier.trim().toUpperCase(Locale.ROOT)
                );
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "존재하지 않는 리그입니다."
                );
            }
        }

        // 선택한 리그의 회원을 주간 점수 내림차순으로 조회
        List<UserSettings> leagueMembers = userSettingsRepository
                .findByLeagueOrderByWeeklyScoreDesc(targetLeague);

        int myRank = 0;

        List<LeagueResponse.LeagueMemberItem> members =
                new ArrayList<>();

        for (int i = 0; i < leagueMembers.size(); i++) {

            UserSettings s = leagueMembers.get(i);

            User user = userRepository
                    .findById(s.getUserId())
                    .orElse(null);

            if (user == null) continue;

            // 현재 사용자가 조회 리그에 속해 있을 때만 순위 설정
            if (s.getUserId().equals(userId)) {
                myRank = i + 1;
            }

            members.add(
                    LeagueResponse.LeagueMemberItem.builder()
                            .rank(i + 1)
                            .userId(user.getId())
                            .nickname(user.getNickname())
                            .profileImageUrl(
                                    s3Service.getProfileImageUrl(user.getProfileImage())
                            )
                            .weeklyScore(s.getWeeklyScore())
                            .featuredBadges(
                                    badgeService.getFeaturedBadges(user.getId())
                            )
                            .build()
            );
        }

        return LeagueResponse.builder()
                .league(targetLeague.name())
                .myRank(myRank)
                .myScore(mySettings.getWeeklyScore())
                .totalMembers(leagueMembers.size())
                .members(members)
                .build();
    }

    @Transactional
    public ProfileImageResponse uploadProfileImage(
            Long userId,
            MultipartFile file
    ) {
        if (file == null || file.isEmpty()
                || file.getSize() > 2L * 1024 * 1024) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이미지는 2MB 이하로 업로드해 주세요."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() != User.Status.ACTIVE) {
            throw new CustomException(ErrorCode.INACTIVE_USER);
        }

        try {
            byte[] bytes = file.getBytes();
            String format = detectImageFormat(bytes);

            if (format == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "JPG, PNG, WEBP 이미지만 지원합니다."
                );
            }

            String contentType = switch (format) {
                case "jpg" -> "image/jpeg";
                case "png" -> "image/png";
                default -> "image/webp";
            };

            String imageKey = s3Service.uploadProfileImage(
                    bytes,
                    userId,
                    format,
                    contentType
            );

            user.updateProfileImage(imageKey);

            return new ProfileImageResponse(
                    s3Service.getProfileImageUrl(imageKey)
            );

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이미지 파일을 읽을 수 없습니다.",
                    e
            );
        }
    }

    private static String detectImageFormat(byte[] b) {
        if (b.length >= 3
                && (b[0] & 255) == 0xFF
                && (b[1] & 255) == 0xD8
                && (b[2] & 255) == 0xFF) {
            return "jpg";
        }

        if (b.length >= 8
                && (b[0] & 255) == 0x89
                && b[1] == 0x50
                && b[2] == 0x4E
                && b[3] == 0x47
                && b[4] == 0x0D
                && b[5] == 0x0A
                && b[6] == 0x1A
                && b[7] == 0x0A) {
            return "png";
        }

        if (b.length >= 12
                && b[0] == 'R'
                && b[1] == 'I'
                && b[2] == 'F'
                && b[3] == 'F'
                && b[8] == 'W'
                && b[9] == 'E'
                && b[10] == 'B'
                && b[11] == 'P') {
            return "webp";
        }

        return null;
    }
}