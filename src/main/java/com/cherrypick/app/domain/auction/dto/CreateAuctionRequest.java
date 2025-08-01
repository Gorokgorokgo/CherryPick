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
    
    @Schema(description = "시작가 (1,000원 단위)", example = "100000", required = true)
    @NotNull(message = "시작가는 필수입니다.")
    @DecimalMin(value = "1000", message = "시작가는 최소 1,000원입니다.")
    private BigDecimal startPrice;
    
    @Schema(description = "희망가 (1,000원 단위)", example = "800000", required = true)
    @NotNull(message = "희망가는 필수입니다.")
    @DecimalMin(value = "1000", message = "희망가는 최소 1,000원입니다.")
    private BigDecimal hopePrice;
    
    @Schema(description = "최저 내정가 (Reserve Price) - 선택사항", example = "500000")
    @DecimalMin(value = "1000", message = "최저 내정가는 최소 1,000원입니다.")
    private BigDecimal reservePrice;
    
    @Schema(description = "경매 진행 시간 (시간 단위)", example = "72", required = true)
    @NotNull(message = "경매 시간은 필수입니다.")
    @Min(value = 24, message = "경매 시간은 최소 24시간입니다.")
    @Max(value = 168, message = "경매 시간은 최대 168시간(7일)입니다.")
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
            
            // Reserve Price 1000원 단위 체크
            if (reservePrice.remainder(BigDecimal.valueOf(1000)).compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException("최저 내정가는 1,000원 단위로 설정해주세요.");
            }
        }
        
        // 1000원 단위 체크
        if (startPrice.remainder(BigDecimal.valueOf(1000)).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("시작가는 1,000원 단위로 설정해주세요.");
        }
        
        if (hopePrice.remainder(BigDecimal.valueOf(1000)).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("희망가는 1,000원 단위로 설정해주세요.");
        }
        
        // 지역 범위에 따른 지역 정보 검증
        if (regionScope != RegionScope.NATIONWIDE && (regionCode == null || regionCode.trim().isEmpty())) {
            throw new IllegalArgumentException("지역별 경매의 경우 지역 정보는 필수입니다.");
        }
    }
    
    public BigDecimal calculateDepositAmount() {
        // 보증금 = 희망가의 10%
        return hopePrice.multiply(BigDecimal.valueOf(0.1));
    }
}