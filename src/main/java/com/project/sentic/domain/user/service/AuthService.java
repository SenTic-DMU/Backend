package com.project.sentic.domain.user.service;

import com.project.sentic.domain.user.dto.*;
import com.project.sentic.domain.user.entity.User;
import com.project.sentic.domain.user.repository.UserRepository;
import com.project.sentic.global.auth.JwtTokenProvider;
import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import com.project.sentic.global.infra.email.EmailService;
import com.project.sentic.global.infra.email.VerificationCodeStore;
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

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 일반 회원가입
     * 1. 이메일 중복 확인
     * 2. 비밀번호 암호화
     * 3. 유저 저장
     * 4. JWT 토큰 발급
     */
    @Transactional
    public TokenResponseDto signup(SignupRequestDto request) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 비밀번호 암호화 후 유저 저장
        User user = User.createLocalUser(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getNickname()
        );
        userRepository.save(user);

        // JWT 토큰 발급
        return generateToken(user);
    }

    /**
     * 일반 로그인
     * 1. 이메일로 유저 조회
     * 2. 비밀번호 확인
     * 3. 계정 활성화 확인
     * 4. JWT 토큰 발급
     */
    public TokenResponseDto login(LoginRequestDto request) {
        // 이메일로 유저 조회
        User user = userRepository.findByEmail(request.getEmail())
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
     * 닉네임으로 이메일(아이디) 찾기
     * 이메일 앞부분을 마스킹하여 반환
     */
    public FindEmailResponseDto findEmail(FindEmailRequestDto request) {
        User user = userRepository.findByNickname(request.getNickname())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return new FindEmailResponseDto(maskEmail(user.getEmail()));
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

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 1) {
            return local + "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}