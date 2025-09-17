package com.cherrypick.app.domain.websocket.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Auction event data class
 * Represents events that occur during auctions
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode
public class AuctionEvent {
    
    private AuctionEventType eventType;
    private Long auctionId;
    private BigDecimal price;
    private String bidderNickname;
    private Integer bidCount;
    private LocalDateTime timestamp;
    
    // Convenience constructors
    public AuctionEvent(AuctionEventType eventType, Long auctionId, BigDecimal price, String bidderNickname) {
        this(eventType, auctionId, price, bidderNickname, null, LocalDateTime.now());
    }
    
    public AuctionEvent(AuctionEventType eventType, Long auctionId, BigDecimal price, String bidderNickname, Integer bidCount) {
        this(eventType, auctionId, price, bidderNickname, bidCount, LocalDateTime.now());
    }
}