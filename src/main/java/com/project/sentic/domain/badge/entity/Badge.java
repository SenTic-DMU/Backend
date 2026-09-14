package com.project.sentic.domain.badge.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "badges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "badge_id")
    private Long id;

    @Column(name = "badge_code", nullable = false, unique = true, length = 50)
    private String badgeCode;

    @Column(name = "badge_name", nullable = false, length = 50)
    private String badgeName;

    @Column(name = "badge_icon", nullable = false, length = 10)
    private String badgeIcon;

    @Column(nullable = false)
    private String description;

    @Column(name = "condition_text", nullable = false)
    private String conditionText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    public enum Difficulty {
        EASY, MEDIUM, HARD, SPECIAL
    }
}