package com.project.sentic.domain.user.service;

import com.project.sentic.domain.user.dto.*;
import com.project.sentic.domain.user.entity.User;
import com.project.sentic.domain.user.repository.UserRepository;
import com.project.sentic.global.auth.JwtTokenProvider;
import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import com.project.sentic.infra.email.EmailService;
import com.project.sentic.infra.email.VerificationCodeStore;
import com.project.sentic.infra.google.GoogleClient;
import com.project.sentic.infra.google.GoogleUserInfo;
import com.project.sentic.infra.kakao.KakaoClient;
import com.project.sentic.infra.kakao.KakaoUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/**
 * 인증 서비스
 *
 * 회원가입, 로그인, 로그아웃, 토큰 재발급 비즈니스 로직을 처리해요.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final VerificationCodeStore verificationCodeStore;
    private final KakaoClient kakaoClient;
    private final GoogleClient googleClient;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 일반 회원가입
     * 1. loginId / 이메일 중복 확인
     * 2. 비밀번호 암호화
     * 3. 유저 저장
     * 4. JWT 토큰 발급
     */
    @Transactional
    public TokenResponseDto signup(SignupRequestDto request) {
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new CustomException(ErrorCode.LOGIN_ID_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.createLocalUser(
                request.getLoginId(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getNickname()
        );
        userRepository.save(user);

        return generateToken(user);
    }

    /**
     * 일반 로그인
     * 1. loginId로 유저 조회
     * 2. 비밀번호 확인
     * 3. 계정 활성화 확인
     * 4. JWT 토큰 발급
     */
    public TokenResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 소셜 로그인 유저는 일반 로그인 불가
        if (user.getPassword() == null) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        // 계정 상태 확인 (ACTIVE가 아니면 로그인 불가)
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new CustomException(ErrorCode.INACTIVE_USER);
        }

        // JWT 토큰 발급
        return generateToken(user);
    }

    /**
     * Refresh Token으로 Access Token 재발급
     */
    public TokenResponseDto refresh(String refreshToken) {
        // Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        // 유저 조회
        Long userId = jwtTokenProvider.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 새 토큰 발급
        return generateToken(user);
    }

    /**
     * 이메일로 loginId(아이디) 찾기
     */
    public FindEmailResponseDto findLoginId(FindEmailRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return new FindEmailResponseDto(user.getLoginId());
    }

    /**
     * 비밀번호 재설정 인증코드 발송
     * 1. 이메일로 유저 조회
     * 2. 소셜 로그인 유저 차단
     * 3. 6자리 인증코드 생성 후 이메일 발송 + 저장 (5분 TTL)
     */
    @Transactional
    public void sendPasswordResetCode(PasswordResetRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getProvider() != User.Provider.LOCAL) {
            throw new CustomException(ErrorCode.SOCIAL_LOGIN_USER);
        }

        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        verificationCodeStore.save(request.getEmail(), code);
        emailService.sendPasswordResetCode(request.getEmail(), code);
    }

    /**
     * 인증코드 확인
     * 코드가 유효하지 않거나 만료된 경우 예외 발생
     */
    public void verifyCode(VerifyCodeRequestDto request) {
        verificationCodeStore.verify(request.getEmail(), request.getCode());
    }

    /**
     * 비밀번호 재설정
     * 1. 인증코드 재검증
     * 2. 새 비밀번호 암호화 후 저장
     * 3. 인증코드 삭제
     */
    @Transactional
    public void resetPassword(PasswordResetDto request) {
        verificationCodeStore.verify(request.getEmail(), request.getCode());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        verificationCodeStore.remove(request.getEmail());
    }

    /**
     * 카카오 소셜 로그인
     * 1. accessToken으로 카카오 사용자 정보 조회
     * 2. provider + providerId로 기존 회원 조회
     * 3. 없으면 자동 가입, 있으면 로그인
     * 4. JWT 발급
     */
    @Transactional
    public TokenResponseDto kakaoLogin(SocialLoginRequest request) {
        KakaoUserInfo userInfo = kakaoClient.getUserInfo(request.getAccessToken());

        String providerId = String.valueOf(userInfo.getId());
        String nickname = extractKakaoNickname(userInfo);
        String email = resolveEmail(extractKakaoEmail(userInfo), User.Provider.KAKAO, providerId);

        User user = userRepository.findByProviderAndProviderId(User.Provider.KAKAO, providerId)
                .orElseGet(() -> registerSocialUser(email, nickname, User.Provider.KAKAO, providerId));

        if (user.getStatus() != User.Status.ACTIVE) {
            throw new CustomException(ErrorCode.INACTIVE_USER);
        }

        return generateToken(user);
    }

    /**
     * 구글 소셜 로그인
     * 1. accessToken으로 구글 사용자 정보 조회
     * 2. provider + providerId로 기존 회원 조회
     * 3. 없으면 자동 가입, 있으면 로그인
     * 4. JWT 발급
     */
    @Transactional
    public TokenResponseDto googleLogin(SocialLoginRequest request) {
        GoogleUserInfo userInfo = googleClient.getUserInfo(request.getAccessToken());

        String providerId = userInfo.getSub();
        String nickname = userInfo.getName();
        String email = resolveEmail(userInfo.getEmail(), User.Provider.GOOGLE, providerId);

        User user = userRepository.findByProviderAndProviderId(User.Provider.GOOGLE, providerId)
                .orElseGet(() -> registerSocialUser(email, nickname, User.Provider.GOOGLE, providerId));

        if (user.getStatus() != User.Status.ACTIVE) {
            throw new CustomException(ErrorCode.INACTIVE_USER);
        }

        return generateToken(user);
    }

    private User registerSocialUser(String email, String nickname, User.Provider provider, String providerId) {
        String loginId = provider.name().toLowerCase() + "_" + providerId;
        String uniqueNickname = resolveNickname(nickname);
        User user = User.createSocialUser(loginId, email, uniqueNickname, provider, providerId);
        return userRepository.save(user);
    }

    // 닉네임 중복 시 숫자 접미사 붙여 유니크하게 만들기
    private String resolveNickname(String base) {
        if (!userRepository.existsByNickname(base)) {
            return base;
        }
        String candidate;
        do {
            candidate = base + SECURE_RANDOM.nextInt(10000);
        } while (userRepository.existsByNickname(candidate));
        return candidate;
    }

    // 이메일 미제공 또는 이미 사용 중인 경우 대체 이메일 생성
    private String resolveEmail(String email, User.Provider provider, String providerId) {
        if (email != null && !userRepository.existsByEmail(email)) {
            return email;
        }
        return provider.name().toLowerCase() + "_" + providerId + "@sentic.local";
    }

    private String extractKakaoNickname(KakaoUserInfo userInfo) {
        if (userInfo.getKakaoAccount() != null
                && userInfo.getKakaoAccount().getProfile() != null
                && userInfo.getKakaoAccount().getProfile().getNickname() != null) {
            return userInfo.getKakaoAccount().getProfile().getNickname();
        }
        return "카카오사용자";
    }

    private String extractKakaoEmail(KakaoUserInfo userInfo) {
        if (userInfo.getKakaoAccount() != null) {
            return userInfo.getKakaoAccount().getEmail();
        }
        return null;
    }

    /**
     * JWT 토큰 생성 공통 메서드
     */
    private TokenResponseDto generateToken(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getId(),
                user.getRole().name()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        return new TokenResponseDto(accessToken, refreshToken);
    }

    public boolean isLoginIdAvailable(String loginId) {
        return !userRepository.existsByLoginId(loginId);
    }

}