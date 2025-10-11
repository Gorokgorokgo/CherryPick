package com.cherrypick.app.domain.bid.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("입찰 검증 서비스 테스트")
class BidValidationServiceTest {

    private BidValidationService validationService;
    private User seller;
    private User bidder;
    private Auction auction;

    @BeforeEach
    void setUp() {
        validationService = new BidValidationService();

        seller = User.builder()
                .id(1L)
                .email("seller@test.com")
                .nickname("판매자")
                .build();

        bidder = User.builder()
                .id(2L)
                .email("bidder@test.com")
                .nickname("입찰자")
                .build();

        auction = Auction.createAuction(
                seller,
                "테스트 경매",
                "설명",
                Category.ELECTRONICS,
                new BigDecimal("10000"),
                new BigDecimal("50000"),
                null,
                24,
                RegionScope.NATIONWIDE,
                null,
                null
        );
    }

    @Test
    @DisplayName("100원 단위 검증 - 성공")
    void validate100UnitSuccess() {
        BigDecimal validAmount = new BigDecimal("15000");

        assertThatCode(() -> validationService.validate100Unit(validAmount))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("100원 단위 검증 - 실패")
    void validate100UnitFail() {
        BigDecimal invalidAmount = new BigDecimal("15050");

        assertThatThrownBy(() -> validationService.validate100Unit(invalidAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100원 단위");
    }

    @Test
    @DisplayName("최소 증가폭 계산 - 1만원 미만 (500원)")
    void calculateMinimumIncrementUnder10K() {
        BigDecimal currentPrice = new BigDecimal("9000");
        BigDecimal minIncrement = validationService.calculateMinimumIncrement(currentPrice);

        assertThat(minIncrement).isEqualTo(new BigDecimal("500"));
    }

    @Test
    @DisplayName("최소 증가폭 계산 - 1만원~100만원 (1000원)")
    void calculateMinimumIncrement10KTo1M() {
        BigDecimal currentPrice = new BigDecimal("50000");
        BigDecimal minIncrement = validationService.calculateMinimumIncrement(currentPrice);

        assertThat(minIncrement).isEqualTo(new BigDecimal("1000"));
    }

    @Test
    @DisplayName("최소 증가폭 계산 - 100만원~1000만원 (5000원)")
    void calculateMinimumIncrement1MTo10M() {
        BigDecimal currentPrice = new BigDecimal("5000000");
        BigDecimal minIncrement = validationService.calculateMinimumIncrement(currentPrice);

        assertThat(minIncrement).isEqualTo(new BigDecimal("5000"));
    }

    @Test
    @DisplayName("최소 증가폭 계산 - 1000만원 이상 (10000원)")
    void calculateMinimumIncrementOver10M() {
        BigDecimal currentPrice = new BigDecimal("20000000");
        BigDecimal minIncrement = validationService.calculateMinimumIncrement(currentPrice);

        assertThat(minIncrement).isEqualTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("최소 입찰가 검증 - 성공")
    void validateMinimumBidSuccess() {
        BigDecimal currentPrice = new BigDecimal("10000");
        BigDecimal bidAmount = new BigDecimal("11000");

        assertThatCode(() -> validationService.validateMinimumBid(currentPrice, bidAmount))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("최소 입찰가 검증 - 실패")
    void validateMinimumBidFail() {
        BigDecimal currentPrice = new BigDecimal("10000");
        BigDecimal bidAmount = new BigDecimal("10500");

        assertThatThrownBy(() -> validationService.validateMinimumBid(currentPrice, bidAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최소");
    }

    @Test
    @DisplayName("최대 입찰 한도 계산 - 1만원 미만 (5만원 고정)")
    void calculateMaximumBidLimitUnder10K() {
        BigDecimal currentPrice = new BigDecimal("5000");
        BigDecimal maxLimit = validationService.calculateMaximumBidLimit(currentPrice);

        assertThat(maxLimit).isEqualTo(new BigDecimal("50000"));
    }

    @Test
    @DisplayName("최대 입찰 한도 계산 - 1만원~10만원 (5배)")
    void calculateMaximumBidLimit10KTo100K() {
        BigDecimal currentPrice = new BigDecimal("50000");
        BigDecimal maxLimit = validationService.calculateMaximumBidLimit(currentPrice);

        assertThat(maxLimit).isEqualTo(new BigDecimal("250000"));
    }

    @Test
    @DisplayName("최대 입찰 한도 검증 - 성공")
    void validateMaximumBidLimitSuccess() {
        BigDecimal currentPrice = new BigDecimal("10000");
        BigDecimal bidAmount = new BigDecimal("40000");

        assertThatCode(() -> validationService.validateMaximumBidLimit(currentPrice, bidAmount))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("최대 입찰 한도 검증 - 실패")
    void validateMaximumBidLimitFail() {
        BigDecimal currentPrice = new BigDecimal("10000");
        BigDecimal bidAmount = new BigDecimal("60000");

        assertThatThrownBy(() -> validationService.validateMaximumBidLimit(currentPrice, bidAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 입찰 한도");
    }

    @Test
    @DisplayName("경매 유효성 검증 - 성공")
    void validateAuctionStatusSuccess() {
        assertThatCode(() -> validationService.validateAuctionStatus(auction))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("본인 경매 입찰 차단")
    void validateNotSelfBid() {
        assertThatThrownBy(() -> validationService.validateNotSelfBid(auction, seller))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("자신의 경매");
    }

    @Test
    @DisplayName("본인이 아닌 경우 입찰 가능")
    void validateNotSelfBidSuccess() {
        assertThatCode(() -> validationService.validateNotSelfBid(auction, bidder))
                .doesNotThrowAnyException();
    }
}