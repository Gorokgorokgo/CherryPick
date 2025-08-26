package com.cherrypick.app.domain.auth.entity;

import com.cherrypick.app.domain.common.entity.BaseEntity;
import com.cherrypick.app.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "social_accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(length = 100)
    private String email;

    @Column(length = 50)
    private String name;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    // 연동된 소셜 계정이 활성 상태인지 (사용자가 연동 해제 가능)
    @Builder.Default
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean isActive = true;

    // 보안 관련 필드
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder.Default
    @Column(name = "login_attempt_count", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer loginAttemptCount = 0;

    @Builder.Default
    @Column(name = "is_locked", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isLocked = false;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    // provider + providerId 조합이 유니크해야 함 (한 소셜 계정은 하나의 사용자에만 연동)
    @Table(name = "social_accounts", 
           uniqueConstraints = {
               @UniqueConstraint(name = "uk_provider_provider_id", 
                               columnNames = {"provider", "provider_id"})
           })
    public static class TableConstraints {}

    // 소셜 로그인 제공자
    public enum SocialProvider {
        GOOGLE("Google"),
        KAKAO("Kakao"),
        NAVER("Naver");

        private final String description;

        SocialProvider(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 보안 관련 메서드
    public void recordSuccessfulLogin(String ipAddress) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ipAddress;
        this.loginAttemptCount = 0;
        this.isLocked = false;
        this.lockedUntil = null;
    }

    public void recordFailedLogin() {
        this.loginAttemptCount = (this.loginAttemptCount == null ? 0 : this.loginAttemptCount) + 1;
        
        if (this.loginAttemptCount >= 5) {
            this.isLocked = true;
            this.lockedUntil = LocalDateTime.now().plusMinutes(30);
        }
    }

    public boolean isAccountLocked() {
        if (!Boolean.TRUE.equals(this.isLocked)) {
            return false;
        }
        
        if (this.lockedUntil != null && LocalDateTime.now().isAfter(this.lockedUntil)) {
            this.isLocked = false;
            this.lockedUntil = null;
            return false;
        }
        
        return true;
    }
}