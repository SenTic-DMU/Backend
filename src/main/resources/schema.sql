-- =====================================================
-- SenTic DB 최종 DDL
-- MySQL 8.x
-- =====================================================
-- 실행 전:
-- CREATE DATABASE sentic CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE sentic;
-- =====================================================

-- --------------------------------------------------------
-- 1. 회원 정보
-- --------------------------------------------------------
CREATE TABLE `users` (
    `user_id`           BIGINT          NOT NULL AUTO_INCREMENT,
    `email`             VARCHAR(100)    NULL,
    `password_hash`     VARCHAR(255)    NULL,
    `nickname`          VARCHAR(50)     NOT NULL,
    `profile_image_url` VARCHAR(500)    NULL,
    `provider`          ENUM('LOCAL','KAKAO','GOOGLE') NOT NULL DEFAULT 'LOCAL',
    `provider_id`       VARCHAR(255)    NULL,
    `role`              ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
    `is_active`         BOOLEAN         NOT NULL DEFAULT TRUE,
    `created_at`        DATETIME        NOT NULL DEFAULT NOW(),
    `updated_at`        DATETIME        NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uq_email` (`email`),
    UNIQUE KEY `uq_provider` (`provider`, `provider_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 2. 사용자 설정 (users와 1:1)
-- --------------------------------------------------------
CREATE TABLE `user_settings` (
    `setting_id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `user_id`               BIGINT      NOT NULL,
    `default_mode`          ENUM('VOICE','CHAT')                        NOT NULL DEFAULT 'CHAT',
    `default_difficulty`    ENUM('BEGINNER','INTERMEDIATE','ADVANCED')  NOT NULL DEFAULT 'BEGINNER',
    `subtitle_enabled`      BOOLEAN     NOT NULL DEFAULT TRUE,
    `notification_enabled`  BOOLEAN     NOT NULL DEFAULT TRUE,
    `updated_at`            DATETIME    NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (`setting_id`),
    UNIQUE KEY `uq_user_settings` (`user_id`),
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 3. 이용권 / 플랜
-- --------------------------------------------------------
CREATE TABLE `subscription_plans` (
    `plan_id`       INT             NOT NULL AUTO_INCREMENT,
    `plan_name`     VARCHAR(50)     NOT NULL,
    `price`         INT             NOT NULL,
    `duration_days` INT             NOT NULL,
    `room_limit`    INT             NULL,
    `is_active`     BOOLEAN         NOT NULL DEFAULT TRUE,
    `created_at`    DATETIME        NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 기본 플랜 데이터
INSERT INTO `subscription_plans` (`plan_name`, `price`, `duration_days`, `room_limit`)
VALUES
    ('무료', 0, 36500, 3),
    ('기본', 4900, 30, 20),
    ('프리미엄', 9900, 30, NULL);

-- --------------------------------------------------------
-- 4. 결제 정보
-- --------------------------------------------------------
CREATE TABLE `payments` (
    `payment_id`            BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`               BIGINT          NOT NULL,
    `plan_id`               INT             NOT NULL,
    `amount`                INT             NOT NULL,
    `status`                ENUM('PENDING','SUCCESS','FAILED','CANCELLED') NOT NULL,
    `payment_method`        VARCHAR(50)     NULL,
    `external_payment_id`   VARCHAR(200)    NULL,
    `paid_at`               DATETIME        NULL,
    `expires_at`            DATETIME        NULL,
    `created_at`            DATETIME        NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`payment_id`),
    INDEX `idx_payments_user_status` (`user_id`, `status`),
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`plan_id`) REFERENCES `subscription_plans`(`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 5. 대화방
-- --------------------------------------------------------
CREATE TABLE `rooms` (
    `room_id`           BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`           BIGINT          NOT NULL,
    `room_type`         ENUM('VOICE','CHAT')                        NOT NULL,
    `room_name`         VARCHAR(100)    NULL,
    `character_name`    VARCHAR(50)     NULL,
    `participant_count` INT             NULL DEFAULT 1,
    `situation`         TEXT            NULL,
    `difficulty`        ENUM('BEGINNER','INTERMEDIATE','ADVANCED')  NOT NULL,
    `is_random`         BOOLEAN         NOT NULL DEFAULT FALSE,
    `last_active_at`    DATETIME        NULL,
    `is_deleted`        BOOLEAN         NOT NULL DEFAULT FALSE,
    `created_at`        DATETIME        NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`room_id`),
    INDEX `idx_rooms_user_active` (`user_id`, `is_deleted`, `last_active_at`),
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 6. 대화 메시지
-- --------------------------------------------------------
CREATE TABLE `messages` (
    `message_id`    BIGINT          NOT NULL AUTO_INCREMENT,
    `room_id`       BIGINT          NOT NULL,
    `sender_type`   ENUM('USER','AI') NOT NULL,
    `content_text`  TEXT            NULL,
    `audio_url`     VARCHAR(500)    NULL,
    `sequence_no`   INT             NOT NULL,
    `created_at`    DATETIME        NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`message_id`),
    INDEX `idx_messages_room_seq` (`room_id`, `sequence_no`),
    FOREIGN KEY (`room_id`) REFERENCES `rooms`(`room_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 7. 피드백
-- --------------------------------------------------------
CREATE TABLE `feedbacks` (
    `feedback_id`           BIGINT  NOT NULL AUTO_INCREMENT,
    `room_id`               BIGINT  NOT NULL,
    `message_id`            BIGINT  NULL,
    `grammar_errors`        JSON    NULL,
    `natural_expressions`   JSON    NULL,
    `slang_expressions`     JSON    NULL,
    `overall_comment`       TEXT    NULL,
    `created_at`            DATETIME NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`feedback_id`),
    INDEX `idx_feedbacks_room` (`room_id`),
    FOREIGN KEY (`room_id`) REFERENCES `rooms`(`room_id`) ON DELETE CASCADE,
    FOREIGN KEY (`message_id`) REFERENCES `messages`(`message_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 8. 스크랩 표현
-- --------------------------------------------------------
CREATE TABLE `scraps` (
    `scrap_id`      BIGINT          NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT          NOT NULL,
    `feedback_id`   BIGINT          NULL,
    `expression`    VARCHAR(500)    NOT NULL,
    `context`       TEXT            NULL,
    `category`      ENUM('단어','문법','문장') NULL,
    `user_note`     TEXT            NULL,
    `created_at`    DATETIME        NOT NULL DEFAULT NOW(),
    PRIMARY KEY (`scrap_id`),
    INDEX `idx_scraps_user_created` (`user_id`, `created_at`),
    INDEX `idx_scraps_user_category` (`user_id`, `category`),
    FOREIGN KEY (`user_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE,
    FOREIGN KEY (`feedback_id`) REFERENCES `feedbacks`(`feedback_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- 9. 공지사항 (관리자)
-- --------------------------------------------------------
CREATE TABLE `announcements` (
    `announcement_id`   BIGINT          NOT NULL AUTO_INCREMENT,
    `admin_id`          BIGINT          NOT NULL,
    `title`             VARCHAR(200)    NOT NULL,
    `content`           TEXT            NOT NULL,
    `is_pinned`         BOOLEAN         NOT NULL DEFAULT FALSE,
    `is_deleted`        BOOLEAN         NOT NULL DEFAULT FALSE,
    `created_at`        DATETIME        NOT NULL DEFAULT NOW(),
    `updated_at`        DATETIME        NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    PRIMARY KEY (`announcement_id`),
    INDEX `idx_announcements_pinned` (`is_pinned`, `is_deleted`),
    FOREIGN KEY (`admin_id`) REFERENCES `users`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
