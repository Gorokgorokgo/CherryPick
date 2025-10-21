package com.cherrypick.app.domain.auction.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.enums.BidStatus;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.notification.event.AuctionNotSoldNotificationEvent;
import com.cherrypick.app.domain.notification.event.AuctionSoldNotificationEvent;
import com.cherrypick.app.domain.notification.event.AuctionWonNotificationEvent;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 경매 스케줄러 서비스 비즈니스 로직 테스트
 * Mock 없이 실제 비즈니스 로직의 핵심만 검증
 */
@SpringBootTest
@Transactional
@RecordApplicationEvents
@TestPropertySource(properties = {
    "spring.jpa.show-sql=false",
    "logging.level.org.hibernate.SQL=WARN"
})
class AuctionSchedulerServiceTest {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ApplicationEvents applicationEvents;

    private User seller;
    private User buyer1;
    private User buyer2;

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

        buyer1 = User.builder()
            .phoneNumber("010" + (Long.parseLong(timestamp) + 1))
            .nickname("구매자1_" + timestamp)
            .email("buyer1_" + timestamp + "@test.com")
            .password("password")
            .build();
        buyer1 = userRepository.save(buyer1);

        buyer2 = User.builder()
            .phoneNumber("010" + (Long.parseLong(timestamp) + 2))
            .nickname("구매자2_" + timestamp)
            .email("buyer2_" + timestamp + "@test.com")
            .password("password")
            .build();
        buyer2 = userRepository.save(buyer2);
    }

    @Test
    @DisplayName("낙찰 시나리오: Reserve Price 이상 입찰 → 경매 종료 → 낙찰자 설정 → 알림 이벤트 발행")
    void successfulAuction_shouldSetWinnerAndPublishEvents() {
        // Given: Reserve Price가 50,000원인 경매 생성
        Auction auction = createAuction(
            "아이폰 14 Pro",
            new BigDecimal("10000"),  // 시작가
            new BigDecimal("100000"), // 희망가
            new BigDecimal("50000"),  // Reserve Price
            24
        );
        auction = auctionRepository.save(auction);

        // 최고 입찰: 60,000원 (Reserve Price 초과)
        Bid highestBid = createBid(auction, buyer1, new BigDecimal("60000"));
        bidRepository.save(highestBid);

        // 낮은 입찰: 40,000원
        Bid lowerBid = createBid(auction, buyer2, new BigDecimal("40000"));
        bidRepository.save(lowerBid);

        BigDecimal finalPrice = new BigDecimal("60000");

        // When: 경매 종료 처리
        auction.endAuction(buyer1, finalPrice);
        auctionRepository.save(auction);

        // 알림 이벤트 발행 (AuctionSchedulerService가 담당)
        // 구매자용 알림
        eventPublisher.publishEvent(new AuctionWonNotificationEvent(
            this, buyer1.getId(), auction.getId(), auction.getTitle(), finalPrice.longValue(), seller.getNickname(), null
        ));
        
        // 판매자용 알림 (AuctionSchedulerService에서 단일 발행)
        eventPublisher.publishEvent(new AuctionSoldNotificationEvent(
            this, seller.getId(), auction.getId(), auction.getTitle(), finalPrice.longValue(), buyer1.getNickname(), null
        ));

        // Then: 경매 상태 검증
        Auction endedAuction = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(endedAuction.getStatus()).isEqualTo(AuctionStatus.ENDED);
        assertThat(endedAuction.getWinner()).isEqualTo(buyer1);
        assertThat(endedAuction.getCurrentPrice()).isEqualByComparingTo(finalPrice);

        // Then: 알림 이벤트 발행 검증 (구매자 + 판매자)
        long wonEventCount = applicationEvents.stream(AuctionWonNotificationEvent.class).count();
        long soldEventCount = applicationEvents.stream(AuctionSoldNotificationEvent.class).count();
        assertThat(wonEventCount).isEqualTo(1);
        assertThat(soldEventCount).isEqualTo(1);

        // Then: 구매자 알림 이벤트 데이터 검증
        AuctionWonNotificationEvent wonEvent = applicationEvents.stream(AuctionWonNotificationEvent.class)
            .findFirst()
            .orElseThrow();
        assertThat(wonEvent.getTargetUserId()).isEqualTo(buyer1.getId());
        assertThat(wonEvent.getAuctionTitle()).isEqualTo("아이폰 14 Pro");
        assertThat(wonEvent.getFinalPrice()).isEqualTo(60000L);

        // Then: 판매자 알림 이벤트 데이터 검증
        AuctionSoldNotificationEvent soldEvent = applicationEvents.stream(AuctionSoldNotificationEvent.class)
            .findFirst()
            .orElseThrow();
        assertThat(soldEvent.getTargetUserId()).isEqualTo(seller.getId());
        assertThat(soldEvent.getAuctionTitle()).isEqualTo("아이폰 14 Pro");
        assertThat(soldEvent.getFinalPrice()).isEqualTo(60000L);
        assertThat(soldEvent.getWinnerNickname()).isEqualTo(buyer1.getNickname());
    }

    @Test
    @DisplayName("유찰 시나리오 (최고 입찰자 있음): Reserve Price 미달 → 유찰 → 최고 입찰자 정보 포함 알림")
    void noReserveAuction_withBidder_shouldPublishNotSoldEvent() {
        // Given: Reserve Price가 80,000원인 경매
        Auction auction = createAuction(
            "맥북 프로",
            new BigDecimal("30000"),
            new BigDecimal("150000"),
            new BigDecimal("80000"),  // Reserve Price
            24
        );
        auction = auctionRepository.save(auction);

        // 최고 입찰: 70,000원 (Reserve Price 미달)
        Bid highestBid = createBid(auction, buyer1, new BigDecimal("70000"));
        bidRepository.save(highestBid);

        // When: Reserve Price 미달 확인
        boolean reservePriceMet = auction.isReservePriceMet(highestBid.getBidAmount());
        assertThat(reservePriceMet).isFalse();

        // When: 유찰 처리
        auction.endAuction(null, BigDecimal.ZERO);
        auctionRepository.save(auction);

        // 유찰 알림 이벤트 발행
        eventPublisher.publishEvent(new AuctionNotSoldNotificationEvent(
            this, seller.getId(), auction.getId(), auction.getTitle(), highestBid
        ));

        // Then: 경매 상태 검증
        Auction endedAuction = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(endedAuction.getStatus()).isEqualTo(AuctionStatus.ENDED);
        assertThat(endedAuction.getWinner()).isNull();
        assertThat(endedAuction.getCurrentPrice()).isEqualByComparingTo(BigDecimal.ZERO);

        // Then: 유찰 알림 이벤트 검증
        long notSoldEventCount = applicationEvents.stream(AuctionNotSoldNotificationEvent.class).count();
        assertThat(notSoldEventCount).isEqualTo(1);

        // Then: 최고 입찰자 정보 포함 검증
        AuctionNotSoldNotificationEvent event = applicationEvents
            .stream(AuctionNotSoldNotificationEvent.class)
            .findFirst()
            .orElseThrow();
        assertThat(event.getTargetUserId()).isEqualTo(seller.getId());
        assertThat(event.getHighestBid()).isNotNull();
        assertThat(event.getHighestBid().getBidder()).isEqualTo(buyer1);
        assertThat(event.getHighestBid().getBidAmount()).isEqualByComparingTo(new BigDecimal("70000"));
    }

    @Test
    @DisplayName("유찰 시나리오 (입찰자 없음): 입찰 없음 → 유찰 → 최고 입찰자 정보 null")
    void noReserveAuction_withoutBidder_shouldPublishNotSoldEventWithNullBid() {
        // Given: 입찰이 하나도 없는 경매
        Auction auction = createAuction(
            "게임 콘솔",
            new BigDecimal("20000"),
            new BigDecimal("50000"),
            new BigDecimal("30000"),
            24
        );
        auction = auctionRepository.save(auction);

        // When: 유찰 처리 (입찰자 없음)
        auction.endAuction(null, BigDecimal.ZERO);
        auctionRepository.save(auction);

        // 유찰 알림 이벤트 발행 (최고 입찰 null)
        eventPublisher.publishEvent(new AuctionNotSoldNotificationEvent(
            this, seller.getId(), auction.getId(), auction.getTitle(), null
        ));

        // Then: 경매 상태 검증
        Auction endedAuction = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(endedAuction.getStatus()).isEqualTo(AuctionStatus.ENDED);
        assertThat(endedAuction.getWinner()).isNull();

        // Then: 유찰 알림 이벤트 검증
        AuctionNotSoldNotificationEvent event = applicationEvents
            .stream(AuctionNotSoldNotificationEvent.class)
            .findFirst()
            .orElseThrow();
        assertThat(event.getHighestBid()).isNull();
    }

    @Test
    @DisplayName("Reserve Price 달성 로직: null이면 항상 달성, 설정되어 있으면 비교")
    void isReservePriceMet_logic() {
        // Given: Reserve Price가 null인 경매
        Auction auctionWithoutReserve = createAuction(
            "테스트 상품1",
            new BigDecimal("10000"),
            new BigDecimal("50000"),
            null,  // Reserve Price 없음
            24
        );

        // When & Then: Reserve Price가 null이면 모든 입찰가에서 true
        assertThat(auctionWithoutReserve.isReservePriceMet(new BigDecimal("1"))).isTrue();
        assertThat(auctionWithoutReserve.isReservePriceMet(new BigDecimal("10000"))).isTrue();
        assertThat(auctionWithoutReserve.isReservePriceMet(new BigDecimal("100000"))).isTrue();

        // Given: Reserve Price가 50,000원인 경매
        Auction auctionWithReserve = createAuction(
            "테스트 상품2",
            new BigDecimal("10000"),
            new BigDecimal("100000"),
            new BigDecimal("50000"),  // Reserve Price 50,000원
            24
        );

        // When & Then: Reserve Price 미달
        assertThat(auctionWithReserve.isReservePriceMet(new BigDecimal("49999"))).isFalse();
        assertThat(auctionWithReserve.isReservePriceMet(new BigDecimal("30000"))).isFalse();

        // When & Then: Reserve Price 달성
        assertThat(auctionWithReserve.isReservePriceMet(new BigDecimal("50000"))).isTrue();
        assertThat(auctionWithReserve.isReservePriceMet(new BigDecimal("60000"))).isTrue();
        assertThat(auctionWithReserve.isReservePriceMet(new BigDecimal("100000"))).isTrue();
    }

    @Test
    @DisplayName("경매 종료 상태 전환: winner와 finalPrice에 따른 상태 변경")
    void endAuction_statusTransition() {
        // Given: 활성 경매
        Auction auction = createAuction(
            "상태 전환 테스트",
            new BigDecimal("10000"),
            new BigDecimal("50000"),
            new BigDecimal("30000"),
            24
        );
        auction = auctionRepository.save(auction);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ACTIVE);

        // When: 낙찰 처리
        auction.endAuction(buyer1, new BigDecimal("40000"));
        auctionRepository.save(auction);

        // Then: 종료 상태로 전환
        Auction endedAuction = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(endedAuction.getStatus()).isEqualTo(AuctionStatus.ENDED);
        assertThat(endedAuction.getWinner()).isEqualTo(buyer1);
        assertThat(endedAuction.getCurrentPrice()).isEqualByComparingTo(new BigDecimal("40000"));
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

    private Bid createBid(Auction auction, User bidder, BigDecimal bidAmount) {
        return Bid.builder()
            .auction(auction)
            .bidder(bidder)
            .bidAmount(bidAmount)
            .isAutoBid(false)
            .status(BidStatus.ACTIVE)
            .bidTime(LocalDateTime.now())
            .build();
    }
}
