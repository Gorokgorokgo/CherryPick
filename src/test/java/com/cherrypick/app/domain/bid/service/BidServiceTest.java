package com.cherrypick.app.domain.bid.service;

import com.cherrypick.app.common.exception.BusinessException;
import com.cherrypick.app.common.exception.EntityNotFoundException;
import com.cherrypick.app.common.exception.ErrorCode;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.bid.dto.request.PlaceBidRequest;
import com.cherrypick.app.domain.bid.dto.response.BidResponse;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.enums.BidStatus;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.point.entity.PointLock;
import com.cherrypick.app.domain.point.enums.PointLockStatus;
import com.cherrypick.app.domain.point.repository.PointLockRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * BidService 단위 테스트
 * 보안 시나리오 및 비즈니스 로직 검증 포함
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BidService 단위 테스트")
class BidServiceTest {

    @Mock
    private BidRepository bidRepository;
    
    @Mock
    private AuctionRepository auctionRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PointLockRepository pointLockRepository;
    
    @Mock
    private com.cherrypick.app.domain.common.service.WebSocketMessagingService webSocketMessagingService;
    
    @InjectMocks
    private BidService bidService;
    
    private User seller;
    private User bidder;
    private Auction activeAuction;
    private PlaceBidRequest validBidRequest;
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 설정
        seller = createUser(1L, "seller@test.com", "판매자", 100000L);
        bidder = createUser(2L, "bidder@test.com", "입찰자", 50000L);
        
        activeAuction = createAuction(1L, seller, BigDecimal.valueOf(10000), 
                AuctionStatus.ACTIVE, LocalDateTime.now().plusHours(2));
        
