package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.notification.enums.NotificationType;
import lombok.Getter;

/**
 * 경매 낙찰 알림 이벤트 (판매자용)
 */
@Getter
public class AuctionSoldNotificationEvent extends NotificationEvent {

    private final String auctionTitle;
    private final Long finalPrice;
    private final String winnerNickname;
    private final Long chatRoomId;

    public AuctionSoldNotificationEvent(Object source, Long sellerId, Long auctionId,
                                        String auctionTitle, Long finalPrice, String winnerNickname,
                                        Long chatRoomId) {
        super(source, NotificationType.AUCTION_SOLD, sellerId,
              "축하합니다! 경매가 낙찰되었습니다! 🎉",
              String.format("'%s' 경매가 %,d원에 낙찰되었습니다. 낙찰자(%s)님과의 거래를 시작해주세요.",
                           auctionTitle, finalPrice, winnerNickname),
              auctionId);
        this.auctionTitle = auctionTitle;
        this.finalPrice = finalPrice;
        this.winnerNickname = winnerNickname;
        this.chatRoomId = chatRoomId;
    }
}
