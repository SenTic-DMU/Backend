package com.project.sentic.domain.user.repository;

import com.project.sentic.domain.user.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    // userId로 설정 조회
    Optional<UserSettings> findByUserId(Long userId);

    // userId로 설정 존재 여부 확인
    boolean existsByUserId(Long userId);
}
