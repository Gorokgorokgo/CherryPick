package com.cherrypick.app.domain.connection.service;

import com.cherrypick.app.domain.connection.dto.response.ConnectionFeeResponse;
import com.cherrypick.app.domain.user.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 연결 서비스 수수료 계산기
 */
@Component
public class ConnectionFeeCalculator {

    /**
     * 현재 기본 수수료율 (0% - 무료 프로모션)
     * 추후 점진적으로 인상 예정 (예: 1% → 2% → 3%)
     */
    private static final BigDecimal BASE_FEE_RATE = BigDecimal.ZERO;
    
    /**
     * 판매자 레벨별 할인율 매핑
     */
    private static final int[] SELLER_DISCOUNT_RATES = {
        0,   // 레벨 1: 0% 할인
        5,   // 레벨 2: 5% 할인
        10,  // 레벨 3: 10% 할인
        15,  // 레벨 4: 15% 할인
        20,  // 레벨 5: 20% 할인
        25,  // 레벨 6: 25% 할인
        30,  // 레벨 7: 30% 할인
        35,  // 레벨 8: 35% 할인
        40,  // 레벨 9: 40% 할인
        50   // 레벨 10: 50% 할인
    };

    /**
     * 연결 서비스 수수료 계산
     * 
     * @param seller 판매자
     * @param finalPrice 낙찰가
     * @return 수수료 정보
     */
    public ConnectionFeeResponse calculateConnectionFee(User seller, BigDecimal finalPrice) {
        
        // 기본 수수료 계산
        BigDecimal baseFee = finalPrice.multiply(BASE_FEE_RATE)
                .setScale(0, RoundingMode.HALF_UP);
        
        // 판매자 레벨별 할인율 계산
        int sellerLevel = seller.getSellerLevel();
        int discountRate = getDiscountRate(sellerLevel);
        
        // 할인 금액 계산
        BigDecimal discountAmount = baseFee.multiply(BigDecimal.valueOf(discountRate))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        
        // 최종 수수료 계산
        BigDecimal finalFee = baseFee.subtract(discountAmount);
        
        return ConnectionFeeResponse.builder()
                .finalPrice(finalPrice)
                .baseFeeRate(BASE_FEE_RATE.multiply(BigDecimal.valueOf(100))) // 퍼센트로 표시
                .baseFee(baseFee)
                .discountRate(discountRate)
                .discountAmount(discountAmount)
                .finalFee(finalFee)
                .sellerLevel(sellerLevel)
                .isFreePromotion(BASE_FEE_RATE.equals(BigDecimal.ZERO))
                .build();
    }
    
    /**
     * 판매자 레벨에 따른 할인율 반환
     */
    private int getDiscountRate(int sellerLevel) {
        if (sellerLevel < 1 || sellerLevel > SELLER_DISCOUNT_RATES.length) {
            return 0;
        }
        return SELLER_DISCOUNT_RATES[sellerLevel - 1];
    }
    
    /**
     * 기본 수수료율 업데이트 (관리자용 - 추후 구현)
     */
    public void updateBaseFeeRate(BigDecimal newRate) {
        // TODO: 설정 테이블 또는 환경변수로 관리하도록 변경 예정
        // 현재는 하드코딩된 상수 사용
    }
}