package com.project.sentic.domain.user.entity;

import com.project.sentic.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String password; // 소셜 로그인은 NULL

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column
    private String profileImage; // S3 URL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider; // LOCAL, KAKAO, GOOGLE

    @Column
    private String providerId; // 소셜 로그인 고유 ID

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // USER, ADMIN

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status; // ACTIVE, INACTIVE, BANNED

    @Column(nullable = false)
    private boolean emailVerified; // 이메일 인증 여부 (SES)

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // 소프트 딜리트


    // ── Enum ──────────────────────────────────────

    public enum Provider {
        LOCAL, KAKAO, GOOGLE
    }

    public enum Role {
        USER, ADMIN
    }

    public enum Status {
        ACTIVE, INACTIVE, BANNED
    }


    // ── 수정 메서드 ──────────────────────────────────

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateStatus(Status status) {
        this.status = status;
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }

    public void deactivate() {
        this.status = Status.INACTIVE;
        this.deletedAt = LocalDateTime.now();
    }

    // ── 정적 생성 메서드 ──────────────────────────────────

    public static User createLocalUser(String loginId, String email, String encodedPassword, String nickname) {
        return User.builder()
                .loginId(loginId)
                .email(email)
                .password(encodedPassword)
                .nickname(nickname)
                .provider(Provider.LOCAL)
                .role(Role.USER)
                .status(Status.ACTIVE)
                .emailVerified(false)
                .build();
    }

    public static User createSocialUser(String loginId, String email, String nickname, Provider provider, String providerId) {
        return User.builder()
                .loginId(loginId)
                .email(email)
                .nickname(nickname)
                .provider(provider)
                .providerId(providerId)
                .role(Role.USER)
                .status(Status.ACTIVE)
                .emailVerified(false)
                .build();
    }
}