package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.notification.enums.NotificationType;
import lombok.Getter;

/**
 * 낙찰 알림 이벤트
 */
@Getter
public class AuctionWonNotificationEvent extends NotificationEvent {

    private final String auctionTitle;
    private final Long finalPrice;
    private final Long chatRoomId;

    public AuctionWonNotificationEvent(Object source, Long buyerId, Long auctionId,
                                     String auctionTitle, Long finalPrice, Long chatRoomId) {
        super(source, NotificationType.AUCTION_WON, buyerId,
              "낙찰되었습니다! 🎉",
              String.format("'%s' 경매에서 %,d원에 낙찰되었습니다. 판매자와 채팅을 시작해주세요.", auctionTitle, finalPrice),
              auctionId);
        this.auctionTitle = auctionTitle;
        this.finalPrice = finalPrice;
        this.chatRoomId = chatRoomId;
    }
}