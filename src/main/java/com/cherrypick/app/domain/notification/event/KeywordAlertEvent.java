package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.notification.enums.NotificationType;
import lombok.Getter;

/**
 * 키워드 알림 이벤트
 * 사용자가 등록한 키워드가 포함된 경매가 새로 등록되었을 때 발송
 */
@Getter
public class KeywordAlertEvent extends NotificationEvent {

    private final Long auctionId;
    private final String auctionTitle;
    private final String matchedKeyword;
    private final Long startPrice;
    private final String categoryName;

    public KeywordAlertEvent(
            Object source,
            Long targetUserId,
            Long auctionId,
            String auctionTitle,
            String matchedKeyword,
            Long startPrice,
            String categoryName) {
        super(source,
              NotificationType.KEYWORD_ALERT,
              targetUserId,
              buildTitle(matchedKeyword),
              buildMessage(auctionTitle, matchedKeyword, startPrice),
              auctionId);
        this.auctionId = auctionId;
        this.auctionTitle = auctionTitle;
        this.matchedKeyword = matchedKeyword;
        this.startPrice = startPrice;
        this.categoryName = categoryName;
    }

    private static String buildTitle(String matchedKeyword) {
        return String.format("🔔 '%s' 관련 새 경매!", matchedKeyword);
    }

    private static String buildMessage(String auctionTitle, String matchedKeyword, Long startPrice) {
        return String.format("'%s' 경매가 등록되었습니다. 시작가: %,d원",
                auctionTitle, startPrice);
    }
}
