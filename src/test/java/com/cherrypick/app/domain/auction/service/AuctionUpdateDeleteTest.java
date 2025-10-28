package com.cherrypick.app.domain.auction.service;

import com.cherrypick.app.common.exception.BusinessException;
import com.cherrypick.app.domain.auction.dto.AuctionResponse;
import com.cherrypick.app.domain.auction.dto.UpdateAuctionRequest;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("경매 수정/삭제 핵심 비즈니스 로직 테스트")
class AuctionUpdateDeleteTest {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BidRepository bidRepository;

    private User seller;
    private User otherUser;
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

        otherUser = User.builder()
                .email("other@test.com")
                .nickname("타사용자")
                .password("password")
                .phoneNumber("01011111111")
                .build();
        otherUser = userRepository.save(otherUser);

        auction = Auction.createAuction(
                seller,
                "테스트 경매",
                "상품 설명",
                Category.ELECTRONICS,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(50000),
                null,
                24,
                RegionScope.CITY,
                "11",
                "서울시",
                8,
                "2024-01-01"
        );
        auction = auctionRepository.save(auction);
    }

    @Test
    @DisplayName("경매 수정 성공 - 판매자가 입찰 없는 경매를 수정")
    void updateAuction_Success() {
        // given
        UpdateAuctionRequest updateRequest = UpdateAuctionRequest.builder()
                .title("수정된 제목")
                .description("수정된 설명")
                .build();

        // when
        AuctionResponse response = auctionService.updateAuction(auction.getId(), seller.getId(), updateRequest);

        // then
        assertThat(response.getTitle()).isEqualTo("수정된 제목");
        assertThat(response.getDescription()).isEqualTo("수정된 설명");
    }

    @Test
    @DisplayName("경매 수정 실패 - 권한 없음 (타 사용자)")
    void updateAuction_Fail_Unauthorized() {
        // given
        UpdateAuctionRequest updateRequest = UpdateAuctionRequest.builder()
                .title("수정된 제목")
                .build();

        // when & then
        assertThatThrownBy(() -> auctionService.updateAuction(auction.getId(), otherUser.getId(), updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("권한");
    }

    @Test
    @DisplayName("경매 수정 실패 - 입찰이 이미 있는 경매")
    void updateAuction_Fail_HasBids() {
        // given
        User bidder = User.builder()
                .email("bidder@test.com")
                .nickname("입찰자")
                .password("password")
                .phoneNumber("01099999999")
                .build();
        bidder = userRepository.save(bidder);

        Bid bid = Bid.createManualBid(auction, bidder, BigDecimal.valueOf(15000));
        bidRepository.save(bid);

        UpdateAuctionRequest updateRequest = UpdateAuctionRequest.builder()
                .title("수정된 제목")
                .build();

        // when & then
        assertThatThrownBy(() -> auctionService.updateAuction(auction.getId(), seller.getId(), updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("입찰");
    }

    @Test
    @DisplayName("경매 수정 실패 - 종료된 경매")
    void updateAuction_Fail_EndedAuction() {
        // given
        auction.endAuction(null, BigDecimal.ZERO);
        auctionRepository.save(auction);

        UpdateAuctionRequest updateRequest = UpdateAuctionRequest.builder()
                .title("수정된 제목")
                .build();

        // when & then
        assertThatThrownBy(() -> auctionService.updateAuction(auction.getId(), seller.getId(), updateRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("종료");
    }

    @Test
    @DisplayName("경매 수정 - 부분 수정 (제목만)")
    void updateAuction_Partial_TitleOnly() {
        // given
        String originalDescription = auction.getDescription();
        UpdateAuctionRequest updateRequest = UpdateAuctionRequest.builder()
                .title("새 제목")
                .build();

        // when
        AuctionResponse response = auctionService.updateAuction(auction.getId(), seller.getId(), updateRequest);

        // then
        assertThat(response.getTitle()).isEqualTo("새 제목");
        assertThat(response.getDescription()).isEqualTo(originalDescription);
    }

    @Test
    @DisplayName("경매 삭제 성공 - 판매자가 입찰 없는 경매를 삭제")
    void deleteAuction_Success() {
        // when
        auctionService.deleteAuction(auction.getId(), seller.getId());

        // then
        Auction deletedAuction = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(deletedAuction.getStatus()).isEqualTo(AuctionStatus.DELETED);
    }

    @Test
    @DisplayName("경매 삭제 실패 - 권한 없음 (타 사용자)")
    void deleteAuction_Fail_Unauthorized() {
        // when & then
        assertThatThrownBy(() -> auctionService.deleteAuction(auction.getId(), otherUser.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("권한");
    }

    @Test
    @DisplayName("경매 삭제 실패 - 입찰이 이미 있는 경매")
    void deleteAuction_Fail_HasBids() {
        // given
        User bidder = User.builder()
                .email("bidder@test.com")
                .nickname("입찰자")
                .password("password")
                .phoneNumber("01099999999")
                .build();
        bidder = userRepository.save(bidder);

        Bid bid = Bid.createManualBid(auction, bidder, BigDecimal.valueOf(15000));
        bidRepository.save(bid);

        // when & then
        assertThatThrownBy(() -> auctionService.deleteAuction(auction.getId(), seller.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("입찰");
    }

    @Test
    @DisplayName("경매 삭제 실패 - 이미 종료된 경매")
    void deleteAuction_Fail_EndedAuction() {
        // given
        auction.endAuction(null, BigDecimal.ZERO);
        auctionRepository.save(auction);

        // when & then
        assertThatThrownBy(() -> auctionService.deleteAuction(auction.getId(), seller.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("종료");
    }

    @Test
    @DisplayName("경매 삭제는 실제 삭제가 아닌 상태 변경 (소프트 삭제)")
    void deleteAuction_SoftDelete() {
        // when
        auctionService.deleteAuction(auction.getId(), seller.getId());

        // then
        assertThat(auctionRepository.findById(auction.getId())).isPresent();
        Auction deletedAuction = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(deletedAuction.getStatus()).isEqualTo(AuctionStatus.DELETED);
    }

    @Test
    @DisplayName("삭제된 경매는 목록에 노출되지 않음")
    void deletedAuction_NotInList() {
        // when
        auctionService.deleteAuction(auction.getId(), seller.getId());

        // then
        List<Auction> activeAuctions = auctionRepository.findByStatusOrderByCreatedAtDesc(AuctionStatus.ACTIVE, org.springframework.data.domain.PageRequest.of(0, 100))
                .getContent();
        assertThat(activeAuctions).doesNotContain(auction);
    }
}
