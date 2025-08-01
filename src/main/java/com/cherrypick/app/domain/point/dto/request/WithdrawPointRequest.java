package com.cherrypick.app.domain.point.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawPointRequest {
    
    @NotNull(message = "출금 금액은 필수입니다.")
    @Min(value = 10000, message = "최소 출금 금액은 10,000원입니다.")
    private Long amount;
    
    @NotNull(message = "출금할 계좌 ID는 필수입니다.")
    private Long accountId;
    
    public void validateAmount() {
        if (amount % 1000 != 0) {
            throw new IllegalArgumentException("출금 금액은 1,000원 단위로만 가능합니다.");
        }
        
        if (amount > 1000000) {
            throw new IllegalArgumentException("1회 최대 출금 금액은 1,000,000원입니다.");
        }
    }
}