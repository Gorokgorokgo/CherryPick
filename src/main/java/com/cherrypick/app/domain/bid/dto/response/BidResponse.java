package com.cherrypick.app.domain.bid.dto.response;

import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.enums.BidStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "입찰 응답")
public class BidResponse {

    @Schema(description = "입찰 ID", example = "1")
    private Long id;

    @Schema(description = "경매 ID", example = "1")
    private Long auctionId;

    @Schema(description = "경매 제목", example = "iPhone 14 Pro 팝니다")
    private String auctionTitle;

    @Schema(description = "입찰자 ID", example = "1")
    private Long bidderId;

    @Schema(description = "입찰자 닉네임", example = "체리픽유저")
    private String bidderNickname;

    @Schema(description = "입찰 금액", example = "15000")
    private BigDecimal bidAmount;

    @Schema(description = "자동 입찰 여부", example = "false")
    private Boolean isAutoBid;

    @Schema(description = "자동 입찰 최대 금액", example = "20000")
    private BigDecimal maxAutoBidAmount;

    @Schema(description = "입찰 상태", example = "ACTIVE")
    private BidStatus status;

    @Schema(description = "입찰 시간", example = "2024-12-25T14:30:00")
    private LocalDateTime bidTime;

    @Schema(description = "최고가 입찰 여부", example = "true")
    private Boolean isHighestBid;

    /**
     * Bid 엔티티로부터 BidResponse 생성
     */
    public static BidResponse from(Bid bid) {
        return BidResponse.builder()
                .id(bid.getId())
                .auctionId(bid.getAuction().getId())
                .auctionTitle(bid.getAuction().getTitle())
                .bidderId(bid.getBidder().getId())
                .bidderNickname(bid.getBidder().getNickname())
                .bidAmount(bid.getBidAmount())
                .isAutoBid(bid.getIsAutoBid())
                .maxAutoBidAmount(bid.getMaxAutoBidAmount())
                .status(bid.getStatus())
                .bidTime(bid.getBidTime())
                .build();
    }

    /**
     * 최고가 입찰 여부를 설정하여 BidResponse 생성
     */
    public static BidResponse from(Bid bid, boolean isHighestBid) {
        BidResponse response = from(bid);
        response.setIsHighestBid(isHighestBid);
        return response;
    }
}