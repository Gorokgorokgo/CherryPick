package com.cherrypick.app.domain.websocket.service;

/**
 * Types of auction events that can occur
 */
public enum AuctionEventType {
    NEW_BID,           // New bid placed
    AUCTION_ENDED,     // Auction has ended
    AUCTION_EXTENDED,  // Auction time extended due to late bids
    BID_COUNT_CHANGED, // Number of bidders changed
    AUCTION_STARTED    // Auction has started
}