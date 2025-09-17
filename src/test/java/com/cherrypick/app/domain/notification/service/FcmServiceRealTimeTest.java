package com.cherrypick.app.domain.notification.service;

import com.cherrypick.app.domain.notification.entity.NotificationHistory;
import com.cherrypick.app.domain.notification.entity.NotificationSetting;
import com.cherrypick.app.domain.notification.enums.NotificationType;
import com.cherrypick.app.domain.notification.repository.NotificationHistoryRepository;
import com.cherrypick.app.domain.notification.repository.NotificationSettingRepository;
import com.cherrypick.app.domain.user.entity.User;
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
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("FcmService 실시간 알람 기능 테스트")
class FcmServiceRealTimeTest {

    @Mock
    private NotificationHistoryRepository notificationHistoryRepository;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private WebSocketMessagingService webSocketMessagingService;

    @InjectMocks
    private FcmService fcmService;

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
    @DisplayName("연결 서비스 결제 요청 알림 시 FCM과 WebSocket이 모두 발송된다")
    void sendConnectionPaymentRequestNotification_Success() {
        // Given
        Long connectionId = 100L;
        String auctionTitle = "iPhone 14 Pro 판매";

        given(notificationSettingRepository.findByUserId(testUser.getId()))
                .willReturn(Optional.of(testSetting));
        given(notificationHistoryRepository.save(any(NotificationHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        fcmService.sendConnectionPaymentRequestNotification(testUser, connectionId, auctionTitle);

        // Then
        // 알림 히스토리 저장 확인
        ArgumentCaptor<NotificationHistory> notificationCaptor = ArgumentCaptor.forClass(NotificationHistory.class);
        then(notificationHistoryRepository).should().save(notificationCaptor.capture());

        NotificationHistory savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getUser()).isEqualTo(testUser);
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.CONNECTION_PAYMENT_REQUEST);
        assertThat(savedNotification.getTitle()).isEqualTo("연결 서비스 결제 요청");
        assertThat(savedNotification.getMessage()).contains(auctionTitle);
        assertThat(savedNotification.getResourceId()).isEqualTo(connectionId);

        // WebSocket 실시간 알림 발송 확인
        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<AuctionUpdateMessage> messageCaptor = ArgumentCaptor.forClass(AuctionUpdateMessage.class);
        then(webSocketMessagingService).should().sendToUser(userIdCaptor.capture(), messageCaptor.capture());

        assertThat(userIdCaptor.getValue()).isEqualTo(testUser.getId());
        AuctionUpdateMessage wsMessage = messageCaptor.getValue();
        assertThat(wsMessage.getAuctionId()).isEqualTo(connectionId);
        assertThat(wsMessage.getMessage()).contains("연결 서비스 결제 요청");
        assertThat(wsMessage.getMessage()).contains(auctionTitle);
    }

    @Test
    @DisplayName("채팅 활성화 알림 시 FCM과 WebSocket이 모두 발송된다")
    void sendChatActivationNotification_Success() {
        // Given
        Long connectionId = 100L;
        String auctionTitle = "MacBook Pro 판매";

        given(notificationSettingRepository.findByUserId(testUser.getId()))
                .willReturn(Optional.of(testSetting));
        given(notificationHistoryRepository.save(any(NotificationHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        fcmService.sendChatActivationNotification(testUser, connectionId, auctionTitle);

        // Then
        ArgumentCaptor<NotificationHistory> notificationCaptor = ArgumentCaptor.forClass(NotificationHistory.class);
        then(notificationHistoryRepository).should().save(notificationCaptor.capture());

        NotificationHistory savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.CHAT_ACTIVATED);
        assertThat(savedNotification.getTitle()).contains("채팅이 활성화");

        then(webSocketMessagingService).should().sendToUser(any(Long.class), any(AuctionUpdateMessage.class));
    }

    @Test
    @DisplayName("새로운 입찰 알림 시 FCM과 WebSocket이 모두 발송된다")
    void sendNewBidNotification_Success() {
        // Given
        Long auctionId = 100L;
        String auctionTitle = "iPad Pro 판매";
        Long bidAmount = 500000L;

        given(notificationSettingRepository.findByUserId(testUser.getId()))
                .willReturn(Optional.of(testSetting));
        given(notificationHistoryRepository.save(any(NotificationHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        fcmService.sendNewBidNotification(testUser, auctionId, auctionTitle, bidAmount);

        // Then
        ArgumentCaptor<NotificationHistory> notificationCaptor = ArgumentCaptor.forClass(NotificationHistory.class);
        then(notificationHistoryRepository).should().save(notificationCaptor.capture());

        NotificationHistory savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.NEW_BID);
        assertThat(savedNotification.getMessage()).contains(String.format("%,d원", bidAmount));

        then(webSocketMessagingService).should().sendToUser(any(Long.class), any(AuctionUpdateMessage.class));
    }

    @Test
    @DisplayName("낙찰 알림 시 FCM과 WebSocket이 모두 발송된다")
    void sendAuctionWonNotification_Success() {
        // Given
        Long auctionId = 100L;
        String auctionTitle = "iPhone 15 Pro 판매";
        Long finalPrice = 800000L;

        given(notificationSettingRepository.findByUserId(testUser.getId()))
                .willReturn(Optional.of(testSetting));
        given(notificationHistoryRepository.save(any(NotificationHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        fcmService.sendAuctionWonNotification(testUser, auctionId, auctionTitle, finalPrice);

        // Then
        ArgumentCaptor<NotificationHistory> notificationCaptor = ArgumentCaptor.forClass(NotificationHistory.class);
        then(notificationHistoryRepository).should().save(notificationCaptor.capture());

        NotificationHistory savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.AUCTION_WON);
        assertThat(savedNotification.getTitle()).contains("낙찰");
        assertThat(savedNotification.getMessage()).contains(String.format("%,d원", finalPrice));

        then(webSocketMessagingService).should().sendToUser(any(Long.class), any(AuctionUpdateMessage.class));
    }

    @Test
    @DisplayName("거래 완료 알림 시 FCM과 WebSocket이 모두 발송된다")
    void sendTransactionCompletedNotification_Success() {
        // Given
        Long connectionId = 100L;
        String auctionTitle = "AirPods Pro 판매";
        boolean isSeller = true;

        given(notificationSettingRepository.findByUserId(testUser.getId()))
                .willReturn(Optional.of(testSetting));
        given(notificationHistoryRepository.save(any(NotificationHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        fcmService.sendTransactionCompletedNotification(testUser, connectionId, auctionTitle, isSeller);

        // Then
        ArgumentCaptor<NotificationHistory> notificationCaptor = ArgumentCaptor.forClass(NotificationHistory.class);
        then(notificationHistoryRepository).should().save(notificationCaptor.capture());

        NotificationHistory savedNotification = notificationCaptor.getValue();
        assertThat(savedNotification.getType()).isEqualTo(NotificationType.TRANSACTION_COMPLETED);
        assertThat(savedNotification.getTitle()).contains("거래가 완료");
        assertThat(savedNotification.getMessage()).contains("판매");

        then(webSocketMessagingService).should().sendToUser(any(Long.class), any(AuctionUpdateMessage.class));
    }

    @Test
    @DisplayName("알림 설정이 비활성화된 경우 FCM과 WebSocket 모두 발송하지 않는다")
    void sendNotification_DisabledSetting() {
        // Given
        Long connectionId = 100L;
        String auctionTitle = "Test 상품";

        NotificationSetting disabledSetting = testSetting.updateSettings(
                null, null, false, null, null, null, null
        );

        given(notificationSettingRepository.findByUserId(testUser.getId()))
                .willReturn(Optional.of(disabledSetting));

        // When
        fcmService.sendConnectionPaymentRequestNotification(testUser, connectionId, auctionTitle);

        // Then
        then(notificationHistoryRepository).should(times(0)).save(any());
        then(webSocketMessagingService).should(times(0)).sendToUser(any(), any());
    }

    @Test
    @DisplayName("알림 설정이 없으면 기본 설정을 생성하고 알림을 발송한다")
    void sendNotification_CreateDefaultSetting() {
        // Given
        Long connectionId = 100L;
        String auctionTitle = "Test 상품";

        given(notificationSettingRepository.findByUserId(testUser.getId()))
                .willReturn(Optional.empty());
        given(notificationSettingRepository.save(any(NotificationSetting.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(notificationHistoryRepository.save(any(NotificationHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // When
        fcmService.sendConnectionPaymentRequestNotification(testUser, connectionId, auctionTitle);

        // Then
        // 기본 설정 생성 확인
        ArgumentCaptor<NotificationSetting> settingCaptor = ArgumentCaptor.forClass(NotificationSetting.class);
        then(notificationSettingRepository).should().save(settingCaptor.capture());

        NotificationSetting createdSetting = settingCaptor.getValue();
        assertThat(createdSetting.getUser()).isEqualTo(testUser);
        assertThat(createdSetting.getConnectionPaymentNotification()).isTrue();

        // 알림 발송 확인
        then(notificationHistoryRepository).should().save(any(NotificationHistory.class));
        then(webSocketMessagingService).should().sendToUser(any(Long.class), any(AuctionUpdateMessage.class));
    }
}