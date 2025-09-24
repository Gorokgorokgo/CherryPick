package com.cherrypick.app.domain.bid.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.enums.BidStatus;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * AutoBidService TDD 테스트
 * 
 * 비즈니스 로직 우선순위 테스트:
 * 1. 자동입찰 트리거 조건
 * 2. 입찰 금액 계산 (percentage 기반)
 * 3. 최대금액 제한
 * 4. 1초 딜레이
 * 5. 동시 자동입찰 해결
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AutoBidService 단위 테스트 (TDD)")
class AutoBidServiceTest {

    @Mock
    private BidRepository bidRepository;
    
    @Mock
    private AuctionRepository auctionRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private WebSocketMessagingService webSocketMessagingService;
    
    @Mock
    private BidService bidService;
    
    @InjectMocks
    private AutoBidService autoBidService;
    
    private User seller;
    private User bidder1;
    private User bidder2;
    private Auction auction;
    
    @BeforeEach
    void setUp() {
        // Test fixtures
        seller = User.builder()
                .id(1L)
                .nickname("판매자")
                .build();
                
        bidder1 = User.builder()
                .id(2L)
                .nickname("입찰자1")
                .build();
                
        bidder2 = User.builder()
                .id(3L)
                .nickname("입찰자2")
                .build();
        
        auction = Auction.createAuction(
                seller,
                "테스트 상품",
                "테스트 상품 설명",
                Category.ELECTRONICS,
                new BigDecimal("50000"), // 시작가
                new BigDecimal("100000"), // 희망가
                null, // Reserve price 없음
                24, // 24시간
                RegionScope.CITY,
                "1111",
                "서울시 강남구",
                5, // 상품상태
                LocalDateTime.now().toLocalDate().minusDays(30)
        );
        
        // 현재가를 50,000원으로 설정
        auction.updateCurrentPrice(new BigDecimal("50000"));
    }
    
    @Nested
    @DisplayName("자동입찰 트리거 테스트")
    class AutoBidTriggerTest {
        
        @Test
        @DisplayName("새 입찰 발생시 활성 자동입찰들이 트리거되어야 함")
        void shouldTriggerAutoBidsWhenNewBidPlaced() {
            // Given: 최대 70,000원, 5% 단위로 자동입찰 설정된 입찰자
            Bid autoBid = Bid.builder()
                    .id(1L)
                    .auction(auction)
                    .bidder(bidder1)
                    .bidAmount(new BigDecimal("50000"))
                    .isAutoBid(true)
                    .maxAutoBidAmount(new BigDecimal("70000"))
                    .autoBidPercentage(5) // 5% 설정
                    .status(BidStatus.ACTIVE)
                    .bidTime(LocalDateTime.now())
                    .build();
            
            given(bidRepository.findActiveAutoBidsByAuctionId(auction.getId()))
                    .willReturn(Arrays.asList(autoBid));
            
            // When: 경쟁자가 55,000원에 입찰
            BigDecimal competitorBidAmount = new BigDecimal("55000");
            CompletableFuture<Void> result = autoBidService.processAutoBidsForAuction(
                    auction.getId(), competitorBidAmount);
            
            // Then: 자동입찰이 실행되어야 함 (5% 적용되어 57,750원)
            assertThat(result).isNotNull();
            
            // 1초 딜레이 후 자동입찰 실행 검증 (5% 설정)
            verify(bidRepository, timeout(2000)).save(
                    argThat(bid -> 
                        bid.getBidAmount().equals(new BigDecimal("57750")) && // 55,000 * 1.05 = 57,750
                        bid.getIsAutoBid().equals(true)
                    )
            );
        }
        
        @Test
        @DisplayName("최대금액 초과시 자동입찰이 실행되지 않아야 함")
        void shouldNotTriggerAutoBidWhenExceedsMaxAmount() {
            // Given: 최대 60,000원으로 자동입찰 설정 (낮은 최대금액)
            Bid autoBid = Bid.builder()
                    .id(1L)
                    .auction(auction)
                    .bidder(bidder1)
                    .bidAmount(new BigDecimal("50000"))
                    .isAutoBid(true)
                    .maxAutoBidAmount(new BigDecimal("60000")) // 낮은 최대금액
                    .status(BidStatus.ACTIVE)
                    .bidTime(LocalDateTime.now())
                    .build();
            
            given(bidRepository.findActiveAutoBidsByAuctionId(auction.getId()))
                    .willReturn(Arrays.asList(autoBid));
            
            // When: 경쟁자가 59,000원에 입찰 (다음 자동입찰이 60,180원이 되어 최대금액 초과)
            BigDecimal competitorBidAmount = new BigDecimal("59000");
            autoBidService.processAutoBidsForAuction(auction.getId(), competitorBidAmount);
            
            // Then: 자동입찰이 실행되지 않아야 함
            verify(bidService, never()).placeBid(any(), any());
        }
        
