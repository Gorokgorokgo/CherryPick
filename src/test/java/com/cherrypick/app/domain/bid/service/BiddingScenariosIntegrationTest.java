package com.cherrypick.app.domain.bid.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.bid.dto.request.PlaceBidRequest;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("입찰 시나리오 통합 테스트 (PostgreSQL)")
public class BiddingScenariosIntegrationTest {

    @Autowired private BidService bidService;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private BidRepository bidRepository;
    @Autowired private UserRepository userRepository;

    private User newUser(String nicknamePrefix) {
        String uniq = UUID.randomUUID().toString().substring(0, 8);
        User u = User.builder()
                .phoneNumber("010" + (System.nanoTime()%90000000 + 10000000))
                .nickname(nicknamePrefix + uniq)
                .email(nicknamePrefix.toLowerCase() + uniq + "@test.com")
                .password("test")
                .build();
        return userRepository.save(u);
    }

    private Auction newAuction(int start) {
        User seller = newUser("SELLER-");
        Auction a = Auction.createAuction(
                seller,
                "시나리오 테스트 경매",
                "설명",
                Category.ELECTRONICS,
                new BigDecimal(start),
                new BigDecimal(start + 200000),
                null,
                24,
                RegionScope.CITY,
                "1111",
                "서울",
                5,
                LocalDateTime.now().toLocalDate().minusDays(7)
        );
        return auctionRepository.save(a);
    }

    private List<Bid> actualBids(Long auctionId) {
        return bidRepository
                .findByAuctionIdOrderByBidAmountDesc(auctionId, org.springframework.data.domain.Pageable.unpaged())
                .getContent();
    }

    @Test
    @DisplayName("Case 1: 수동입찰만 → C 27,000 최고")
    void case1_manualOnly() {
        Auction auction = newAuction(25000);
        User A = newUser("A-");
        User B = newUser("B-");
        User C = newUser("C-");

        PlaceBidRequest r = new PlaceBidRequest();
        r.setAuctionId(auction.getId());
        r.setIsAutoBid(false);

        r.setBidAmount(new BigDecimal("25000"));
        bidService.placeBid(A.getId(), r);
        r.setBidAmount(new BigDecimal("26000"));
        bidService.placeBid(B.getId(), r);
        r.setBidAmount(new BigDecimal("27000"));
        bidService.placeBid(C.getId(), r);

        List<Bid> bids = actualBids(auction.getId());
        Bid highest = bids.get(0);
        assertThat(highest.getBidAmount()).isEqualByComparingTo("27000");
        assertThat(highest.getBidder().getId()).isEqualTo(C.getId());
        assertThat(highest.getIsAutoBid()).isFalse();
    }

    @Test
    @DisplayName("Case 2: A만 자동입찰 → A 29,000 최고")
    void case2_AutoOnlyA() {
        Auction auction = newAuction(25000);
        User A = newUser("A-");
        User B = newUser("B-");
        User C = newUser("C-");

        bidService.setupAutoBid(A.getId(), auction.getId(), new BigDecimal("30000"));

        PlaceBidRequest r = new PlaceBidRequest();
        r.setAuctionId(auction.getId());
        r.setIsAutoBid(false);

        r.setBidAmount(new BigDecimal("26000"));
        bidService.placeBid(B.getId(), r);
        await().atMost(java.time.Duration.ofSeconds(3)).untilAsserted(() -> {
            assertThat(actualBids(auction.getId()).get(0).getBidAmount()).isEqualByComparingTo("27000");
        });

        r.setBidAmount(new BigDecimal("28000"));
        bidService.placeBid(C.getId(), r);
        await().atMost(java.time.Duration.ofSeconds(3)).untilAsserted(() -> {
            Bid highest = actualBids(auction.getId()).get(0);
            assertThat(highest.getBidAmount()).isEqualByComparingTo("29000");
            assertThat(highest.getBidder().getId()).isEqualTo(A.getId());
        });
    }

