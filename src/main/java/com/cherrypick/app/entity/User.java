package com.cherrypick.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;

/**
 * 사용자 엔티티
 */
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
    @Pattern(regexp = "^01[0-9]-[0-9]{4}-[0-9]{4}$", 
             message = "휴대폰 번호 형식이 올바르지 않습니다.")
    @Column(name = "phone_number", unique = true, nullable = false, length = 13)
    private String phoneNumber;

    @NotBlank
    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Builder.Default
    @Column(name = "buyer_level", nullable = false)
    private Integer buyerLevel = 1;

    @Builder.Default
    @Column(name = "seller_level", nullable = false)
    private Integer sellerLevel = 1;

    @Builder.Default
    @Column(name = "buyer_exp", nullable = false)
    private Integer buyerExp = 0;

    @Builder.Default
    @Column(name = "seller_exp", nullable = false)
    private Integer sellerExp = 0;

    @Builder.Default
    @Column(name = "point_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal pointBalance = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;

    /**
     * 신뢰도 계산 (구매자 레벨과 판매자 레벨 평균)
     */
    public Double getTrustScore() {
        return (buyerLevel + sellerLevel) / 2.0 * 10;
    }

    /**
     * 포인트 충전
     */
    public void chargePoints(BigDecimal amount) {
        this.pointBalance = this.pointBalance.add(amount);
    }

    /**
     * 포인트 차감
     */
    public boolean deductPoints(BigDecimal amount) {
        if (this.pointBalance.compareTo(amount) >= 0) {
            this.pointBalance = this.pointBalance.subtract(amount);
            return true;
        }
        return false;
    }

    /**
     * 구매자 경험치 추가 및 레벨업 처리
     */
    public void addBuyerExp(Integer exp) {
        this.buyerExp += exp;
        updateBuyerLevel();
    }

    /**
     * 판매자 경험치 추가 및 레벨업 처리
     */
    public void addSellerExp(Integer exp) {
        this.sellerExp += exp;
        updateSellerLevel();
    }

    private void updateBuyerLevel() {
        int newLevel = calculateLevel(this.buyerExp);
        if (newLevel > this.buyerLevel) {
            this.buyerLevel = newLevel;
        }
    }

    private void updateSellerLevel() {
        int newLevel = calculateLevel(this.sellerExp);
        if (newLevel > this.sellerLevel) {
            this.sellerLevel = newLevel;
        }
    }

    private int calculateLevel(int exp) {
        // 레벨 계산 로직: 100 * 레벨^2 경험치 필요
        return (int) Math.floor(Math.sqrt(exp / 100.0)) + 1;
    }
}