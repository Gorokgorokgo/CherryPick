package com.cherrypick.app.domain.bid.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "입찰 요청")
public class PlaceBidRequest {

    @NotNull(message = "경매 ID는 필수입니다")
    @Schema(description = "경매 ID", example = "1")
    private Long auctionId;

    @NotNull(message = "입찰 금액은 필수입니다")
    @DecimalMin(value = "1000", message = "입찰 금액은 최소 1,000원 이상이어야 합니다")
    @Schema(description = "입찰 금액 (1000원 단위)", example = "15000")
    private BigDecimal bidAmount;

    @Schema(description = "자동 입찰 여부", example = "false")
    private Boolean isAutoBid = false;

    @Schema(description = "자동 입찰 최대 금액 (자동 입찰 시 필수)", example = "20000")
    private BigDecimal maxAutoBidAmount;

    /**
     * 요청 데이터 유효성 검증
     */
    public void validate() {
        // 입찰 금액은 1000원 단위여야 함
        if (bidAmount.remainder(BigDecimal.valueOf(1000)).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("입찰 금액은 1,000원 단위로 입력해주세요.");
        }

        // 자동 입찰인 경우 최대 금액 검증
        if (Boolean.TRUE.equals(isAutoBid)) {
            if (maxAutoBidAmount == null) {
                throw new IllegalArgumentException("자동 입찰 시 최대 금액은 필수입니다.");
            }
            
            if (maxAutoBidAmount.compareTo(bidAmount) < 0) {
                throw new IllegalArgumentException("자동 입찰 최대 금액은 현재 입찰 금액보다 커야 합니다.");
            }
            
            // 최대 금액도 1000원 단위여야 함
            if (maxAutoBidAmount.remainder(BigDecimal.valueOf(1000)).compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException("자동 입찰 최대 금액은 1,000원 단위로 입력해주세요.");
            }
        }
    }
}