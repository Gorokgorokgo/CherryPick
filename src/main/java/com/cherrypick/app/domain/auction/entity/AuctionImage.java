package com.cherrypick.app.domain.auction.entity;

import com.cherrypick.app.domain.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "auction_images")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}