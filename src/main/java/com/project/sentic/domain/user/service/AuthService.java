package com.project.sentic.domain.user.service;

import com.project.sentic.domain.user.dto.LoginRequestDto;
import com.project.sentic.domain.user.dto.SignupRequestDto;
import com.project.sentic.domain.user.dto.TokenResponseDto;
import com.project.sentic.domain.user.entity.User;
import com.project.sentic.domain.user.repository.UserRepository;
import com.project.sentic.global.auth.JwtTokenProvider;
import com.project.sentic.global.exception.CustomException;
import com.project.sentic.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}