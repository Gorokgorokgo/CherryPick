package com.cherrypick.app.domain.auction;

import com.cherrypick.app.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auction_extensions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionExtension extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_scope", nullable = false)
    private RegionScope fromScope;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_scope", nullable = false)
    private RegionScope toScope;

    @Column(name = "extended_at", nullable = false)
    private LocalDateTime extendedAt;
}