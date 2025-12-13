package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.notification.enums.NotificationType;
import lombok.Getter;

/**
 * 유찰 알림 이벤트 (최고 입찰자용)
 * Reserve Price 미달로 유찰된 경우 최고 입찰자에게 전송
 */
@Getter
public class AuctionNotSoldForHighestBidderEvent extends NotificationEvent {

    private final String auctionTitle;
    private final Long highestBidAmount;

    public AuctionNotSoldForHighestBidderEvent(
            Object source,
            Long targetUserId,
            Long auctionId,
            String auctionTitle,
            Long highestBidAmount) {

        super(source,
              NotificationType.AUCTION_NOT_SOLD_HIGHEST_BIDDER,
              targetUserId,
              "경매가 유찰되었습니다",
              String.format("'%s' 경매가 유찰되었습니다.\n최고 입찰가: %,d원", auctionTitle, highestBidAmount),
              auctionId);

        this.auctionTitle = auctionTitle;
        this.highestBidAmount = highestBidAmount;
    }
}