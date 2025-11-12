package com.cherrypick.app.domain.auction.dto;

import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "경매 등록 요청")
@Getter
@Setter
public class CreateAuctionRequest {
    
    @Schema(description = "경매 제목", example = "아이폰 15 Pro 256GB 팝니다", required = true)
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다.")
    private String title;
    
    @Schema(description = "상품 설명", example = "사용 기간 6개월, 케이스 끼고 사용해서 깨끗합니다.", required = true)
    @NotBlank(message = "상품 설명은 필수입니다.")
    @Size(max = 2000, message = "상품 설명은 2000자를 넘을 수 없습니다.")
    private String description;
    
    @Schema(description = "상품 카테고리", example = "ELECTRONICS", required = true)
    @NotNull(message = "카테고리는 필수입니다.")
    private Category category;
    
    @Schema(description = "시작가 (100원 단위)", example = "100000", required = true)
    @NotNull(message = "시작가는 필수입니다.")
    @DecimalMin(value = "100", message = "시작가는 최소 100원입니다.")
    private BigDecimal startPrice;
    
    @Schema(description = "희망가 (100원 단위)", example = "800000", required = true)
    @NotNull(message = "희망가는 필수입니다.")
    @DecimalMin(value = "100", message = "희망가는 최소 100원입니다.")
    private BigDecimal hopePrice;
    
    @Schema(description = "최저 내정가 (Reserve Price) - 선택사항", example = "500000")
    private BigDecimal reservePrice;
    
    @Schema(description = "경매 진행 시간 (3, 6, 12, 24, 48, 72시간 중 선택)", example = "24", required = true)
    @NotNull(message = "경매 시간은 필수입니다.")
    @Min(value = 3, message = "경매 시간은 최소 3시간입니다.")
    @Max(value = 72, message = "경매 시간은 최대 72시간입니다.")
    private Integer auctionTimeHours;
    
    @Schema(description = "지역 범위", example = "NATIONWIDE", required = true)
    @NotNull(message = "지역 범위는 필수입니다.")
    private RegionScope regionScope;
    
    @Schema(description = "지역 코드", example = "11")
    private String regionCode;
    
    @Schema(description = "지역명", example = "서울특별시")
    private String regionName;
    
    @Schema(description = "상품 이미지 URL 목록", example = "[\"https://example.com/image1.jpg\", \"https://example.com/image2.jpg\"]", required = true)
    @NotEmpty(message = "상품 이미지는 최소 1개 이상 필요합니다.")
    @Size(max = 10, message = "상품 이미지는 최대 10개까지 가능합니다.")
    private List<String> imageUrls;
    
    @Schema(description = "상품 상태 (1-10점)", example = "8", required = true)
    @NotNull(message = "상품 상태는 필수입니다.")
    @Min(value = 1, message = "상품 상태는 1점 이상이어야 합니다.")
    @Max(value = 10, message = "상품 상태는 10점 이하여야 합니다.")
    private Integer productCondition;

    @Schema(description = "상품 구매일", example = "2023년 3월 구매", required = false)
    private String purchaseDate;

    // GPS 위치 정보 (선택사항)
    @Schema(description = "경매 위치 위도 (선택사항)", example = "37.5665")
    @Min(value = 33, message = "위도는 33도 이상이어야 합니다.")
    @Max(value = 43, message = "위도는 43도 이하여야 합니다.")
    private Double latitude;

    @Schema(description = "경매 위치 경도 (선택사항)", example = "126.9780")
    @Min(value = 124, message = "경도는 124도 이상이어야 합니다.")
    @Max(value = 132, message = "경도는 132도 이하여야 합니다.")
    private Double longitude;

    @Schema(description = "거래 희망 장소 (선택사항)", example = "강남역 1번 출구")
    @Size(max = 200, message = "거래 희망 장소는 200자를 넘을 수 없습니다.")
    private String preferredLocation;

    public void validate() {
        if (startPrice.compareTo(hopePrice) > 0) {
            throw new IllegalArgumentException("시작가는 희망가보다 클 수 없습니다.");
        }
        
        // Reserve Price 유효성 검증
        if (reservePrice != null) {
            if (reservePrice.compareTo(startPrice) < 0) {
                throw new IllegalArgumentException("최저 내정가는 시작가보다 작을 수 없습니다.");
            }
            if (reservePrice.compareTo(hopePrice) > 0) {
                throw new IllegalArgumentException("최저 내정가는 희망가보다 클 수 없습니다.");
            }
        }
        
        // 100원 단위 체크
        if (startPrice.remainder(BigDecimal.valueOf(100)).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("시작가는 100원 단위로 설정해주세요.");
        }
        
        if (hopePrice.remainder(BigDecimal.valueOf(100)).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("희망가는 100원 단위로 설정해주세요.");
        }
        
        // Reserve Price도 100원 단위 체크
        if (reservePrice != null && reservePrice.remainder(BigDecimal.valueOf(100)).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("최저 내정가는 100원 단위로 설정해주세요.");
        }
        
        // 경매 시간 검증 (허용된 시간만)
        List<Integer> allowedHours = List.of(3, 6, 12, 24, 48, 72);
        if (!allowedHours.contains(auctionTimeHours)) {
            throw new IllegalArgumentException("경매 시간은 3, 6, 12, 24, 48, 72시간 중에서만 선택 가능합니다.");
        }
        
        // 지역 범위에 따른 지역 정보 검증
        if (regionScope != RegionScope.NATIONWIDE && (regionCode == null || regionCode.trim().isEmpty())) {
            throw new IllegalArgumentException("지역별 경매의 경우 지역 정보는 필수입니다.");
        }
    }
    
}