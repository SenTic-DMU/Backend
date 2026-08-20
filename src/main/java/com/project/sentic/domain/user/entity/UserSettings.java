package com.project.sentic.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

    @Column(name = "mon_minutes", nullable = false)
    @Builder.Default
    private int monMinutes = 0;

    @Column(name = "tue_minutes", nullable = false)
    @Builder.Default
    private int tueMinutes = 0;

    @Column(name = "wed_minutes", nullable = false)
    @Builder.Default
    private int wedMinutes = 0;

    @Column(name = "thu_minutes", nullable = false)
    @Builder.Default
    private int thuMinutes = 0;

    @Column(name = "fri_minutes", nullable = false)
    @Builder.Default
    private int friMinutes = 0;

    @Column(name = "sat_minutes", nullable = false)
    @Builder.Default
    private int satMinutes = 0;

    @Column(name = "sun_minutes", nullable = false)
    @Builder.Default
    private int sunMinutes = 0;

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
        DayOfWeek today = LocalDate.now().getDayOfWeek();

        // 요일별 학습 시간 누적
        switch (today) {
            case MONDAY    -> this.monMinutes += (int) minutes;
            case TUESDAY   -> this.tueMinutes += (int) minutes;
            case WEDNESDAY -> this.wedMinutes += (int) minutes;
            case THURSDAY  -> this.thuMinutes += (int) minutes;
            case FRIDAY    -> this.friMinutes += (int) minutes;
            case SATURDAY  -> this.satMinutes += (int) minutes;
            case SUNDAY    -> this.sunMinutes += (int) minutes;
        }

        // 이번 주 총 학습 시간 업데이트
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