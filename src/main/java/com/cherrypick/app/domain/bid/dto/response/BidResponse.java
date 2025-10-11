package com.cherrypick.app.domain.bid.dto.response;

import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.enums.BidStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class BidResponse {
    private Long id;
    private Long auctionId;
    private Long bidderId;
    private String bidderNickname;
    private BigDecimal bidAmount;
    private Boolean isAutoBid;
    private BigDecimal maxAutoBidAmount;
    private BidStatus status;
    private LocalDateTime bidTime;
    private Boolean isHighestBid;

    public static BidResponse from(Bid bid) {
        return BidResponse.builder()
                .id(bid.getId())
                .auctionId(bid.getAuction().getId())
                .bidderId(bid.getBidder().getId())
                .bidderNickname(bid.getBidder().getNickname())
                .bidAmount(bid.getBidAmount())
                .isAutoBid(bid.getIsAutoBid())
                .maxAutoBidAmount(bid.getMaxAutoBidAmount())
                .status(bid.getStatus())
                .bidTime(bid.getBidTime())
                .isHighestBid(false)
                .build();
    }

    public static BidResponse from(Bid bid, boolean isHighestBid) {
        return BidResponse.builder()
                .id(bid.getId())
                .auctionId(bid.getAuction().getId())
                .bidderId(bid.getBidder().getId())
                .bidderNickname(bid.getBidder().getNickname())
                .bidAmount(bid.getBidAmount())
                .isAutoBid(bid.getIsAutoBid())
                .maxAutoBidAmount(bid.getMaxAutoBidAmount())
                .status(bid.getStatus())
                .bidTime(bid.getBidTime())
                .isHighestBid(isHighestBid)
                .build();
    }
}