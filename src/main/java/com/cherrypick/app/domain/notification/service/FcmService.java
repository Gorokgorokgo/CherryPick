package com.cherrypick.app.domain.notification.service;

import com.cherrypick.app.domain.notification.entity.NotificationHistory;
import com.cherrypick.app.domain.notification.entity.NotificationSetting;
import com.cherrypick.app.domain.notification.enums.NotificationType;
import com.cherrypick.app.domain.notification.repository.NotificationHistoryRepository;
import com.cherrypick.app.domain.notification.repository.NotificationSettingRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import com.cherrypick.app.domain.websocket.dto.AuctionUpdateMessage;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * FCM 푸시 알림 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FcmService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final WebSocketMessagingService webSocketMessagingService;
    
    /**
     * 연결 서비스 결제 요청 알림 (판매자용)
     */
    @Transactional
    public void sendConnectionPaymentRequestNotification(User seller, Long connectionId, String auctionTitle) {
        // 알림 설정 확인
        NotificationSetting setting = getOrCreateNotificationSetting(seller);
        if (!setting.getConnectionPaymentNotification()) {
            log.info("연결 서비스 결제 요청 알림이 비활성화되어 있습니다. userId: {}", seller.getId());
            return;
        }
        
        String title = "연결 서비스 결제 요청";
        String message = String.format("'%s' 경매의 연결 서비스 수수료를 결제하고 구매자와 채팅을 시작하세요!", auctionTitle);
        
        // 알림 히스토리 저장
        NotificationHistory notification = NotificationHistory.createNotification(
                seller, NotificationType.CONNECTION_PAYMENT_REQUEST, title, message, connectionId);
        notificationHistoryRepository.save(notification);
        
        // FCM 푸시 발송 (현재는 로그만, 추후 실제 FCM 연동)
        sendFcmPush(setting.getFcmToken(), title, message, notification);

        // WebSocket 실시간 알림 발송
        sendWebSocketNotification(seller.getId(), NotificationType.CONNECTION_PAYMENT_REQUEST, title, message, connectionId);

        log.info("연결 서비스 결제 요청 알림 발송 완료. userId: {}, connectionId: {}", seller.getId(), connectionId);
    }
    
    /**
     * 채팅 활성화 알림 (구매자용)
     */
    @Transactional
    public void sendChatActivationNotification(User buyer, Long connectionId, String auctionTitle) {
        // 알림 설정 확인
        NotificationSetting setting = getOrCreateNotificationSetting(buyer);
        if (!setting.getChatActivationNotification()) {
            log.info("채팅 활성화 알림이 비활성화되어 있습니다. userId: {}", buyer.getId());
            return;
        }
        
        String title = "채팅이 활성화되었습니다!";
        String message = String.format("'%s' 경매의 판매자와 채팅을 시작할 수 있습니다. 거래 조건을 협의해보세요!", auctionTitle);
        
        // 알림 히스토리 저장
        NotificationHistory notification = NotificationHistory.createNotification(
                buyer, NotificationType.CHAT_ACTIVATED, title, message, connectionId);
        notificationHistoryRepository.save(notification);
        
        // FCM 푸시 발송
        sendFcmPush(setting.getFcmToken(), title, message, notification);

        // WebSocket 실시간 알림 발송
        sendWebSocketNotification(buyer.getId(), NotificationType.CHAT_ACTIVATED, title, message, connectionId);

        log.info("채팅 활성화 알림 발송 완료. userId: {}, connectionId: {}", buyer.getId(), connectionId);
    }
    
    /**
     * 새로운 입찰 알림 (판매자용)
     */
    @Transactional
    public void sendNewBidNotification(User seller, Long auctionId, String auctionTitle, Long bidAmount) {
        NotificationSetting setting = getOrCreateNotificationSetting(seller);
        if (!setting.getBidNotification()) {
            return;
        }

        String title = "새로운 입찰이 있습니다!";
        String message = String.format("'%s' 경매에 %,d원 입찰이 들어왔습니다.", auctionTitle, bidAmount);

        NotificationHistory notification = NotificationHistory.createNotification(
                seller, NotificationType.NEW_BID, title, message, auctionId);
        notificationHistoryRepository.save(notification);

        sendFcmPush(setting.getFcmToken(), title, message, notification);

        // WebSocket 실시간 알림 발송
        sendWebSocketNotification(seller.getId(), NotificationType.NEW_BID, title, message, auctionId);
    }
    
    /**
     * 낙찰 알림 (구매자용)
     */
    @Transactional
    public void sendAuctionWonNotification(User buyer, Long auctionId, String auctionTitle, Long finalPrice, String sellerNickname) {
        NotificationSetting setting = getOrCreateNotificationSetting(buyer);
        if (!setting.getWinningNotification()) {
            return;
        }

        String title = "낙찰되었습니다! 🎉";
        String message = String.format("'%s' 경매가 %,d원에 낙찰되었습니다. 판매자(%s)님과의 거래를 시작해주세요.", auctionTitle, finalPrice, sellerNickname);

        NotificationHistory notification = NotificationHistory.createNotification(
                buyer, NotificationType.AUCTION_WON, title, message, auctionId);
        notificationHistoryRepository.save(notification);

        sendFcmPush(setting.getFcmToken(), title, message, notification);

        // WebSocket 실시간 알림 발송
        sendWebSocketNotification(buyer.getId(), NotificationType.AUCTION_WON, title, message, auctionId);
    }

    /**
     * 판매 완료 알림 (판매자용)
     */
    @Transactional
    public void sendAuctionSoldNotification(User seller, Long auctionId, String auctionTitle, Long finalPrice, String buyerNickname) {
        NotificationSetting setting = getOrCreateNotificationSetting(seller);
        if (!setting.getBidNotification()) {
            return;
        }

        String title = "경매 낙찰 완료! 🎉";
        String message = String.format("'%s' 경매가 %,d원에 낙찰되었습니다. 낙찰자(%s)님과의 거래를 시작해주세요.", auctionTitle, finalPrice, buyerNickname);

        NotificationHistory notification = NotificationHistory.createNotification(
                seller, NotificationType.AUCTION_SOLD, title, message, auctionId);
        notificationHistoryRepository.save(notification);

        sendFcmPush(setting.getFcmToken(), title, message, notification);

        // WebSocket 실시간 알림 발송
        sendWebSocketNotification(seller.getId(), NotificationType.AUCTION_SOLD, title, message, auctionId);
    }
    
    /**
     * 거래 완료 알림
     */
    @Transactional
    public void sendTransactionCompletedNotification(User user, Long connectionId, String auctionTitle, boolean isSeller) {
        NotificationSetting setting = getOrCreateNotificationSetting(user);
        if (!setting.getTransactionCompletionNotification()) {
            return;
        }

        String title = "거래가 완료되었습니다! ✅";
        String role = isSeller ? "판매" : "구매";
        String message = String.format("'%s' %s 거래가 성공적으로 완료되었습니다. 수고하셨습니다!", auctionTitle, role);

        NotificationHistory notification = NotificationHistory.createNotification(
                user, NotificationType.TRANSACTION_COMPLETED, title, message, connectionId);
        notificationHistoryRepository.save(notification);

        sendFcmPush(setting.getFcmToken(), title, message, notification);

        // WebSocket 실시간 알림 발송
        sendWebSocketNotification(user.getId(), NotificationType.TRANSACTION_COMPLETED, title, message, connectionId);
    }
    
    /**
     * FCM 토큰 업데이트
     */
    @Transactional
    public void updateFcmToken(Long userId, String fcmToken) {
        NotificationSetting setting = notificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // 사용자 조회해서 기본 설정 생성
                    // User user = userRepository.findById(userId).orElseThrow();
                    // return NotificationSetting.createDefaultSetting(user);
                    // 임시로 null 반환, 실제로는 User 엔티티 필요
                    return null;
                });
        
        if (setting != null) {
            setting.updateFcmToken(fcmToken);
            notificationSettingRepository.save(setting);
            log.info("FCM 토큰 업데이트 완료. userId: {}", userId);
        }
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
    
    /**
     * WebSocket 실시간 알림 발송
     */
    private void sendWebSocketNotification(Long userId, NotificationType type, String title, String message, Long resourceId) {
        try {
            // NotificationType을 MessageType으로 매핑
            AuctionUpdateMessage.MessageType messageType = mapNotificationTypeToMessageType(type);

            // 기존 AuctionUpdateMessage 구조 활용하여 알림 메시지 생성
            AuctionUpdateMessage wsMessage = AuctionUpdateMessage.builder()
                    .messageType(messageType)
                    .auctionId(resourceId)
                    .message(message)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            // WebSocket으로 사용자에게 실시간 알림 발송
            webSocketMessagingService.sendToUser(userId, wsMessage);

            log.debug("WebSocket 실시간 알림 발송 성공. userId: {}, type: {}, messageType: {}", userId, type, messageType);

        } catch (Exception e) {
            log.error("WebSocket 실시간 알림 발송 실패. userId: {}, type: {}, error: {}", userId, type, e.getMessage());
        }
    }

    /**
     * NotificationType을 AuctionUpdateMessage.MessageType으로 매핑
     */
    private AuctionUpdateMessage.MessageType mapNotificationTypeToMessageType(NotificationType type) {
        switch (type) {
            case NEW_BID:
                return AuctionUpdateMessage.MessageType.NEW_BID;
            case AUCTION_WON:
                return AuctionUpdateMessage.MessageType.AUCTION_WON;
            case AUCTION_SOLD:
                return AuctionUpdateMessage.MessageType.AUCTION_SOLD;
            case CONNECTION_PAYMENT_REQUEST:
                return AuctionUpdateMessage.MessageType.CONNECTION_PAYMENT_REQUEST;
            case CHAT_ACTIVATED:
                return AuctionUpdateMessage.MessageType.CHAT_ACTIVATED;
            case TRANSACTION_COMPLETED:
                return AuctionUpdateMessage.MessageType.TRANSACTION_COMPLETED;
            case NEW_MESSAGE:
                return AuctionUpdateMessage.MessageType.NEW_MESSAGE;
            case PROMOTION:
                return AuctionUpdateMessage.MessageType.PROMOTION;
            case AUCTION_EXTENDED:
                return AuctionUpdateMessage.MessageType.AUCTION_EXTENDED;
            default:
                return AuctionUpdateMessage.MessageType.NEW_BID; // 기본값
        }
    }

    /**
     * 스나이핑 방지 시간 연장 알림 (입찰자들에게)
     */
    @Transactional
    public void sendAuctionExtendedNotification(User bidder, Long auctionId, String auctionTitle) {
        NotificationSetting setting = getOrCreateNotificationSetting(bidder);
        if (!setting.getBidNotification()) {
            log.debug("입찰 알림이 비활성화되어 있습니다. userId: {}", bidder.getId());
            return;
        }

        String title = "⏰ 경매 시간 연장";
        String message = String.format("'%s' 경매가 마감 직전 입찰로 인해 3분 연장되었습니다.", auctionTitle);

        // 알림 히스토리 저장
        NotificationHistory notification = NotificationHistory.createNotification(
                bidder, NotificationType.AUCTION_EXTENDED, title, message, auctionId);
        notificationHistoryRepository.save(notification);

        // FCM 푸시 발송
        sendFcmPush(setting.getFcmToken(), title, message, notification);

        // WebSocket 실시간 알림 발송
        sendWebSocketNotification(bidder.getId(), NotificationType.AUCTION_EXTENDED, title, message, auctionId);

        log.info("스나이핑 방지 시간 연장 알림 발송 완료. userId: {}, auctionId: {}", bidder.getId(), auctionId);
    }

    /**
     * 새 채팅 메시지 푸시 알림 발송
     */
    @Transactional
    public void sendNewMessageNotification(User receiver, Long chatRoomId, String senderNickname, String messagePreview) {
        NotificationSetting setting = getOrCreateNotificationSetting(receiver);
        if (!setting.getMessageNotification()) {
            log.debug("메시지 알림이 비활성화되어 있습니다. userId: {}", receiver.getId());
            return;
        }

        String title = senderNickname;
        String message = messagePreview.length() > 100
                ? messagePreview.substring(0, 100) + "..."
                : messagePreview;

        // 알림 히스토리 저장
        NotificationHistory notification = NotificationHistory.createNotification(
                receiver, NotificationType.NEW_MESSAGE, title, message, chatRoomId);
        notificationHistoryRepository.save(notification);

        // FCM 푸시 발송
        sendFcmPush(setting.getFcmToken(), title, message, notification, chatRoomId, "CHAT");

        log.info("새 메시지 알림 발송 완료. receiverId: {}, chatRoomId: {}", receiver.getId(), chatRoomId);
    }

    /**
     * 실제 FCM 푸시 발송
     */
    private void sendFcmPush(String fcmToken, String title, String message, NotificationHistory notification) {
        sendFcmPush(fcmToken, title, message, notification, null, null);
    }

    /**
     * 실제 FCM 푸시 발송 (추가 데이터 포함)
     */
    private void sendFcmPush(String fcmToken, String title, String message, NotificationHistory notification, Long resourceId, String notificationType) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("FCM 토큰이 없어 푸시 알림을 발송할 수 없습니다. notificationId: {}", notification.getId());
            return;
        }

        // Firebase가 초기화되지 않은 경우
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase가 초기화되지 않았습니다. 푸시 알림을 발송할 수 없습니다.");
            return;
        }

        try {
            // Android 설정 (알림 우선순위, 소리 등)
            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                            .setSound("default")
                            .setChannelId("chat_messages")
                            .build())
                    .build();

            // FCM 메시지 빌더
            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(message)
                            .build())
                    .setAndroidConfig(androidConfig);

            // 추가 데이터 설정 (앱에서 처리할 수 있도록)
            if (resourceId != null) {
                messageBuilder.putData("resourceId", String.valueOf(resourceId));
            }
            if (notificationType != null) {
                messageBuilder.putData("type", notificationType);
            }
            messageBuilder.putData("notificationId", String.valueOf(notification.getId()));

            // FCM 메시지 발송
            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());

            log.info("FCM 푸시 발송 성공: messageId={}, title={}", response, title);

            // 발송 성공 처리 (불변 객체 패턴)
            NotificationHistory updatedNotification = notification.markFcmSent();
            notificationHistoryRepository.save(updatedNotification);

        } catch (FirebaseMessagingException e) {
            log.error("FCM 푸시 발송 실패. notificationId: {}, errorCode: {}, error: {}",
                    notification.getId(), e.getMessagingErrorCode(), e.getMessage());

            // 토큰이 유효하지 않은 경우 처리
            if (e.getMessagingErrorCode() != null) {
                switch (e.getMessagingErrorCode()) {
                    case UNREGISTERED:
                    case INVALID_ARGUMENT:
                        log.warn("유효하지 않은 FCM 토큰입니다. 토큰 삭제가 필요합니다.");
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            log.error("FCM 푸시 발송 중 예외 발생. notificationId: {}, error: {}", notification.getId(), e.getMessage());
        }
    }
}