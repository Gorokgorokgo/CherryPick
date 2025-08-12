package com.cherrypick.app.domain.connection.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 연결 서비스 수수료 결제 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class PayConnectionFeeRequest {

    @NotNull(message = "연결 서비스 ID는 필수입니다.")
    private Long connectionId;

    /**
     * 결제 확인용 - 프론트엔드에서 계산한 수수료와 서버 계산이 일치하는지 검증
     */
    @NotNull(message = "예상 수수료는 필수입니다.")
    private Long expectedFee;
}