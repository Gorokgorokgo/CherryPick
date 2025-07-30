package com.cherrypick.app.domain.auction;

import com.cherrypick.app.domain.common.BaseEntity;
import com.cherrypick.app.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auctions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Auction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(name = "start_price", nullable = false, precision = 10, scale = 0)
    private BigDecimal startPrice;

    @Column(name = "current_price", nullable = false, precision = 10, scale = 0)
    private BigDecimal currentPrice;

    @Column(name = "hope_price", nullable = false, precision = 10, scale = 0)
    private BigDecimal hopePrice;

    @Column(name = "deposit_amount", nullable = false, precision = 10, scale = 0)
    private BigDecimal depositAmount;

    @Column(name = "auction_time_hours", nullable = false)
    private Integer auctionTimeHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "region_scope", nullable = false)
    private RegionScope regionScope;

    @Column(name = "region_code")
    private String regionCode;

    @Column(name = "region_name")
    private String regionName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Column(name = "bid_count", nullable = false)
    private Integer bidCount = 0;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;
}