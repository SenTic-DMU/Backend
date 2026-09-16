package com.project.sentic.domain.report.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "daily_study_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DailyStudyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "study_date", nullable = false)
    private LocalDate studyDate;

    @Column(name = "study_minutes", nullable = false)
    @Builder.Default
    private int studyMinutes = 0;

    @Column(name = "voice_count", nullable = false)
    @Builder.Default
    private int voiceCount = 0;

    @Column(name = "chat_count", nullable = false)
    @Builder.Default
    private int chatCount = 0;

    public void addStudyMinutes(int minutes) {
        this.studyMinutes += minutes;
    }

    public void incrementVoiceCount() {
        this.voiceCount += 1;
    }

    public void incrementChatCount() {
        this.chatCount += 1;
    }
}