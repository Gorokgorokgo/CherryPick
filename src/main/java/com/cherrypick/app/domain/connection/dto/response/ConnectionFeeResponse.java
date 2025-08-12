package com.cherrypick.app.domain.connection.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 연결 서비스 수수료 정보 응답 DTO
 */
@Getter
@Builder
public class ConnectionFeeResponse {

    private Long connectionId;
    
    /**
     * 낙찰가
     */
    private BigDecimal finalPrice;
    
    /**
     * 기본 수수료율 (현재 0%, 추후 인상 예정)
     */
    private BigDecimal baseFeeRate;
    
    /**
     * 기본 수수료
     */
    private BigDecimal baseFee;
    
    /**
     * 판매자 레벨별 할인율 (%)
     */
    private Integer discountRate;
    
    /**
     * 할인 금액
     */
    private BigDecimal discountAmount;
    
    /**
     * 최종 수수료 (판매자 부담)
     */
    private BigDecimal finalFee;
    
    /**
     * 판매자 레벨
     */
    private Integer sellerLevel;
    
    /**
     * 무료 기간 여부
     */
    private Boolean isFreePromotion;
}