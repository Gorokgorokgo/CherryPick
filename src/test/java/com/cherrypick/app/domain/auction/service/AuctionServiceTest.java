package com.cherrypick.app.domain.auction.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.notification.event.AuctionSoldNotificationEvent;
import com.cherrypick.app.domain.notification.event.AuctionWonNotificationEvent;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuctionService 알림 중복 제거 테스트
 * processSuccessfulAuction에서 AuctionSoldNotificationEvent를 발행하지 않는지 검증
 */
@SpringBootTest
@Transactional
@RecordApplicationEvents
@TestPropertySource(properties = {
    "spring.jpa.show-sql=false",
    "logging.level.org.hibernate.SQL=WARN"
})
class AuctionServiceTest {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationEvents applicationEvents;

    private User seller;
    private User buyer;

    @BeforeEach
    void setUp() {
        // 고유한 전화번호 생성 (타임스탬프 기반)
        String timestamp = String.valueOf(System.currentTimeMillis() % 100000000); // 8자리

        // 테스트 사용자 생성
        seller = User.builder()
            .phoneNumber("010" + timestamp.substring(0, 8))
            .nickname("판매자_" + timestamp)
            .email("seller_" + timestamp + "@test.com")
            .password("password")
            .build();
        seller = userRepository.save(seller);

        buyer = User.builder()
            .phoneNumber("010" + (Long.parseLong(timestamp) + 1))
            .nickname("구매자_" + timestamp)
            .email("buyer_" + timestamp + "@test.com")
            .password("password")
            .build();
        buyer = userRepository.save(buyer);
    }

    @Test
    @DisplayName("processAuctionEnd 호출 시 AuctionSoldNotificationEvent를 발행하지 않음 (중복 방지)")
    void processAuctionEnd_shouldNotPublishAuctionSoldNotificationEvent() {
        // Given: Reserve Price가 없는 경매 생성
        Auction auction = createAuction(
            "테스트 경매",
            new BigDecimal("10000"),  // 시작가
            new BigDecimal("50000"),  // 희망가
            null,                     // Reserve Price 없음
            24
        );
        auction = auctionRepository.save(auction);

        BigDecimal highestBidPrice = new BigDecimal("30000");

        // When: 경매 종료 처리
        auctionService.processAuctionEnd(auction.getId(), highestBidPrice, buyer);

        // Then: 경매 상태 검증
        Auction endedAuction = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(endedAuction.getStatus()).isEqualTo(AuctionStatus.ENDED);
        assertThat(endedAuction.getWinner()).isEqualTo(buyer);
        assertThat(endedAuction.getCurrentPrice()).isEqualByComparingTo(highestBidPrice);

        // Then: AuctionSoldNotificationEvent가 발행되지 않음 검증
        long soldEventCount = applicationEvents.stream(AuctionSoldNotificationEvent.class).count();
        assertThat(soldEventCount).isEqualTo(0);

        // Then: AuctionWonNotificationEvent는 정상 발행됨 검증 (구매자용)
        long wonEventCount = applicationEvents.stream(AuctionWonNotificationEvent.class).count();
        assertThat(wonEventCount).isEqualTo(1);

        // Then: 구매자 알림 이벤트 데이터 검증
        AuctionWonNotificationEvent wonEvent = applicationEvents.stream(AuctionWonNotificationEvent.class)
            .findFirst()
            .orElseThrow();
        assertThat(wonEvent.getTargetUserId()).isEqualTo(buyer.getId());
        assertThat(wonEvent.getAuctionTitle()).isEqualTo("테스트 경매");
        assertThat(wonEvent.getFinalPrice()).isEqualTo(30000L);
    }

    @Test
    @DisplayName("Reserve Price 미달 시 유찰 처리되고 알림 이벤트 발행하지 않음")
    void processAuctionEnd_reservePriceNotMet_shouldNotPublishAnyNotificationEvent() {
        // Given: Reserve Price가 50,000원인 경매
        Auction auction = createAuction(
            "Reserve Price 테스트",
            new BigDecimal("10000"),  // 시작가
            new BigDecimal("100000"), // 희망가
            new BigDecimal("50000"),  // Reserve Price
            24
        );
        auction = auctionRepository.save(auction);

        BigDecimal highestBidPrice = new BigDecimal("40000"); // Reserve Price 미달

        // When: 경매 종료 처리
        auctionService.processAuctionEnd(auction.getId(), highestBidPrice, buyer);

        // Then: 유찰 처리 검증
        Auction endedAuction = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(endedAuction.getStatus()).isEqualTo(AuctionStatus.ENDED);
        assertThat(endedAuction.getWinner()).isNull();
        assertThat(endedAuction.getCurrentPrice()).isEqualByComparingTo(BigDecimal.ZERO);

        // Then: 어떤 알림 이벤트도 발행되지 않음 검증
        long soldEventCount = applicationEvents.stream(AuctionSoldNotificationEvent.class).count();
        long wonEventCount = applicationEvents.stream(AuctionWonNotificationEvent.class).count();
        assertThat(soldEventCount).isEqualTo(0);
        assertThat(wonEventCount).isEqualTo(0);
    }

    // === 헬퍼 메서드 ===

    private Auction createAuction(
        String title,
        BigDecimal startPrice,
        BigDecimal hopePrice,
        BigDecimal reservePrice,
        int auctionTimeHours
    ) {
        return Auction.createAuction(
            seller,
            title,
            "테스트 경매 설명",
            Category.ELECTRONICS,
            startPrice,
            hopePrice,
            reservePrice,
            auctionTimeHours,
            RegionScope.NATIONWIDE,
            null,
            null,
            5,
            "2024-01"
        );
    }

    @Test
    @DisplayName("통합 테스트: 경매 종료 시 중복 알림 발송 방지 검증")
    void integrationTest_noDuplicateNotifications() {
        // Given: 정상 낙찰 가능한 경매
        Auction auction = createAuction(
            "통합 테스트 경매",
            new BigDecimal("5000"),
            new BigDecimal("30000"),
            new BigDecimal("20000"),  // Reserve Price
            24
        );
        auction = auctionRepository.save(auction);

        BigDecimal finalPrice = new BigDecimal("25000"); // Reserve Price 초과

        // When: AuctionService.processAuctionEnd 호출
        auctionService.processAuctionEnd(auction.getId(), finalPrice, buyer);

        // Then: 전체 알림 이벤트 수 검증
        long totalNotificationEvents = applicationEvents.stream()
            .filter(event -> event instanceof AuctionSoldNotificationEvent || 
                           event instanceof AuctionWonNotificationEvent)
            .count();

        // AuctionService에서는 AuctionWonNotificationEvent만 발행해야 함
        assertThat(totalNotificationEvents).isEqualTo(1);
        
        long soldEvents = applicationEvents.stream(AuctionSoldNotificationEvent.class).count();
        long wonEvents = applicationEvents.stream(AuctionWonNotificationEvent.class).count();
        
        assertThat(soldEvents).isEqualTo(0); // 판매자 알림 없음
        assertThat(wonEvents).isEqualTo(1);  // 구매자 알림만 있음
    }
}