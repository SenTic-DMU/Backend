package com.project.sentic.domain.room.entity;

import com.project.sentic.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Room extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    private RoomType roomType;

    @Column(name = "room_name", nullable = false, length = 100)
    private String roomName;

    @Column(columnDefinition = "TEXT")
    private String situation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(columnDefinition = "TEXT")
    private String characters; // JSON array of CharacterInfo

    @Column(name = "memory_bank", columnDefinition = "TEXT")
    private String memoryBank; // JSON

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


    // ── Enum ──────────────────────────────────────

    public enum RoomType {
        VOICE, CHAT
    }

    public enum Difficulty {
        BEGINNER, INTERMEDIATE, ADVANCED
    }


    // ── 수정 메서드 ──────────────────────────────────

    public void updateMemoryBank(String memoryBank) {
        this.memoryBank = memoryBank;
    }

    public void updateLastActiveAt() {
        this.lastActiveAt = LocalDateTime.now();
    }

    public void delete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
    }
}