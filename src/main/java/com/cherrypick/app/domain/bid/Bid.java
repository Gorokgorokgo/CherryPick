package com.cherrypick.app.domain.bid;

import com.cherrypick.app.domain.common.BaseEntity;
import com.cherrypick.app.domain.auction.Auction;
import com.cherrypick.app.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false)
    private Long amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bidder_id", nullable = false)
    private User bidder;

    @Column(nullable = false)
    private Boolean isWinning = false;
}