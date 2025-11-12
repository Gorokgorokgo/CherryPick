package com.cherrypick.app.domain.user.entity;

import com.cherrypick.app.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Pattern(regexp = "^010[0-9]{8}$|^$", message = "올바른 전화번호 형식이 아닙니다.")
    @Column(unique = true)
    private String phoneNumber; // OAuth 사용자는 null 또는 빈 문자열 가능

    @NotBlank
    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(unique = true, length = 100)
    private String email;

    @Column
    private String password;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
    private Long pointBalance = 0L;

    // 구매력 관련
    @Builder.Default
    @Column(name = "buyer_level", nullable = false, columnDefinition = "INTEGER DEFAULT 1")
    private Integer buyerLevel = 1;

    @Builder.Default
    @Column(name = "buyer_exp", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer buyerExp = 0;

    // 판매력 관련
    @Builder.Default
    @Column(name = "seller_level", nullable = false, columnDefinition = "INTEGER DEFAULT 1")
    private Integer sellerLevel = 1;

    @Builder.Default
    @Column(name = "seller_exp", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer sellerExp = 0;

    // 판매자 후기 통계
    @Builder.Default
    @Column(name = "seller_review_good", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer sellerReviewGood = 0;

    @Builder.Default
    @Column(name = "seller_review_normal", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer sellerReviewNormal = 0;

    @Builder.Default
    @Column(name = "seller_review_bad", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer sellerReviewBad = 0;

    // 구매자 후기 통계
    @Builder.Default
    @Column(name = "buyer_review_good", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer buyerReviewGood = 0;

    @Builder.Default
    @Column(name = "buyer_review_normal", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer buyerReviewNormal = 0;

    @Builder.Default
    @Column(name = "buyer_review_bad", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer buyerReviewBad = 0;

    // 프로필 이미지
    @Column(length = 500)
    private String profileImageUrl;

    // 추가 개인정보
    @Column(length = 50)
    private String realName;

    @Column
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(length = 200)
    private String address;

    @Column(length = 20)
    private String zipCode;

    @Column(length = 500)
    private String bio; // 자기소개

    // GPS 위치 정보
    @Column(name = "latitude")
    private Double latitude; // 위도

    @Column(name = "longitude")
    private Double longitude; // 경도

    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt; // 위치 업데이트 시각

    @Column(name = "verified_region", length = 100)
    private String verifiedRegion; // 인증된 행정동명 (예: "서울시 강남구 역삼1동")

    // 프로필 공개 설정
    @Builder.Default
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean isProfilePublic = true;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isRealNamePublic = false;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isBirthDatePublic = false;

    // 계정 상태
    @Builder.Default
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean enabled = true;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean emailVerified = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20) DEFAULT 'USER'")
    private Role role = Role.USER;

    // 역할 및 권한 메서드
    public boolean hasRole(String roleName) {
        return this.role != null && this.role.name().equals(roleName);
    }

    public boolean isEnabled() {
        return this.enabled != null && this.enabled;
    }

    public boolean isEmailVerified() {
        return this.emailVerified != null && this.emailVerified;
    }

    // 성별 enum
    public enum Gender {
        MALE("남성"),
        FEMALE("여성"),
        OTHER("기타");

        private final String description;

        Gender(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // 역할 enum
    public enum Role {
        USER("일반 사용자"),
        ADMIN("관리자"),
        SUPER_ADMIN("최고 관리자");

        private final String description;

        Role(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}