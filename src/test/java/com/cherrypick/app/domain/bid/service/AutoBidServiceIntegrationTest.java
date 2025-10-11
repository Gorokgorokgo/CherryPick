package com.cherrypick.app.domain.bid.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.bid.dto.response.BidResponse;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("자동 입찰 서비스 통합 테스트")
class AutoBidServiceIntegrationTest {

    @Autowired
    private AutoBidService autoBidService;

    @Autowired
    private BidService bidService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BidRepository bidRepository;

    private User seller;
    private User bidder1;
    private User bidder2;
    private User bidder3;
    private Auction auction;

    @BeforeEach
    void setUp() {
        // 사용자 생성
        seller = createUser("seller@test.com", "판매자", "01012345678");
        bidder1 = createUser("bidder1@test.com", "입찰자1", "01098765432");
        bidder2 = createUser("bidder2@test.com", "입찰자2", "01011112222");
        bidder3 = createUser("bidder3@test.com", "입찰자3", "01033334444");

        // 경매 생성
        auction = Auction.createAuction(
                seller,
                "테스트 경매",
                "상품 설명",
                Category.ELECTRONICS,
                new BigDecimal("10000"),
                new BigDecimal("100000"),
                null,
                24,
                RegionScope.NATIONWIDE,
                null,
                null
        );
        auction = auctionRepository.save(auction);
    }

    private User createUser(String email, String nickname, String phone) {
        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .password("password")
                .phoneNumber(phone)
                .build();
        return userRepository.save(user);
    }

    @Test
    @DisplayName("자동 입찰 설정 - 첫 입찰일 때 시작가로 즉시 입찰")
    void setupAutoBidFirstBid() {
        // when
        BidResponse response = autoBidService.setupAutoBid(
                auction.getId(),
                bidder1.getId(),
                new BigDecimal("50000")
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.getIsAutoBid()).isTrue();
        assertThat(response.getMaxAutoBidAmount()).isEqualByComparingTo(new BigDecimal("50000"));

        // 자동 입찰 설정 레코드 확인
        Optional<Bid> autoBidSetting = bidRepository.findByAuctionIdAndBidderIdAndIsAutoBidTrueAndStatus(
                auction.getId(), bidder1.getId(), com.cherrypick.app.domain.bid.enums.BidStatus.ACTIVE);
        assertThat(autoBidSetting).isPresent();
        assertThat(autoBidSetting.get().getBidAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        // 실제 입찰이 시작가로 생성되었는지 확인
        Optional<Bid> actualBid = bidRepository.findTopByAuctionIdAndBidAmountGreaterThanOrderByBidAmountDesc(
                auction.getId(), BigDecimal.ZERO);
        assertThat(actualBid).isPresent();
        assertThat(actualBid.get().getBidAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(actualBid.get().getBidder().getId()).isEqualTo(bidder1.getId());

        // 경매 현재가 확인
        Auction updatedAuction = auctionRepository.findById(auction.getId()).get();
        assertThat(updatedAuction.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("자동 입찰 경쟁 - 2명이 자동입찰 설정 시 최대 금액 높은 사람이 승리")
    void autoBidCompetition() throws InterruptedException {
        // given: bidder1이 먼저 15,000원으로 수동 입찰
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("15000"));

        // when: bidder2가 30,000원 자동입찰, bidder3가 50,000원 자동입찰 설정
        autoBidService.setupAutoBid(auction.getId(), bidder2.getId(), new BigDecimal("30000"));
        Thread.sleep(100); // 순서 보장을 위한 짧은 딜레이
        autoBidService.setupAutoBid(auction.getId(), bidder3.getId(), new BigDecimal("50000"));

        // then: bidder3가 승리하고 최종 입찰가는 bidder2의 최대금액 + 입찰단위
        Auction updatedAuction = auctionRepository.findById(auction.getId()).get();
        BigDecimal expectedPrice = new BigDecimal("31000"); // 30,000 + 1,000
        assertThat(updatedAuction.getCurrentPrice()).isEqualByComparingTo(expectedPrice);

        // 최고 입찰자 확인
        Optional<Bid> highestBid = bidRepository.findTopByAuctionIdAndBidAmountGreaterThanOrderByBidAmountDesc(
                auction.getId(), BigDecimal.ZERO);
        assertThat(highestBid).isPresent();
        assertThat(highestBid.get().getBidder().getId()).isEqualTo(bidder3.getId());
    }

    @Test
    @DisplayName("자동 입찰 취소")
    void cancelAutoBid() {
        // given
        autoBidService.setupAutoBid(auction.getId(), bidder1.getId(), new BigDecimal("50000"));

        // when
        autoBidService.cancelAutoBid(auction.getId(), bidder1.getId());

        // then
        Optional<Bid> autoBidSetting = bidRepository.findByAuctionIdAndBidderIdAndIsAutoBidTrueAndStatus(
                auction.getId(), bidder1.getId(), com.cherrypick.app.domain.bid.enums.BidStatus.ACTIVE);
        assertThat(autoBidSetting).isEmpty();
    }

    @Test
    @DisplayName("자동 입찰 실패 - 이미 최고 입찰자")
    void autoBidFailAlreadyHighest() {
        // given: bidder1이 이미 최고가로 입찰
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("20000"));

        // when: bidder1이 자동입찰을 더 높은 금액으로 설정하려고 시도
        autoBidService.setupAutoBid(auction.getId(), bidder1.getId(), new BigDecimal("50000"));

        // then: 자동입찰은 설정되지만 추가 입찰은 발생하지 않음 (이미 최고가이므로)
        Auction updatedAuction = auctionRepository.findById(auction.getId()).get();
        assertThat(updatedAuction.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("20000"));
    }

    @Test
    @DisplayName("자동 입찰 검증 - 최대 금액이 현재가보다 낮으면 실패")
    void autoBidFailMaxAmountTooLow() {
        // given: 현재가가 20,000원
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("20000"));

        // when & then: 15,000원으로 자동입찰 설정 시도하면 실패
        assertThatThrownBy(() ->
                autoBidService.setupAutoBid(auction.getId(), bidder2.getId(), new BigDecimal("15000"))
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("최대 자동입찰 금액은");
    }
}