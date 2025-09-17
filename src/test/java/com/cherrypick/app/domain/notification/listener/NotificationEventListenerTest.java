package com.cherrypick.app.domain.notification.listener;

import com.cherrypick.app.domain.notification.entity.NotificationHistory;
import com.cherrypick.app.domain.notification.entity.NotificationSetting;
import com.cherrypick.app.domain.notification.enums.NotificationType;
import com.cherrypick.app.domain.notification.event.NewBidNotificationEvent;
import com.cherrypick.app.domain.notification.event.AuctionWonNotificationEvent;
import com.cherrypick.app.domain.notification.event.ChatActivatedNotificationEvent;
import com.cherrypick.app.domain.notification.repository.NotificationHistoryRepository;
import com.cherrypick.app.domain.notification.repository.NotificationSettingRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import com.cherrypick.app.domain.websocket.dto.AuctionUpdateMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 이벤트 리스너 테스트")
class NotificationEventListenerTest {

    @Mock
    private NotificationHistoryRepository notificationHistoryRepository;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WebSocketMessagingService webSocketMessagingService;

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    private User testUser;
    private NotificationSetting testSetting;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .nickname("testUser")
                .build();

        testSetting = NotificationSetting.builder()
                .id(1L)
                .user(testUser)
                .bidNotification(true)
                .winningNotification(true)
                .connectionPaymentNotification(true)
                .chatActivationNotification(true)
                .messageNotification(true)
                .transactionCompletionNotification(true)
                .promotionNotification(false)
                .fcmToken("test-fcm-token")
                .build();
    }

    @Test
    @DisplayName("새로운 입찰 이벤트 발생 시 알림 히스토리 저장 및 실시간 알림 발송")
    void handleNewBidNotification_Success() {
        // Given
        Long sellerId = 1L;
        Long auctionId = 100L;
        String auctionTitle = "iPhone 14 Pro 판매";
        Long bidAmount = 500000L;
        String bidderNickname = "bidder1";

        NewBidNotificationEvent event = new NewBidNotificationEvent(
                this, sellerId, auctionId, auctionTitle, bidAmount, bidderNickname);

        given(userRepository.findById(sellerId)).willReturn(Optional.of(testUser));
        given(notificationSettingRepository.findByUserId(sellerId)).willReturn(Optional.of(testSetting));
        given(notificationHistoryRepository.save(any(NotificationHistory.class)))
                .willAnswer(invocation -> {
                    NotificationHistory notification = invocation.getArgument(0);
                    return NotificationHistory.builder()
                            .id(1L)
                            .user(notification.getUser())
                            .type(notification.getType())
                            .title(notification.getTitle())
                            .message(notification.getMessage())
                            .resourceId(notification.getResourceId())
                            .isRead(false)
                            .fcmSent(false)
                            .build();
                });

        // When
        notificationEventListener.handleNewBidNotification(event);

        // Then
        // 알림 히스토리 저장 확인
        ArgumentCaptor<NotificationHistory> notificationCaptor = ArgumentCaptor.forClass(NotificationHistory.class);
        then(notificationHistoryRepository).should().save(notificationCaptor.capture());

        NotificationHistory savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getUser()).isEqualTo(testUser);
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.NEW_BID);
        assertThat(savedNotification.getTitle()).isEqualTo(event.getTitle());
        assertThat(savedNotification.getMessage()).isEqualTo(event.getMessage());
        assertThat(savedNotification.getResourceId()).isEqualTo(auctionId);

        // WebSocket 실시간 알림 발송 확인
        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<AuctionUpdateMessage> messageCaptor = ArgumentCaptor.forClass(AuctionUpdateMessage.class);
        then(webSocketMessagingService).should().sendToUser(userIdCaptor.capture(), messageCaptor.capture());

        assertThat(userIdCaptor.getValue()).isEqualTo(sellerId);
        AuctionUpdateMessage wsMessage = messageCaptor.getValue();
        assertThat(wsMessage.getAuctionId()).isEqualTo(auctionId);
        assertThat(wsMessage.getMessage()).contains("새로운 입찰");
        assertThat(wsMessage.getMessage()).contains(auctionTitle);
    }

    @Test
    @DisplayName("낙찰 이벤트 발생 시 알림 히스토리 저장 및 실시간 알림 발송")
    void handleAuctionWonNotification_Success() {
        // Given
        Long buyerId = 2L;
        Long auctionId = 100L;
        String auctionTitle = "MacBook Pro 판매";
        Long finalPrice = 1200000L;

        AuctionWonNotificationEvent event = new AuctionWonNotificationEvent(
                this, buyerId, auctionId, auctionTitle, finalPrice);

        given(userRepository.findById(buyerId)).willReturn(Optional.of(testUser));
        given(notificationSettingRepository.findByUserId(buyerId)).willReturn(Optional.of(testSetting));
        given(notificationHistoryRepository.save(any(NotificationHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        notificationEventListener.handleAuctionWonNotification(event);

        // Then
        ArgumentCaptor<NotificationHistory> notificationCaptor = ArgumentCaptor.forClass(NotificationHistory.class);
        then(notificationHistoryRepository).should().save(notificationCaptor.capture());

        NotificationHistory savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.AUCTION_WON);
        assertThat(savedNotification.getTitle()).contains("낙찰");
        assertThat(savedNotification.getMessage()).contains(String.format("%,d원", finalPrice));

        then(webSocketMessagingService).should().sendToUser(any(), any());
    }

    @Test
    @DisplayName("채팅 활성화 이벤트 발생 시 알림 히스토리 저장 및 실시간 알림 발송")
    void handleChatActivatedNotification_Success() {
        // Given
        Long buyerId = 2L;
        Long chatRoomId = 50L;
        String auctionTitle = "iPad Pro 판매";

        ChatActivatedNotificationEvent event = new ChatActivatedNotificationEvent(
                this, buyerId, chatRoomId, auctionTitle);

        given(userRepository.findById(buyerId)).willReturn(Optional.of(testUser));
        given(notificationSettingRepository.findByUserId(buyerId)).willReturn(Optional.of(testSetting));
        given(notificationHistoryRepository.save(any(NotificationHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        notificationEventListener.handleChatActivatedNotification(event);

        // Then
        ArgumentCaptor<NotificationHistory> notificationCaptor = ArgumentCaptor.forClass(NotificationHistory.class);
        then(notificationHistoryRepository).should().save(notificationCaptor.capture());

        NotificationHistory savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.CHAT_ACTIVATED);
        assertThat(savedNotification.getTitle()).contains("채팅이 활성화");
        assertThat(savedNotification.getResourceId()).isEqualTo(chatRoomId);

        then(webSocketMessagingService).should().sendToUser(any(), any());
    }

    @Test
    @DisplayName("알림 설정이 비활성화된 경우 알림을 발송하지 않는다")
    void handleNotification_DisabledSetting() {
        // Given
        Long sellerId = 1L;
        Long auctionId = 100L;

        NotificationSetting disabledSetting = testSetting.updateSettings(
                false, null, null, null, null, null, null
        );

        NewBidNotificationEvent event = new NewBidNotificationEvent(
                this, sellerId, auctionId, "Test 상품", 100000L, "bidder");

        given(userRepository.findById(sellerId)).willReturn(Optional.of(testUser));
        given(notificationSettingRepository.findByUserId(sellerId)).willReturn(Optional.of(disabledSetting));

        // When
        notificationEventListener.handleNewBidNotification(event);

        // Then
        then(notificationHistoryRepository).should(never()).save(any());
        then(webSocketMessagingService).should(never()).sendToUser(any(), any());
    }

    @Test
    @DisplayName("사용자가 존재하지 않으면 알림을 발송하지 않는다")
    void handleNotification_UserNotFound() {
        // Given
        Long sellerId = 999L;
        NewBidNotificationEvent event = new NewBidNotificationEvent(
                this, sellerId, 100L, "Test 상품", 100000L, "bidder");

        given(userRepository.findById(sellerId)).willReturn(Optional.empty());

        // When
        notificationEventListener.handleNewBidNotification(event);

        // Then
        then(notificationHistoryRepository).should(never()).save(any());
        then(webSocketMessagingService).should(never()).sendToUser(any(), any());
    }

    @Test
    @DisplayName("알림 설정이 없으면 기본 설정을 생성하고 알림을 발송한다")
    void handleNotification_CreateDefaultSetting() {
        // Given
        Long sellerId = 1L;
        NewBidNotificationEvent event = new NewBidNotificationEvent(
                this, sellerId, 100L, "Test 상품", 100000L, "bidder");

        given(userRepository.findById(sellerId)).willReturn(Optional.of(testUser));
        given(notificationSettingRepository.findByUserId(sellerId)).willReturn(Optional.empty());
        given(notificationSettingRepository.save(any(NotificationSetting.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(notificationHistoryRepository.save(any(NotificationHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        notificationEventListener.handleNewBidNotification(event);

        // Then
        // 기본 설정 생성 확인
        ArgumentCaptor<NotificationSetting> settingCaptor = ArgumentCaptor.forClass(NotificationSetting.class);
        then(notificationSettingRepository).should().save(settingCaptor.capture());

        NotificationSetting createdSetting = settingCaptor.getValue();
        assertThat(createdSetting.getUser()).isEqualTo(testUser);
        assertThat(createdSetting.getBidNotification()).isTrue();

        // 알림 발송 확인
        then(notificationHistoryRepository).should().save(any(NotificationHistory.class));
        then(webSocketMessagingService).should().sendToUser(any(), any());
    }

    @Test
    @DisplayName("FCM 토큰이 없으면 FCM 발송 없이 WebSocket만 발송한다")
    void handleNotification_NoFcmToken() {
        // Given
        Long sellerId = 1L;
        NotificationSetting settingWithoutToken = testSetting.updateFcmToken(null);

        NewBidNotificationEvent event = new NewBidNotificationEvent(
                this, sellerId, 100L, "Test 상품", 100000L, "bidder");

        given(userRepository.findById(sellerId)).willReturn(Optional.of(testUser));
        given(notificationSettingRepository.findByUserId(sellerId)).willReturn(Optional.of(settingWithoutToken));
        given(notificationHistoryRepository.save(any(NotificationHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        notificationEventListener.handleNewBidNotification(event);

        // Then
        // 알림 히스토리는 저장되고 WebSocket은 발송되어야 함
        then(notificationHistoryRepository).should().save(any(NotificationHistory.class));
        then(webSocketMessagingService).should().sendToUser(any(), any());

        // FCM은 토큰이 없으므로 발송되지 않음 (로그만 출력)
    }
}