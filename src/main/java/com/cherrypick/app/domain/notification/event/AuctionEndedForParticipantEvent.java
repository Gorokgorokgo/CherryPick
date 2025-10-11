package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.notification.enums.NotificationType;
import lombok.Getter;

/**
 * 경매 종료 알림 이벤트 (일반 참여자용)
 * 낙찰자가 아닌 다른 입찰 참여자들에게 전송
 */
@Getter
public class AuctionEndedForParticipantEvent extends NotificationEvent {

    private final String auctionTitle;
    private final Long finalPrice;
    private final boolean wasSuccessful; // 낙찰 성공 여부 (true: 낙찰, false: 유찰)

    public AuctionEndedForParticipantEvent(
            Object source,
            Long targetUserId,
            Long auctionId,
            String auctionTitle,
            Long finalPrice,
            boolean wasSuccessful) {

        super(source,
              NotificationType.AUCTION_ENDED,
              targetUserId,
              "경매가 종료되었습니다",
              wasSuccessful ?
                      String.format("'%s' 경매가 %,d원에 낙찰되었습니다.", auctionTitle, finalPrice) :
                      String.format("'%s' 경매가 유찰되었습니다.", auctionTitle),
              auctionId);

        this.auctionTitle = auctionTitle;
        this.finalPrice = finalPrice;
        this.wasSuccessful = wasSuccessful;
    }
}