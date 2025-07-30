package com.cherrypick.app.domain.point;

import com.cherrypick.app.domain.auction.Auction;
import com.cherrypick.app.domain.bid.Bid;
import com.cherrypick.app.domain.common.BaseEntity;
import com.cherrypick.app.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "point_locks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointLock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id", nullable = false)
    private Bid bid;

    @Column(name = "locked_amount", nullable = false, precision = 10, scale = 0)
    private BigDecimal lockedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointLockStatus status = PointLockStatus.LOCKED;

    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;
}