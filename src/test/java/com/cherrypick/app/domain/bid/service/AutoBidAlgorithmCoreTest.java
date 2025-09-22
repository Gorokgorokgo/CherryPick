package com.cherrypick.app.domain.bid.service;

import com.cherrypick.app.domain.auction.entity.Auction;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoBid 알고리즘 핵심 단위 테스트")
class AutoBidAlgorithmCoreTest {

    @Mock private BidRepository bidRepository;
    @Mock private AuctionRepository auctionRepository;
    @Mock private UserRepository userRepository;
    @Mock private WebSocketMessagingService webSocketMessagingService;

    @InjectMocks
    private AutoBidService autoBidService;

    private User seller;
    private User a; // 자동입찰자 A
    private User b; // 자동입찰자 B
    private Auction auction;

    @BeforeEach
    void setUp() {
        seller = User.builder().id(1L).nickname("판매자").build();
        a = User.builder().id(10L).nickname("A").build();
        b = User.builder().id(20L).nickname("B").build();

        auction = Auction.createAuction(
                seller,
                "테스트 경매",
                "설명",
                Category.ELECTRONICS,
                new BigDecimal("65000"), // 시작가
                new BigDecimal("200000"),
                null,
                24,
                RegionScope.NATIONWIDE,
                null,
                null,
                5,
                (String) null
        );
        // 현재가를 65,000으로 명확히 설정
        auction.updateCurrentPrice(new BigDecimal("65000"));

        given(auctionRepository.findByIdForUpdate(1L)).willReturn(Optional.of(auction));
        given(auctionRepository.findById(1L)).willReturn(Optional.of(auction));
        // 현재 최고입찰 없음(초기)
        given(bidRepository.findTopByAuctionIdOrderByBidAmountDesc(1L)).willReturn(Optional.empty());
    }

    @Test
    @DisplayName("복수 자동입찰자 경쟁: B 80,000 vs A 100,000 -> 저장 2건(B:80,000, A:81,000), 현재가 81,000")
    void competition_TwoAutoBidders_FinalTwoOnly() {
        // 활성 자동입찰 설정 (설정 레코드: bidAmount=0)
        Bid bConfig = Bid.builder()
                .id(101L).auction(auction).bidder(b)
                .bidAmount(BigDecimal.ZERO)
                .isAutoBid(true)
                .maxAutoBidAmount(new BigDecimal("80000"))
                .status(BidStatus.ACTIVE)
                .bidTime(LocalDateTime.now())
                .build();
        Bid aConfig = Bid.builder()
                .id(102L).auction(auction).bidder(a)
                .bidAmount(BigDecimal.ZERO)
                .isAutoBid(true)
                .maxAutoBidAmount(new BigDecimal("100000"))
                .status(BidStatus.ACTIVE)
                .bidTime(LocalDateTime.now())
                .build();

        given(bidRepository.findActiveAutoBidsByAuctionId(1L)).willReturn(List.of(aConfig, bConfig));

        // 실행: 새 수동입찰 직후 경쟁 시뮬레이션
        boolean result = autoBidService.processCompetitionAfterNewBid(1L);
        assertThat(result).isTrue();

        // 저장된 입찰 금액 캡처
        ArgumentCaptor<Bid> bidCaptor = ArgumentCaptor.forClass(Bid.class);
        then(bidRepository).should().save(bidCaptor.capture());
        then(bidRepository).should().save(bidCaptor.capture());

        List<Bid> saved = bidCaptor.getAllValues();
        assertThat(saved).hasSize(2);
        // 패자: B 80,000, 승자: A 81,000
        assertThat(saved.stream().anyMatch(x -> x.getBidder().getId().equals(20L)
                && x.getBidAmount().compareTo(new BigDecimal("80000")) == 0)).isTrue();
        assertThat(saved.stream().anyMatch(x -> x.getBidder().getId().equals(10L)
                && x.getBidAmount().compareTo(new BigDecimal("81000")) == 0)).isTrue();
    }

