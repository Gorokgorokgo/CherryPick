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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("입찰 서비스 핵심 로직 테스트")
class BidServiceCoreTest {

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
    @DisplayName("내 입찰 내역 조회 - 최신순 정렬")
    void getMyBids_sortedByTimeDesc() throws InterruptedException {
        // given: bidder1이 여러 경매에 입찰
        Auction auction2 = Auction.createAuction(
                seller,
                "두번째 경매",
                "상품 설명",
                Category.CLOTHING,
                new BigDecimal("5000"),
                new BigDecimal("30000"),
                null,
                24,
                RegionScope.NATIONWIDE,
                null,
                null
        );
        auction2 = auctionRepository.save(auction2);

        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));
        Thread.sleep(100); // 시간 차이 보장
        bidService.placeBid(auction2.getId(), bidder1.getId(), new BigDecimal("5000"));
        Thread.sleep(100);
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("12000"));

        // when: 내 입찰 내역 조회
        Page<BidResponse> result = bidService.getMyBids(bidder1.getId(), PageRequest.of(0, 10));

        // then: 최신순으로 정렬됨
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getBidAmount()).isEqualByComparingTo(new BigDecimal("12000"));
        assertThat(result.getContent().get(1).getBidAmount()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(result.getContent().get(2).getBidAmount()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("내 입찰 내역 조회 - 다른 사용자 입찰 제외")
    void getMyBids_excludeOtherBidders() {
        // given: 여러 사용자가 입찰
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));
        bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("12000"));
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("14000"));

        // when: bidder1의 입찰 내역 조회
        Page<BidResponse> result = bidService.getMyBids(bidder1.getId(), PageRequest.of(0, 10));

        // then: bidder1의 입찰만 조회됨
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(bid -> bid.getBidderNickname().equals("입찰자1"));
    }

    @Test
    @DisplayName("경매 현재가 업데이트 - 수동 입찰 시 경매 엔티티 업데이트")
    void placeBid_updateAuctionCurrentPrice() {
        // when: 입찰 실행
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));

        // then: 경매 현재가가 업데이트됨
        Auction updatedAuction = auctionRepository.findById(auction.getId()).get();
        assertThat(updatedAuction.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("경매 입찰 횟수 증가 - 입찰 시마다 bidCount 증가")
    void placeBid_increaseBidCount() {
        // given: 초기 입찰 횟수 확인
        Auction initialAuction = auctionRepository.findById(auction.getId()).get();
        int initialCount = initialAuction.getBidCount();

        // when: 3번 입찰
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));
        bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("12000"));
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("14000"));

        // then: 입찰 횟수가 3 증가
        Auction updatedAuction = auctionRepository.findById(auction.getId()).get();
        assertThat(updatedAuction.getBidCount()).isEqualTo(initialCount + 3);
    }

    @Test
    @DisplayName("수동 입찰 플래그 - isAutoBid가 false로 설정")
    void placeBid_manualBidFlag() {
        // when: 수동 입찰
        BidResponse response = bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));

        // then: 수동 입찰 플래그 확인
        assertThat(response.getIsAutoBid()).isFalse();

        // DB에서도 확인
        Bid savedBid = bidRepository.findById(response.getId()).get();
        assertThat(savedBid.getIsAutoBid()).isFalse();
    }

    @Test
    @DisplayName("첫 입찰 여부 확인 - 최고가가 없으면 true")
    void placeBid_firstBidDetection() {
        // when: 첫 입찰
        BidResponse response = bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));

        // then: 시작가로 입찰 가능
        assertThat(response.getBidAmount()).isEqualByComparingTo(new BigDecimal("10000"));

        // 두 번째 입찰은 최소 증가폭 적용
        assertThatThrownBy(() ->
                bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("10000"))
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("입찰 검증 호출 - BidValidationService 통합 확인")
    void placeBid_validationIntegration() {
        // when & then: 100원 단위 위반 시 검증 실패
        assertThatThrownBy(() ->
                bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10050"))
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("100원 단위");

        // when & then: 시작가 미달 시 검증 실패
        assertThatThrownBy(() ->
                bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("5000"))
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("시작가");
    }

    @Test
    @DisplayName("입찰 생성 및 저장 - Bid 엔티티 정상 생성")
    void placeBid_createAndSaveBid() {
        // when: 입찰 실행
        BidResponse response = bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));

        // then: Bid 엔티티가 정상 저장됨
        Optional<Bid> savedBid = bidRepository.findById(response.getId());
        assertThat(savedBid).isPresent();
        assertThat(savedBid.get().getBidAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(savedBid.get().getBidder().getId()).isEqualTo(bidder1.getId());
        assertThat(savedBid.get().getAuction().getId()).isEqualTo(auction.getId());
    }

    @Test
    @DisplayName("경매별 입찰 내역 조회 - 0원 초과 입찰만 조회")
    void getAuctionBids_excludeZeroAmount() {
        // given: 정상 입찰
        bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));
        bidService.placeBid(auction.getId(), bidder2.getId(), new BigDecimal("12000"));

        // when: 입찰 내역 조회
        Page<BidResponse> result = bidService.getAuctionBids(auction.getId(), PageRequest.of(0, 10));

        // then: 모든 입찰이 0원 초과
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).allMatch(bid -> bid.getBidAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("응답 생성 - BidResponse 정확한 데이터 매핑")
    void placeBid_responseMapping() {
        // when: 입찰 실행
        BidResponse response = bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("15000"));

        // then: 응답 데이터 정확성 확인
        assertThat(response.getId()).isNotNull();
        assertThat(response.getBidAmount()).isEqualByComparingTo(new BigDecimal("15000"));
        assertThat(response.getBidderNickname()).isEqualTo("입찰자1");
        assertThat(response.getIsAutoBid()).isFalse();
        assertThat(response.getBidTime()).isNotNull();
    }

    @Test
    @DisplayName("동시성 보장 - 비관적 잠금으로 경매 조회")
    void placeBid_pessimisticLock() {
        // when: 입찰 실행
        BidResponse response = bidService.placeBid(auction.getId(), bidder1.getId(), new BigDecimal("10000"));

        // then: 입찰 성공 및 데이터 정합성 확인
        assertThat(response).isNotNull();

        Auction lockedAuction = auctionRepository.findById(auction.getId()).get();
        assertThat(lockedAuction.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(lockedAuction.getBidCount()).isEqualTo(1);
    }
}
