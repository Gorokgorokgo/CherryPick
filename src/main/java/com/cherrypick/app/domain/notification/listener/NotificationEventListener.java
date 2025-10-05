package com.cherrypick.app.domain.notification.listener;

import com.cherrypick.app.domain.notification.entity.NotificationHistory;
import com.cherrypick.app.domain.notification.entity.NotificationSetting;
import com.cherrypick.app.domain.notification.event.*;
import com.cherrypick.app.domain.notification.repository.NotificationHistoryRepository;
import com.cherrypick.app.domain.notification.repository.NotificationSettingRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import com.cherrypick.app.domain.websocket.dto.AuctionUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 알림 이벤트 리스너
 * 비즈니스 이벤트 발생 시 자동으로 알림 발송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationHistoryRepository notificationHistoryRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;
    private final WebSocketMessagingService webSocketMessagingService;

    /**
     * 새로운 입찰 알림 이벤트 처리
     */
    @Async
    @EventListener
    @Transactional
    public void handleNewBidNotification(NewBidNotificationEvent event) {
        log.info("새로운 입찰 알림 이벤트 수신. sellerId: {}, auctionId: {}, bidAmount: {}",
                event.getTargetUserId(), event.getResourceId(), event.getBidAmount());

        processNotificationEvent(event);
    }

    /**
     * 낙찰 알림 이벤트 처리 (구매자용)
     */
    @Async
    @EventListener
    @Transactional
    public void handleAuctionWonNotification(AuctionWonNotificationEvent event) {
        log.info("낙찰 알림 이벤트 수신 (구매자). buyerId: {}, auctionId: {}, finalPrice: {}",
                event.getTargetUserId(), event.getResourceId(), event.getFinalPrice());

        processNotificationEvent(event);
    }

    /**
     * 경매 낙찰 알림 이벤트 처리 (판매자용)
     */
    @Async
    @EventListener
    @Transactional
    public void handleAuctionSoldNotification(AuctionSoldNotificationEvent event) {
        log.info("경매 낙찰 알림 이벤트 수신 (판매자). sellerId: {}, auctionId: {}, finalPrice: {}, winner: {}",
                event.getTargetUserId(), event.getResourceId(), event.getFinalPrice(), event.getWinnerNickname());

        processNotificationEvent(event);
    }

    /**
     * 경매 유찰 알림 이벤트 처리 (판매자용)
     */
    @Async
    @EventListener
    @Transactional
    public void handleAuctionNotSoldNotification(AuctionNotSoldNotificationEvent event) {
        log.info("경매 유찰 알림 이벤트 수신 (판매자). sellerId: {}, auctionId: {}, hasHighestBid: {}",
                event.getTargetUserId(), event.getResourceId(), event.getHighestBid() != null);

        processNotificationEvent(event);
    }

    /**
     * 경매 유찰 알림 이벤트 처리 (최고 입찰자용)
     */
    @Async
    @EventListener
    @Transactional
    public void handleAuctionNotSoldForHighestBidderNotification(AuctionNotSoldForHighestBidderEvent event) {
        log.info("경매 유찰 알림 이벤트 수신 (최고 입찰자). bidderId: {}, auctionId: {}, highestBidAmount: {}",
                event.getTargetUserId(), event.getResourceId(), event.getHighestBidAmount());

        processNotificationEvent(event);
    }

    /**
     * 경매 종료 알림 이벤트 처리 (일반 참여자용)
     */
    @Async
    @EventListener
    @Transactional
    public void handleAuctionEndedForParticipantNotification(AuctionEndedForParticipantEvent event) {
        log.info("경매 종료 알림 이벤트 수신 (참여자). participantId: {}, auctionId: {}, wasSuccessful: {}",
                event.getTargetUserId(), event.getResourceId(), event.isWasSuccessful());

        processNotificationEvent(event);
    }

    /**
     * 연결 서비스 결제 요청 알림 이벤트 처리
     */
    @Async
    @EventListener
    @Transactional
    public void handleConnectionPaymentRequestNotification(ConnectionPaymentRequestNotificationEvent event) {
        log.info("연결 서비스 결제 요청 알림 이벤트 수신. sellerId: {}, connectionId: {}",
                event.getTargetUserId(), event.getResourceId());

        processNotificationEvent(event);
    }

    /**
     * 채팅 활성화 알림 이벤트 처리
     */
    @Async
    @EventListener
    @Transactional
    public void handleChatActivatedNotification(ChatActivatedNotificationEvent event) {
        log.info("채팅 활성화 알림 이벤트 수신. buyerId: {}, chatRoomId: {}",
                event.getTargetUserId(), event.getResourceId());

        processNotificationEvent(event);
    }

    /**
     * 거래 완료 알림 이벤트 처리
     */
    @Async
    @EventListener
    @Transactional
    public void handleTransactionCompletedNotification(TransactionCompletedNotificationEvent event) {
        log.info("거래 완료 알림 이벤트 수신. userId: {}, connectionId: {}, isSeller: {}",
                event.getTargetUserId(), event.getResourceId(),
                ((TransactionCompletedNotificationEvent) event).isSeller());

        processNotificationEvent(event);
    }

    /**
     * 알림 이벤트 공통 처리 로직
     */
    private void processNotificationEvent(NotificationEvent event) {
        try {
            // 사용자 조회
            User user = userRepository.findById(event.getTargetUserId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "사용자를 찾을 수 없습니다. userId: " + event.getTargetUserId()));

            // 알림 설정 확인
            NotificationSetting setting = getOrCreateNotificationSetting(user);
            if (!isNotificationEnabled(setting, event.getNotificationType())) {
                log.info("알림이 비활성화되어 있습니다. userId: {}, type: {}",
                        user.getId(), event.getNotificationType());
                return;
            }

            // 알림 히스토리 저장
            NotificationHistory notification = NotificationHistory.createNotification(
                    user, event.getNotificationType(), event.getTitle(),
                    event.getMessage(), event.getResourceId());
            notificationHistoryRepository.save(notification);

            // FCM 푸시 알림 발송 (모의)
            sendFcmNotification(setting.getFcmToken(), event.getTitle(), event.getMessage(), notification);

            // WebSocket 실시간 알림 발송
            sendWebSocketNotification(user.getId(), event);

            log.info("알림 발송 완료. userId: {}, type: {}, resourceId: {}",
                    user.getId(), event.getNotificationType(), event.getResourceId());

        } catch (Exception e) {
            log.error("알림 이벤트 처리 중 오류 발생. event: {}, error: {}",
                    event.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    /**
     * 알림 설정 확인
     */
    private boolean isNotificationEnabled(NotificationSetting setting, com.cherrypick.app.domain.notification.enums.NotificationType type) {
        return switch (type) {
            case NEW_BID -> setting.getBidNotification();
            case AUCTION_WON -> setting.getWinningNotification();
            case AUCTION_SOLD -> setting.getWinningNotification(); // 판매자용 낙찰 알림
            case AUCTION_NOT_SOLD -> setting.getBidNotification(); // 유찰 알림
            case AUCTION_ENDED -> setting.getBidNotification(); // 경매 종료 알림 (일반 참여자)
            case CONNECTION_PAYMENT_REQUEST -> setting.getConnectionPaymentNotification();
            case CHAT_ACTIVATED -> setting.getChatActivationNotification();
            case NEW_MESSAGE -> setting.getMessageNotification();
            case TRANSACTION_COMPLETED -> setting.getTransactionCompletionNotification();
            case PROMOTION -> setting.getPromotionNotification();
        };
    }

    /**
     * FCM 푸시 알림 발송 (모의)
     */
    private void sendFcmNotification(String fcmToken, String title, String message, NotificationHistory notification) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.debug("FCM 토큰이 없어 푸시 알림을 건너뜁니다. notificationId: {}", notification.getId());
            return;
        }

        try {
            // TODO: 실제 FCM SDK 연동
            log.info("FCM 푸시 발송 (모의): token={}, title={}, message={}",
                    fcmToken.substring(0, Math.min(10, fcmToken.length())) + "...", title, message);

            // 발송 성공 처리
            NotificationHistory updatedNotification = notification.markFcmSent();
            notificationHistoryRepository.save(updatedNotification);

        } catch (Exception e) {
            log.error("FCM 푸시 발송 실패. notificationId: {}, error: {}", notification.getId(), e.getMessage());
        }
    }

    /**
     * WebSocket 실시간 알림 발송
     */
    private void sendWebSocketNotification(Long userId, NotificationEvent event) {
        try {
            // 프론트엔드 NotificationMessage 형식에 맞춰 JSON 메시지 생성
            NotificationWebSocketMessage wsNotification = NotificationWebSocketMessage.builder()
                    .id(String.valueOf(System.currentTimeMillis())) // 임시 ID (실제로는 NotificationHistory의 ID 사용 가능)
                    .type(event.getNotificationType().name())
                    .title(event.getTitle())
                    .message(event.getMessage())
                    .timestamp(System.currentTimeMillis())
                    .isRead(false)
                    .resourceId(event.getResourceId())
                    .build();

            webSocketMessagingService.sendNotificationToUser(userId, wsNotification);

            log.info("✅ WebSocket 실시간 알림 발송 성공. userId: {}, type: {}, resourceId: {}",
                    userId, event.getNotificationType(), event.getResourceId());

        } catch (Exception e) {
            log.error("❌ WebSocket 실시간 알림 발송 실패. userId: {}, type: {}, error: {}",
                    userId, event.getNotificationType(), e.getMessage(), e);
        }
    }

    /**
     * WebSocket 알림 메시지 DTO (프론트엔드 NotificationMessage와 동일 구조)
     */
    @lombok.Builder
    @lombok.Getter
    private static class NotificationWebSocketMessage {
        private String id;
        private String type;
        private String title;
        private String message;
        private long timestamp;
        private boolean isRead;
        private Long resourceId;
    }

    /**
     * 알림 설정 조회 또는 생성
     */
    private NotificationSetting getOrCreateNotificationSetting(User user) {
        return notificationSettingRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    NotificationSetting setting = NotificationSetting.createDefaultSetting(user);
                    return notificationSettingRepository.save(setting);
                });
    }
}