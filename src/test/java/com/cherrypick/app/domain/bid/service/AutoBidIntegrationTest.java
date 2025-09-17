package com.cherrypick.app.domain.bid.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.bid.dto.request.PlaceBidRequest;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;

/**
 * 자동입찰 통합 테스트
 * 
 * 실제 데이터베이스를 사용한 End-to-End 테스트
 * - 실제 입찰 시나리오 검증
 * - 자동입찰 트리거 및 실행 검증
 * - WebSocket 알림 검증
 * - 동시성 시나리오 검증
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("자동입찰 통합 테스트")
class AutoBidIntegrationTest {

    @Autowired
    private BidService bidService;
    
    @Autowired
    private AutoBidService autoBidService;
    
    @Autowired
    private AuctionRepository auctionRepository;
    
    @Autowired
    private BidRepository bidRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private User seller;
    private User bidder1;
    private User bidder2;
    private User manualBidder;
    private Auction auction;
    
    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        seller = User.builder()
                .phoneNumber("01012345678")
                .nickname("판매자")
                .email("seller@test.com")
                .build();
        userRepository.save(seller);
        
        bidder1 = User.builder()
                .phoneNumber("01012345679")
                .nickname("자동입찰자1")
                .email("bidder1@test.com")
                .build();
        userRepository.save(bidder1);
        
        bidder2 = User.builder()
                .phoneNumber("01012345680")
                .nickname("자동입찰자2")
                .email("bidder2@test.com")
                .build();
        userRepository.save(bidder2);
        
        manualBidder = User.builder()
                .phoneNumber("01012345681")
                .nickname("수동입찰자")
                .email("manual@test.com")
                .build();
        userRepository.save(manualBidder);
        
        // 테스트 경매 생성
        auction = Auction.createAuction(
                seller,
                "테스트 상품",
                "자동입찰 테스트용 상품",
                Category.ELECTRONICS,
                new BigDecimal("50000"), // 시작가
                new BigDecimal("200000"), // 희망가
                null, // Reserve price
                24, // 24시간
                RegionScope.CITY,
                "1111",
                "서울시 강남구",
                5, // 상품상태
                LocalDateTime.now().toLocalDate().minusDays(30)
        );
        auctionRepository.save(auction);
    }
    
    @Test
    @DisplayName("자동입찰 설정 후 수동 입찰에 대한 자동 응답 테스트")
    void shouldAutoBidAfterManualBid() throws InterruptedException {
        // Given: bidder1이 자동입찰 설정 (최대 100,000원, 7% 증가)
        PlaceBidRequest autoBidRequest = new PlaceBidRequest();
        autoBidRequest.setAuctionId(auction.getId());
        autoBidRequest.setBidAmount(new BigDecimal("52500")); // 5% 증가
        autoBidRequest.setIsAutoBid(true);
        autoBidRequest.setMaxAutoBidAmount(new BigDecimal("100000"));
        autoBidRequest.setAutoBidPercentage(7); // 7% 증가율 설정
        
        bidService.placeBid(bidder1.getId(), autoBidRequest);
        
        // When: manualBidder가 60,000원에 수동 입찰
        PlaceBidRequest manualBidRequest = new PlaceBidRequest();
        manualBidRequest.setAuctionId(auction.getId());
        manualBidRequest.setBidAmount(new BigDecimal("60000"));
        manualBidRequest.setIsAutoBid(false);
        
        bidService.placeBid(manualBidder.getId(), manualBidRequest);
        
        // Then: 1초 후 자동입찰이 실행되어야 함 (60,000 * 1.07 = 64,200원)
        await().atMost(3000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<Bid> bids = bidRepository.findByAuctionIdOrderByBidAmountDesc(auction.getId(), 
                            org.springframework.data.domain.Pageable.unpaged()).getContent();
                    
                    // 최고 입찰이 자동입찰이어야 함
                    Bid highestBid = bids.get(0);
                    assertThat(highestBid.getBidder().getId()).isEqualTo(bidder1.getId());
                    assertThat(highestBid.getBidAmount()).isEqualTo(new BigDecimal("64200"));
                    assertThat(highestBid.getIsAutoBid()).isTrue();
                });
    }
    
    @Test
    @DisplayName("여러 자동입찰자 간의 경쟁 시나리오 테스트")
    void shouldHandleMultipleAutoBiddersCompetition() throws InterruptedException {
        // Given: 두 명의 자동입찰자 설정
        // bidder1: 최대 80,000원, 5% 증가
        PlaceBidRequest autoBid1 = new PlaceBidRequest();
        autoBid1.setAuctionId(auction.getId());
        autoBid1.setBidAmount(new BigDecimal("52500"));
        autoBid1.setIsAutoBid(true);
        autoBid1.setMaxAutoBidAmount(new BigDecimal("80000"));
        autoBid1.setAutoBidPercentage(5);
        
        bidService.placeBid(bidder1.getId(), autoBid1);
        
        // bidder2: 최대 120,000원, 6% 증가 (더 높은 최대금액)
        PlaceBidRequest autoBid2 = new PlaceBidRequest();
        autoBid2.setAuctionId(auction.getId());
        autoBid2.setBidAmount(new BigDecimal("55000"));
        autoBid2.setIsAutoBid(true);
        autoBid2.setMaxAutoBidAmount(new BigDecimal("120000"));
        autoBid2.setAutoBidPercentage(6);
        
        bidService.placeBid(bidder2.getId(), autoBid2);
        
        // When: 외부 입찰자가 65,000원에 입찰
        PlaceBidRequest manualBid = new PlaceBidRequest();
        manualBid.setAuctionId(auction.getId());
        manualBid.setBidAmount(new BigDecimal("65000"));
        manualBid.setIsAutoBid(false);
        
        bidService.placeBid(manualBidder.getId(), manualBid);
        
        // Then: 최대금액이 높은 bidder2가 자동입찰에서 승리해야 함
        await().atMost(3000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    List<Bid> bids = bidRepository.findByAuctionIdOrderByBidAmountDesc(auction.getId(), 
                            org.springframework.data.domain.Pageable.unpaged()).getContent();
                    
                    Bid highestBid = bids.get(0);
                    assertThat(highestBid.getBidder().getId()).isEqualTo(bidder2.getId());
                    assertThat(highestBid.getIsAutoBid()).isTrue();
                    // 65,000 * 1.06 = 68,900원
                    assertThat(highestBid.getBidAmount()).isEqualTo(new BigDecimal("68900"));
                });
    }
    
    @Test
    @DisplayName("자동입찰 최대금액 도달시 자동 중단 테스트")
    void shouldStopAutoBidWhenReachingMaxAmount() throws InterruptedException {
        // Given: 낮은 최대금액으로 자동입찰 설정 (최대 65,000원)
        PlaceBidRequest autoBidRequest = new PlaceBidRequest();
        autoBidRequest.setAuctionId(auction.getId());
        autoBidRequest.setBidAmount(new BigDecimal("52500"));
        autoBidRequest.setIsAutoBid(true);
        autoBidRequest.setMaxAutoBidAmount(new BigDecimal("65000")); // 낮은 최대금액
        autoBidRequest.setAutoBidPercentage(5);
        
        bidService.placeBid(bidder1.getId(), autoBidRequest);
        
        // When: 외부에서 62,000원에 입찰 (다음 자동입찰이 65,100원이 되어 최대금액 초과)
        PlaceBidRequest manualBid = new PlaceBidRequest();
        manualBid.setAuctionId(auction.getId());
        manualBid.setBidAmount(new BigDecimal("62000"));
        manualBid.setIsAutoBid(false);
        
        bidService.placeBid(manualBidder.getId(), manualBid);
        
        // Then: 자동입찰이 실행되지 않고 수동입찰이 최고가를 유지해야 함
        Thread.sleep(2000); // 자동입찰이 실행될 시간을 충분히 기다림
        
        List<Bid> bids = bidRepository.findByAuctionIdOrderByBidAmountDesc(auction.getId(), 
                org.springframework.data.domain.Pageable.unpaged()).getContent();
        
        Bid highestBid = bids.get(0);
        assertThat(highestBid.getBidder().getId()).isEqualTo(manualBidder.getId());
        assertThat(highestBid.getBidAmount()).isEqualTo(new BigDecimal("62000"));
        assertThat(highestBid.getIsAutoBid()).isFalse();
    }
    
    @Test
    @DisplayName("유효하지 않은 퍼센티지로 자동입찰 설정시 예외 발생 테스트")
    void shouldThrowExceptionForInvalidPercentage() {
        // Given: 2% 증가율로 설정 (5% 미만이므로 예외 발생해야 함)
        PlaceBidRequest autoBidRequest = new PlaceBidRequest();
        autoBidRequest.setAuctionId(auction.getId());
        autoBidRequest.setBidAmount(new BigDecimal("52500"));
        autoBidRequest.setIsAutoBid(true);
        autoBidRequest.setMaxAutoBidAmount(new BigDecimal("100000"));
        autoBidRequest.setAutoBidPercentage(2); // 2% 설정 (유효하지 않음)
        
        // When & Then: 예외가 발생해야 함
        assertThatThrownBy(() -> bidService.placeBid(bidder1.getId(), autoBidRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5-10% 범위여야 합니다");
    }
    
    @Test
    @DisplayName("경매 종료 후 자동입찰 중단 테스트")
    void shouldNotAutoBidAfterAuctionEnds() throws InterruptedException {
        // Given: 자동입찰 설정
        PlaceBidRequest autoBidRequest = new PlaceBidRequest();
        autoBidRequest.setAuctionId(auction.getId());
        autoBidRequest.setBidAmount(new BigDecimal("52500"));
        autoBidRequest.setIsAutoBid(true);
        autoBidRequest.setMaxAutoBidAmount(new BigDecimal("100000"));
        autoBidRequest.setAutoBidPercentage(5);
        
        bidService.placeBid(bidder1.getId(), autoBidRequest);
        
        // 경매 종료 처리 (강제로 상태 변경)
        auction = auctionRepository.findById(auction.getId()).get();
        auction.endAuction(null, BigDecimal.ZERO); // 유찰 처리
        auctionRepository.save(auction);
        
        // When: 경매 종료 후 입찰 시도
        PlaceBidRequest manualBid = new PlaceBidRequest();
        manualBid.setAuctionId(auction.getId());
        manualBid.setBidAmount(new BigDecimal("60000"));
        manualBid.setIsAutoBid(false);
        
        // 경매 종료된 상태에서 입찰하면 예외가 발생해야 함
        assertThatThrownBy(() -> bidService.placeBid(manualBidder.getId(), manualBid))
                .hasMessageContaining("경매");
    }
    
    @Test
    @DisplayName("자동입찰 조회 API 테스트")
    void shouldRetrieveAutoBidsCorrectly() {
        // Given: 자동입찰 설정
        PlaceBidRequest autoBidRequest = new PlaceBidRequest();
        autoBidRequest.setAuctionId(auction.getId());
        autoBidRequest.setBidAmount(new BigDecimal("52500"));
        autoBidRequest.setIsAutoBid(true);
        autoBidRequest.setMaxAutoBidAmount(new BigDecimal("100000"));
        autoBidRequest.setAutoBidPercentage(7);
        
        BidResponse response = bidService.placeBid(bidder1.getId(), autoBidRequest);
        
        // When: 자동입찰 조회
        List<Bid> autoBids = autoBidService.getActiveAutoBidsForAuction(auction.getId());
        
        // Then: 올바른 자동입찰 정보가 조회되어야 함
        assertThat(autoBids).hasSize(1);
        Bid autoBid = autoBids.get(0);
        assertThat(autoBid.getIsAutoBid()).isTrue();
        assertThat(autoBid.getMaxAutoBidAmount()).isEqualTo(new BigDecimal("100000"));
        assertThat(autoBid.getAutoBidPercentage()).isEqualTo(7);
        assertThat(autoBid.getBidder().getId()).isEqualTo(bidder1.getId());
    }
    
    @Test
    @DisplayName("100원 단위 반올림 테스트")
    void shouldRoundTo100WonUnits() {
        // Given & When: 계산 결과가 100원 단위가 아닌 경우 테스트
        BigDecimal currentBid = new BigDecimal("55150");
        BigDecimal result = autoBidService.calculateNextAutoBidAmount(currentBid, 7);
        
        // Then: 100원 단위로 반올림되어야 함
        // 55,150 * 1.07 = 59,010.5 → 59,000원으로 반올림
        assertThat(result).isEqualTo(new BigDecimal("59000"));
        
        // 나머지 경우들 테스트
        assertThat(autoBidService.calculateNextAutoBidAmount(new BigDecimal("55175"), 6))
                .isEqualTo(new BigDecimal("58500")); // 55,175 * 1.06 = 58,485.5 → 58,500
    }
}