package com.cherrypick.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 입찰 엔티티
 */
@Entity
@Table(name = "bids")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bid extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @NotNull
    @Positive
    @Column(name = "bid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal bidAmount;

    @NotNull
    @Column(name = "bid_time", nullable = false)
    private LocalDateTime bidTime;

    @Builder.Default
    @Column(name = "is_auto_bid", nullable = false)
    private Boolean isAutoBid = false;

    @Column(name = "max_auto_bid_amount", precision = 15, scale = 2)
    private BigDecimal maxAutoBidAmount;

    @Builder.Default
    @Column(name = "is_winning", nullable = false)
    private Boolean isWinning = false;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (this.bidTime == null) {
            this.bidTime = LocalDateTime.now();
        }
    }

    /**
     * 자동 입찰 설정
     */
    public void setAutoBid(BigDecimal maxAmount) {
        this.isAutoBid = true;
        this.maxAutoBidAmount = maxAmount;
    }

    /**
     * 자동 입찰 해제
     */
    public void disableAutoBid() {
        this.isAutoBid = false;
        this.maxAutoBidAmount = null;
    }

    /**
     * 낙찰 여부 설정
     */
    public void setAsWinning() {
        this.isWinning = true;
    }

    /**
     * 자동 입찰 가능 여부 확인
     */
    public boolean canAutoBid(BigDecimal targetAmount) {
        return this.isAutoBid && 
               this.maxAutoBidAmount != null && 
               this.maxAutoBidAmount.compareTo(targetAmount) >= 0;
    }

    /**
     * 입찰 시간이 경매 마감 직전인지 확인 (스나이핑 입찰)
     */
    public boolean isSnipingBid(LocalDateTime auctionEndTime) {
        if (auctionEndTime == null) {
            return false;
        }
        
        long minutesBeforeEnd = java.time.Duration.between(this.bidTime, auctionEndTime).toMinutes();
        return minutesBeforeEnd <= 10; // 마감 10분 전 이후 입찰을 스나이핑으로 간주
    }
}