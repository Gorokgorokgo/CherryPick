package com.cherrypick.app.domain.websocket.service;

import com.cherrypick.app.domain.websocket.dto.AuctionUpdateMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Observer Pattern Test for Auction Events
 * TDD Red Phase for Publisher-Subscriber pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Auction Event Observer Pattern Test")
class AuctionEventObserverTest {

    @Mock
    private WebSocketMessagingService webSocketMessagingService;

    private AuctionEventPublisher auctionEventPublisher;
    private TestAuctionEventObserver testObserver1;
    private TestAuctionEventObserver testObserver2;

    @BeforeEach
    void setUp() {
        auctionEventPublisher = new AuctionEventPublisher();
        testObserver1 = new TestAuctionEventObserver("Observer1");
        testObserver2 = new TestAuctionEventObserver("Observer2");
    }

    @Test
    @DisplayName("Observer can subscribe and receive auction events")
    void should_ReceiveEvents_When_ObserverSubscribed() {
        // Given: Observer subscribes to auction events
        auctionEventPublisher.subscribe(testObserver1);
        
        AuctionEvent bidEvent = new AuctionEvent(
            AuctionEventType.NEW_BID, 1L, new BigDecimal("15000"), "TestBidder"
        );

        // When: Event is published
        auctionEventPublisher.publishEvent(bidEvent);

        // Then: Observer receives the event
        assertThat(testObserver1.getReceivedEvents()).hasSize(1);
        assertThat(testObserver1.getReceivedEvents().get(0)).isEqualTo(bidEvent);
    }

    @Test
    @DisplayName("Multiple observers can receive the same event")
    void should_NotifyAllObservers_When_EventPublished() {
        // Given: Multiple observers subscribe
        auctionEventPublisher.subscribe(testObserver1);
        auctionEventPublisher.subscribe(testObserver2);
        
        AuctionEvent endEvent = new AuctionEvent(
            AuctionEventType.AUCTION_ENDED, 2L, new BigDecimal("50000"), "Winner"
        );

        // When: Event is published
        auctionEventPublisher.publishEvent(endEvent);

        // Then: Both observers receive the event
        assertThat(testObserver1.getReceivedEvents()).hasSize(1);
        assertThat(testObserver2.getReceivedEvents()).hasSize(1);
        
        assertThat(testObserver1.getReceivedEvents().get(0)).isEqualTo(endEvent);
        assertThat(testObserver2.getReceivedEvents().get(0)).isEqualTo(endEvent);
    }

    @Test
    @DisplayName("Observer can unsubscribe and stop receiving events")
    void should_NotReceiveEvents_When_ObserverUnsubscribed() {
        // Given: Observer subscribes and receives initial event
        auctionEventPublisher.subscribe(testObserver1);
        auctionEventPublisher.publishEvent(
            new AuctionEvent(AuctionEventType.NEW_BID, 1L, new BigDecimal("10000"), "Bidder1")
        );
        assertThat(testObserver1.getReceivedEvents()).hasSize(1);

        // When: Observer unsubscribes
        auctionEventPublisher.unsubscribe(testObserver1);
        auctionEventPublisher.publishEvent(
            new AuctionEvent(AuctionEventType.NEW_BID, 1L, new BigDecimal("20000"), "Bidder2")
        );

        // Then: Observer does not receive new events
        assertThat(testObserver1.getReceivedEvents()).hasSize(1); // Still only the first event
    }

    @Test
    @DisplayName("WebSocket publisher integrates with observer pattern")
    void should_SendWebSocketMessage_When_AuctionEventObserved() {
        // Given: WebSocket messaging service as observer
        WebSocketAuctionObserver webSocketObserver = new WebSocketAuctionObserver(webSocketMessagingService);
        auctionEventPublisher.subscribe(webSocketObserver);

        AuctionEvent bidEvent = new AuctionEvent(
            AuctionEventType.NEW_BID, 1L, new BigDecimal("25000"), "WebSocketTester"
        );

        // When: Auction event is published
        auctionEventPublisher.publishEvent(bidEvent);

        // Then: WebSocket messaging service is called
        verify(webSocketMessagingService).notifyNewBid(
            eq(1L), eq(new BigDecimal("25000")), any(), eq("WebSocketTester")
        );
    }

    @Test
    @DisplayName("Event filtering works correctly for specific auction IDs")
    void should_FilterEvents_When_ObserverSubscribesToSpecificAuction() {
        // Given: Observer subscribes to specific auction only
        FilteredAuctionObserver filteredObserver = new FilteredAuctionObserver(1L);
        auctionEventPublisher.subscribe(filteredObserver);

        // When: Events for different auctions are published
        auctionEventPublisher.publishEvent(
            new AuctionEvent(AuctionEventType.NEW_BID, 1L, new BigDecimal("10000"), "Bidder1")
        );
        auctionEventPublisher.publishEvent(
            new AuctionEvent(AuctionEventType.NEW_BID, 2L, new BigDecimal("20000"), "Bidder2")
        );
        auctionEventPublisher.publishEvent(
            new AuctionEvent(AuctionEventType.NEW_BID, 1L, new BigDecimal("15000"), "Bidder3")
        );

        // Then: Observer only receives events for auction ID 1
        assertThat(filteredObserver.getReceivedEvents()).hasSize(2);
        assertThat(filteredObserver.getReceivedEvents())
            .allMatch(event -> event.getAuctionId().equals(1L));
    }

    // Test helper classes
    private static class TestAuctionEventObserver implements AuctionEventObserver {
        private final String name;
        private final List<AuctionEvent> receivedEvents = new CopyOnWriteArrayList<>();

        public TestAuctionEventObserver(String name) {
            this.name = name;
        }

        @Override
        public void onAuctionEvent(AuctionEvent event) {
            receivedEvents.add(event);
        }

        public List<AuctionEvent> getReceivedEvents() {
            return receivedEvents;
        }
    }

    private static class FilteredAuctionObserver implements AuctionEventObserver {
        private final Long targetAuctionId;
        private final List<AuctionEvent> receivedEvents = new CopyOnWriteArrayList<>();

        public FilteredAuctionObserver(Long targetAuctionId) {
            this.targetAuctionId = targetAuctionId;
        }

        @Override
        public void onAuctionEvent(AuctionEvent event) {
            if (targetAuctionId.equals(event.getAuctionId())) {
                receivedEvents.add(event);
            }
        }

        public List<AuctionEvent> getReceivedEvents() {
            return receivedEvents;
        }
    }
}