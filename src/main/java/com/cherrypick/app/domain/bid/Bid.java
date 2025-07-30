package com.cherrypick.app.domain.bid;

import com.cherrypick.app.domain.common.BaseEntity;
import com.cherrypick.app.domain.auction.Auction;
import com.cherrypick.app.domain.user.User;
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
    private Boolean isAutoBid = false;

    @Column(name = "max_auto_bid_amount", precision = 10, scale = 0)
    private BigDecimal maxAutoBidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BidStatus status = BidStatus.ACTIVE;

    @Column(name = "bid_time", nullable = false)
    private LocalDateTime bidTime;
}