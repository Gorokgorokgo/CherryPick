package com.cherrypick.app.domain.websocket.service;

/**
 * Observer interface for auction events
 * Implements Observer pattern for real-time auction notifications
 */
public interface AuctionEventObserver {
    
    /**
     * Called when an auction event occurs
     * @param event The auction event that occurred
     */
    void onAuctionEvent(AuctionEvent event);
}