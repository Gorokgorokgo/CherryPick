package com.cherrypick.app.domain.notification.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 알림 설정 업데이트 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class UpdateNotificationSettingRequest {

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
}