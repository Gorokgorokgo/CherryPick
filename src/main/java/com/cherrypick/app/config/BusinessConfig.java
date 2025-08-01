package com.cherrypick.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@ConfigurationProperties(prefix = "business")
@Getter
@Setter
public class BusinessConfig {

    private Commission commission = new Commission();

    @Getter
    @Setter
    public static class Commission {
        /**
         * 기본 수수료율 (0.03 = 3%)
         */
        private BigDecimal rate = new BigDecimal("0.03");
        
        /**
         * 신규 사용자 무료 기간 (일)
         */
        private int newUserFreeDays = 30;
        
        /**
         * 프로모션 설정
         */
        private Promotion promotion = new Promotion();
    }

    @Getter
    @Setter
    public static class Promotion {
        /**
         * 프로모션 활성화 여부
         */
        private boolean enabled = false;
        
        /**
         * 프로모션 수수료율 (0.00 = 0%)
         */
        private BigDecimal rate = BigDecimal.ZERO;
        
        /**
         * 프로모션 종료일
         */
        private LocalDate endDate = LocalDate.of(2024, 12, 31);
    }

    /**
     * 현재 적용할 수수료율 계산
     */
    public BigDecimal getCurrentCommissionRate() {
        if (commission.promotion.enabled && 
            LocalDate.now().isBefore(commission.promotion.endDate.plusDays(1))) {
            return commission.promotion.rate;
        }
        return commission.rate;
    }

    /**
     * 사용자별 수수료율 계산 (신규 사용자 무료 기간 고려)
     */
    public BigDecimal getCommissionRateForUser(LocalDate userCreatedDate) {
        // 신규 사용자 무료 기간 체크
        if (userCreatedDate.isAfter(LocalDate.now().minusDays(commission.newUserFreeDays))) {
            return BigDecimal.ZERO;
        }
        
        return getCurrentCommissionRate();
    }

    /**
     * 판매자 레벨별 할인을 적용한 최종 수수료율 계산
     * 마이너스 수수료 방지 로직 포함
     */
    public BigDecimal getFinalCommissionRateForSeller(LocalDate userCreatedDate, Integer sellerLevel) {
        // 기본 수수료율 계산
        BigDecimal baseRate = getCommissionRateForUser(userCreatedDate);
        
        // 기본 수수료가 0%면 할인 적용 안함 (프로모션 중이거나 신규 사용자)
        if (baseRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        // 판매자 레벨별 할인율 계산
        BigDecimal discountRate = getSellerDiscountRate(sellerLevel);
        
        // 최종 수수료율 = 기본 수수료율 - 할인율
        BigDecimal finalRate = baseRate.subtract(discountRate);
        
        // 마이너스 방지: 최소 0% 보장
        return finalRate.max(BigDecimal.ZERO);
    }

    /**
     * 판매자 레벨별 할인율 계산
     */
    private BigDecimal getSellerDiscountRate(Integer sellerLevel) {
        if (sellerLevel >= 100) return new BigDecimal("0.018"); // 1.8% 할인
        if (sellerLevel >= 90) return new BigDecimal("0.012");  // 1.2% 할인  
        if (sellerLevel >= 70) return new BigDecimal("0.006");  // 0.6% 할인
        if (sellerLevel >= 50) return new BigDecimal("0.004");  // 0.4% 할인
        if (sellerLevel >= 30) return new BigDecimal("0.003");  // 0.3% 할인
        if (sellerLevel >= 10) return new BigDecimal("0.002");  // 0.2% 할인
        return BigDecimal.ZERO; // 할인 없음
    }
}