package com.cherrypick.app.domain.point.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "포인트 충전 요청")
@Getter
@Setter
public class ChargePointRequest {
    
    @Schema(description = "충전 금액", example = "50000", required = true)
    @NotNull(message = "충전 금액은 필수입니다.")
    private Long amount;
    
    @Schema(description = "결제할 계좌 ID", example = "1", required = true)
    @NotNull(message = "결제할 계좌 ID는 필수입니다.")
    private Long accountId;
    
    /**
     * 충전 금액 유효성 검증
     */
    public void validateAmount() {
        // 유효성 검증 로직 제거 - 금액 제한 없음
    }
}