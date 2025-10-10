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
@lombok.Builder(toBuilder = true)
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

    @Column(name = "hope_price", nullable = false, precision = 10, scale = 0)
    private BigDecimal hopePrice;

    @Column(name = "reserve_price", precision = 10, scale = 0)
    private BigDecimal reservePrice;

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

    @Column(name = "view_count", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer viewCount;

    @Column(name = "current_price", nullable = false, precision = 10, scale = 0)
    private BigDecimal currentPrice;

    @Column(name = "bid_count", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer bidCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "product_condition")
    private Integer productCondition;

    @Column(name = "purchase_date")
    private String purchaseDate;

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
            BigDecimal reservePrice,
            Integer auctionTimeHours,
            RegionScope regionScope,
            String regionCode,
            String regionName,
            Integer productCondition,
            String purchaseDate) {
        
        LocalDateTime now = LocalDateTime.now();
        
        return new Auction(
            null, // id는 DB에서 생성
            seller,
            title,
            description,
            category,
            startPrice,
            hopePrice,
            reservePrice,
            auctionTimeHours,
            regionScope,
            regionCode,
            regionName,
            AuctionStatus.ACTIVE,
            0, // viewCount - DB 기본값
            startPrice, // currentPrice - 시작가로 초기화
            0, // bidCount - DB 기본값
            null, // winner
            now,
            now.plusHours(auctionTimeHours),
            productCondition,
            purchaseDate
        );
    }

    // 하위호환 오버로드 (productCondition, purchaseDate 없이)
    public static Auction createAuction(
            User seller,
            String title,
            String description,
            Category category,
            BigDecimal startPrice,
            BigDecimal hopePrice,
            BigDecimal reservePrice,
            Integer auctionTimeHours,
            RegionScope regionScope,
            String regionCode,
            String regionName
    ) {
        return createAuction(
            seller, title, description, category,
            startPrice, hopePrice, reservePrice,
            auctionTimeHours, regionScope, regionCode, regionName,
            5, (String) null
        );
    }

    // 하위호환 오버로드 (마지막 인자를 LocalDate로 받음)
    public static Auction createAuction(
            User seller,
            String title,
            String description,
            Category category,
            BigDecimal startPrice,
            BigDecimal hopePrice,
            BigDecimal reservePrice,
            Integer auctionTimeHours,
            RegionScope regionScope,
            String regionCode,
            String regionName,
            Integer productCondition,
            java.time.LocalDate purchaseDate
    ) {
        return createAuction(
            seller, title, description, category,
            startPrice, hopePrice, reservePrice,
            auctionTimeHours, regionScope, regionCode, regionName,
            productCondition,
            purchaseDate != null ? purchaseDate.toString() : null
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
     * Reserve Price 달성 여부 확인
     */
    public boolean isReservePriceMet(BigDecimal currentBidPrice) {
        return reservePrice == null || currentBidPrice.compareTo(reservePrice) >= 0;
    }

    /**
     * Reserve Price 설정 여부 확인
     */
    public boolean hasReservePrice() {
        return reservePrice != null && reservePrice.compareTo(BigDecimal.ZERO) > 0;
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

    /**
     * 경매 강제 종료 (테스트용)
     */
    public void forceEnd() {
        this.status = AuctionStatus.ENDED;
        this.endAt = LocalDateTime.now();
    }

    /**
     * 현재가 업데이트
     */
    public void updateCurrentPrice(BigDecimal newPrice) {
        this.currentPrice = newPrice;
    }

    /**
     * 입찰 횟수 증가
     */
    public void increaseBidCount() {
        this.bidCount = (this.bidCount == null ? 0 : this.bidCount) + 1;
    }

    /**
     * 낙찰자 설정
     */
    public void setWinner(User winner, BigDecimal finalPrice) {
        this.winner = winner;
        this.currentPrice = finalPrice;
        this.status = AuctionStatus.ENDED;
    }

    /**
     * 경매 종료 처리
     */
    public void endAuction(User winner, BigDecimal finalPrice) {
        this.winner = winner;
        this.currentPrice = finalPrice;
        this.status = finalPrice.compareTo(BigDecimal.ZERO) > 0 ? AuctionStatus.ENDED : AuctionStatus.ENDED;
    }

    /**
     * 경매 종료 시간 조정 (개발/테스트용)
     * @param minutes 조정할 분 (양수: 시간 추가, 음수: 시간 감소)
     */
    public void adjustEndTime(int minutes) {
        this.endAt = this.endAt.plusMinutes(minutes);
    }

    /**
     * 종료된 경매를 재활성화 (개발/테스트용)
     * @param hours 재활성화 후 진행할 시간 (시간)
     */
    public void reactivateAuction(int hours) {
        this.status = AuctionStatus.ACTIVE;
        this.endAt = LocalDateTime.now().plusHours(hours);
    }
}