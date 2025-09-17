package com.cherrypick.app.domain.bid.service;

import com.cherrypick.app.common.exception.BusinessException;
import com.cherrypick.app.domain.auction.entity.Auction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD 백엔드 가이드 준수:
 * 1. 비즈니스 로직 단위 테스트 (최우선)
 * 2. 외부 의존성 없이 순수한 로직 검증  
 * 3. 모킹 최소화
 */
@DisplayName("입찰 검증 비즈니스 로직 단위 테스트")
class BidValidationBusinessLogicTest {

    private final BidService bidService = new BidService(null, null, null, null);

    /**
     * 리플렉션으로 private validateBidAmount 메서드 직접 테스트
     * 외부 의존성(DB, API) 완전 배제한 순수 비즈니스 로직 검증
     */
    private void validateBidAmount(BigDecimal currentPrice, BigDecimal bidAmount) throws Exception {
        Auction auction = Auction.builder()
                .currentPrice(currentPrice)
                .build();
        
        Method method = BidService.class.getDeclaredMethod("validateBidAmount", Auction.class, BigDecimal.class);
        method.setAccessible(true);
        method.invoke(bidService, auction, bidAmount);
    }

    @Nested
    @DisplayName("가격대별 최소 입찰 단위 비즈니스 로직")
    class MinimumBidUnitLogicTest {

