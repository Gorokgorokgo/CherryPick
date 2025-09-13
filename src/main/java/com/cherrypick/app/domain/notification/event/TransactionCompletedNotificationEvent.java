package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.notification.enums.NotificationType;
import lombok.Getter;

/**
 * 거래 완료 알림 이벤트
 */
@Getter
public class TransactionCompletedNotificationEvent extends NotificationEvent {

    private final String auctionTitle;
    private final boolean isSeller;

    public TransactionCompletedNotificationEvent(Object source, Long userId, Long connectionId,
                                               String auctionTitle, boolean isSeller) {
        super(source, NotificationType.TRANSACTION_COMPLETED, userId,
              "거래가 완료되었습니다! ✅",
              String.format("'%s' %s 거래가 성공적으로 완료되었습니다. 수고하셨습니다!",
                          auctionTitle, isSeller ? "판매" : "구매"),
              connectionId);
        this.auctionTitle = auctionTitle;
        this.isSeller = isSeller;
    }
}