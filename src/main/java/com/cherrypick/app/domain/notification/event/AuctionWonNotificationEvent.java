package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.notification.enums.NotificationType;
import lombok.Getter;

/**
 * 낙찰 알림 이벤트 (구매자용)
 */
@Getter
public class AuctionWonNotificationEvent extends NotificationEvent {

    private final String auctionTitle;
    private final Long finalPrice;
    private final String sellerNickname;
    private final Long chatRoomId;

    public AuctionWonNotificationEvent(Object source, Long buyerId, Long auctionId,
                                     String auctionTitle, Long finalPrice, String sellerNickname, Long chatRoomId) {
        super(source, NotificationType.AUCTION_WON, buyerId,
              "낙찰되었습니다! 🎉",
              String.format("'%s' 경매가 %,d원에 낙찰되었습니다. 판매자(%s)님과의 거래를 시작해주세요.", auctionTitle, finalPrice, sellerNickname),
              auctionId);
        this.auctionTitle = auctionTitle;
        this.finalPrice = finalPrice;
        this.sellerNickname = sellerNickname;
        this.chatRoomId = chatRoomId;
    }

    // 기존 생성자 호환성 유지 (deprecated)
    @Deprecated
    public AuctionWonNotificationEvent(Object source, Long buyerId, Long auctionId,
                                     String auctionTitle, Long finalPrice, Long chatRoomId) {
        this(source, buyerId, auctionId, auctionTitle, finalPrice, "판매자", chatRoomId);
    }
}