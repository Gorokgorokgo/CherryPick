package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.notification.enums.NotificationType;
import lombok.Getter;

/**
 * 거래 취소 알림 이벤트
 * 상대방이 거래를 취소했을 때 발생
 */
@Getter
public class TransactionCancelledNotificationEvent extends NotificationEvent {

    private final String auctionTitle;
    private final String cancellerRole;  // 취소한 사람의 역할 (판매자/구매자)
    private final Long chatRoomId;

    public TransactionCancelledNotificationEvent(Object source, Long userId, Long auctionId,
                                                 String auctionTitle, String cancellerRole, Long chatRoomId) {
        super(source, NotificationType.TRANSACTION_CANCELLED, userId,
              "거래가 취소되었습니다 ❌",
              String.format("'%s' 거래가 %s님에 의해 취소되었습니다.",
                          auctionTitle, cancellerRole),
              auctionId);
        this.auctionTitle = auctionTitle;
        this.cancellerRole = cancellerRole;
        this.chatRoomId = chatRoomId;
    }
    
    @Override
    public Long getChatRoomId() {
        return chatRoomId;
    }
}