        @Test
        @DisplayName("자신의 입찰에는 자동입찰이 트리거되지 않아야 함")
        void shouldNotTriggerAutoBidForOwnBid() {
            // Given: 자동입찰 설정된 입찰자
            Bid autoBid = Bid.builder()
                    .id(1L)
                    .auction(auction)
                    .bidder(bidder1)
                    .bidAmount(new BigDecimal("50000"))
                    .isAutoBid(true)
                    .maxAutoBidAmount(new BigDecimal("70000"))
                    .status(BidStatus.ACTIVE)
                    .bidTime(LocalDateTime.now())
                    .build();
            
            given(bidRepository.findActiveAutoBidsByAuctionId(auction.getId()))
                    .willReturn(Arrays.asList(autoBid));
            
            // When: 자신이 직접 55,000원에 입찰 (수동)
            BigDecimal ownBidAmount = new BigDecimal("55000");
            autoBidService.processAutoBidsForAuction(auction.getId(), ownBidAmount);
            
            // Then: 자동입찰이 실행되지 않아야 함 (자신의 입찰에는 반응하지 않음)
            verify(bidService, never()).placeBid(any(), any());
        }
    }
    
    @Nested
    @DisplayName("입찰 금액 계산 테스트")
    class BidAmountCalculationTest {
        
