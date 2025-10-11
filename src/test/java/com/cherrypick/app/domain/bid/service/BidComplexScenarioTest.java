package com.cherrypick.app.domain.bid.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.bid.dto.response.BidResponse;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.enums.BidStatus;
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
@DisplayName("입찰 복잡 시나리오 테스트")
class BidComplexScenarioTest {

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
    private Auction auction;

    @BeforeEach
    void setUp() {
        seller = User.builder()
                .email("seller@test.com")
                .nickname("판매자")
                .password("password")
                .phoneNumber("01012345678")
                .build();
        seller = userRepository.save(seller);
    }

    @Test
    @DisplayName("자동 입찰 체인 반응 - 7명 자동입찰 + 1명 수동입찰")
    void autoBidChainReaction() {
        // given: 경매 생성 (시작가 10,000원)
        auction = Auction.createAuction(
                seller,
                "고가 상품 경매",
                "체인 반응 테스트",
                Category.ELECTRONICS,
                new BigDecimal("10000"),
                new BigDecimal("500000"),
                null,
                24,
                RegionScope.NATIONWIDE,
                null,
                null
        );
        auction = auctionRepository.save(auction);

        // 7명의 자동입찰자 생성
        User[] autoBidders = new User[7];
        BigDecimal[] maxAmounts = {
                new BigDecimal("100000"),  // bidder1: 10만
                new BigDecimal("150000"),  // bidder2: 15만
                new BigDecimal("200000"),  // bidder3: 20만
                new BigDecimal("250000"),  // bidder4: 25만
                new BigDecimal("300000"),  // bidder5: 30만
                new BigDecimal("350000"),  // bidder6: 35만
                new BigDecimal("400000")   // bidder7: 40만
        };

        for (int i = 0; i < 7; i++) {
            autoBidders[i] = User.builder()
                    .email("bidder" + (i + 1) + "@test.com")
                    .nickname("자동입찰자" + (i + 1))
                    .password("password")
                    .phoneNumber("0101111" + String.format("%04d", i))
                    .build();
            autoBidders[i] = userRepository.save(autoBidders[i]);
        }

        // when: 7명 모두 자동입찰 설정
        for (int i = 0; i < 7; i++) {
            autoBidService.setupAutoBid(auction.getId(), autoBidders[i].getId(), maxAmounts[i]);
        }

        // then: 최고 금액자(bidder7)가 2등(bidder6)의 최대금액 + 증가폭으로 입찰되어야 함
        Auction finalAuction = auctionRepository.findById(auction.getId()).get();

        // 35만원(bidder6 최대) + 1,000원 = 351,000원
        assertThat(finalAuction.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("351000"));

        Optional<Bid> highestBid = bidRepository.findTopByAuctionIdAndBidAmountGreaterThanOrderByBidAmountDesc(
                auction.getId(), BigDecimal.ZERO);
        assertThat(highestBid).isPresent();
        assertThat(highestBid.get().getBidder().getId()).isEqualTo(autoBidders[6].getId());
        assertThat(highestBid.get().getIsAutoBid()).isTrue();

        // 수동 입찰자 추가
        User manualBidder = User.builder()
                .email("manual@test.com")
                .nickname("수동입찰자")
                .password("password")
                .phoneNumber("01099999999")
                .build();
        manualBidder = userRepository.save(manualBidder);

        // when: 수동입찰자가 38만원 입찰
        bidService.placeBid(auction.getId(), manualBidder.getId(), new BigDecimal("380000"));

        // then: bidder7의 자동입찰이 반응하여 381,000원 입찰
        finalAuction = auctionRepository.findById(auction.getId()).get();
        assertThat(finalAuction.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("381000"));

        highestBid = bidRepository.findTopByAuctionIdAndBidAmountGreaterThanOrderByBidAmountDesc(
                auction.getId(), BigDecimal.ZERO);
        assertThat(highestBid.get().getBidder().getId()).isEqualTo(autoBidders[6].getId());

        // when: 수동입찰자가 45만원 입찰 (bidder7 최대금액 40만 초과)
        bidService.placeBid(auction.getId(), manualBidder.getId(), new BigDecimal("450000"));

        // then: 자동입찰 반응 없음
        finalAuction = auctionRepository.findById(auction.getId()).get();
        assertThat(finalAuction.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("450000"));

        highestBid = bidRepository.findTopByAuctionIdAndBidAmountGreaterThanOrderByBidAmountDesc(
                auction.getId(), BigDecimal.ZERO);
        assertThat(highestBid.get().getBidder().getId()).isEqualTo(manualBidder.getId());
        assertThat(highestBid.get().getIsAutoBid()).isFalse();
    }

