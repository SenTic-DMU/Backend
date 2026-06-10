package com.project.sentic.domain.room.entity;

import com.project.sentic.domain.user.entity.User;
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
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title; // 대화방 제목 (예: 카페에서 주문하기)

    @Column(columnDefinition = "TEXT")
    private String description; // 상황 설명

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomType roomType; // VOICE, CHAT

    @Column(length = 255)
    private String lastMessage; // 목록에서 보이는 마지막 대화 미리보기

    @Column
    private LocalDateTime lastMessageAt; // 마지막 대화 시간

    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false; // 소프트 딜리트


    // ── Enum ──────────────────────────────────────

    public enum RoomType {
        VOICE, CHAT
    }


    // ── 수정 메서드 ──────────────────────────────────

    public void updateLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
        this.lastMessageAt = LocalDateTime.now();
    }

    public void delete() {
        this.deleted = true;
    }
}