package com.project.sentic.domain.user.repository;

import com.project.sentic.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 유저 조회 (일반 로그인, 중복 체크)
    Optional<User> findByEmail(String email);

    // 소셜 로그인 유저 조회
    Optional<User> findByProviderAndProviderId(User.Provider provider, String providerId);

    // 이메일 중복 체크
    boolean existsByEmail(String email);

    // 닉네임 중복 체크
    boolean existsByNickname(String nickname);
}