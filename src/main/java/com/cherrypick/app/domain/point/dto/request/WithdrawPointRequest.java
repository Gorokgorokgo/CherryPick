package com.cherrypick.app.domain.point.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawPointRequest {
    
    @NotNull(message = "출금 금액은 필수입니다.")
    private Long amount;
    
    @NotNull(message = "출금할 계좌 ID는 필수입니다.")
    private Long accountId;
    
    public void validateAmount() {
        // 출금 금액에 대한 특별한 제한 없음
    }
}