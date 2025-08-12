package com.cherrypick.app.domain.notification.entity;

import com.cherrypick.app.domain.common.entity.BaseEntity;
import com.cherrypick.app.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자별 알림 설정
 */
@Entity
@Table(name = "notification_settings")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * FCM 토큰 (모바일 기기별)
     */
    @Column(name = "fcm_token", length = 500)
    private String fcmToken;

    // === 입찰 관련 알림 ===
    
    /**
     * 새로운 입찰 알림 (판매자용)
     */
    @Builder.Default
    @Column(name = "bid_notification", nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean bidNotification = true;

    /**
     * 낙찰 알림 (구매자용)
     */
    @Builder.Default
    @Column(name = "winning_notification", nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean winningNotification = true;

    // === 연결 서비스 관련 알림 ===
    
    /**
     * 연결 서비스 결제 요청 알림 (판매자용)
     */
    @Builder.Default
    @Column(name = "connection_payment_notification", nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean connectionPaymentNotification = true;

    /**
     * 채팅 활성화 알림 (구매자용)
     */
    @Builder.Default
    @Column(name = "chat_activation_notification", nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean chatActivationNotification = true;

    // === 채팅 관련 알림 ===
    
    /**
     * 새 메시지 알림
     */
    @Builder.Default
    @Column(name = "message_notification", nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean messageNotification = true;

    // === 거래 관련 알림 ===
    
    /**
     * 거래 완료 알림
     */
    @Builder.Default
    @Column(name = "transaction_completion_notification", nullable = false, columnDefinition = "BOOLEAN DEFAULT true")
    private Boolean transactionCompletionNotification = true;

    // === 마케팅 알림 ===
    
    /**
     * 프로모션 알림
     */
    @Builder.Default
    @Column(name = "promotion_notification", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean promotionNotification = false;

    // === 정적 팩토리 메서드 ===
    
    /**
     * 기본 알림 설정 생성
     */
    public static NotificationSetting createDefaultSetting(User user) {
        return NotificationSetting.builder()
                .user(user)
                .build();
    }

    // === 비즈니스 메서드 ===
    
    /**
     * FCM 토큰 업데이트
     */
    public NotificationSetting updateFcmToken(String fcmToken) {
        return NotificationSetting.builder()
                .id(this.id)
                .user(this.user)
                .fcmToken(fcmToken)
                .bidNotification(this.bidNotification)
                .winningNotification(this.winningNotification)
                .connectionPaymentNotification(this.connectionPaymentNotification)
                .chatActivationNotification(this.chatActivationNotification)
                .messageNotification(this.messageNotification)
                .transactionCompletionNotification(this.transactionCompletionNotification)
                .promotionNotification(this.promotionNotification)
                .build();
    }
    
    /**
     * 모든 알림 끄기
     */
    public NotificationSetting disableAllNotifications() {
        return NotificationSetting.builder()
                .id(this.id)
                .user(this.user)
                .fcmToken(this.fcmToken)
                .bidNotification(false)
                .winningNotification(false)
                .connectionPaymentNotification(false)
                .chatActivationNotification(false)
                .messageNotification(false)
                .transactionCompletionNotification(false)
                .promotionNotification(false)
                .build();
    }
    
    /**
     * 기본 알림만 켜기 (중요 알림)
     */
    public NotificationSetting enableEssentialNotificationsOnly() {
        return NotificationSetting.builder()
                .id(this.id)
                .user(this.user)
                .fcmToken(this.fcmToken)
                .bidNotification(true)
                .winningNotification(true)
                .connectionPaymentNotification(true)
                .chatActivationNotification(true)
                .messageNotification(false) // 채팅은 선택사항
                .transactionCompletionNotification(true)
                .promotionNotification(false)
                .build();
    }
    
    /**
     * 개별 알림 설정 업데이트
     */
    public NotificationSetting updateSettings(
            Boolean bidNotification,
            Boolean winningNotification,
            Boolean connectionPaymentNotification,
            Boolean chatActivationNotification,
            Boolean messageNotification,
            Boolean transactionCompletionNotification,
            Boolean promotionNotification) {
        
        return NotificationSetting.builder()
                .id(this.id)
                .user(this.user)
                .fcmToken(this.fcmToken)
                .bidNotification(bidNotification != null ? bidNotification : this.bidNotification)
                .winningNotification(winningNotification != null ? winningNotification : this.winningNotification)
                .connectionPaymentNotification(connectionPaymentNotification != null ? connectionPaymentNotification : this.connectionPaymentNotification)
                .chatActivationNotification(chatActivationNotification != null ? chatActivationNotification : this.chatActivationNotification)
                .messageNotification(messageNotification != null ? messageNotification : this.messageNotification)
                .transactionCompletionNotification(transactionCompletionNotification != null ? transactionCompletionNotification : this.transactionCompletionNotification)
                .promotionNotification(promotionNotification != null ? promotionNotification : this.promotionNotification)
                .build();
    }
}