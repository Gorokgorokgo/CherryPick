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

    @Column(name = "last_price_before_end", precision = 10, scale = 0)
    private BigDecimal lastPriceBeforeEnd;

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

    // GPS 위치 정보
    @Column(name = "latitude")
    private Double latitude; // 경매 위치 위도

    @Column(name = "longitude")
    private Double longitude; // 경매 위치 경도

    @Column(name = "preferred_location", length = 200)
    private String preferredLocation; // 거래 희망 장소 (예: "강남역 1번 출구")

    @Column(name = "seller_verified_region_at_creation", length = 100)
    private String sellerVerifiedRegionAtCreation; // 경매 등록 당시 판매자의 GPS 인증 주소 (스냅샷)

    // === 정적 팩토리 메서드 ===
    
    /**
     * 새로운 경매 생성 (GPS 위치 정보 포함)
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
            String purchaseDate,
            Double latitude,
            Double longitude,
            String preferredLocation) {

        LocalDateTime now = LocalDateTime.now();

        return Auction.builder()
            .seller(seller)
            .title(title)
            .description(description)
            .category(category)
            .startPrice(startPrice)
            .hopePrice(hopePrice)
            .reservePrice(reservePrice)
            .auctionTimeHours(auctionTimeHours)
            .regionScope(regionScope)
            .regionCode(regionCode)
            .regionName(regionName)
            .status(AuctionStatus.ACTIVE)
            .viewCount(0)
            .currentPrice(startPrice)
            .bidCount(0)
            .startAt(now)
            .endAt(now.plusHours(auctionTimeHours))
            .productCondition(productCondition)
            .purchaseDate(purchaseDate)
            .latitude(latitude)
            .longitude(longitude)
            .preferredLocation(preferredLocation)
            .sellerVerifiedRegionAtCreation(seller.getVerifiedRegion()) // 경매 등록 당시 판매자 주소 스냅샷
            .build();
    }

    /**
     * 새로운 경매 생성 (GPS 위치 정보 없이 - 하위 호환)
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

        return createAuction(
            seller, title, description, category,
            startPrice, hopePrice, reservePrice,
            auctionTimeHours, regionScope, regionCode, regionName,
            productCondition, purchaseDate,
            null, null, null // GPS 위치 정보 없음
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
     * reservePrice가 null이거나 0 이하면 Reserve Price 없음으로 간주
     */
    public boolean isReservePriceMet(BigDecimal currentBidPrice) {
        // Reserve Price가 없으면 항상 충족
        if (!hasReservePrice()) {
            return true;
        }
        // Reserve Price가 있으면 비교
        return currentBidPrice.compareTo(reservePrice) >= 0;
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
        // 종료 전 현재가를 저장
        this.lastPriceBeforeEnd = this.currentPrice;
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
     * Reserve Price 확인 후 적절한 상태로 설정
     */
    public void setWinner(User winner, BigDecimal finalPrice) {
        // 종료 전 현재가를 저장
        this.lastPriceBeforeEnd = this.currentPrice;
        this.currentPrice = finalPrice;

        // Reserve Price 확인
        if (this.hasReservePrice() && !this.isReservePriceMet(finalPrice)) {
            // Reserve Price 미달 - 유찰
            this.status = AuctionStatus.NO_RESERVE_MET;
            this.winner = null; // 유찰 시 낙찰자 제거
        } else {
            // Reserve Price 충족 또는 Reserve Price 없음 - 낙찰
            this.winner = winner;
            this.status = AuctionStatus.ENDED;
        }
    }

    /**
     * 경매 종료 처리
     *
     * 비즈니스 로직:
     * - 낙찰(finalPrice > 0): currentPrice를 finalPrice로 업데이트
     * - 유찰(finalPrice = 0): currentPrice를 유지 (마지막 입찰가 보존)
     */
    public void endAuction(User winner, BigDecimal finalPrice) {
        // 종료 전 현재가를 저장
        this.lastPriceBeforeEnd = this.currentPrice;
        this.winner = winner;

        // 유찰인 경우 currentPrice를 유지 (0원으로 덮어쓰지 않음)
        if (finalPrice.compareTo(BigDecimal.ZERO) > 0) {
            this.currentPrice = finalPrice;
            this.status = AuctionStatus.ENDED;
        } else {
            // 유찰: currentPrice는 그대로 유지 (마지막 입찰가)
            this.status = AuctionStatus.NO_RESERVE_MET;
        }

        // 종료 시간을 현재 시간으로 설정
        this.endAt = LocalDateTime.now();
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
        // 종료 시점의 가격을 복구 (lastPriceBeforeEnd가 있으면 사용, 없으면 currentPrice 사용)
        BigDecimal priceToRestore = (this.lastPriceBeforeEnd != null && this.lastPriceBeforeEnd.compareTo(BigDecimal.ZERO) > 0)
            ? this.lastPriceBeforeEnd
            : this.currentPrice;

        this.startPrice = priceToRestore;
        this.currentPrice = priceToRestore;

        // 낙찰자 정보 초기화
        this.winner = null;

        // 상태 재활성화 및 종료 시간 연장
        this.status = AuctionStatus.ACTIVE;
        this.endAt = LocalDateTime.now().plusHours(hours);
    }

    /**
     * 경매 제목 수정
     */
    public void updateTitle(String newTitle) {
        if (newTitle == null || newTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("제목은 비어있을 수 없습니다.");
        }
        this.title = newTitle;
    }

    /**
     * 경매 설명 수정
     */
    public void updateDescription(String newDescription) {
        if (newDescription == null || newDescription.trim().isEmpty()) {
            throw new IllegalArgumentException("설명은 비어있을 수 없습니다.");
        }
        this.description = newDescription;
    }

    /**
     * 경매 삭제 (소프트 삭제)
     */
    public void markAsDeleted() {
        this.status = AuctionStatus.DELETED;
    }

    /**
     * 위치 정보 설정
     */
    public void setLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}