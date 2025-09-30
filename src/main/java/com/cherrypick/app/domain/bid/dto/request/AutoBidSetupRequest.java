package com.cherrypick.app.domain.bid.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoBidSetupRequest {

    @NotNull(message = "경매 ID는 필수입니다")
    private Long auctionId;

    @NotNull(message = "최대 자동입찰 금액은 필수입니다")
    private BigDecimal maxAutoBidAmount;
}