package com.cherrypick.app.domain.bid.event;

import java.math.BigDecimal;

/**
 * 수동입찰 완료 이벤트
 * 수동입찰 트랜잭션 커밋 후 자동입찰 처리를 위한 이벤트
 */
public class ManualBidCompletedEvent {
    private final Long auctionId;
    private final BigDecimal bidAmount;
    private final Long bidderId;

    public ManualBidCompletedEvent(Long auctionId, BigDecimal bidAmount, Long bidderId) {
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
        this.bidderId = bidderId;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public Long getBidderId() {
        return bidderId;
    }

    @Override
    public String toString() {
        return "ManualBidCompletedEvent{" +
                "auctionId=" + auctionId +
                ", bidAmount=" + bidAmount +
                ", bidderId=" + bidderId +
                '}';
    }
}