        validBidRequest = createBidRequest(1L, BigDecimal.valueOf(10500), false, null);
    }
    
    @Nested
    @DisplayName("입찰하기 - placeBid()")
    class PlaceBidTest {
        
        @Test
        @DisplayName("성공: 정상적인 입찰 처리")
        void placeBid_Success() {
            // given
            given(auctionRepository.findById(1L)).willReturn(Optional.of(activeAuction));
            given(userRepository.findById(2L)).willReturn(Optional.of(bidder));
            
            Bid savedBid = createBid(1L, activeAuction, bidder, BigDecimal.valueOf(10500));
            given(bidRepository.save(any(Bid.class))).willReturn(savedBid);
            given(auctionRepository.save(any(Auction.class))).willReturn(activeAuction);
            
            // when
            BidResponse result = bidService.placeBid(2L, validBidRequest);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.getBidAmount()).isEqualTo(BigDecimal.valueOf(10500));
            assertThat(result.getBidderId()).isEqualTo(2L);
            assertThat(result.getIsHighestBid()).isTrue();
            
            verify(bidRepository).save(any(Bid.class));
            verify(auctionRepository).save(any(Auction.class));
            // 포인트 예치 시스템 제거로 인해 pointLockRepository.save() 호출하지 않음
        }
        
        @Test
        @DisplayName("실패: 존재하지 않는 경매")
        void placeBid_AuctionNotFound() {
            // given
            given(auctionRepository.findById(1L)).willReturn(Optional.empty());
            
            // when & then
            assertThatThrownBy(() -> bidService.placeBid(2L, validBidRequest))
                    .isInstanceOf(EntityNotFoundException.class);
        }
        
        @Test
        @DisplayName("실패: 존재하지 않는 사용자")
        void placeBid_UserNotFound() {
            // given
            given(auctionRepository.findById(1L)).willReturn(Optional.of(activeAuction));
            given(userRepository.findById(2L)).willReturn(Optional.empty());
            
            // when & then
            assertThatThrownBy(() -> bidService.placeBid(2L, validBidRequest))
                    .isInstanceOf(EntityNotFoundException.class);
        }
        
        @Test
        @DisplayName("보안: 자신의 경매에 입찰 시도")
        void placeBid_SelfBidNotAllowed() {
            // given
            given(auctionRepository.findById(1L)).willReturn(Optional.of(activeAuction));
            given(userRepository.findById(1L)).willReturn(Optional.of(seller));
            
            // when & then
            assertThatThrownBy(() -> bidService.placeBid(1L, validBidRequest))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.SELF_BID_NOT_ALLOWED);
                    });
        }
        
        @Test
        @DisplayName("보안: 입찰 금액 부족 시 실제 금액 정보 노출 방지 - 추가 시나리오")
        void placeBid_InvalidBidAmount_AdditionalSecurityCheck() {
            // given
            PlaceBidRequest veryLowBidRequest = createBidRequest(1L, BigDecimal.valueOf(5000), false, null);
            given(auctionRepository.findById(1L)).willReturn(Optional.of(activeAuction));
            given(userRepository.findById(2L)).willReturn(Optional.of(bidder));
            
            // when & then
            assertThatThrownBy(() -> bidService.placeBid(2L, veryLowBidRequest))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_BID_AMOUNT);
                        // 실제 현재가나 최소 증가폭 정보가 노출되지 않는지 확인
                        assertThat(be.getMessage()).doesNotContain("10000");
                        assertThat(be.getMessage()).doesNotContain("1000");
                        assertThat(be.getMessage()).doesNotContain("11000");
                    });
        }
        
        @Test
        @DisplayName("실패: 비활성 경매에 입찰")
        void placeBid_InactiveAuction() {
            // given
            Auction inactiveAuction = createAuction(1L, seller, BigDecimal.valueOf(10000), 
                    AuctionStatus.ENDED, LocalDateTime.now().plusHours(2));
            given(auctionRepository.findById(1L)).willReturn(Optional.of(inactiveAuction));
            
            // when & then
            assertThatThrownBy(() -> bidService.placeBid(2L, validBidRequest))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.AUCTION_NOT_ACTIVE);
                    });
        }
        
        @Test
        @DisplayName("실패: 종료된 경매에 입찰")
        void placeBid_ExpiredAuction() {
            // given
            Auction expiredAuction = createAuction(1L, seller, BigDecimal.valueOf(10000), 
                    AuctionStatus.ACTIVE, LocalDateTime.now().minusHours(1));
            given(auctionRepository.findById(1L)).willReturn(Optional.of(expiredAuction));
            
            // when & then
            assertThatThrownBy(() -> bidService.placeBid(2L, validBidRequest))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.AUCTION_ENDED);
                    });
        }
        
        @Test
        @DisplayName("보안: 입찰 금액 부족 시 실제 금액 정보 노출 방지")
        void placeBid_InvalidBidAmount_NoSensitiveDataExposure() {
            // given
            PlaceBidRequest lowBidRequest = createBidRequest(1L, BigDecimal.valueOf(8000), false, null);
            given(auctionRepository.findById(1L)).willReturn(Optional.of(activeAuction));
            given(userRepository.findById(2L)).willReturn(Optional.of(bidder));
            
            // when & then
            assertThatThrownBy(() -> bidService.placeBid(2L, lowBidRequest))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INVALID_BID_AMOUNT);
                        // 실제 최소 입찰가 정보가 노출되지 않는지 확인
                        assertThat(be.getMessage()).doesNotContain("11000");
                        assertThat(be.getMessage()).doesNotContain("11,000");
                    });
        }
    }
    
    @Nested
    @DisplayName("최고가 입찰 조회 - getHighestBid()")
    class GetHighestBidTest {
        
        @Test
        @DisplayName("성공: 최고가 입찰 조회")
        void getHighestBid_Success() {
            // given
            Bid highestBid = createBid(1L, activeAuction, bidder, BigDecimal.valueOf(10500));
            given(bidRepository.findTopByAuctionIdOrderByBidAmountDesc(1L))
                    .willReturn(Optional.of(highestBid));
            
            // when
            BidResponse result = bidService.getHighestBid(1L);
            
            // then
            assertThat(result).isNotNull();
            assertThat(result.getBidAmount()).isEqualTo(BigDecimal.valueOf(10500));
            assertThat(result.getIsHighestBid()).isTrue();
        }
        
        @Test
        @DisplayName("실패: 입찰이 없는 경매")
        void getHighestBid_NoBidsExists() {
            // given
            given(bidRepository.findTopByAuctionIdOrderByBidAmountDesc(1L))
                    .willReturn(Optional.empty());
            
            // when & then
            assertThatThrownBy(() -> bidService.getHighestBid(1L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.NO_BID_EXISTS);
                    });
        }
    }
    
    // 테스트 헬퍼 메서드들
    private User createUser(Long id, String email, String nickname, Long pointBalance) {
        User user = User.builder()
                .email(email)
                .nickname(nickname)
                .pointBalance(pointBalance)
                .build();
        // Reflection을 사용하여 ID 설정 (테스트 전용)
        setFieldValue(user, "id", id);
        return user;
    }
    
    private Auction createAuction(Long id, User seller, BigDecimal currentPrice, 
                                 AuctionStatus status, LocalDateTime endAt) {
        // 정적 팩토리 메서드 사용
        Auction auction = Auction.createAuction(
                seller,
                "테스트 경매",
                "테스트 설명",
                Category.ELECTRONICS,
                currentPrice,
                currentPrice.add(BigDecimal.valueOf(5000)), // hopePrice
                null, // reservePrice
                24, // auctionTimeHours
                RegionScope.NATIONWIDE,
                null,
                null
        );
        
        // 테스트를 위한 필드 값 수정
        setFieldValue(auction, "id", id);
        setFieldValue(auction, "currentPrice", currentPrice);
        setFieldValue(auction, "status", status);
        setFieldValue(auction, "endAt", endAt);
        return auction;
    }
    
    private Bid createBid(Long id, Auction auction, User bidder, BigDecimal bidAmount) {
        Bid bid = Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .bidAmount(bidAmount)
                .status(BidStatus.ACTIVE)
                .bidTime(LocalDateTime.now())
                .build();
        setFieldValue(bid, "id", id);
        return bid;
    }
    
    private PlaceBidRequest createBidRequest(Long auctionId, BigDecimal bidAmount, 
                                           Boolean isAutoBid, BigDecimal maxAutoBidAmount) {
        PlaceBidRequest request = new PlaceBidRequest();
        request.setAuctionId(auctionId);
        request.setBidAmount(bidAmount);
        request.setIsAutoBid(isAutoBid);
        request.setMaxAutoBidAmount(maxAutoBidAmount);
        return request;
    }
    
    // Reflection을 사용한 필드 값 설정 (테스트 전용)
    private void setFieldValue(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            // BaseEntity의 필드인 경우 상위 클래스에서 찾기
            try {
                Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to set field: " + fieldName, ex);
            }
        }
    }
}