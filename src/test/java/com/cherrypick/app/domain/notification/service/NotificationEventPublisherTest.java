package com.cherrypick.app.domain.notification.service;

import com.cherrypick.app.domain.notification.event.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 이벤트 발행 서비스 테스트")
class NotificationEventPublisherTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationEventPublisher notificationEventPublisher;

    @Test
    @DisplayName("새로운 입찰 알림 이벤트를 성공적으로 발행한다")
    void publishNewBidNotification_Success() {
        // Given
        Long sellerId = 1L;
        Long auctionId = 100L;
        String auctionTitle = "iPhone 14 Pro 판매";
        Long bidAmount = 500000L;
        String bidderNickname = "bidder1";

        // When
        notificationEventPublisher.publishNewBidNotification(
                sellerId, auctionId, auctionTitle, bidAmount, bidderNickname);

        // Then
        ArgumentCaptor<NewBidNotificationEvent> eventCaptor = ArgumentCaptor.forClass(NewBidNotificationEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());

        NewBidNotificationEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getTargetUserId()).isEqualTo(sellerId);
        assertThat(publishedEvent.getResourceId()).isEqualTo(auctionId);
        assertThat(publishedEvent.getAuctionTitle()).isEqualTo(auctionTitle);
        assertThat(publishedEvent.getBidAmount()).isEqualTo(bidAmount);
        assertThat(publishedEvent.getBidderNickname()).isEqualTo(bidderNickname);
        assertThat(publishedEvent.getTitle()).isEqualTo("새로운 입찰이 있습니다!");
        assertThat(publishedEvent.getMessage()).contains(auctionTitle);
        assertThat(publishedEvent.getMessage()).contains(String.format("%,d원", bidAmount));
    }

    @Test
    @DisplayName("낙찰 알림 이벤트를 성공적으로 발행한다")
    void publishAuctionWonNotification_Success() {
        // Given
        Long buyerId = 2L;
        Long auctionId = 100L;
        String auctionTitle = "MacBook Pro 판매";
        Long finalPrice = 1200000L;

        // When
        notificationEventPublisher.publishAuctionWonNotification(
                buyerId, auctionId, auctionTitle, finalPrice);

        // Then
        ArgumentCaptor<AuctionWonNotificationEvent> eventCaptor = ArgumentCaptor.forClass(AuctionWonNotificationEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());

        AuctionWonNotificationEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getTargetUserId()).isEqualTo(buyerId);
        assertThat(publishedEvent.getResourceId()).isEqualTo(auctionId);
        assertThat(publishedEvent.getAuctionTitle()).isEqualTo(auctionTitle);
        assertThat(publishedEvent.getFinalPrice()).isEqualTo(finalPrice);
        assertThat(publishedEvent.getTitle()).contains("낙찰");
        assertThat(publishedEvent.getMessage()).contains(String.format("%,d원", finalPrice));
    }

    @Test
    @DisplayName("연결 서비스 결제 요청 알림 이벤트를 성공적으로 발행한다")
    void publishConnectionPaymentRequestNotification_Success() {
        // Given
        Long sellerId = 1L;
        Long connectionId = 50L;
        String auctionTitle = "iPad Pro 판매";

        // When
        notificationEventPublisher.publishConnectionPaymentRequestNotification(
                sellerId, connectionId, auctionTitle);

        // Then
        ArgumentCaptor<ConnectionPaymentRequestNotificationEvent> eventCaptor =
                ArgumentCaptor.forClass(ConnectionPaymentRequestNotificationEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());

        ConnectionPaymentRequestNotificationEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getTargetUserId()).isEqualTo(sellerId);
        assertThat(publishedEvent.getResourceId()).isEqualTo(connectionId);
        assertThat(publishedEvent.getAuctionTitle()).isEqualTo(auctionTitle);
        assertThat(publishedEvent.getTitle()).isEqualTo("연결 서비스 결제 요청");
        assertThat(publishedEvent.getMessage()).contains(auctionTitle);
    }

    @Test
    @DisplayName("채팅 활성화 알림 이벤트를 성공적으로 발행한다")
    void publishChatActivatedNotification_Success() {
        // Given
        Long buyerId = 2L;
        Long chatRoomId = 75L;
        String auctionTitle = "AirPods Pro 판매";

        // When
        notificationEventPublisher.publishChatActivatedNotification(
                buyerId, chatRoomId, auctionTitle);

        // Then
        ArgumentCaptor<ChatActivatedNotificationEvent> eventCaptor =
                ArgumentCaptor.forClass(ChatActivatedNotificationEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());

        ChatActivatedNotificationEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getTargetUserId()).isEqualTo(buyerId);
        assertThat(publishedEvent.getResourceId()).isEqualTo(chatRoomId);
        assertThat(publishedEvent.getAuctionTitle()).isEqualTo(auctionTitle);
        assertThat(publishedEvent.getTitle()).contains("채팅이 활성화");
        assertThat(publishedEvent.getMessage()).contains(auctionTitle);
    }

    @Test
    @DisplayName("거래 완료 알림 이벤트를 성공적으로 발행한다")
    void publishTransactionCompletedNotification_Success() {
        // Given
        Long userId = 1L;
        Long connectionId = 50L;
        String auctionTitle = "Nintendo Switch 판매";
        boolean isSeller = true;

        // When
        notificationEventPublisher.publishTransactionCompletedNotification(
                userId, connectionId, auctionTitle, isSeller);

        // Then
        ArgumentCaptor<TransactionCompletedNotificationEvent> eventCaptor =
                ArgumentCaptor.forClass(TransactionCompletedNotificationEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());

        TransactionCompletedNotificationEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.getTargetUserId()).isEqualTo(userId);
        assertThat(publishedEvent.getResourceId()).isEqualTo(connectionId);
        assertThat(publishedEvent.getAuctionTitle()).isEqualTo(auctionTitle);
        assertThat(publishedEvent.isSeller()).isEqualTo(isSeller);
        assertThat(publishedEvent.getTitle()).contains("거래가 완료");
        assertThat(publishedEvent.getMessage()).contains("판매");
    }

    @Test
    @DisplayName("다중 사용자에게 새로운 입찰 알림 이벤트를 발행한다")
    void publishNewBidNotificationToMultiple_Success() {
        // Given
        Long[] userIds = {1L, 2L, 3L};
        Long auctionId = 100L;
        String auctionTitle = "Test 상품";
        Long bidAmount = 100000L;
        String bidderNickname = "bidder";

        // When
        notificationEventPublisher.publishNewBidNotificationToMultiple(
                userIds, auctionId, auctionTitle, bidAmount, bidderNickname);

        // Then
        then(eventPublisher).should(times(3)).publishEvent(any(NewBidNotificationEvent.class));
    }

    @Test
    @DisplayName("거래 완료 알림에서 구매자인 경우 메시지가 올바르게 생성된다")
    void publishTransactionCompletedNotification_Buyer() {
        // Given
        Long userId = 2L;
        Long connectionId = 50L;
        String auctionTitle = "PlayStation 5 판매";
        boolean isSeller = false; // 구매자

        // When
        notificationEventPublisher.publishTransactionCompletedNotification(
                userId, connectionId, auctionTitle, isSeller);

        // Then
        ArgumentCaptor<TransactionCompletedNotificationEvent> eventCaptor =
                ArgumentCaptor.forClass(TransactionCompletedNotificationEvent.class);
        then(eventPublisher).should().publishEvent(eventCaptor.capture());

        TransactionCompletedNotificationEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.isSeller()).isFalse();
        assertThat(publishedEvent.getMessage()).contains("구매");
        assertThat(publishedEvent.getMessage()).doesNotContain("판매");
    }
}