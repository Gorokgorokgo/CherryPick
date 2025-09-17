package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.notification.enums.NotificationType;
import lombok.Getter;

/**
 * 채팅 활성화 알림 이벤트
 */
@Getter
public class ChatActivatedNotificationEvent extends NotificationEvent {

    private final String auctionTitle;

    public ChatActivatedNotificationEvent(Object source, Long buyerId, Long chatRoomId,
                                        String auctionTitle) {
        super(source, NotificationType.CHAT_ACTIVATED, buyerId,
              "채팅이 활성화되었습니다!",
              String.format("'%s' 경매의 판매자와 채팅을 시작할 수 있습니다. 거래 조건을 협의해보세요!", auctionTitle),
              chatRoomId);
        this.auctionTitle = auctionTitle;
    }
}