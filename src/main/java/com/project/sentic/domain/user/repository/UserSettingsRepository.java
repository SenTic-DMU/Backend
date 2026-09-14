package com.project.sentic.domain.user.repository;

import com.project.sentic.domain.user.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    // userId로 설정 조회
    Optional<UserSettings> findByUserId(Long userId);

    // userId로 설정 존재 여부 확인
    boolean existsByUserId(Long userId);

    @Modifying
    @Query("UPDATE UserSettings s SET s.weeklyScore = 0")
    int resetAllWeeklyScores();

    // 점수 높은 순 정렬
    List<UserSettings> findAllByOrderByWeeklyScoreDesc();
}
