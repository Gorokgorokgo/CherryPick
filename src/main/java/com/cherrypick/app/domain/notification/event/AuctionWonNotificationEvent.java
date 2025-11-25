package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.notification.enums.NotificationType;
import com.cherrypick.app.domain.user.dto.response.ExperienceGainResponse;
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
    private final ExperienceGainResponse experienceGain; // 낙찰 경험치 정보
    public AuctionWonNotificationEvent(Object source, Long buyerId, Long auctionId,
                                     String auctionTitle, Long finalPrice, String sellerNickname, Long chatRoomId,
                                     ExperienceGainResponse experienceGain) {
        super(source, NotificationType.AUCTION_WON, buyerId,
              "낙찰되었습니다! 🎉",
              String.format("'%s' 경매가 %,d원에 낙찰되었습니다. +%d EXP를 획득하셨습니다!",
                  auctionTitle, finalPrice, experienceGain != null ? experienceGain.getExpGained() : 0),
              auctionId);
        this.auctionTitle = auctionTitle;
        this.finalPrice = finalPrice;
        this.sellerNickname = sellerNickname;
        this.chatRoomId = chatRoomId;
        this.experienceGain = experienceGain;
    }

    // 기존 생성자 호환성 유지 (deprecated)
    @Deprecated
    public AuctionWonNotificationEvent(Object source, Long buyerId, Long auctionId,
                                     String auctionTitle, Long finalPrice, String sellerNickname, Long chatRoomId) {
        this(source, buyerId, auctionId, auctionTitle, finalPrice, sellerNickname, chatRoomId, null);
    }
}