package com.cherrypick.app.domain.auction.entity;

import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.common.entity.BaseEntity;
import com.cherrypick.app.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auctions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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

    // 캐시성 데이터 - DB 기본값으로 처리
    @Column(name = "view_count", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer viewCount;

    @Column(name = "bid_count", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer bidCount;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    // === 정적 팩토리 메서드 ===
    
    /**
     * 새로운 경매 생성
     */
    public static Auction createAuction(
            User seller, 
            String title, 
            String description,
            Category category,
            BigDecimal startPrice,
            BigDecimal hopePrice,
            Integer auctionTimeHours,
            RegionScope regionScope,
            String regionCode,
            String regionName) {
        
        LocalDateTime now = LocalDateTime.now();
        
        return new Auction(
            null, // id는 DB에서 생성
            seller,
            title,
            description,
            category,
            startPrice,
            startPrice, // currentPrice는 startPrice로 시작
            hopePrice,
            hopePrice.multiply(BigDecimal.valueOf(0.1)), // 보증금은 희망가의 10%
            auctionTimeHours,
            regionScope,
            regionCode,
            regionName,
            AuctionStatus.ACTIVE,
            0, // viewCount - DB 기본값 
            0, // bidCount - DB 기본값
            now,
            now.plusHours(auctionTimeHours),
            null // winner는 null로 시작
        );
    }

    // === 비즈니스 메서드 ===
    
    /**
     * 조회수 증가
     */
    public void increaseViewCount() {
        this.viewCount = (this.viewCount == null ? 0 : this.viewCount) + 1;
    }
    
    /**
     * 입찰수 증가  
     */
    public void increaseBidCount() {
        this.bidCount = (this.bidCount == null ? 0 : this.bidCount) + 1;
    }
    
    /**
     * 현재 가격 업데이트
     */
    public void updateCurrentPrice(BigDecimal newPrice) {
        if (newPrice.compareTo(this.currentPrice) > 0) {
            this.currentPrice = newPrice;
        }
    }
    
    /**
     * 경매 종료 처리
     */
    public void endAuction(User winner) {
        this.status = AuctionStatus.ENDED;
        this.winner = winner;
    }
    
    /**
     * 경매가 진행중인지 확인
     */
    public boolean isActive() {
        return this.status == AuctionStatus.ACTIVE && LocalDateTime.now().isBefore(this.endAt);
    }
    
    /**
     * 경매가 종료되었는지 확인  
     */
    public boolean isEnded() {
        return this.status == AuctionStatus.ENDED || LocalDateTime.now().isAfter(this.endAt);
    }
}