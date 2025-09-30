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
}