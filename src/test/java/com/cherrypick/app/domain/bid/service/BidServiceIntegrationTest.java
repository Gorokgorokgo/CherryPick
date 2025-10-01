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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("입찰 서비스 통합 테스트")
class BidServiceIntegrationTest {

    @Autowired
    private BidService bidService;

    @Autowired
    private AutoBidService autoBidService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BidRepository bidRepository;

    private User seller;
    private User bidder1;
    private User bidder2;
    private Auction auction;

    @BeforeEach
    void setUp() {
        // 사용자 생성
        seller = User.builder()
                .email("seller@test.com")
                .nickname("판매자")
                .password("password")
                .phoneNumber("01012345678")
                .build();
        seller = userRepository.save(seller);

        bidder1 = User.builder()
                .email("bidder1@test.com")
                .nickname("입찰자1")
                .password("password")
                .phoneNumber("01098765432")
                .build();
        bidder1 = userRepository.save(bidder1);

        bidder2 = User.builder()
                .email("bidder2@test.com")
                .nickname("입찰자2")
                .password("password")
                .phoneNumber("01011112222")
                .build();
        bidder2 = userRepository.save(bidder2);

        // 경매 생성
        auction = Auction.createAuction(
                seller,
                "테스트 경매",
                "상품 설명",
                Category.ELECTRONICS,
                new BigDecimal("10000"),
                new BigDecimal("50000"),
                null,
                24,
                RegionScope.NATIONWIDE,
                null,
                null
        );
        auction = auctionRepository.save(auction);
    }

