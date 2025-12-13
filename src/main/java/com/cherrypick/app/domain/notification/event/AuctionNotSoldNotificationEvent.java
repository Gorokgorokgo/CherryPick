package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.notification.enums.NotificationType;
import lombok.Getter;

/**
 * 경매 유찰 알림 이벤트
 */
@Getter
public class AuctionNotSoldNotificationEvent extends NotificationEvent {

    private final String auctionTitle;
    private final Bid highestBid; // 최고 입찰 정보 (null일 수 있음)
    private final boolean hasHighestBidder; // 입찰자 유무

    public AuctionNotSoldNotificationEvent(Object source, Long sellerId, Long auctionId,
                                          String auctionTitle, Bid highestBid) {
        super(source, NotificationType.AUCTION_NOT_SOLD, sellerId,
              "경매가 유찰되었습니다",
              buildMessage(auctionTitle, highestBid),
              auctionId);
        this.auctionTitle = auctionTitle;
        this.highestBid = highestBid;
        this.hasHighestBidder = highestBid != null;
    }

    private static String buildMessage(String auctionTitle, Bid highestBid) {
        if (highestBid != null) {
            String bidderNickname = highestBid.getBidder().getNickname() != null ?
                highestBid.getBidder().getNickname() : "익명" + highestBid.getBidder().getId();
            return String.format("'%s' 경매가 유찰되었습니다.\n최고 입찰자(%s)님과 개인 거래를 시도해보세요.",
                                auctionTitle, bidderNickname);
        } else {
            return String.format("'%s' 경매가 유찰되었습니다. 입찰자가 없었습니다.", auctionTitle);
        }
    }
}