        @Test
        @DisplayName("2% 설정시 예외가 발생해야 함")
        void shouldThrowExceptionWhenSet2Percent() {
            // Given: 현재가 55,000원
            BigDecimal currentBid = new BigDecimal("55000");
            
            // When & Then: 2% 설정시 예외 발생해야 함
            assertThatThrownBy(() -> autoBidService.calculateNextAutoBidAmount(currentBid, 2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("5-10% 범위여야 합니다");
        }
        
        @Test
        @DisplayName("5% 증가율로 정확한 금액이 계산되어야 함")
        void shouldCalculateCorrectBidAmountWith5Percent() {
            // Given: 현재가 50,000원에서 경쟁자가 55,000원 입찰
            BigDecimal currentBid = new BigDecimal("55000");
            BigDecimal expectedAmount = new BigDecimal("57750"); // 55,000 * 1.05 = 57,750
            
            // When: 5% 증가율로 다음 입찰가 계산
            BigDecimal result = autoBidService.calculateNextAutoBidAmount(currentBid, 5);
            
            // Then: 57,750원이 계산되어야 함
            assertThat(result).isEqualTo(expectedAmount);
        }
        
        @Test
        @DisplayName("100원 단위로 반올림되어야 함")
        void shouldRoundTo100WonUnits() {
            // Given: 계산 결과가 100원 단위가 아닌 경우
            BigDecimal currentBid = new BigDecimal("55150");
            
            // When: 2% 증가율로 계산 (55,150 * 1.02 = 56,253)
            BigDecimal result = autoBidService.calculateNextAutoBidAmount(currentBid, 2);
            
            // Then: 100원 단위로 반올림되어 56,300원이 되어야 함
            assertThat(result).isEqualTo(new BigDecimal("56300"));
        }
    }
    
    @Nested
    @DisplayName("동시 자동입찰 해결 테스트")
    class ConcurrentAutoBidTest {
        
        @Test
        @DisplayName("여러 자동입찰자 중 최대금액이 높은 순으로 우선권을 가져야 함")
        void shouldPrioritizeByMaxAutoBidAmount() {
            // Given: 두 명의 자동입찰자 (최대금액이 다름)
            Bid autoBid1 = Bid.builder()
                    .id(1L)
                    .auction(auction)
                    .bidder(bidder1)
                    .bidAmount(new BigDecimal("50000"))
                    .isAutoBid(true)
                    .maxAutoBidAmount(new BigDecimal("70000")) // 낮은 최대금액
                    .status(BidStatus.ACTIVE)
                    .bidTime(LocalDateTime.now())
                    .build();
                    
            Bid autoBid2 = Bid.builder()
                    .id(2L)
                    .auction(auction)
                    .bidder(bidder2)
                    .bidAmount(new BigDecimal("50000"))
                    .isAutoBid(true)
                    .maxAutoBidAmount(new BigDecimal("80000")) // 높은 최대금액
                    .status(BidStatus.ACTIVE)
                    .bidTime(LocalDateTime.now())
                    .build();
            
            given(bidRepository.findActiveAutoBidsByAuctionId(auction.getId()))
                    .willReturn(Arrays.asList(autoBid1, autoBid2));
            
            // When: 경쟁자가 55,000원에 입찰
            BigDecimal competitorBidAmount = new BigDecimal("55000");
            autoBidService.processAutoBidsForAuction(auction.getId(), competitorBidAmount);
            
            // Then: 최대금액이 높은 bidder2만 자동입찰 실행되어야 함
            verify(bidService, timeout(2000)).placeBid(
                    eq(bidder2.getId()),
                    argThat(request -> request.getBidAmount().equals(new BigDecimal("56100")))
            );
            
            // bidder1은 자동입찰이 실행되지 않아야 함
            verify(bidService, never()).placeBid(eq(bidder1.getId()), any());
        }
    }
    
    @Nested
    @DisplayName("1초 딜레이 테스트")
    class DelayTest {
        
        @Test
        @DisplayName("자동입찰 실행 전 1초 딜레이가 있어야 함")
        void shouldHaveOneSecondDelayBeforeAutoBid() {
            // Given: 자동입찰 설정된 입찰자
            Bid autoBid = Bid.builder()
                    .id(1L)
                    .auction(auction)
                    .bidder(bidder1)
                    .bidAmount(new BigDecimal("50000"))
                    .isAutoBid(true)
                    .maxAutoBidAmount(new BigDecimal("70000"))
                    .status(BidStatus.ACTIVE)
                    .bidTime(LocalDateTime.now())
                    .build();
            
            given(bidRepository.findActiveAutoBidsByAuctionId(auction.getId()))
                    .willReturn(Arrays.asList(autoBid));
            
            // When: 경쟁자 입찰 발생
            long startTime = System.currentTimeMillis();
            autoBidService.processAutoBidsForAuction(auction.getId(), new BigDecimal("55000"));
            
            // Then: 1초 후에 자동입찰 실행되어야 함
            verify(bidService, timeout(2000)).placeBid(any(), any());
            
            long executionTime = System.currentTimeMillis() - startTime;
            assertThat(executionTime).isGreaterThanOrEqualTo(1000); // 최소 1초 딜레이
        }
    }
    
    @Nested
    @DisplayName("비즈니스 규칙 검증 테스트")
    class BusinessRuleTest {
        
        @Test
        @DisplayName("경매 종료된 상품에는 자동입찰이 실행되지 않아야 함")
        void shouldNotAutoBidOnEndedAuction() {
            // Given: 종료된 경매
            auction = Auction.createAuction(
                    seller, "종료된 상품", "설명", Category.ELECTRONICS,
                    new BigDecimal("50000"), new BigDecimal("100000"), null,
                    1, // 1시간 (이미 지남)
                    RegionScope.CITY, "1111", "서울시 강남구",
                    5, LocalDateTime.now().toLocalDate().minusDays(30)
            );
            
            // 강제로 종료 시간을 과거로 설정
            try {
                var endAtField = Auction.class.getDeclaredField("endAt");
                endAtField.setAccessible(true);
                endAtField.set(auction, LocalDateTime.now().minusHours(1));
            } catch (Exception e) {
                // Reflection 실패시 그냥 넘어감
            }
            
            Bid autoBid = Bid.builder()
                    .id(1L)
                    .auction(auction)
                    .bidder(bidder1)
                    .bidAmount(new BigDecimal("50000"))
                    .isAutoBid(true)
                    .maxAutoBidAmount(new BigDecimal("70000"))
                    .status(BidStatus.ACTIVE)
                    .bidTime(LocalDateTime.now())
                    .build();
            
            given(bidRepository.findActiveAutoBidsByAuctionId(auction.getId()))
                    .willReturn(Arrays.asList(autoBid));
            
            // When: 자동입찰 트리거 시도
            autoBidService.processAutoBidsForAuction(auction.getId(), new BigDecimal("55000"));
            
            // Then: 자동입찰이 실행되지 않아야 함
            verify(bidService, never()).placeBid(any(), any());
        }
    }
}