package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.notification.enums.NotificationType;
import lombok.Getter;

/**
 * 경매 마감 임박 알림 이벤트
 * 관심 경매(찜/입찰)가 곧 마감될 때 발송
 */
@Getter
public class AuctionEndingSoonEvent extends NotificationEvent {

    private final Long auctionId;
    private final String auctionTitle;
    private final Long currentPrice;
    private final int minutesRemaining; // 15 또는 5

    public AuctionEndingSoonEvent(
            Object source,
            Long targetUserId,
            Long auctionId,
            String auctionTitle,
            Long currentPrice,
            int minutesRemaining) {
        super(source,
              minutesRemaining == 15 ? NotificationType.AUCTION_ENDING_SOON_15M : NotificationType.AUCTION_ENDING_SOON_5M,
              targetUserId,
              buildTitle(minutesRemaining),
              buildMessage(auctionTitle, currentPrice, minutesRemaining),
              auctionId);
        this.auctionId = auctionId;
        this.auctionTitle = auctionTitle;
        this.currentPrice = currentPrice;
        this.minutesRemaining = minutesRemaining;
    }

    private static String buildTitle(int minutesRemaining) {
        return String.format("⏰ 관심 경매 %d분 전 마감!", minutesRemaining);
    }

    private static String buildMessage(String auctionTitle, Long currentPrice, int minutesRemaining) {
        return String.format("'%s' 경매가 %d분 후 마감됩니다.\n현재가: %,d원",
                auctionTitle, minutesRemaining, currentPrice);
    }
}