    @Test
    @DisplayName("단독 자동입찰자: 현재가 50,000, 최대 70,000 -> 51,000 자동입찰 1건만 저장")
    void singleAutoBidder_MinIncrementOnce() {
        Auction auc = Auction.createAuction(
                seller,
                "테스트 경매2",
                "설명",
                Category.ELECTRONICS,
                new BigDecimal("50000"),
                new BigDecimal("200000"),
                null,
                24,
                RegionScope.NATIONWIDE,
                null,
                null,
                5,
                (String) null
        );
        auc.updateCurrentPrice(new BigDecimal("50000"));

        given(auctionRepository.findByIdForUpdate(2L)).willReturn(Optional.of(auc));
        given(bidRepository.findTopByAuctionIdOrderByBidAmountDesc(2L)).willReturn(Optional.empty());

        Bid aOnly = Bid.builder()
                .id(201L).auction(auc).bidder(a)
                .bidAmount(BigDecimal.ZERO)
                .isAutoBid(true)
                .maxAutoBidAmount(new BigDecimal("70000"))
                .status(BidStatus.ACTIVE)
                .bidTime(LocalDateTime.now())
                .build();

        given(bidRepository.findActiveAutoBidsByAuctionId(2L)).willReturn(List.of(aOnly));

        boolean result = autoBidService.processCompetitionAfterNewBid(2L);
        assertThat(result).isTrue();

        ArgumentCaptor<Bid> bidCaptor = ArgumentCaptor.forClass(Bid.class);
        then(bidRepository).should().save(bidCaptor.capture());
        then(bidRepository).should(never()).save(argThat(bid -> bid != bidCaptor.getValue()));

        Bid saved = bidCaptor.getValue();
        assertThat(saved.getBidder().getId()).isEqualTo(10L);
        assertThat(saved.getBidAmount()).isEqualByComparingTo(new BigDecimal("51000"));
    }

    @Test
    @DisplayName("자동입찰 설정 직후: 첫 입찰 상황이면 시작가로 즉시 입찰")
    void triggerImmediate_FirstBid_AtStartPrice() {
        Auction auc = Auction.createAuction(
                seller,
                "테스트 경매3",
                "설명",
                Category.ELECTRONICS,
                new BigDecimal("10000"),
                new BigDecimal("50000"),
                null,
                24,
                RegionScope.NATIONWIDE,
                null,
                null,
                5,
                (String) null
        );
        // 첫 입찰 상황: current=start, bidCount=0 (생성 직후 상태)
        given(auctionRepository.findByIdForUpdate(3L)).willReturn(Optional.of(auc));
        given(bidRepository.findTopByAuctionIdOrderByBidAmountDesc(3L)).willReturn(Optional.empty());

        // 설정 레코드(방금 생성된 것으로 가정)
        Bid config = Bid.builder()
                .id(301L).auction(auc).bidder(b)
                .bidAmount(BigDecimal.ZERO)
                .isAutoBid(true)
                .maxAutoBidAmount(new BigDecimal("30000"))
                .status(BidStatus.ACTIVE)
                .bidTime(LocalDateTime.now())
                .build();
        given(bidRepository.findActiveAutoBidsByAuctionId(3L)).willReturn(List.of(config));

        boolean triggered = autoBidService.triggerImmediateBidOnSetup(3L, 20L);
        assertThat(triggered).isTrue();

        ArgumentCaptor<Bid> bidCaptor = ArgumentCaptor.forClass(Bid.class);
        then(bidRepository).should().save(bidCaptor.capture());
        Bid saved = bidCaptor.getValue();
        assertThat(saved.getBidder().getId()).isEqualTo(20L);
        assertThat(saved.getBidAmount()).isEqualByComparingTo(new BigDecimal("10000"));
    }
}
