package com.project.sentic.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_mode", nullable = false)
    @Builder.Default
    private DefaultMode defaultMode = DefaultMode.CHAT;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_difficulty", nullable = false)
    @Builder.Default
    private DefaultDifficulty defaultDifficulty = DefaultDifficulty.BEGINNER;

    @Column(name = "subtitle_enabled", nullable = false)
    @Builder.Default
    private boolean subtitleEnabled = true;

    @Column(name = "notification_enabled", nullable = false)
    @Builder.Default
    private boolean notificationEnabled = true;

    @Column(name = "weekly_study_time", nullable = false)
    @Builder.Default
    private int weeklyStudyTime = 0; // 분 단위

    @Column(name = "daily_avg_time", nullable = false)
    @Builder.Default
    private int dailyAvgTime = 0; // 분 단위

    @Column(name = "streak_days", nullable = false)
    @Builder.Default
    private int streakDays = 0;

    @Column(name = "last_studied_at")
    private LocalDateTime lastStudiedAt;

    @Column(name = "session_started_at")
    private LocalDateTime sessionStartedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    // ── Enum ──────────────────────────────────────

    public enum DefaultMode {
        VOICE, CHAT
    }

    public enum DefaultDifficulty {
        BEGINNER, INTERMEDIATE, ADVANCED
    }


    // ── 수정 메서드 ──────────────────────────────────

    public void updateNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    public void updateDefaultMode(DefaultMode defaultMode) {
        this.defaultMode = defaultMode;
    }

    public void updateDefaultDifficulty(DefaultDifficulty defaultDifficulty) {
        this.defaultDifficulty = defaultDifficulty;
    }

    public void startSession() {
        this.sessionStartedAt = LocalDateTime.now();
    }

    public void endSession() {
        if (sessionStartedAt == null) return;

        long minutes = java.time.Duration.between(sessionStartedAt, LocalDateTime.now()).toMinutes();

        // 이번 주 학습 시간 업데이트
        this.weeklyStudyTime += (int) minutes;

        // 연속 학습일 업데이트
        LocalDateTime now = LocalDateTime.now();
        if (lastStudiedAt == null || lastStudiedAt.toLocalDate().isBefore(now.toLocalDate().minusDays(1))) {
            this.streakDays = 1;
        } else if (lastStudiedAt.toLocalDate().isBefore(now.toLocalDate())) {
            this.streakDays += 1;
        }

        this.lastStudiedAt = now;
        this.sessionStartedAt = null;
        this.updatedAt = now;
    }

    @PrePersist
    public void prePersist() {
        this.updatedAt = LocalDateTime.now();
    }
}