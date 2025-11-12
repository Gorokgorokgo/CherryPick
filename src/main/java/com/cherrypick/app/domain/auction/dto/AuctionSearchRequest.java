package com.cherrypick.app.domain.auction.dto;

import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AuctionSearchRequest {
    
    // 키워드 검색
    private String keyword; // 제목, 설명에서 검색
    
    // 카테고리 필터
    private Category category;
    
    // 지역 필터
    private RegionScope regionScope;
    private String regionCode;
    
    // 가격 범위 필터
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    
    // 상태 필터 (기본값: ACTIVE)
    private AuctionStatus status = AuctionStatus.ACTIVE;
    
    // 정렬 옵션
    private SortOption sortBy = SortOption.CREATED_DESC;
    
    // 마감 임박 필터 (시간 단위)
    private Integer endingSoonHours; // N시간 이내 마감 경매만
    
    // 최소 입찰 수 필터
    private Integer minBidCount;

    // GPS 위치 기반 검색 (사용자 현재 위치)
    private Double latitude;   // 사용자 위치 위도
    private Double longitude;  // 사용자 위치 경도
    private Double maxDistanceKm; // 최대 거리 (km), 기본값: null (거리 제한 없음)

    public enum SortOption {
        CREATED_DESC,      // 최신순 (기본값)
        CREATED_ASC,       // 오래된순
        PRICE_ASC,         // 낮은 가격순
        PRICE_DESC,        // 높은 가격순
        ENDING_SOON,       // 마감 임박순
        VIEW_COUNT_DESC,   // 조회수 높은순
        BID_COUNT_DESC,    // 입찰수 높은순
        DISTANCE_ASC       // 거리순 (가까운 순) - GPS 검색 시에만 사용
    }
    
    /**
     * 검색 조건이 비어있는지 확인
     */
    public boolean isEmpty() {
        return keyword == null
            && category == null
            && regionScope == null
            && regionCode == null
            && minPrice == null
            && maxPrice == null
            && endingSoonHours == null
            && minBidCount == null
            && latitude == null
            && longitude == null
            && maxDistanceKm == null
            && status == AuctionStatus.ACTIVE; // 기본 상태만 있는 경우 비어있다고 간주
    }

    /**
     * GPS 위치 기반 검색인지 확인
     */
    public boolean isLocationBasedSearch() {
        return latitude != null && longitude != null;
    }
    
    /**
     * 가격 범위 검증
     */
    public void validatePriceRange() {
        if (minPrice != null && maxPrice != null) {
            if (minPrice.compareTo(maxPrice) > 0) {
                throw new IllegalArgumentException("최소 가격은 최대 가격보다 작아야 합니다.");
            }
        }
    }
}