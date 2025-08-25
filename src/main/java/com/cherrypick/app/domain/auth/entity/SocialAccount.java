package com.cherrypick.app.domain.auth.entity;

import com.cherrypick.app.domain.common.entity.BaseEntity;
import com.cherrypick.app.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

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
}