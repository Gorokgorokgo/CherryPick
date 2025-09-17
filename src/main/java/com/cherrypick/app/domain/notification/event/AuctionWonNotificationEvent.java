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

    public AuctionWonNotificationEvent(Object source, Long buyerId, Long auctionId,
                                     String auctionTitle, Long finalPrice) {
        super(source, NotificationType.AUCTION_WON, buyerId,
              "낙찰되었습니다! 🎉",
              String.format("'%s' 경매에서 %,d원에 낙찰되었습니다. 판매자의 연결 서비스 결제를 기다려주세요.", auctionTitle, finalPrice),
              auctionId);
        this.auctionTitle = auctionTitle;
        this.finalPrice = finalPrice;
    }
}