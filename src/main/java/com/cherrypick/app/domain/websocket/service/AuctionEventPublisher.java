package com.cherrypick.app.domain.websocket.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Publisher component for auction events
 * Implements Publisher-Subscriber pattern for real-time auction notifications
 */
@Slf4j
@Component
public class AuctionEventPublisher {
    
    private final List<AuctionEventObserver> observers = new CopyOnWriteArrayList<>();
    
    /**
     * Subscribe an observer to receive auction events
     * @param observer The observer to subscribe
     */
    public void subscribe(AuctionEventObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            log.debug("Observer subscribed: {}", observer.getClass().getSimpleName());
        }
    }
    
    /**
     * Unsubscribe an observer from receiving auction events
     * @param observer The observer to unsubscribe
     */
    public void unsubscribe(AuctionEventObserver observer) {
        if (observers.remove(observer)) {
            log.debug("Observer unsubscribed: {}", observer.getClass().getSimpleName());
        }
    }
    
    /**
     * Publish an auction event to all subscribed observers
     * @param event The auction event to publish
     */
    public void publishEvent(AuctionEvent event) {
        log.debug("Publishing auction event: {} for auction {}", 
                 event.getEventType(), event.getAuctionId());
        
        for (AuctionEventObserver observer : observers) {
            try {
                observer.onAuctionEvent(event);
            } catch (Exception e) {
                log.error("Error notifying observer {} of auction event: {}", 
                         observer.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }
    
    /**
     * Get the current number of subscribed observers
     * @return Number of observers
     */
    public int getObserverCount() {
        return observers.size();
    }
}