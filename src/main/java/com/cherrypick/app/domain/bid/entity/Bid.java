package com.cherrypick.app.domain.bid.entity;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.bid.enums.BidStatus;
import com.cherrypick.app.domain.common.entity.BaseEntity;
import com.cherrypick.app.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
public class Bid extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @Column(name = "bid_amount", nullable = false, precision = 10, scale = 0)
    private BigDecimal bidAmount;

    @Column(name = "is_auto_bid", nullable = false)
    private Boolean isAutoBid;

    @Column(name = "max_auto_bid_amount", precision = 10, scale = 0)
    private BigDecimal maxAutoBidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BidStatus status;

    @Column(name = "bid_time", nullable = false)
    private LocalDateTime bidTime;

    // === 정적 팩토리 메서드 ===

    /**
     * 수동 입찰 생성
     */
    public static Bid createManualBid(Auction auction, User bidder, BigDecimal bidAmount) {
        return Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .bidAmount(bidAmount)
                .isAutoBid(false)
                .maxAutoBidAmount(null)
                .status(BidStatus.ACTIVE)
                .bidTime(LocalDateTime.now())
                .build();
    }

    /**
     * 자동 입찰 설정 레코드 생성 (bidAmount = 0)
     */
    public static Bid createAutoBidSetting(Auction auction, User bidder, BigDecimal maxAutoBidAmount) {
        return Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .bidAmount(BigDecimal.ZERO)
                .isAutoBid(true)
                .maxAutoBidAmount(maxAutoBidAmount)
                .status(BidStatus.ACTIVE)
                .bidTime(LocalDateTime.now())
                .build();
    }

    /**
     * 자동 입찰 실행 레코드 생성
     */
    public static Bid createAutoBidExecution(Auction auction, User bidder, BigDecimal bidAmount, BigDecimal maxAutoBidAmount) {
        return Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .bidAmount(bidAmount)
                .isAutoBid(true)
                .maxAutoBidAmount(maxAutoBidAmount)
                .status(BidStatus.ACTIVE)
                .bidTime(LocalDateTime.now())
                .build();
    }

    // === 비즈니스 메서드 ===

    /**
     * 입찰 취소
     */
    public void cancel() {
        this.status = BidStatus.CANCELLED;
    }

    /**
     * 다른 입찰에 밀림
     */
    public void markAsOutbid() {
        this.status = BidStatus.OUTBID;
    }

    /**
     * 최고 입찰인지 확인
     */
    public boolean isHighestBid(BigDecimal currentHighestBid) {
        return this.bidAmount.compareTo(currentHighestBid) >= 0;
    }
}