    @Test
    @DisplayName("첫 입찰 - 시작가로 입찰 성공")
    void placeFirstBid() {
        // when
        BidResponse response = bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));

        // then
        assertThat(response).isNotNull();
        assertThat(response.getBidAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(response.getIsAutoBid()).isFalse();

        // 경매 현재가 확인
        Auction updatedAuction = auctionRepository.findById(auction.getId()).get();
        assertThat(updatedAuction.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(updatedAuction.getBidCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("두 번째 입찰 - 최소 증가폭 만족")
    void placeSecondBid() {
        // given
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));

        // when
        BidResponse response = bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("11000"));

        // then
        assertThat(response.getBidAmount()).isEqualByComparingTo(new BigDecimal("11000"));

        Auction updatedAuction = auctionRepository.findById(auction.getId()).get();
        assertThat(updatedAuction.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("11000"));
        assertThat(updatedAuction.getBidCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("입찰 실패 - 100원 단위 위반")
    void failBid100Unit() {
        assertThatThrownBy(() ->
            bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10050"))
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("100원 단위");
    }

    @Test
    @DisplayName("입찰 실패 - 최소 증가폭 미달")
    void failBidMinimumIncrement() {
        // given
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));

        // when & then
        assertThatThrownBy(() ->
            bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("10500"))
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("최소");
    }

    @Test
    @DisplayName("입찰 실패 - 본인 경매")
    void failBidSelfAuction() {
        assertThatThrownBy(() ->
            bidService.placeBid(auction.getId(), seller.getId(), new BigDecimal("15000"))
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("자신의 경매");
    }

    @Test
    @DisplayName("입찰 실패 - 최대 한도 초과")
    void failBidMaximumLimit() {
        assertThatThrownBy(() ->
            bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("60000"))
        ).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("최대 입찰 한도");
    }

    @Test
    @DisplayName("연속 입찰 - 3명이 순차적으로 입찰")
    void consecutiveBids() {
        // when
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));
        bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("12000"));
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("15000"));

        // then
        Auction updatedAuction = auctionRepository.findById(auction.getId()).get();
        assertThat(updatedAuction.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("15000"));
        assertThat(updatedAuction.getBidCount()).isEqualTo(3);

        // 최고 입찰 확인
        Optional<Bid> highestBid = bidRepository.findTopByAuctionIdAndBidAmountGreaterThanOrderByBidAmountDesc(
                auction.getId(), BigDecimal.ZERO);
        assertThat(highestBid).isPresent();
        assertThat(highestBid.get().getBidAmount()).isEqualByComparingTo(new BigDecimal("15000"));
        assertThat(highestBid.get().getBidder().getId()).isEqualTo(bidder1.getId());
    }

    @Test
    @DisplayName("수동/자동 입찰 통합 - 자동입찰자가 수동입찰에 반응")
    void manualAndAutoBidIntegration() {
        // given: bidder1이 최대 50,000원으로 자동입찰 설정
        autoBidService.setupAutoBid(auction.getId(), bidder1.getId(), new BigDecimal("50000"));

        // 자동입찰 즉시 실행으로 10,000원(시작가)으로 입찰됨
        Auction updatedAuction = auctionRepository.findById(auction.getId()).get();
        assertThat(updatedAuction.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("10000"));

        // when: bidder2가 20,000원으로 수동 입찰
        bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("20000"));

        // then: bidder1의 자동입찰이 반응하여 21,000원으로 자동 입찰 (20,000 + 1,000)
        updatedAuction = auctionRepository.findById(auction.getId()).get();
        assertThat(updatedAuction.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("21000"));

        Optional<Bid> highestBid = bidRepository.findTopByAuctionIdAndBidAmountGreaterThanOrderByBidAmountDesc(
                auction.getId(), BigDecimal.ZERO);
        assertThat(highestBid).isPresent();
        assertThat(highestBid.get().getBidAmount()).isEqualByComparingTo(new BigDecimal("21000"));
        assertThat(highestBid.get().getBidder().getId()).isEqualTo(bidder1.getId());
        assertThat(highestBid.get().getIsAutoBid()).isTrue();
    }

    @Test
    @DisplayName("수동/자동 입찰 통합 - 자동입찰 최대금액 초과 시 반응 안 함")
    void manualBidExceedsAutoBidMax() {
        // given: bidder1이 최대 30,000원으로 자동입찰 설정
        autoBidService.setupAutoBid(auction.getId(), bidder1.getId(), new BigDecimal("30000"));

        // when: bidder2가 40,000원으로 수동 입찰 (자동입찰 최대금액 초과)
        bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("40000"));

        // then: 자동입찰이 반응하지 않음 (41,000원이 최대금액 30,000원을 초과)
        Auction updatedAuction = auctionRepository.findById(auction.getId()).get();
        assertThat(updatedAuction.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("40000"));

        Optional<Bid> highestBid = bidRepository.findTopByAuctionIdAndBidAmountGreaterThanOrderByBidAmountDesc(
                auction.getId(), BigDecimal.ZERO);
        assertThat(highestBid).isPresent();
        assertThat(highestBid.get().getBidder().getId()).isEqualTo(bidder2.getId());
        assertThat(highestBid.get().getIsAutoBid()).isFalse();
    }

    @Test
    @DisplayName("수동/자동 입찰 통합 - 여러 자동입찰자 중 최대금액 높은 사람이 반응")
    void multipleAutoBiddersReactToManualBid() {
        // given: 2명이 서로 다른 최대금액으로 자동입찰 설정
        User bidder3 = User.builder()
                .email("bidder3@test.com")
                .nickname("입찰자3")
                .password("password")
                .phoneNumber("01011113333")
                .build();
        bidder3 = userRepository.save(bidder3);

        // bidder1: 최대 30,000원
        autoBidService.setupAutoBid(auction.getId(), bidder1.getId(), new BigDecimal("30000"));

        // bidder2: 최대 50,000원 (더 높음)
        autoBidService.setupAutoBid(auction.getId(), bidder2.getId(), new BigDecimal("50000"));
        // 자동 입찰 경쟁으로 현재가는 31,000원이 됨 (bidder1 최대 30,000 + 1,000)

        // when: bidder3가 40,000원으로 수동 입찰
        bidService.placeBid(auction.getId(), bidder3.getId(), new BigDecimal("40000"));

        // then: 최대금액이 더 높은 bidder2가 41,000원으로 반응 (40,000 + 1,000)
        Auction updatedAuction = auctionRepository.findById(auction.getId()).get();
        assertThat(updatedAuction.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("41000"));

        Optional<Bid> highestBid = bidRepository.findTopByAuctionIdAndBidAmountGreaterThanOrderByBidAmountDesc(
                auction.getId(), BigDecimal.ZERO);
        assertThat(highestBid).isPresent();
        assertThat(highestBid.get().getBidder().getId()).isEqualTo(bidder2.getId());
        assertThat(highestBid.get().getIsAutoBid()).isTrue();
    }

    @Test
    @DisplayName("수동/자동 입찰 통합 - 입찰 단위 가격 구간별 적용")
    void autoBidReactionWithDifferentIncrements() {
        // given: bidder1이 15,000원 수동 입찰 (10,000~100만원 구간, 입찰단위 1,000원)
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("15000"));

        // bidder2가 최대 60,000원으로 자동입찰 설정 (15,000 * 4배 = 60,000, 최대한도 이내)
        // 즉시 16,000원으로 입찰됨 (15,000 + 1,000)
        autoBidService.setupAutoBid(auction.getId(), bidder2.getId(), new BigDecimal("60000"));

        // when: bidder1이 25,000원으로 수동 입찰
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("25000"));

        // then: bidder2의 자동입찰이 반응하여 26,000원으로 자동 입찰 (25,000 + 1,000)
        Auction updatedAuction = auctionRepository.findById(auction.getId()).get();
        assertThat(updatedAuction.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("26000"));

        Optional<Bid> highestBid = bidRepository.findTopByAuctionIdAndBidAmountGreaterThanOrderByBidAmountDesc(
                auction.getId(), BigDecimal.ZERO);
        assertThat(highestBid).isPresent();
        assertThat(highestBid.get().getBidAmount()).isEqualByComparingTo(new BigDecimal("26000"));
        assertThat(highestBid.get().getBidder().getId()).isEqualTo(bidder2.getId());
    }

    @Test
    @DisplayName("5명 혼합 입찰 시나리오 - 수동 3명, 자동 2명")
    void fiveBiddersMixedScenario() {
        // given: 5명의 입찰자 생성
        User bidder3 = User.builder()
                .email("bidder3@test.com")
                .nickname("입찰자3")
                .password("password")
                .phoneNumber("01033334444")
                .build();
        bidder3 = userRepository.save(bidder3);

        User bidder4 = User.builder()
                .email("bidder4@test.com")
                .nickname("입찰자4")
                .password("password")
                .phoneNumber("01044445555")
                .build();
        bidder4 = userRepository.save(bidder4);

        User bidder5 = User.builder()
                .email("bidder5@test.com")
                .nickname("입찰자5")
                .password("password")
                .phoneNumber("01055556666")
                .build();
        bidder5 = userRepository.save(bidder5);

        // when & then: 입찰 시나리오

        // 1. bidder1이 10,000원으로 첫 수동 입찰
        BidResponse bid1 = bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));
        assertThat(bid1.getBidAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(bid1.getIsAutoBid()).isFalse();

        // 2. bidder2가 최대 25,000원으로 자동입찰 설정 -> 즉시 11,000원 입찰
        autoBidService.setupAutoBid(auction.getId(), bidder2.getId(), new BigDecimal("25000"));
        Auction auction2 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction2.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("11000"));

        // 3. bidder3가 15,000원으로 수동 입찰
        bidService.placeBid(auction.getId(), bidder3.getId(), new BigDecimal("15000"));

        // 4. bidder2의 자동입찰이 반응 -> 16,000원 자동 입찰
        Auction auction3 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction3.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("16000"));

        // 5. bidder4가 최대 40,000원으로 자동입찰 설정
        // bidder2와 자동입찰 경쟁 발생 -> bidder2가 25,000원까지 가고, bidder4가 26,000원에서 승리
        autoBidService.setupAutoBid(auction.getId(), bidder4.getId(), new BigDecimal("40000"));
        Auction auction4 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction4.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("26000"));

        Optional<Bid> currentHighest = bidRepository.findTopByAuctionIdAndBidAmountGreaterThanOrderByBidAmountDesc(
                auction.getId(), BigDecimal.ZERO);
        assertThat(currentHighest.get().getBidder().getId()).isEqualTo(bidder4.getId());
        assertThat(currentHighest.get().getIsAutoBid()).isTrue();

        // 6. bidder5가 30,000원으로 수동 입찰
        bidService.placeBid(auction.getId(), bidder5.getId(), new BigDecimal("30000"));

        // 7. bidder4의 자동입찰이 반응 -> 31,000원 자동 입찰
        Auction auction5 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction5.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("31000"));

        // 8. bidder1이 35,000원으로 재입찰
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("35000"));

        // 9. bidder4의 자동입찰이 반응 -> 36,000원 자동 입찰
        Auction auction6 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction6.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("36000"));

        // 10. bidder3가 45,000원으로 수동 입찰 (bidder4의 최대금액 40,000 초과)
        bidService.placeBid(auction.getId(), bidder3.getId(), new BigDecimal("45000"));

        // 11. 자동입찰이 반응하지 않음 (최대금액 초과)
        Auction finalAuction = auctionRepository.findById(auction.getId()).get();
        assertThat(finalAuction.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("45000"));

        Optional<Bid> finalHighest = bidRepository.findTopByAuctionIdAndBidAmountGreaterThanOrderByBidAmountDesc(
                auction.getId(), BigDecimal.ZERO);
        assertThat(finalHighest.get().getBidder().getId()).isEqualTo(bidder3.getId());
        assertThat(finalHighest.get().getIsAutoBid()).isFalse();

        // 최종 검증: 총 입찰 내역 확인
        long totalBids = bidRepository.countByAuctionId(auction.getId());
        assertThat(totalBids).isGreaterThanOrEqualTo(10); // 수동 5개 + 자동 5개 이상
    }
}