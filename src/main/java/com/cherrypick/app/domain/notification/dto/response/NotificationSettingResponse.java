package com.cherrypick.app.domain.notification.dto.response;

import com.cherrypick.app.domain.notification.entity.NotificationSetting;
import lombok.Builder;
import lombok.Getter;

/**
 * 알림 설정 응답 DTO
 */
@Getter
@Builder
public class NotificationSettingResponse {

    private Long id;
    private Long userId;
    
    /**
     * 새로운 입찰 알림 (판매자용)
     */
    private Boolean bidNotification;

    /**
     * 낙찰 알림 (구매자용)
     */
    private Boolean winningNotification;

    /**
     * 연결 서비스 결제 요청 알림 (판매자용)
     */
    private Boolean connectionPaymentNotification;

    /**
     * 채팅 활성화 알림 (구매자용)
     */
    private Boolean chatActivationNotification;

    /**
     * 새 메시지 알림
     */
    private Boolean messageNotification;

    /**
     * 거래 완료 알림
     */
    private Boolean transactionCompletionNotification;

    /**
     * 프로모션 알림
     */
    private Boolean promotionNotification;
    
    /**
     * FCM 토큰 등록 여부
     */
    private Boolean fcmTokenRegistered;

    /**
     * Entity에서 DTO로 변환
     */
    public static NotificationSettingResponse from(NotificationSetting setting) {
        return NotificationSettingResponse.builder()
                .id(setting.getId())
                .userId(setting.getUser().getId())
                .bidNotification(setting.getBidNotification())
                .winningNotification(setting.getWinningNotification())
                .connectionPaymentNotification(setting.getConnectionPaymentNotification())
                .chatActivationNotification(setting.getChatActivationNotification())
                .messageNotification(setting.getMessageNotification())
                .transactionCompletionNotification(setting.getTransactionCompletionNotification())
                .promotionNotification(setting.getPromotionNotification())
                .fcmTokenRegistered(setting.getFcmToken() != null && !setting.getFcmToken().isEmpty())
                .build();
    }
}