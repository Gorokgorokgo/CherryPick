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
     * 사용자별 수수료율 계산 (신규 사용자 무료 고려)
     */
    public BigDecimal getCommissionRateForUser(LocalDate userCreatedDate) {
        // 신규 사용자 무료 기간 체크
        if (userCreatedDate.isAfter(LocalDate.now().minusDays(commission.newUserFreeDays))) {
            return BigDecimal.ZERO;
        }
        
        return getCurrentCommissionRate();
    }
}