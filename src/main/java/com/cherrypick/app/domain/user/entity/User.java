package com.cherrypick.app.domain.user.entity;

import com.cherrypick.app.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;

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

    @NotBlank
    @Pattern(regexp = "^010[0-9]{8}$", message = "올바른 전화번호 형식이 아닙니다.")
    @Column(unique = true, nullable = false)
    private String phoneNumber;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String nickname;

    @NotBlank
    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @NotBlank
    @Column(nullable = false)
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
}