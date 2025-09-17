package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.notification.enums.NotificationType;
import lombok.Getter;

/**
 * 새로운 입찰 알림 이벤트
 */
@Getter
public class NewBidNotificationEvent extends NotificationEvent {

    private final String auctionTitle;
    private final Long bidAmount;
    private final String bidderNickname;

    public NewBidNotificationEvent(Object source, Long sellerId, Long auctionId,
                                 String auctionTitle, Long bidAmount, String bidderNickname) {
        super(source, NotificationType.NEW_BID, sellerId,
              "새로운 입찰이 있습니다!",
              String.format("'%s' 경매에 %,d원 입찰이 들어왔습니다.", auctionTitle, bidAmount),
              auctionId);
        this.auctionTitle = auctionTitle;
        this.bidAmount = bidAmount;
        this.bidderNickname = bidderNickname;
    }
}