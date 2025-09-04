package com.cherrypick.app.domain.websocket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * WebSocket-specific implementation of AuctionEventObserver
 * Bridges auction events to WebSocket message publishing
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuctionObserver implements AuctionEventObserver {
    
    private final WebSocketMessagingService webSocketMessagingService;
    
    @Override
    public void onAuctionEvent(AuctionEvent event) {
        log.debug("Processing auction event: {} for auction {}", 
                 event.getEventType(), event.getAuctionId());
        
        switch (event.getEventType()) {
            case NEW_BID:
                webSocketMessagingService.notifyNewBid(
                    event.getAuctionId(),
                    event.getPrice(),
                    event.getBidCount(),
                    event.getBidderNickname()
                );
                break;
                
            case AUCTION_ENDED:
                webSocketMessagingService.notifyAuctionEnded(
                    event.getAuctionId(),
                    event.getPrice(),
                    event.getBidderNickname()
                );
                break;
                
            case BID_COUNT_CHANGED:
                webSocketMessagingService.notifyBidderCountChanged(
                    event.getAuctionId(),
                    event.getBidCount()
                );
                break;
                
            default:
                log.debug("Unhandled auction event type: {}", event.getEventType());
                break;
        }
    }
}