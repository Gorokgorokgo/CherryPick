package com.cherrypick.app.entity;

import com.cherrypick.app.entity.enums.TransactionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 거래 엔티티
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @NotNull
    @Column(name = "final_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal finalPrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "seller_fee", precision = 15, scale = 2)
    private BigDecimal sellerFee;

    @Column(name = "buyer_fee", precision = 15, scale = 2)
    private BigDecimal buyerFee;

    @Builder.Default
    @Column(name = "seller_confirmed", nullable = false)
    private Boolean sellerConfirmed = false;

    @Builder.Default
    @Column(name = "buyer_confirmed", nullable = false)
    private Boolean buyerConfirmed = false;

    @Column(name = "seller_confirmed_at")
    private LocalDateTime sellerConfirmedAt;

    @Column(name = "buyer_confirmed_at")
    private LocalDateTime buyerConfirmedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "meeting_location", length = 200)
    private String meetingLocation;

    @Column(name = "meeting_time")
    private LocalDateTime meetingTime;

    /**
     * 판매자 거래 확인
     */
    public void confirmBySeller() {
        this.sellerConfirmed = true;
        this.sellerConfirmedAt = LocalDateTime.now();
        checkAndCompleteTransaction();
    }

    /**
     * 구매자 거래 확인
     */
    public void confirmByBuyer() {
        this.buyerConfirmed = true;
        this.buyerConfirmedAt = LocalDateTime.now();
        checkAndCompleteTransaction();
    }

    /**
     * 양측 확인 시 거래 완료 처리
     */
    private void checkAndCompleteTransaction() {
        if (this.sellerConfirmed && this.buyerConfirmed) {
            this.status = TransactionStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
        }
    }

    /**
     * 거래 취소
     */
    public void cancel() {
        this.status = TransactionStatus.CANCELLED;
    }

    /**
     * 분쟁 상태로 변경
     */
    public void setDispute() {
        this.status = TransactionStatus.DISPUTE;
    }

    /**
     * 수수료 계산 및 설정
     */
    public void calculateFees() {
        // 판매자 수수료: 거래액의 5%
        this.sellerFee = this.finalPrice.multiply(BigDecimal.valueOf(0.05));
        
        // 구매자 수수료: 거래액의 2%
        this.buyerFee = this.finalPrice.multiply(BigDecimal.valueOf(0.02));
    }

    /**
     * 거래 진행 상태로 변경
     */
    public void startTransaction() {
        this.status = TransactionStatus.IN_PROGRESS;
        calculateFees();
    }

    /**
     * 거래 완료 여부 확인
     */
    public boolean isCompleted() {
        return this.status == TransactionStatus.COMPLETED;
    }

    /**
     * 미팅 정보 설정
     */
    public void setMeetingInfo(String location, LocalDateTime time) {
        this.meetingLocation = location;
        this.meetingTime = time;
    }
}