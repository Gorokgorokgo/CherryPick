package com.cherrypick.app.domain.websocket.service;

import com.cherrypick.app.domain.websocket.dto.AuctionUpdateMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WebSocket 메시징 서비스 단위 테스트
 * TDD Red-Green-Refactor 패턴 적용
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocket 메시징 서비스 테스트")
class WebSocketMessagingServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketMessagingService webSocketMessagingService;

    @Test
    @DisplayName("새로운 입찰 발생시 올바른 메시지가 전송된다")
    void should_SendCorrectNewBidMessage_When_NewBidPlaced() {
        // Given: 새로운 입찰 정보
        Long auctionId = 1L;
        BigDecimal currentPrice = new BigDecimal("15000");
        Integer bidCount = 5;
        String bidderNickname = "입찰자123";
        
        // When: 새로운 입찰 알림 전송
        webSocketMessagingService.notifyNewBid(auctionId, currentPrice, bidCount, bidderNickname);
        
        // Then: 올바른 메시지가 올바른 토픽으로 전송되어야 함
        ArgumentCaptor<AuctionUpdateMessage> messageCaptor = 
            ArgumentCaptor.forClass(AuctionUpdateMessage.class);
        verify(messagingTemplate).convertAndSend(
            eq("/topic/auctions/1"), 
            messageCaptor.capture()
        );
        
        AuctionUpdateMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getMessageType()).isEqualTo("NEW_BID");
        assertThat(capturedMessage.getAuctionId()).isEqualTo(auctionId);
        assertThat(capturedMessage.getCurrentPrice()).isEqualTo(currentPrice);
        assertThat(capturedMessage.getBidCount()).isEqualTo(bidCount);
        assertThat(capturedMessage.getHighestBidderNickname()).isEqualTo(bidderNickname);
        assertThat(capturedMessage.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("경매 종료시 올바른 종료 메시지가 전송된다")
    void should_SendCorrectEndMessage_When_AuctionEnded() {
        // Given: 경매 종료 정보
        Long auctionId = 2L;
        BigDecimal finalPrice = new BigDecimal("50000");
        String winnerNickname = "우승자456";
        
        // When: 경매 종료 알림 전송
        webSocketMessagingService.notifyAuctionEnded(auctionId, finalPrice, winnerNickname);
        
        // Then: 올바른 종료 메시지가 전송되어야 함
        ArgumentCaptor<AuctionUpdateMessage> messageCaptor = 
            ArgumentCaptor.forClass(AuctionUpdateMessage.class);
        verify(messagingTemplate).convertAndSend(
            eq("/topic/auctions/2"), 
            messageCaptor.capture()
        );
        
        AuctionUpdateMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getMessageType()).isEqualTo("AUCTION_ENDED");
        assertThat(capturedMessage.getAuctionId()).isEqualTo(auctionId);
        assertThat(capturedMessage.getCurrentPrice()).isEqualTo(finalPrice);
        assertThat(capturedMessage.getHighestBidderNickname()).isEqualTo(winnerNickname);
        assertThat(capturedMessage.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("입찰자 수 변경시 올바른 메시지가 전송된다")
    void should_SendCorrectBidderCountMessage_When_BidderCountChanged() {
        // Given: 입찰자 수 변경 정보
        Long auctionId = 3L;
        Integer currentBidderCount = 12;
        
        // When: 입찰자 수 변경 알림 전송
        webSocketMessagingService.notifyBidderCountChanged(auctionId, currentBidderCount);
        
        // Then: 올바른 입찰자 수 메시지가 전송되어야 함
        ArgumentCaptor<AuctionUpdateMessage> messageCaptor = 
            ArgumentCaptor.forClass(AuctionUpdateMessage.class);
        verify(messagingTemplate).convertAndSend(
            eq("/topic/auctions/3"), 
            messageCaptor.capture()
        );
        
        AuctionUpdateMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getMessageType()).isEqualTo("BIDDER_COUNT_CHANGED");
        assertThat(capturedMessage.getAuctionId()).isEqualTo(auctionId);
        assertThat(capturedMessage.getBidCount()).isEqualTo(currentBidderCount);
        assertThat(capturedMessage.getTimestamp()).isNotNull();
    }

    // === TDD Red Phase: 에러 처리 및 경계 케이스 테스트 ===

    @Test
    @DisplayName("잘못된 경매 ID로 메시지 전송시 적절히 처리된다")
    void should_HandleGracefully_When_InvalidAuctionId() {
        // Given: 잘못된 경매 ID들
        Long nullAuctionId = null;
        Long negativeAuctionId = -1L;
        Long zeroAuctionId = 0L;
        BigDecimal currentPrice = new BigDecimal("15000");
        Integer bidCount = 5;
        String bidderNickname = "테스터";
        
        // When & Then: null 경매 ID - 메시지 전송되지 않아야 함
        webSocketMessagingService.notifyNewBid(nullAuctionId, currentPrice, bidCount, bidderNickname);
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(AuctionUpdateMessage.class));
        
        // When & Then: 음수 경매 ID - 메시지 전송되지 않아야 함  
        webSocketMessagingService.notifyNewBid(negativeAuctionId, currentPrice, bidCount, bidderNickname);
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(AuctionUpdateMessage.class));
        
        // When & Then: 0 경매 ID - 메시지 전송되지 않아야 함
        webSocketMessagingService.notifyNewBid(zeroAuctionId, currentPrice, bidCount, bidderNickname);
        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(AuctionUpdateMessage.class));
    }

    @Test
    @DisplayName("null 입찰가로 메시지 전송시 적절히 처리된다")
    void should_HandleGracefully_When_NullBidPrice() {
        // Given: null 입찰가
        Long auctionId = 1L;
        BigDecimal nullPrice = null;
        Integer bidCount = 5;
        String bidderNickname = "테스터";
        
        // When: null 입찰가로 메시지 전송
        webSocketMessagingService.notifyNewBid(auctionId, nullPrice, bidCount, bidderNickname);
        
        // Then: 메시지 전송이 중단되지 않고 처리됨 (비즈니스 요구사항에 따라)
        ArgumentCaptor<AuctionUpdateMessage> messageCaptor = 
            ArgumentCaptor.forClass(AuctionUpdateMessage.class);
        verify(messagingTemplate).convertAndSend(
            eq("/topic/auctions/1"), 
            messageCaptor.capture()
        );
        
        AuctionUpdateMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getCurrentPrice()).isNull();
        assertThat(capturedMessage.getAuctionId()).isEqualTo(auctionId);
    }

    @Test
    @DisplayName("매우 큰 입찰가 처리가 가능하다")
    void should_HandleLargeBidPrice_When_HighValueAuction() {
        // Given: 매우 큰 입찰가 (억 단위)
        Long auctionId = 1L;
        BigDecimal largePrice = new BigDecimal("999999999999.99"); // 거의 1조
        Integer bidCount = 100;
        String bidderNickname = "거대자본가";
        
        // When: 큰 금액으로 입찰 알림
        webSocketMessagingService.notifyNewBid(auctionId, largePrice, bidCount, bidderNickname);
        
        // Then: 정상적으로 처리되어야 함
        ArgumentCaptor<AuctionUpdateMessage> messageCaptor = 
            ArgumentCaptor.forClass(AuctionUpdateMessage.class);
        verify(messagingTemplate).convertAndSend(
            eq("/topic/auctions/1"), 
            messageCaptor.capture()
        );
        
        AuctionUpdateMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getCurrentPrice()).isEqualByComparingTo(largePrice);
    }

    @Test
    @DisplayName("특수문자 포함 닉네임 처리가 가능하다")
    void should_HandleSpecialCharacters_When_NicknameHasSpecialChars() {
        // Given: 특수문자 포함 닉네임
        Long auctionId = 1L;
        BigDecimal currentPrice = new BigDecimal("15000");
        Integer bidCount = 5;
        String specialNickname = "테스터@#$%^&*()_+{}|:<>?[];',./`~";
        
        // When: 특수문자 닉네임으로 메시지 전송
        webSocketMessagingService.notifyNewBid(auctionId, currentPrice, bidCount, specialNickname);
        
        // Then: 정상 처리되어야 함
        ArgumentCaptor<AuctionUpdateMessage> messageCaptor = 
            ArgumentCaptor.forClass(AuctionUpdateMessage.class);
        verify(messagingTemplate).convertAndSend(
            eq("/topic/auctions/1"), 
            messageCaptor.capture()
        );
        
        AuctionUpdateMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getHighestBidderNickname()).isEqualTo(specialNickname);
    }

    @Test
    @DisplayName("매우 긴 닉네임 처리가 가능하다")
    void should_HandleLongNickname_When_NicknameExceedsNormalLength() {
        // Given: 매우 긴 닉네임 (100자)
        Long auctionId = 1L;
        BigDecimal currentPrice = new BigDecimal("15000");
        Integer bidCount = 5;
        String longNickname = "매우긴닉네임".repeat(20); // 200자
        
        // When: 긴 닉네임으로 메시지 전송
        webSocketMessagingService.notifyNewBid(auctionId, currentPrice, bidCount, longNickname);
        
        // Then: 정상 처리되어야 함
        ArgumentCaptor<AuctionUpdateMessage> messageCaptor = 
            ArgumentCaptor.forClass(AuctionUpdateMessage.class);
        verify(messagingTemplate).convertAndSend(
            eq("/topic/auctions/1"), 
            messageCaptor.capture()
        );
        
        AuctionUpdateMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage.getHighestBidderNickname()).isEqualTo(longNickname);
    }

    @Test
    @DisplayName("동시 다발적 메시지 전송이 처리된다")
    void should_HandleConcurrentMessages_When_MultipleMessagesAtOnce() {
        // Given: 동시 전송할 여러 메시지
        Long auctionId1 = 1L;
        Long auctionId2 = 2L;
        BigDecimal price1 = new BigDecimal("15000");
        BigDecimal price2 = new BigDecimal("25000");
        
        // When: 동시 메시지 전송
        webSocketMessagingService.notifyNewBid(auctionId1, price1, 5, "입찰자1");
        webSocketMessagingService.notifyNewBid(auctionId2, price2, 8, "입찰자2");
        webSocketMessagingService.notifyAuctionEnded(auctionId1, price1, "우승자1");
        
        // Then: 모든 메시지가 정상 전송되어야 함
        verify(messagingTemplate, times(3)).convertAndSend(any(String.class), any(AuctionUpdateMessage.class));
    }
}