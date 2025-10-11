package com.cherrypick.app.domain.bid.controller;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.bid.dto.response.BidResponse;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.bid.service.BidService;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("입찰 컨트롤러 통합 테스트")
class BidControllerIntegrationTest {

    @Autowired
    private BidController bidController;

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
                .phoneNumber("01011111111")
                .build();
        bidder1 = userRepository.save(bidder1);

        bidder2 = User.builder()
                .email("bidder2@test.com")
                .nickname("입찰자2")
                .password("password")
                .phoneNumber("01022222222")
                .build();
        bidder2 = userRepository.save(bidder2);

        bidder3 = User.builder()
                .email("bidder3@test.com")
                .nickname("입찰자3")
                .password("password")
                .phoneNumber("01033333333")
                .build();
        bidder3 = userRepository.save(bidder3);

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
    @DisplayName("경매별 입찰 내역 조회 - 금액 높은 순으로 정렬")
    void getAuctionBids_sortByAmountDesc() {
        // given: 3명이 서로 다른 금액으로 입찰
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));
        bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("15000"));
        bidService.placeBid(auction.getId(), bidder3.getId(), new BigDecimal("20000"));

        // when: 입찰 내역 조회
        Page<BidResponse> result = bidController.getAuctionBids(auction.getId(), 0, 10).getBody();

        // then: 금액 높은 순으로 정렬되어 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getBidAmount()).isEqualByComparingTo(new BigDecimal("20000"));
        assertThat(result.getContent().get(1).getBidAmount()).isEqualByComparingTo(new BigDecimal("15000"));
        assertThat(result.getContent().get(2).getBidAmount()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("경매별 입찰 내역 조회 - 페이징 처리")
    void getAuctionBids_pagination() {
        // given: 5명이 입찰
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));
        bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("12000"));
        bidService.placeBid(auction.getId(), bidder3.getId(), new BigDecimal("14000"));
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("16000"));
        bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("18000"));

        // when: 페이지 크기 2로 첫 번째 페이지 조회
        Page<BidResponse> page1 = bidController.getAuctionBids(auction.getId(), 0, 2).getBody();

        // then: 최고가 2개만 조회됨
        assertThat(page1).isNotNull();
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isEqualTo(5);
        assertThat(page1.getContent().get(0).getBidAmount()).isEqualByComparingTo(new BigDecimal("18000"));
        assertThat(page1.getContent().get(1).getBidAmount()).isEqualByComparingTo(new BigDecimal("16000"));

        // when: 두 번째 페이지 조회
        Page<BidResponse> page2 = bidController.getAuctionBids(auction.getId(), 1, 2).getBody();

        // then: 다음 2개 조회됨
        assertThat(page2).isNotNull();
        assertThat(page2.getContent()).hasSize(2);
        assertThat(page2.getContent().get(0).getBidAmount()).isEqualByComparingTo(new BigDecimal("14000"));
        assertThat(page2.getContent().get(1).getBidAmount()).isEqualByComparingTo(new BigDecimal("12000"));
    }

    @Test
    @DisplayName("경매별 입찰 내역 조회 - 입찰 내역 없음")
    void getAuctionBids_noBids() {
        // when: 입찰 내역이 없는 경매 조회
        Page<BidResponse> result = bidController.getAuctionBids(auction.getId(), 0, 10).getBody();

        // then: 빈 페이지 반환
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("경매별 입찰 내역 조회 - 동일 입찰자가 여러 번 입찰한 경우")
    void getAuctionBids_sameBidderMultipleTimes() {
        // given: bidder1이 3번 입찰
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));
        bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("12000"));
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("14000"));
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("16000"));

        // when: 입찰 내역 조회
        Page<BidResponse> result = bidController.getAuctionBids(auction.getId(), 0, 10).getBody();

        // then: 모든 입찰 내역이 조회됨
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(4);

        // bidder1의 입찰이 3개, bidder2의 입찰이 1개
        long bidder1Count = result.getContent().stream()
                .filter(bid -> bid.getBidderNickname().equals("입찰자1"))
                .count();
        long bidder2Count = result.getContent().stream()
                .filter(bid -> bid.getBidderNickname().equals("입찰자2"))
                .count();

        assertThat(bidder1Count).isEqualTo(3);
        assertThat(bidder2Count).isEqualTo(1);
    }

    @Test
    @DisplayName("경매별 입찰 내역 조회 - 큰 금액 범위에서 정렬 확인")
    void getAuctionBids_largeAmountRange() {
        // given: 다양한 금액대로 입찰
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));
        bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("20000"));
        bidService.placeBid(auction.getId(), bidder3.getId(), new BigDecimal("30000"));
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("35000"));
        bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("40000"));

        // when: 입찰 내역 조회
        Page<BidResponse> result = bidController.getAuctionBids(auction.getId(), 0, 10).getBody();

        // then: 정확한 내림차순 정렬 확인
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(5);

        for (int i = 0; i < result.getContent().size() - 1; i++) {
            BigDecimal current = result.getContent().get(i).getBidAmount();
            BigDecimal next = result.getContent().get(i + 1).getBidAmount();
            assertThat(current).isGreaterThanOrEqualTo(next);
        }
    }
}
