package com.cherrypick.app.entity;

import com.cherrypick.app.entity.enums.AuctionStatus;
import com.cherrypick.app.entity.enums.Category;
import com.cherrypick.app.entity.enums.RegionScope;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 경매 엔티티
 */
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

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @NotBlank
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category;

    @ElementCollection
    @CollectionTable(name = "auction_images", joinColumns = @JoinColumn(name = "auction_id"))
    @Column(name = "image_url", length = 500)
    @Builder.Default
    private List<String> images = new ArrayList<>();

    @NotNull
    @Positive
    @Column(name = "start_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal startPrice;

    @NotNull
    @Column(name = "current_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentPrice;

    @NotNull
    @Positive
    @Column(name = "hope_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal hopePrice;

    @NotNull
    @Positive
    @Column(name = "auction_time", nullable = false)
    private Integer auctionTime; // 경매 진행 시간 (시간 단위)

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "region_scope", nullable = false)
    @Builder.Default
    private RegionScope regionScope = RegionScope.DISTRICT;

    @NotBlank
    @Column(name = "region", nullable = false, length = 100)
    private String region;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AuctionStatus status = AuctionStatus.PENDING;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "bid_count", nullable = false)
    @Builder.Default
    private Integer bidCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_bidder_id")
    private User winningBidder;

    @Column(name = "deposit_amount", precision = 15, scale = 2)
    private BigDecimal depositAmount;

    /**
     * 경매 시작
     */
    public void startAuction() {
        this.status = AuctionStatus.ACTIVE;
        this.startAt = LocalDateTime.now();
        this.endAt = this.startAt.plusHours(this.auctionTime);
        this.currentPrice = this.startPrice;
        
        // 보증금 계산 (희망가의 10%)
        this.depositAmount = this.hopePrice.multiply(BigDecimal.valueOf(0.1));
    }

    /**
     * 경매 종료
     */
    public void endAuction() {
        this.status = AuctionStatus.COMPLETED;
    }

    /**
     * 경매 취소
     */
    public void cancelAuction() {
        this.status = AuctionStatus.CANCELLED;
    }

    /**
     * 입찰가 업데이트
     */
    public void updateCurrentPrice(BigDecimal newPrice, User bidder) {
        this.currentPrice = newPrice;
        this.bidCount++;
        // 낙찰자는 경매 종료 시에만 확정
    }

    /**
     * 낙찰자 설정
     */
    public void setWinningBidder(User winningBidder) {
        this.winningBidder = winningBidder;
        this.status = AuctionStatus.COMPLETED;
    }

    /**
     * 경매 진행 중 여부 확인
     */
    public boolean isActive() {
        return this.status == AuctionStatus.ACTIVE && 
               LocalDateTime.now().isBefore(this.endAt);
    }

    /**
     * 경매 종료 여부 확인
     */
    public boolean isExpired() {
        return this.endAt != null && LocalDateTime.now().isAfter(this.endAt);
    }

    /**
     * 지역 범위 확장
     */
    public void expandRegionScope() {
        switch (this.regionScope) {
            case DISTRICT -> this.regionScope = RegionScope.CITY;
            case CITY -> this.regionScope = RegionScope.NATIONAL;
            case NATIONAL -> {
                // 이미 최대 범위
            }
        }
    }

    /**
     * 남은 시간 계산 (분 단위)
     */
    public Long getRemainingMinutes() {
        if (this.endAt == null) {
            return null;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(this.endAt)) {
            return 0L;
        }
        
        return java.time.Duration.between(now, this.endAt).toMinutes();
    }
}