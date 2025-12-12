package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.notification.enums.NotificationType;
import lombok.Getter;

/**
 * 거래 확인 대기 알림 이벤트
 * 상대방이 거래 확인 버튼을 눌렀을 때 발생
 */
@Getter
public class TransactionPendingNotificationEvent extends NotificationEvent {

    private final String auctionTitle;
    private final String confirmerRole;  // 확인한 사람의 역할 (판매자/구매자)
    private final Long chatRoomId;

    public TransactionPendingNotificationEvent(Object source, Long userId, Long auctionId,
                                               String auctionTitle, String confirmerRole, Long chatRoomId) {
        super(source, NotificationType.TRANSACTION_PENDING, userId,
              "거래 확인 요청 ⏳",
              String.format("'%s' %s님이 거래 확인을 완료했습니다. 회원님도 확인해주세요!",
                          auctionTitle, confirmerRole),
              auctionId);
        this.auctionTitle = auctionTitle;
        this.confirmerRole = confirmerRole;
        this.chatRoomId = chatRoomId;
    }
    
    @Override
    public Long getChatRoomId() {
        return chatRoomId;
    }
}