        @Test
        @DisplayName("1만원 미만: 500원 단위, 500원 증가 - 성공")
        void under10K_valid500Unit() throws Exception {
            // Given: 비즈니스 규칙 - 1만원 미만은 500원 단위, 최소 500원 증가
            BigDecimal currentPrice = BigDecimal.valueOf(5000);
            BigDecimal validBidAmount = BigDecimal.valueOf(5500); // 500원 증가, 500원 단위
            
            // When & Then: 순수 비즈니스 로직 검증
            assertThatCode(() -> validateBidAmount(currentPrice, validBidAmount))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("1만원 미만: 500원 단위 위반 - 실패")
        void under10K_invalid500Unit() throws Exception {
            // Given: 500원 단위가 아닌 입찰가
            BigDecimal currentPrice = BigDecimal.valueOf(5000);
            BigDecimal invalidBidAmount = BigDecimal.valueOf(5200); // 200원 단위 (위반)
            
            // When & Then: 비즈니스 예외 발생해야 함
            assertThatThrownBy(() -> validateBidAmount(currentPrice, invalidBidAmount))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("500원 단위");
        }

        @Test
        @DisplayName("1만원 미만: 최소 증가 미달 - 실패") 
        void under10K_belowMinimumIncrease() throws Exception {
            // Given: 500원 미만 증가
            BigDecimal currentPrice = BigDecimal.valueOf(5000);
            BigDecimal insufficientBidAmount = BigDecimal.valueOf(5000); // 증가 없음
            
            // When & Then: 최소 증가 위반 예외
            assertThatThrownBy(() -> validateBidAmount(currentPrice, insufficientBidAmount))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("500원 이상 증가");
        }

        @Test
        @DisplayName("1만원~100만원: 1,000원 단위, 1,000원 증가 - 성공")
        void between10KAnd1M_valid1000Unit() throws Exception {
            // Given: 1만원 이상 구간 비즈니스 규칙
            BigDecimal currentPrice = BigDecimal.valueOf(50000);
            BigDecimal validBidAmount = BigDecimal.valueOf(51000); // 1,000원 증가, 1,000원 단위
            
            // When & Then: 성공
            assertThatCode(() -> validateBidAmount(currentPrice, validBidAmount))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("100만원~1,000만원: 5,000원 단위, 5,000원 증가 - 성공")
        void between1MAnd10M_valid5000Unit() throws Exception {
            // Given: 100만원 이상 구간 비즈니스 규칙
            BigDecimal currentPrice = BigDecimal.valueOf(2000000);
            BigDecimal validBidAmount = BigDecimal.valueOf(2005000); // 5,000원 증가, 5,000원 단위
            
            // When & Then: 성공
            assertThatCode(() -> validateBidAmount(currentPrice, validBidAmount))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("1,000만원 이상: 10,000원 단위, 10,000원 증가 - 성공")
        void over10M_valid10000Unit() throws Exception {
            // Given: 1,000만원 이상 구간 비즈니스 규칙
            BigDecimal currentPrice = BigDecimal.valueOf(15000000);
            BigDecimal validBidAmount = BigDecimal.valueOf(15010000); // 10,000원 증가, 10,000원 단위
            
            // When & Then: 성공
            assertThatCode(() -> validateBidAmount(currentPrice, validBidAmount))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("가격대별 최대 입찰 제한 비즈니스 로직")
    class MaximumBidLimitLogicTest {

        @Test
        @DisplayName("1만원 미만: 5만원 고정 상한 - 성공")
        void under10K_within50KLimit() throws Exception {
            // Given: 1만원 미만 = 5만원 고정 상한 규칙
            BigDecimal currentPrice = BigDecimal.valueOf(8000);
            BigDecimal maxAllowedBid = BigDecimal.valueOf(50000); // 상한선
            
            // When & Then: 상한선 내 입찰 성공
            assertThatCode(() -> validateBidAmount(currentPrice, maxAllowedBid))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("1만원 미만: 5만원 초과 - 실패")
        void under10K_exceeds50KLimit() throws Exception {
            // Given: 5만원 상한 초과
            BigDecimal currentPrice = BigDecimal.valueOf(8000);
            BigDecimal excessiveBid = BigDecimal.valueOf(50500); // 상한 초과
            
            // When & Then: 상한 초과 예외
            assertThatThrownBy(() -> validateBidAmount(currentPrice, excessiveBid))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("최대 입찰 한도를 초과");
        }

        @Test
        @DisplayName("1만원~10만원: 현재가의 5배 한도 - 성공")
        void between10KAnd100K_within5TimesLimit() throws Exception {
            // Given: 현재가의 5배 한도 규칙
            BigDecimal currentPrice = BigDecimal.valueOf(20000);
            BigDecimal maxAllowedBid = BigDecimal.valueOf(100000); // 5배
            
            // When & Then: 5배 한도 내 성공
            assertThatCode(() -> validateBidAmount(currentPrice, maxAllowedBid))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("10만원~100만원: 현재가의 4배 한도 - 성공") 
        void between100KAnd1M_within4TimesLimit() throws Exception {
            // Given: 현재가의 4배 한도 규칙
            BigDecimal currentPrice = BigDecimal.valueOf(200000);
            BigDecimal maxAllowedBid = BigDecimal.valueOf(800000); // 4배
            
            // When & Then: 4배 한도 내 성공
            assertThatCode(() -> validateBidAmount(currentPrice, maxAllowedBid))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("100만원~1,000만원: 현재가의 3배 한도 - 성공")
        void between1MAnd10M_within3TimesLimit() throws Exception {
            // Given: 현재가의 3배 한도 규칙
            BigDecimal currentPrice = BigDecimal.valueOf(2000000);
            BigDecimal maxAllowedBid = BigDecimal.valueOf(6000000); // 3배
            
            // When & Then: 3배 한도 내 성공
            assertThatCode(() -> validateBidAmount(currentPrice, maxAllowedBid))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("1,000만원 이상: 현재가의 2배 한도 - 성공")
        void over10M_within2TimesLimit() throws Exception {
            // Given: 현재가의 2배 한도 규칙
            BigDecimal currentPrice = BigDecimal.valueOf(15000000);
            BigDecimal maxAllowedBid = BigDecimal.valueOf(30000000); // 2배
            
            // When & Then: 2배 한도 내 성공
            assertThatCode(() -> validateBidAmount(currentPrice, maxAllowedBid))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("1,000만원 이상: 현재가의 2배 초과 - 실패")
        void over10M_exceeds2TimesLimit() throws Exception {
            // Given: 2배 한도 초과
            BigDecimal currentPrice = BigDecimal.valueOf(15000000);
            BigDecimal excessiveBid = BigDecimal.valueOf(30010000); // 2배 초과
            
            // When & Then: 한도 초과 예외
            assertThatThrownBy(() -> validateBidAmount(currentPrice, excessiveBid))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("최대 입찰 한도를 초과");
        }
    }

    @Nested
    @DisplayName("경계값 비즈니스 로직 테스트")
    class BoundaryValueLogicTest {

        @Test
        @DisplayName("경계값: 9,999원 -> 500원 단위 적용")
        void boundary_9999Won_applies500Unit() throws Exception {
            // Given: 9,999원은 1만원 미만 구간
            BigDecimal currentPrice = BigDecimal.valueOf(9999);
            BigDecimal validBid = BigDecimal.valueOf(10499); // 500원 증가, 500원 단위
            
            // When & Then: 500원 단위 규칙 적용
            assertThatCode(() -> validateBidAmount(currentPrice, validBid))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("경계값: 10,000원 -> 1,000원 단위 적용")
        void boundary_10000Won_applies1000Unit() throws Exception {
            // Given: 10,000원은 1만원 이상 구간
            BigDecimal currentPrice = BigDecimal.valueOf(10000);
            BigDecimal validBid = BigDecimal.valueOf(11000); // 1,000원 증가, 1,000원 단위
            
            // When & Then: 1,000원 단위 규칙 적용  
            assertThatCode(() -> validateBidAmount(currentPrice, validBid))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("경계값: 999,999원 -> 1,000원 단위 적용")
        void boundary_999999Won_applies1000Unit() throws Exception {
            // Given: 999,999원은 100만원 미만 구간
            BigDecimal currentPrice = BigDecimal.valueOf(999999);
            BigDecimal validBid = BigDecimal.valueOf(1000999); // 1,000원 증가, 1,000원 단위
            
            // When & Then: 1,000원 단위 규칙 적용
            assertThatCode(() -> validateBidAmount(currentPrice, validBid))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("경계값: 1,000,000원 -> 5,000원 단위 적용")
        void boundary_1000000Won_applies5000Unit() throws Exception {
            // Given: 1,000,000원은 100만원 이상 구간
            BigDecimal currentPrice = BigDecimal.valueOf(1000000);
            BigDecimal validBid = BigDecimal.valueOf(1005000); // 5,000원 증가, 5,000원 단위
            
            // When & Then: 5,000원 단위 규칙 적용
            assertThatCode(() -> validateBidAmount(currentPrice, validBid))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("에러 메시지 정확성 검증")
    class ErrorMessageAccuracyTest {

        @Test
        @DisplayName("500원 단위 위반시 정확한 에러 메시지")
        void errorMessage_500UnitViolation() throws Exception {
            // Given: 500원 단위 위반
            BigDecimal currentPrice = BigDecimal.valueOf(5000);
            BigDecimal invalidBid = BigDecimal.valueOf(5200);
            
            // When & Then: 정확한 에러 메시지 검증
            assertThatThrownBy(() -> validateBidAmount(currentPrice, invalidBid))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("입찰가는 500원 단위로 입력해주세요.");
        }

        @Test
        @DisplayName("최소 증가 미달시 정확한 에러 메시지")
        void errorMessage_minimumIncreaseViolation() throws Exception {
            // Given: 1,000원 구간에서 최소 증가 미달
            BigDecimal currentPrice = BigDecimal.valueOf(50000);
            BigDecimal insufficientBid = BigDecimal.valueOf(50000);
            
            // When & Then: 정확한 에러 메시지 검증
            assertThatThrownBy(() -> validateBidAmount(currentPrice, insufficientBid))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("최소 1,000원 이상 증가해야 합니다.");
        }

        @Test
        @DisplayName("최대 한도 초과시 정확한 에러 메시지")  
        void errorMessage_maximumLimitExceeded() throws Exception {
            // Given: 5만원 고정 상한 초과
            BigDecimal currentPrice = BigDecimal.valueOf(8000);
            BigDecimal excessiveBid = BigDecimal.valueOf(60000);
            
            // When & Then: 정확한 에러 메시지 검증
            assertThatThrownBy(() -> validateBidAmount(currentPrice, excessiveBid))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("최대 입찰 한도를 초과했습니다. (한도: 50000원)");
        }
    }
}