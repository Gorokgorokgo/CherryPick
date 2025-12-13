package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.notification.enums.NotificationType;
import lombok.Getter;

/**
 * Outbid 알림 이벤트 (이전 최고 입찰자에게)
 * 다른 사용자가 더 높은 금액으로 입찰했을 때 발송
 */
@Getter
public class OutbidNotificationEvent extends NotificationEvent {

    private final Long auctionId;
    private final String auctionTitle;
    private final Long previousBidAmount;
    private final Long newBidAmount;
    private final String newBidderNickname;
    private final int outbidCount; // 그룹 알림용: "A님 외 N명이 입찰"

    public OutbidNotificationEvent(
            Object source,
            Long previousBidderId,
            Long auctionId,
            String auctionTitle,
            Long previousBidAmount,
            Long newBidAmount,
            String newBidderNickname,
            int outbidCount) {
        super(source,
              NotificationType.OUTBID,
              previousBidderId,
              "더 높은 입찰이 있습니다!",
              buildMessage(auctionTitle, newBidderNickname, outbidCount, newBidAmount, previousBidAmount),
              auctionId);
        this.auctionId = auctionId;
        this.auctionTitle = auctionTitle;
        this.previousBidAmount = previousBidAmount;
        this.newBidAmount = newBidAmount;
        this.newBidderNickname = newBidderNickname;
        this.outbidCount = outbidCount;
    }

    private static String buildMessage(String auctionTitle, String newBidderNickname,
                                        int outbidCount, Long newBidAmount, Long previousBidAmount) {
        if (outbidCount > 1) {
            // 그룹 알림: "A님 외 N명이 입찰했습니다"
            return String.format("'%s' 경매에 %s님 외 %d명이 입찰했습니다. 현재가: %,d원",
                    auctionTitle, newBidderNickname, outbidCount - 1, newBidAmount);
        } else {
            return String.format("'%s' 경매에 %,d원의 새로운 입찰이 있습니다. (내 입찰: %,d원)",
                    auctionTitle, newBidAmount, previousBidAmount);
        }
    }
}
