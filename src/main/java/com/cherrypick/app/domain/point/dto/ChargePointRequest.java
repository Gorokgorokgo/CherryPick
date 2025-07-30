package com.cherrypick.app.domain.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "포인트 충전 요청")
@Getter
@Setter
public class ChargePointRequest {
    
    @Schema(description = "충전 금액 (1,000원 단위)", example = "50000", required = true)
    @NotNull(message = "충전 금액은 필수입니다.")
    @Min(value = 1000, message = "최소 충전 금액은 1,000원입니다.")
    private Long amount;
    
    @Schema(description = "결제할 계좌 ID", example = "1", required = true)
    @NotNull(message = "결제할 계좌 ID는 필수입니다.")
    private Long accountId;
    
    /**
     * 충전 금액 유효성 검증
     * - 1,000원 단위 확인
     * - 최대 충전 금액 확인 (1,000,000원)
     */
    public void validateAmount() {
        if (amount % 1000 != 0) {
            throw new IllegalArgumentException("충전 금액은 1,000원 단위로만 가능합니다.");
        }
        
        if (amount > 1000000) {
            throw new IllegalArgumentException("1회 최대 충전 금액은 1,000,000원입니다.");
        }
    }
}