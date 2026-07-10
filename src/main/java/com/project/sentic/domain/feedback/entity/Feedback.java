package com.project.sentic.domain.feedback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "word_errors", columnDefinition = "JSON")
    private String wordErrors;

    @Column(name = "grammar_errors", columnDefinition = "JSON")
    private String grammarErrors;

    @Column(name = "expression_errors", columnDefinition = "JSON")
    private String expressionErrors;

    @Column(name = "perfect_sentence", columnDefinition = "TEXT")
    private String perfectSentence;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}