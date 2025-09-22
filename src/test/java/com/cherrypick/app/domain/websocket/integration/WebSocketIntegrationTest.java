package com.cherrypick.app.domain.websocket.integration;

import com.cherrypick.app.domain.websocket.dto.AuctionUpdateMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WebSocket STOMP 통합 테스트
 * 실제 WebSocket 연결을 통한 메시지 송수신 테스트
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("WebSocket STOMP 통합 테스트")
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;
    private String wsUrl;

    @BeforeEach
    void setUp() {
        wsUrl = "ws://localhost:" + port + "/ws";
        
        // STOMP 클라이언트 설정
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        stompClient.setDefaultHeartbeat(new long[]{0, 0}); // 테스트용 하트비트 비활성화
        
        log.info("WebSocket 테스트 URL: {}", wsUrl);
    }

    @Test
    @DisplayName("클라이언트가 경매 구독 후 실시간 메시지를 수신할 수 있다")
    void should_ReceiveRealtimeMessages_When_SubscribedToAuction() throws Exception {
        // Given: WebSocket 연결 및 메시지 수신 큐 준비
        BlockingQueue<AuctionUpdateMessage> messageQueue = new LinkedBlockingQueue<>();
        
        StompSession session = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                log.info("WebSocket 연결 성공");
            }
            
            @Override
            public void handleException(StompSession session, StompCommand command, 
                                      StompHeaders headers, byte[] payload, Throwable exception) {
                log.error("WebSocket 오류: ", exception);
            }
        }).get(10, TimeUnit.SECONDS);

        // 경매 구독
        Long auctionId = 1L;
        session.subscribe("/topic/auctions/" + auctionId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return AuctionUpdateMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                log.info("메시지 수신: {}", payload);
                messageQueue.offer((AuctionUpdateMessage) payload);
            }
        });

        // When: 서버에서 메시지 발송 (실제 입찰 API 호출 시뮬레이션)
        AuctionUpdateMessage testMessage = AuctionUpdateMessage.builder()
                .messageType("NEW_BID")
                .auctionId(auctionId)
                .currentPrice(new BigDecimal("25000"))
                .bidCount(3)
                .highestBidderNickname("테스트입찰자")
                .timestamp(java.time.LocalDateTime.now())
                .build();

        // 메시지 전송을 위해 별도 스레드에서 실행
        new Thread(() -> {
            try {
                Thread.sleep(500); // 구독이 완전히 처리될 때까지 대기
                session.send("/app/test-message", testMessage); // 테스트용 엔드포인트
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // Then: 메시지 수신 확인 (최대 5초 대기)
        AuctionUpdateMessage receivedMessage = messageQueue.poll(5, TimeUnit.SECONDS);
        
        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage.getMessageType()).isEqualTo("NEW_BID");
        assertThat(receivedMessage.getAuctionId()).isEqualTo(auctionId);
        assertThat(receivedMessage.getCurrentPrice()).isEqualTo(new BigDecimal("25000"));
        assertThat(receivedMessage.getBidCount()).isEqualTo(3);
        assertThat(receivedMessage.getHighestBidderNickname()).isEqualTo("테스트입찰자");

        session.disconnect();
        log.info("WebSocket 연결 해제");
    }

    @Test
    @DisplayName("여러 클라이언트가 동일한 경매를 구독할 수 있다")
    void should_SupportMultipleClientsSubscription_When_MultipleClientsConnected() throws Exception {
        // Given: 두 개의 독립적인 클라이언트 연결
        BlockingQueue<AuctionUpdateMessage> client1Queue = new LinkedBlockingQueue<>();
        BlockingQueue<AuctionUpdateMessage> client2Queue = new LinkedBlockingQueue<>();
        
        // 첫 번째 클라이언트
StompSession session1 = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
        session1.subscribe("/topic/auctions/1", createFrameHandler(client1Queue));
        
        // 두 번째 클라이언트
StompSession session2 = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
        session2.subscribe("/topic/auctions/1", createFrameHandler(client2Queue));

        // When: 메시지 브로드캐스트
        AuctionUpdateMessage broadcastMessage = AuctionUpdateMessage.builder()
                .messageType("NEW_BID")
                .auctionId(1L)
                .currentPrice(new BigDecimal("30000"))
                .bidCount(5)
                .highestBidderNickname("멀티클라이언트테스트")
                .timestamp(java.time.LocalDateTime.now())
                .build();

        // 메시지 전송
        new Thread(() -> {
            try {
                Thread.sleep(500);
                session1.send("/app/test-message", broadcastMessage);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // Then: 두 클라이언트 모두 메시지 수신
        AuctionUpdateMessage client1Message = client1Queue.poll(5, TimeUnit.SECONDS);
        AuctionUpdateMessage client2Message = client2Queue.poll(5, TimeUnit.SECONDS);
        
        assertThat(client1Message).isNotNull();
        assertThat(client2Message).isNotNull();
        
        assertThat(client1Message.getCurrentPrice()).isEqualTo(new BigDecimal("30000"));
        assertThat(client2Message.getCurrentPrice()).isEqualTo(new BigDecimal("30000"));
        
        assertThat(client1Message.getHighestBidderNickname()).isEqualTo("멀티클라이언트테스트");
        assertThat(client2Message.getHighestBidderNickname()).isEqualTo("멀티클라이언트테스트");

        session1.disconnect();
        session2.disconnect();
    }

    @Test
    @DisplayName("구독하지 않은 경매의 메시지는 수신하지 않는다")
    void should_NotReceiveMessage_When_NotSubscribedToAuction() throws Exception {
        // Given: 경매 1번만 구독한 클라이언트
        BlockingQueue<AuctionUpdateMessage> messageQueue = new LinkedBlockingQueue<>();
        
StompSession session = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
        session.subscribe("/topic/auctions/1", createFrameHandler(messageQueue));

        // When: 경매 2번으로 메시지 전송
        AuctionUpdateMessage otherAuctionMessage = AuctionUpdateMessage.builder()
                .messageType("NEW_BID")
                .auctionId(2L) // 다른 경매 ID
                .currentPrice(new BigDecimal("40000"))
                .bidCount(7)
                .highestBidderNickname("다른경매입찰자")
                .timestamp(java.time.LocalDateTime.now())
                .build();

        new Thread(() -> {
            try {
                Thread.sleep(500);
                session.send("/app/test-message", otherAuctionMessage);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // Then: 메시지를 수신하지 않아야 함 (2초 대기 후 null 확인)
        AuctionUpdateMessage receivedMessage = messageQueue.poll(2, TimeUnit.SECONDS);
        assertThat(receivedMessage).isNull();

        session.disconnect();
    }

    @Test
    @DisplayName("경매 종료 메시지를 올바르게 수신한다")
    void should_ReceiveAuctionEndMessage_When_AuctionEnds() throws Exception {
        // Given: 경매 구독
        BlockingQueue<AuctionUpdateMessage> messageQueue = new LinkedBlockingQueue<>();
        
StompSession session = stompClient.connect(wsUrl, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
        session.subscribe("/topic/auctions/1", createFrameHandler(messageQueue));

        // When: 경매 종료 메시지 전송
        AuctionUpdateMessage endMessage = AuctionUpdateMessage.builder()
                .messageType("AUCTION_ENDED")
                .auctionId(1L)
                .currentPrice(new BigDecimal("55000"))
                .bidCount(10)
                .highestBidderNickname("최종낙찰자")
                .timestamp(java.time.LocalDateTime.now())
                .build();

        new Thread(() -> {
            try {
                Thread.sleep(500);
                session.send("/app/test-message", endMessage);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // Then: 경매 종료 메시지 수신 확인
        AuctionUpdateMessage receivedMessage = messageQueue.poll(5, TimeUnit.SECONDS);
        
        assertThat(receivedMessage).isNotNull();
        assertThat(receivedMessage.getMessageType()).isEqualTo("AUCTION_ENDED");
        assertThat(receivedMessage.getCurrentPrice()).isEqualTo(new BigDecimal("55000"));
        assertThat(receivedMessage.getHighestBidderNickname()).isEqualTo("최종낙찰자");

        session.disconnect();
    }

    /**
     * StompFrameHandler 생성 헬퍼 메서드
     */
    private StompFrameHandler createFrameHandler(BlockingQueue<AuctionUpdateMessage> messageQueue) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return AuctionUpdateMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageQueue.offer((AuctionUpdateMessage) payload);
            }
        };
    }
}