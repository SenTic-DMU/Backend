package com.project.sentic.global.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 토큰 제공자
 *
 * JWT 토큰의 생성, 검증, 파싱을 담당해요.
 *
 * Access Token:  30분 유효 → API 호출할 때 사용
 * Refresh Token: 7일 유효  → Access Token 만료 시 재발급용
 *
 * 토큰 구조:
 * Header.Payload.Signature
 * - Payload에 userId, role 정보가 담겨있어요
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiry}") long accessTokenExpiry,
            @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiry
    ) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    /**
     * Access Token 생성
     * @param userId 사용자 ID
     * @param role 사용자 권한 (USER or ADMIN)
     */
    public String createAccessToken(Long userId, String role) {
        return createToken(userId, role, accessTokenExpiry);
    }

    /**
     * Refresh Token 생성
     * @param userId 사용자 ID
     */
    public String createRefreshToken(Long userId) {
        return createToken(userId, null, refreshTokenExpiry);
    }

    /**
     * 토큰 생성 공통 메서드
     */
    private String createToken(Long userId, String role, long expiry) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiry);

        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    /**
     * 토큰에서 userId 추출
     */
    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    /**
     * 토큰에서 role 추출
     */
    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    /**
     * 토큰 유효성 검증
     * @return true: 유효, false: 유효하지 않음
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("유효하지 않은 JWT 토큰: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 토큰에서 Claims(payload) 추출
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
