package com.project.sentic.global.config;

import com.project.sentic.global.auth.JwtAuthenticationFilter;
import com.project.sentic.global.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 *
 * 어떤 API는 누구나 접근 가능하고
 * 어떤 API는 로그인한 사용자만 접근 가능한지 설정해요.
 *
 * JWT 기반 인증이라 세션을 사용하지 않아요 (STATELESS)
 *
 * 접근 권한:
 * - 누구나: 회원가입, 로그인, 소셜로그인, 헬스체크, Swagger
 * - 로그인 유저: 나머지 모든 API
 * - 관리자만: /api/admin/** 경로
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 비밀번호 암호화 방식 (BCrypt)
     * 회원가입 시 비밀번호 암호화에 사용
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 활성화 (CorsConfig 빈 사용)
                .cors(Customizer.withDefaults())

                // CSRF 비활성화 (REST API는 필요 없음)
                .csrf(AbstractHttpConfigurer::disable)

                // 폼 로그인 비활성화 (JWT 사용)
                .formLogin(AbstractHttpConfigurer::disable)

                // HTTP Basic 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)

                // 세션 사용 안 함 (JWT는 STATELESS)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // API 접근 권한 설정
                .authorizeHttpRequests(auth -> auth

                        // OPTIONS 요청 허용 (CORS preflight)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 누구나 접근 가능
                        .requestMatchers("/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // AI 선행 검증용 (검증 완료 후 제거)
                        .requestMatchers("/api/ai/test/**").permitAll()

                        // 누구나 접근 가능
                        .requestMatchers("/api/faq/**").permitAll()
                        .requestMatchers("/api/announcements/**").permitAll()
                        .requestMatchers("/api/quiz/**").permitAll()

                        // 관리자만 접근 가능
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 나머지는 로그인 필요
                        .anyRequest().authenticated()
                )

                // JWT 필터 추가 (UsernamePasswordAuthenticationFilter 앞에)
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
