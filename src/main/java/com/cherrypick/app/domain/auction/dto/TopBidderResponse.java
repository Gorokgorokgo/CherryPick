package com.cherrypick.app.domain.auction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 유찰 경매의 최고입찰자 정보 응답 DTO
 */
@Getter
@Builder
@Schema(description = "최고입찰자 정보 응답")
public class TopBidderResponse {

    @Schema(description = "최고입찰자 사용자 ID", example = "123")
    private Long userId;

    @Schema(description = "최고입찰자 닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "최고입찰가", example = "500000")
    private BigDecimal bidAmount;
}
