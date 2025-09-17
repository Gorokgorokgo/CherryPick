package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.notification.enums.NotificationType;
import lombok.Getter;

/**
 * 연결 서비스 결제 요청 알림 이벤트
 */
@Getter
public class ConnectionPaymentRequestNotificationEvent extends NotificationEvent {

    private final String auctionTitle;

    public ConnectionPaymentRequestNotificationEvent(Object source, Long sellerId, Long connectionId,
                                                   String auctionTitle) {
        super(source, NotificationType.CONNECTION_PAYMENT_REQUEST, sellerId,
              "연결 서비스 결제 요청",
              String.format("'%s' 경매의 연결 서비스 수수료를 결제하고 구매자와 채팅을 시작하세요!", auctionTitle),
              connectionId);
        this.auctionTitle = auctionTitle;
    }
}