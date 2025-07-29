package com.cherrypick.app.entity;

import com.cherrypick.app.entity.enums.PointTransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * 포인트 거래 내역 엔티티
 */
@Entity
@Table(name = "point_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private PointTransactionType transactionType;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "description", length = 200)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id")
    private Auction auction;

    @Column(name = "external_transaction_id", length = 100)
    private String externalTransactionId; // 외부 결제 시스템 거래 ID

    /**
     * 포인트 충전 거래 생성
     */
    public static PointTransaction createChargeTransaction(User user, BigDecimal amount, 
                                                           String externalTransactionId) {
        return PointTransaction.builder()
                .user(user)
                .transactionType(PointTransactionType.CHARGE)
                .amount(amount)
                .balanceAfter(user.getPointBalance().add(amount))
                .description("포인트 충전")
                .externalTransactionId(externalTransactionId)
                .build();
    }

    /**
     * 보증금 예치 거래 생성
     */
    public static PointTransaction createDepositLockTransaction(User user, BigDecimal amount, 
                                                                Auction auction) {
        return PointTransaction.builder()
                .user(user)
                .transactionType(PointTransactionType.DEPOSIT_LOCK)
                .amount(amount.negate()) // 차감이므로 음수
                .balanceAfter(user.getPointBalance().subtract(amount))
                .description("경매 등록 보증금 예치: " + auction.getTitle())
                .auction(auction)
                .build();
    }

    /**
     * 보증금 해제 거래 생성
     */
    public static PointTransaction createDepositUnlockTransaction(User user, BigDecimal amount, 
                                                                  Auction auction) {
        return PointTransaction.builder()
                .user(user)
                .transactionType(PointTransactionType.DEPOSIT_UNLOCK)
                .amount(amount)
                .balanceAfter(user.getPointBalance().add(amount))
                .description("경매 완료 보증금 반환: " + auction.getTitle())
                .auction(auction)
                .build();
    }

    /**
     * 낙찰 결제 거래 생성
     */
    public static PointTransaction createBidPaymentTransaction(User user, BigDecimal amount, 
                                                               Auction auction) {
        return PointTransaction.builder()
                .user(user)
                .transactionType(PointTransactionType.BID_PAYMENT)
                .amount(amount.negate()) // 차감이므로 음수
                .balanceAfter(user.getPointBalance().subtract(amount))
                .description("낙찰 결제: " + auction.getTitle())
                .auction(auction)
                .build();
    }

    /**
     * 수수료 차감 거래 생성
     */
    public static PointTransaction createFeeDeductionTransaction(User user, BigDecimal amount, 
                                                                 Auction auction) {
        return PointTransaction.builder()
                .user(user)
                .transactionType(PointTransactionType.FEE_DEDUCTION)
                .amount(amount.negate()) // 차감이므로 음수
                .balanceAfter(user.getPointBalance().subtract(amount))
                .description("거래 수수료: " + auction.getTitle())
                .auction(auction)
                .build();
    }

    /**
     * 거래가 수입인지 확인
     */
    public boolean isIncome() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 거래가 지출인지 확인
     */
    public boolean isExpense() {
        return this.amount.compareTo(BigDecimal.ZERO) < 0;
    }
}