    @Test
    @DisplayName("Case 3: A,B 자동 경쟁 → B 30,000, A 31,000 저장; C 32,000 → A 33,000")
    void case3_ABCompetition_then_CManual() {
        Auction auction = newAuction(25000);
        User A = newUser("A-");
        User B = newUser("B-");
        User C = newUser("C-");

        bidService.setupAutoBid(A.getId(), auction.getId(), new BigDecimal("35000"));
        bidService.setupAutoBid(B.getId(), auction.getId(), new BigDecimal("30000"));
        await().atMost(java.time.Duration.ofSeconds(3)).untilAsserted(() -> {
            List<Bid> bids = actualBids(auction.getId());
            assertThat(bids.stream().anyMatch(x -> x.getBidAmount().compareTo(new BigDecimal("30000"))==0 && x.getBidder().getId().equals(B.getId()))).isTrue();
            assertThat(bids.stream().anyMatch(x -> x.getBidAmount().compareTo(new BigDecimal("31000"))==0 && x.getBidder().getId().equals(A.getId()))).isTrue();
        });

        PlaceBidRequest r = new PlaceBidRequest();
        r.setAuctionId(auction.getId());
        r.setIsAutoBid(false);
        r.setBidAmount(new BigDecimal("32000"));
        bidService.placeBid(C.getId(), r);
        await().atMost(java.time.Duration.ofSeconds(3)).untilAsserted(() -> {
            Bid highest = actualBids(auction.getId()).get(0);
            assertThat(highest.getBidAmount()).isEqualByComparingTo("33000");
            assertThat(highest.getBidder().getId()).isEqualTo(A.getId());
        });
    }

    @Test
    @DisplayName("Case 4: 동일 최대금액 자동입찰 → 선설정 A 승리(30,000) → C 31,000 수동으로 최고")
    void case4_sameMax() {
        Auction auction = newAuction(25000);
        User A = newUser("A-");
        User B = newUser("B-");
        User C = newUser("C-");

        bidService.setupAutoBid(A.getId(), auction.getId(), new BigDecimal("30000"));
        bidService.setupAutoBid(B.getId(), auction.getId(), new BigDecimal("30000"));
        await().atMost(java.time.Duration.ofSeconds(3)).untilAsserted(() -> {
            Bid highest = actualBids(auction.getId()).get(0);
            assertThat(highest.getBidAmount()).isEqualByComparingTo("30000");
            assertThat(highest.getBidder().getId()).isEqualTo(A.getId());
        });

        PlaceBidRequest r = new PlaceBidRequest();
        r.setAuctionId(auction.getId());
        r.setIsAutoBid(false);
        r.setBidAmount(new BigDecimal("31000"));
        bidService.placeBid(C.getId(), r);
        await().atMost(java.time.Duration.ofSeconds(3)).untilAsserted(() -> {
            Bid highest = actualBids(auction.getId()).get(0);
            assertThat(highest.getBidAmount()).isEqualByComparingTo("31000");
            assertThat(highest.getBidder().getId()).isEqualTo(C.getId());
        });
    }

    @Test
    @DisplayName("Case 5: 3자 자동입찰 경쟁 → 최종 A 36,000")
    void case5_threeWayCompetition() {
        Auction auction = newAuction(25000);
        User A = newUser("A-");
        User B = newUser("B-");
        User C = newUser("C-");

        bidService.setupAutoBid(A.getId(), auction.getId(), new BigDecimal("40000"));
        bidService.setupAutoBid(B.getId(), auction.getId(), new BigDecimal("35000"));
        bidService.setupAutoBid(C.getId(), auction.getId(), new BigDecimal("30000"));

        await().atMost(java.time.Duration.ofSeconds(3)).untilAsserted(() -> {
            Bid highest = actualBids(auction.getId()).get(0);
            assertThat(highest.getBidAmount()).isEqualByComparingTo("36000");
            assertThat(highest.getBidder().getId()).isEqualTo(A.getId());
        });
    }

    @Test
    @DisplayName("Case 6: 혼합 패턴(수동→자동→수동) → 최종 B 31,000")
    void case6_mixed() {
        Auction auction = newAuction(25000);
        User A = newUser("A-");
        User B = newUser("B-");
        User C = newUser("C-");

        PlaceBidRequest r = new PlaceBidRequest();
        r.setAuctionId(auction.getId());
        r.setIsAutoBid(false);
        r.setBidAmount(new BigDecimal("25000"));
        bidService.placeBid(A.getId(), r);

        bidService.setupAutoBid(B.getId(), auction.getId(), new BigDecimal("35000"));
        await().atMost(java.time.Duration.ofSeconds(3)).untilAsserted(() -> {
            assertThat(actualBids(auction.getId()).get(0).getBidAmount()).isEqualByComparingTo("26000");
        });

        r.setBidAmount(new BigDecimal("30000"));
        bidService.placeBid(C.getId(), r);
        await().atMost(java.time.Duration.ofSeconds(3)).untilAsserted(() -> {
            Bid highest = actualBids(auction.getId()).get(0);
            assertThat(highest.getBidAmount()).isEqualByComparingTo("31000");
            assertThat(highest.getBidder().getId()).isEqualTo(B.getId());
        });
    }
}
