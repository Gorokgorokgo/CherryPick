package com.cherrypick.app.domain.transaction;

import com.cherrypick.app.domain.auction.Auction;
import com.cherrypick.app.domain.common.BaseEntity;
import com.cherrypick.app.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(name = "final_price", nullable = false, precision = 10, scale = 0)
    private BigDecimal finalPrice;

    @Column(name = "commission_fee", nullable = false, precision = 10, scale = 0)
    private BigDecimal commissionFee;

    @Column(name = "seller_amount", nullable = false, precision = 10, scale = 0)
    private BigDecimal sellerAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "seller_confirmed", nullable = false)
    private Boolean sellerConfirmed = false;

    @Column(name = "buyer_confirmed", nullable = false)
    private Boolean buyerConfirmed = false;

    @Column(name = "seller_confirmed_at")
    private LocalDateTime sellerConfirmedAt;

    @Column(name = "buyer_confirmed_at")
    private LocalDateTime buyerConfirmedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}