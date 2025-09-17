package com.cherrypick.app.domain.bid.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "자동입찰 설정 요청 (즉시 입찰 없이 최대 금액만 설정)")
public class AutoBidSetupRequest {

    @NotNull(message = "경매 ID는 필수입니다")
    @Schema(description = "경매 ID", example = "1")
    private Long auctionId;

    @NotNull(message = "자동 입찰 최대 금액은 필수입니다")
    @Schema(description = "자동 입찰 최대 금액 (100원 단위)", example = "20000")
    private BigDecimal maxAutoBidAmount;

    public void validate() {
        if (maxAutoBidAmount == null || maxAutoBidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("자동 입찰 최대 금액은 0원보다 커야 합니다.");
        }
        if (maxAutoBidAmount.remainder(BigDecimal.valueOf(100)).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("자동 입찰 최대 금액은 100원 단위로 입력해주세요.");
        }
    }
}
