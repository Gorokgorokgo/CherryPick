package com.cherrypick.app.domain.bid.entity;

import com.cherrypick.app.domain.common.entity.BaseEntity;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.bid.enums.BidStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bid extends BaseEntity {
    /*
     * 동시성 이슈 방지를 위한 데이터베이스 제약조건 필요:
     * CREATE UNIQUE INDEX idx_unique_auction_bid_amount
     * ON bids (auction_id, bid_amount)
     * WHERE bid_amount > 0;
     *
     * 이 제약조건은 같은 경매에서 같은 금액으로 중복 입찰을 원천 차단합니다.
     * bid_amount > 0 조건으로 자동입찰 설정(bid_amount = 0)은 제외됩니다.
     */

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

    @Builder.Default
    @Column(name = "is_auto_bid", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isAutoBid = false;

    @Column(name = "max_auto_bid_amount", precision = 10, scale = 0)
    private BigDecimal maxAutoBidAmount;

    @Builder.Default
    @Column(name = "auto_bid_percentage", columnDefinition = "INTEGER DEFAULT 5")
    private Integer autoBidPercentage = 5; // 기본 5% (2-50% 범위)

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BidStatus status = BidStatus.ACTIVE;

    @Column(name = "bid_time", nullable = false)
    private LocalDateTime bidTime;
}