package com.project.sentic.domain.user.repository;

import com.project.sentic.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 이메일로 유저 조회 (일반 로그인, 중복 체크)
    Optional<User> findByEmail(String email);

    // 소셜 로그인 유저 조회
    Optional<User> findByProviderAndProviderId(User.Provider provider, String providerId);

    // 이메일 중복 체크
    boolean existsByEmail(String email);

    // 로그인 아이디로 유저 조회
    Optional<User> findByLoginId(String loginId);

    // 로그인 아이디 중복 체크
    boolean existsByLoginId(String loginId);

    // 닉네임으로 유저 조회 (아이디 찾기)
    Optional<User> findByNickname(String nickname);

    // 닉네임 중복 체크
    boolean existsByNickname(String nickname);

    // 닉네임 중복 체크 - 본인 제외 (마이페이지 수정 시 사용)
    boolean existsByNicknameAndIdNot(String nickname, Long id);

    // 검색 메서드
    List<User> findByNicknameContainingOrEmailContaining(String nickname, String email);
}