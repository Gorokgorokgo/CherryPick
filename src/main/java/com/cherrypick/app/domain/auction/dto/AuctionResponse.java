package com.cherrypick.app.domain.auction.dto;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.entity.AuctionImage;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
public class AuctionResponse {
    
    private Long id;
    private String title;
    private String description;
    private Category category;
    private BigDecimal startPrice;
    private BigDecimal currentPrice;
    private BigDecimal hopePrice;
    private BigDecimal reservePrice;
    private Integer auctionTimeHours;
    private RegionScope regionScope;
    private String regionCode;
    private String regionName;
    private AuctionStatus status;
    private Integer viewCount;
    private Integer bidCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime startAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime endAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;
    
    // 판매자 정보
    private Long sellerId;
    private String sellerNickname;
    private String sellerProfileImageUrl;
    private Integer sellerLevel;
    private Integer sellerReviewCount; // 총 후기 수 (good + normal + bad)
    
    // 이미지 목록
    private List<String> imageUrls;
    
    // 시간 관련 계산 필드
    private Long remainingTimeMs; // 남은 시간 (밀리초)
    private boolean isExpired;
    
    // 상품 메타정보
    private Integer productCondition; // 상품 상태 (1-10점)
    private String purchaseDate; // 구매일

    // 북마크 정보 (전역/사용자)
    private Long bookmarkCount; // 전체 찜 수
    private boolean isBookmarked; // 현재 사용자 기준 찜 여부

    // GPS 위치 정보
    private Double latitude; // 경매 위치 위도
    private Double longitude; // 경매 위치 경도
    private String preferredLocation; // 거래 희망 장소
    private Double distanceKm; // 사용자 위치로부터의 거리 (km) - GPS 검색 시에만 포함

    // 서버 시간 (클라이언트 시계 보정용)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime serverTime; // 응답 생성 시점의 서버 시간 (UTC)
    
    public static AuctionResponse from(Auction auction, List<AuctionImage> images) {
        AuctionResponse response = new AuctionResponse();
        
        response.setId(auction.getId());
        response.setTitle(auction.getTitle());
        response.setDescription(auction.getDescription());
        response.setCategory(auction.getCategory());
        response.setStartPrice(auction.getStartPrice());
        response.setCurrentPrice(auction.getCurrentPrice());
        response.setHopePrice(auction.getHopePrice());
        response.setReservePrice(auction.getReservePrice());
        response.setAuctionTimeHours(auction.getAuctionTimeHours());
        response.setRegionScope(auction.getRegionScope());
        response.setRegionCode(auction.getRegionCode());
        response.setRegionName(auction.getRegionName());
        response.setStatus(auction.getStatus());
        response.setViewCount(auction.getViewCount());
        response.setBidCount(auction.getBidCount());
        response.setStartAt(auction.getStartAt());
        response.setEndAt(auction.getEndAt());
        response.setCreatedAt(auction.getCreatedAt());
        
        // 판매자 정보
        response.setSellerId(auction.getSeller().getId());
        response.setSellerNickname(auction.getSeller().getNickname());
        response.setSellerProfileImageUrl(auction.getSeller().getProfileImageUrl());
        response.setSellerLevel(auction.getSeller().getSellerLevel());

        // 판매자 총 후기 수 계산
        int totalReviews = (auction.getSeller().getSellerReviewGood() != null ? auction.getSeller().getSellerReviewGood() : 0)
                + (auction.getSeller().getSellerReviewNormal() != null ? auction.getSeller().getSellerReviewNormal() : 0)
                + (auction.getSeller().getSellerReviewBad() != null ? auction.getSeller().getSellerReviewBad() : 0);
        response.setSellerReviewCount(totalReviews);
        
        // 이미지 URL 목록
        response.setImageUrls(images.stream()
                .map(AuctionImage::getImageUrl)
                .toList());
        
        // 상품 메타정보 (기존 데이터 호환성을 위한 기본값 처리)
        response.setProductCondition(auction.getProductCondition() != null ? auction.getProductCondition() : 7);
        response.setPurchaseDate(auction.getPurchaseDate() != null ? auction.getPurchaseDate() : "구매일 미상");
        
        // 시간 계산 (한국 시간대 기준)
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        if (auction.getEndAt().isAfter(now)) {
            response.setRemainingTimeMs(java.time.Duration.between(now, auction.getEndAt()).toMillis());
            response.setExpired(false);
        } else {
            response.setRemainingTimeMs(0L);
            response.setExpired(true);
        }
        
        // 북마크 필드는 컨트롤러/서비스에서 채워짐 (기본값)
        response.setBookmarkCount(null);
        response.setBookmarked(false);

        // GPS 위치 정보
        response.setLatitude(auction.getLatitude());
        response.setLongitude(auction.getLongitude());
        response.setPreferredLocation(auction.getPreferredLocation());
        // distanceKm는 서비스/컨트롤러에서 계산하여 설정 (기본값 null)
        response.setDistanceKm(null);

        // 서버 시간 설정 (클라이언트 시계 보정용)
        response.setServerTime(LocalDateTime.now(java.time.ZoneOffset.UTC));

        return response;
    }
}
