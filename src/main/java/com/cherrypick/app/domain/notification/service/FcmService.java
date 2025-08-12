package com.cherrypick.app.domain.notification.service;

import com.cherrypick.app.domain.notification.entity.NotificationHistory;
import com.cherrypick.app.domain.notification.entity.NotificationSetting;
import com.cherrypick.app.domain.notification.enums.NotificationType;
import com.cherrypick.app.domain.notification.repository.NotificationHistoryRepository;
import com.cherrypick.app.domain.notification.repository.NotificationSettingRepository;
import com.cherrypick.app.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    }
    
    /**
     * 낙찰 알림 (구매자용)
     */
    @Transactional
    public void sendAuctionWonNotification(User buyer, Long auctionId, String auctionTitle, Long finalPrice) {
        NotificationSetting setting = getOrCreateNotificationSetting(buyer);
        if (!setting.getWinningNotification()) {
            return;
        }
        
        String title = "낙찰되었습니다! 🎉";
        String message = String.format("'%s' 경매에서 %,d원에 낙찰되었습니다. 판매자의 연결 서비스 결제를 기다려주세요.", auctionTitle, finalPrice);
        
        NotificationHistory notification = NotificationHistory.createNotification(
                buyer, NotificationType.AUCTION_WON, title, message, auctionId);
        notificationHistoryRepository.save(notification);
        
        sendFcmPush(setting.getFcmToken(), title, message, notification);
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
     * 실제 FCM 푸시 발송 (현재는 로그만, 추후 Firebase SDK 연동)
     */
    private void sendFcmPush(String fcmToken, String title, String message, NotificationHistory notification) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("FCM 토큰이 없어 푸시 알림을 발송할 수 없습니다. notificationId: {}", notification.getId());
            return;
        }
        
        try {
            // TODO: 실제 FCM SDK를 통한 푸시 발송
            // FirebaseMessaging.getInstance().send(
            //     Message.builder()
            //         .setToken(fcmToken)
            //         .setNotification(Notification.builder()
            //             .setTitle(title)
            //             .setBody(message)
            //             .build())
            //         .build()
            // );
            
            // 현재는 로그로 대체
            log.info("FCM 푸시 발송 (모의): token={}, title={}, message={}", 
                    fcmToken.substring(0, Math.min(20, fcmToken.length())) + "...", title, message);
            
            // 발송 성공 처리 (불변 객체 패턴)
            NotificationHistory updatedNotification = notification.markFcmSent();
            notificationHistoryRepository.save(updatedNotification);
            
        } catch (Exception e) {
            log.error("FCM 푸시 발송 실패. notificationId: {}, error: {}", notification.getId(), e.getMessage());
        }
    }
}