    @Test
    @DisplayName("가격 구간 변경 - 1만원 경계 (500원 → 1,000원)")
    void priceRangeBoundary10K() {
        // given: 시작가 9,000원
        auction = Auction.createAuction(
                seller,
                "1만원 경계 테스트",
                "입찰 단위 변경",
                Category.CLOTHING,
                new BigDecimal("9000"),
                new BigDecimal("50000"),
                null,
                24,
                RegionScope.NATIONWIDE,
                null,
                null
        );
        auction = auctionRepository.save(auction);

        User bidder1 = createUser("bidder1@test.com", "입찰자1", "01011111111");
        User bidder2 = createUser("bidder2@test.com", "입찰자2", "01022222222");

        // when: 9,000원으로 첫 입찰
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("9000"));

        // then: 현재가 9,000원 (1만원 미만이므로 500원 단위)
        Auction auction1 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction1.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("9000"));

        // when: 9,500원 입찰 (500원 증가)
        bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("9500"));

        // then: 현재가 9,500원
        Auction auction2 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction2.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("9500"));

        // when: 10,000원 입찰 (경계 넘김)
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));

        // then: 현재가 10,000원
        Auction auction3 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction3.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("10000"));

        // when: bidder2가 자동입찰 설정 (최대 15,000원)
        autoBidService.setupAutoBid(auction.getId(), bidder2.getId(), new BigDecimal("15000"));

        // then: 11,000원으로 자동 입찰 (1만원 이상이므로 1,000원 단위)
        Auction auction4 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction4.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("11000"));

        // when: 11,500원 입찰 시도 (500원 단위 위반)
        assertThatThrownBy(() ->
                bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("11500")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최소");
    }

    @Test
    @DisplayName("가격 구간 변경 - 100만원 경계 (1,000원 → 5,000원)")
    void priceRangeBoundary1M() {
        // given: 시작가 990,000원
        auction = Auction.createAuction(
                seller,
                "100만원 경계 테스트",
                "입찰 단위 변경",
                Category.ELECTRONICS,
                new BigDecimal("990000"),
                new BigDecimal("5000000"),
                null,
                24,
                RegionScope.NATIONWIDE,
                null,
                null
        );
        auction = auctionRepository.save(auction);

        User bidder1 = createUser("bidder1@test.com", "입찰자1", "01011111111");
        User bidder2 = createUser("bidder2@test.com", "입찰자2", "01022222222");

        // when: 999,000원 입찰 (1,000원 단위)
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("999000"));

        // when: 1,000,000원 입찰 (경계)
        bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("1000000"));

        // then: 현재가 1,000,000원
        Auction auction1 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction1.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("1000000"));

        // when: 자동입찰 설정 (최대 1,500,000원)
        autoBidService.setupAutoBid(auction.getId(), bidder1.getId(), new BigDecimal("1500000"));

        // then: 1,005,000원으로 자동 입찰 (100만원 이상이므로 5,000원 단위)
        Auction auction2 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction2.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("1005000"));

        // when: 1,001,000원 입찰 시도 (5,000원 단위 위반)
        assertThatThrownBy(() ->
                bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("1001000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최소");
    }

    @Test
    @DisplayName("가격 구간 변경 - 1,000만원 경계 (5,000원 → 10,000원)")
    void priceRangeBoundary10M() {
        // given: 시작가 9,990,000원
        auction = Auction.createAuction(
                seller,
                "1000만원 경계 테스트",
                "입찰 단위 변경",
                Category.ELECTRONICS,
                new BigDecimal("9990000"),
                new BigDecimal("50000000"),
                null,
                24,
                RegionScope.NATIONWIDE,
                null,
                null
        );
        auction = auctionRepository.save(auction);

        User bidder1 = createUser("bidder1@test.com", "입찰자1", "01011111111");
        User bidder2 = createUser("bidder2@test.com", "입찰자2", "01022222222");

        // when: 9,995,000원 입찰 (5,000원 단위)
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("9995000"));

        // when: 10,000,000원 입찰 (경계)
        bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("10000000"));

        // then: 현재가 10,000,000원
        Auction auction1 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction1.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("10000000"));

        // when: 자동입찰 설정 (최대 15,000,000원)
        autoBidService.setupAutoBid(auction.getId(), bidder1.getId(), new BigDecimal("15000000"));

        // then: 10,010,000원으로 자동 입찰 (1000만원 이상이므로 10,000원 단위)
        Auction auction2 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction2.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("10010000"));
    }

    @Test
    @DisplayName("자동입찰 취소 후 재설정 - 기존 입찰 내역 보존")
    void cancelAndResetAutoBid() {
        // given: 시작가 10,000원
        auction = Auction.createAuction(
                seller,
                "자동입찰 취소 테스트",
                "취소 후 재설정",
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

        User bidder1 = createUser("bidder1@test.com", "입찰자1", "01011111111");
        User bidder2 = createUser("bidder2@test.com", "입찰자2", "01022222222");

        // when: bidder1이 첫 수동 입찰 (10,000원)
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));

        // when: bidder2가 자동입찰 설정 (최대 30,000원) → 11,000원 입찰
        autoBidService.setupAutoBid(auction.getId(), bidder2.getId(), new BigDecimal("30000"));

        Auction auction1 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction1.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("11000"));

        // when: bidder1이 15,000원 입찰
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("15000"));

        // then: bidder2 자동 반응 → 16,000원
        Auction auction2 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction2.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("16000"));

        // when: bidder2가 자동입찰 취소
        autoBidService.cancelAutoBid(auction.getId(), bidder2.getId());

        // 자동입찰 설정이 취소되었는지 확인
        Optional<Bid> autoBidSetting = bidRepository.findByAuctionIdAndBidderIdAndIsAutoBidTrueAndStatus(
                auction.getId(), bidder2.getId(), BidStatus.ACTIVE);
        assertThat(autoBidSetting).isEmpty(); // 자동입찰 설정 없음

        // when: bidder1이 20,000원 입찰
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("20000"));

        // then: bidder2의 자동입찰이 반응하지 않음 (취소되었으므로)
        Auction auction3 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction3.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("20000"));

        // when: bidder2가 다시 자동입찰 설정 (최대 50,000원)
        autoBidService.setupAutoBid(auction.getId(), bidder2.getId(), new BigDecimal("50000"));

        // then: 21,000원으로 즉시 자동 입찰
        Auction auction4 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction4.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("21000"));

        Optional<Bid> highestBid = bidRepository.findTopByAuctionIdAndBidAmountGreaterThanOrderByBidAmountDesc(
                auction.getId(), BigDecimal.ZERO);
        assertThat(highestBid.get().getBidder().getId()).isEqualTo(bidder2.getId());
        assertThat(highestBid.get().getIsAutoBid()).isTrue();

        // when: bidder1이 25,000원 입찰
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("25000"));

        // then: 새 자동입찰이 정상 작동 → 26,000원
        Auction auction5 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction5.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("26000"));
    }

    @Test
    @DisplayName("최대 한도 경계 테스트 - 5배 한도 내/외")
    void maximumBidLimitBoundary() {
        // given: 시작가 10,000원 (1만원 미만은 5만원 고정 한도)
        auction = Auction.createAuction(
                seller,
                "최대 한도 테스트",
                "5배 한도 검증",
                Category.ELECTRONICS,
                new BigDecimal("10000"),
                new BigDecimal("500000"),
                null,
                24,
                RegionScope.NATIONWIDE,
                null,
                null
        );
        auction = auctionRepository.save(auction);

        User bidder1 = createUser("bidder1@test.com", "입찰자1", "01011111111");
        User bidder2 = createUser("bidder2@test.com", "입찰자2", "01022222222");

        // when: 10,000원 첫 입찰 (현재가 10,000원, 한도 5만원 고정)
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));

        // when: 49,000원 입찰 (5만원 이내)
        bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("49000"));

        // then: 입찰 성공
        Auction auction1 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction1.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("49000"));

        // when: 250,000원 입찰 시도 (현재가 49,000의 5배 = 245,000 초과)
        assertThatThrownBy(() ->
                bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("250000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 입찰 한도");

        // when: 100,000원 입찰 (현재가 49,000의 5배 이내)
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("100000"));

        // then: 현재가 100,000원 (이제 4배 한도 적용)
        Auction auction2 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction2.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("100000"));

        // when: 450,000원 입찰 시도 (현재가 100,000의 4배 = 400,000 초과)
        assertThatThrownBy(() ->
                bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("450000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("최대 입찰 한도");

        // when: 400,000원 입찰 (4배 한도 정확히)
        bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("400000"));

        // then: 입찰 성공
        Auction auction3 = auctionRepository.findById(auction.getId()).get();
        assertThat(auction3.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("400000"));
    }


    private User createUser(String email, String nickname, String phoneNumber) {
        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .password("password")
                .phoneNumber(phoneNumber)
                .build();
        return userRepository.save(user);
    }
}
