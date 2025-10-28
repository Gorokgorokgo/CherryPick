package com.cherrypick.app.domain.websocket.service;

import com.cherrypick.app.domain.chat.dto.response.ChatMessageResponse;
import com.cherrypick.app.domain.websocket.dto.AuctionUpdateMessage;
import com.cherrypick.app.config.WebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

/**
 * WebSocket 실시간 메시징 서비스
 * 경매 관련 실시간 업데이트를 클라이언트에게 전송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketMessagingService {

    private final WebSocketHandler webSocketHandler;
    
    @PostConstruct
    public void init() {
        // 초기화 완료
    }
    
    /**
     * 특정 경매의 모든 구독자에게 메시지 브로드캐스트
     * 
     * @param auctionId 경매 ID
     * @param message 전송할 메시지
     */
    public void broadcastToAuction(Long auctionId, AuctionUpdateMessage message) {
        // 입력값 검증
        if (auctionId == null || auctionId <= 0) {
            log.warn("잘못된 auctionId: {}", auctionId);
            return;
        }
        
        if (message == null) {
            log.warn("메시지가 null입니다. auctionId: {}", auctionId);
            return;
        }
        
        String destination = "/topic/auctions/" + auctionId;
        
        try {
            webSocketHandler.sendToAuctionSubscribers(destination, message);
        } catch (Exception e) {
            // 메시지 전송 실패 무시
        }
    }
    
    /**
     * 특정 사용자에게만 개인 메시지 전송
     *
     * @param userId 사용자 ID
     * @param message 전송할 메시지
     */
    public void sendToUser(Long userId, AuctionUpdateMessage message) {
        String destination = "/queue/users/" + userId;

        try {
            webSocketHandler.sendToAuctionSubscribers(destination, message);
        } catch (Exception e) {
            // 개인 메시지 전송 실패 무시
        }
    }

    /**
     * 특정 사용자에게 알림 전송 (알림 전용 엔드포인트)
     *
     * @param userId 사용자 ID
     * @param notification 알림 메시지
     */
    public void sendNotificationToUser(Long userId, Object notification) {
        String destination = "/topic/notifications/" + userId;

        try {
            webSocketHandler.sendToAuctionSubscribers(destination, notification);
        } catch (Exception e) {
            // 실시간 알림 전송 실패 무시
        }
    }
    
    /**
     * 모든 클라이언트에게 글로벌 메시지 브로드캐스트
     * (시스템 점검 알림 등)
     * 
     * @param message 전송할 메시지
     */
    public void broadcastGlobal(AuctionUpdateMessage message) {
        String destination = "/topic/global";
        
        try {
            webSocketHandler.sendToAuctionSubscribers(destination, message);
        } catch (Exception e) {
            // 글로벌 메시지 전송 실패 무시
        }
    }
    
    /**
     * 새로운 입찰 발생시 실시간 알림
     */
    public void notifyNewBid(Long auctionId, java.math.BigDecimal currentPrice, 
                           Integer bidCount, String bidderNickname) {
        try {
            AuctionUpdateMessage message = AuctionUpdateMessage.newBid(
                auctionId, currentPrice, bidCount, bidderNickname
            );
            
            broadcastToAuction(auctionId, message);
            
        } catch (Exception e) {
            // notifyNewBid 실행 중 예외 무시
        }
    }
    
    /**
     * 경매 종료시 실시간 알림
     */
    public void notifyAuctionEnded(Long auctionId, java.math.BigDecimal finalPrice, 
                                 String winnerNickname) {
        AuctionUpdateMessage message = AuctionUpdateMessage.auctionEnded(
            auctionId, finalPrice, winnerNickname
        );
        broadcastToAuction(auctionId, message);
    }
    
    /**
     * 자동입찰 경쟁 시작 알림
     */
    public void notifyAutoBidCompeting(Long auctionId, java.math.BigDecimal currentPrice, Integer bidCount) {
        AuctionUpdateMessage message = AuctionUpdateMessage.autoBidCompeting(auctionId, currentPrice, bidCount);
        broadcastToAuction(auctionId, message);
    }

    /**
     * 자동입찰 경쟁 결과 알림
     */
    public void notifyAutoBidResult(Long auctionId, java.math.BigDecimal currentPrice, Integer bidCount, String winnerNickname) {
        AuctionUpdateMessage message = AuctionUpdateMessage.autoBidResult(auctionId, currentPrice, bidCount, winnerNickname);
        broadcastToAuction(auctionId, message);
    }

    /**
     * 입찰 참여자 수 변경시 실시간 알림
     */
    public void notifyBidCountUpdate(Long auctionId, Integer bidCount) {
        AuctionUpdateMessage message = AuctionUpdateMessage.bidCountUpdate(auctionId, bidCount);
        broadcastToAuction(auctionId, message);
    }

    /**
     * 입찰자 수 변경시 실시간 알림 (별칭 메서드)
     */
    public void notifyBidderCountChanged(Long auctionId, Integer bidderCount) {
        AuctionUpdateMessage message = AuctionUpdateMessage.bidderCountChanged(auctionId, bidderCount);
        broadcastToAuction(auctionId, message);
    }
    
    // === 채팅 관련 WebSocket 메서드 ===
    
    /**
     * 채팅방의 모든 참여자에게 새 메시지 브로드캐스트
     * 
     * @param chatRoomId 채팅방 ID
     * @param message 채팅 메시지
     */
    public void sendChatMessage(Long chatRoomId, ChatMessageResponse message) {
        // 입력값 검증
        if (chatRoomId == null || chatRoomId <= 0) {
            log.warn("잘못된 chatRoomId: {}", chatRoomId);
            return;
        }
        
        if (message == null) {
            log.warn("메시지가 null입니다. chatRoomId: {}", chatRoomId);
            return;
        }
        
        String destination = "/topic/chat/" + chatRoomId;
        
        try {
            webSocketHandler.sendToAuctionSubscribers(destination, message);
        } catch (Exception e) {
            // 채팅 메시지 전송 실패 무시
        }
    }
    
    /**
     * 채팅방 상태 변경 알림 (활성화, 비활성화 등)
     * 
     * @param chatRoomId 채팅방 ID
     * @param status 변경된 상태
     * @param message 상태 변경 메시지
     */
    public void notifyChatRoomStatusChange(Long chatRoomId, String status, String message) {
        String destination = "/topic/chat/" + chatRoomId + "/status";
        
        // 상태 변경 메시지 객체 생성
        ChatStatusMessage statusMessage = new ChatStatusMessage(chatRoomId, status, message);
        
        try {
            webSocketHandler.sendToAuctionSubscribers(destination, statusMessage);
        } catch (Exception e) {
            // 채팅방 상태 변경 알림 전송 실패 무시
        }
    }
    
    /**
     * 사용자의 온라인 상태 변경 알림
     * 
     * @param userId 사용자 ID
     * @param isOnline 온라인 상태
     */
    public void notifyUserOnlineStatus(Long userId, boolean isOnline) {
        String destination = "/topic/users/" + userId + "/status";
        
        UserStatusMessage statusMessage = new UserStatusMessage(userId, isOnline);
        
        try {
            webSocketHandler.sendToAuctionSubscribers(destination, statusMessage);
        } catch (Exception e) {
            // 사용자 온라인 상태 알림 전송 실패 무시
        }
    }
    
    /**
     * 메시지 읽음 상태 알림
     * 
     * @param chatRoomId 채팅방 ID
     * @param messageId 읽은 메시지 ID
     * @param readerId 읽은 사용자 ID
     */
    public void notifyMessageRead(Long chatRoomId, Long messageId, Long readerId) {
        String destination = "/topic/chat/" + chatRoomId + "/read";
        
        MessageReadEvent readEvent = new MessageReadEvent(messageId, readerId, System.currentTimeMillis());
        
        try {
            webSocketHandler.sendToAuctionSubscribers(destination, readEvent);
        } catch (Exception e) {
            // 메시지 읽음 상태 알림 전송 실패 무시
        }
    }
    
    /**
     * 메시지 전달됨 상태 알림
     * 
     * @param chatRoomId 채팅방 ID
     * @param messageId 전달된 메시지 ID
     * @param receiverId 수신자 ID
     */
    public void notifyMessageDelivered(Long chatRoomId, Long messageId, Long receiverId) {
        String destination = "/topic/chat/" + chatRoomId + "/delivered";
        
        MessageDeliveredEvent deliveredEvent = new MessageDeliveredEvent(messageId, receiverId, System.currentTimeMillis());
        
        try {
            webSocketHandler.sendToAuctionSubscribers(destination, deliveredEvent);
        } catch (Exception e) {
            // 메시지 전달됨 상태 알림 전송 실패 무시
        }
    }
    
    /**
     * 타이핑 상태 알림
     * 
     * @param chatRoomId 채팅방 ID
     * @param userId 타이핑하는 사용자 ID
     * @param userNickname 사용자 닉네임
     * @param isTyping 타이핑 중인지 여부
     */
    public void notifyTypingStatus(Long chatRoomId, Long userId, String userNickname, boolean isTyping) {
        String destination = "/topic/chat/" + chatRoomId + "/typing";
        
        TypingStatusEvent typingEvent = new TypingStatusEvent(userId, userNickname, isTyping, System.currentTimeMillis());
        
        try {
            webSocketHandler.sendToAuctionSubscribers(destination, typingEvent);
        } catch (Exception e) {
            // 타이핑 상태 알림 전송 실패 무시
        }
    }
    
    // === 내부 메시지 클래스들 ===
    
    /**
     * 채팅방 상태 변경 메시지
     */
    public static class ChatStatusMessage {
        public final Long chatRoomId;
        public final String status;
        public final String message;
        public final long timestamp;
        
        public ChatStatusMessage(Long chatRoomId, String status, String message) {
            this.chatRoomId = chatRoomId;
            this.status = status;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * 사용자 온라인 상태 메시지
     */
    public static class UserStatusMessage {
        public final Long userId;
        public final boolean isOnline;
        public final long timestamp;
        
        public UserStatusMessage(Long userId, boolean isOnline) {
            this.userId = userId;
            this.isOnline = isOnline;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * 메시지 읽음 이벤트
     */
    public static class MessageReadEvent {
        public final Long messageId;
        public final Long readerId;
        public final long timestamp;
        
        public MessageReadEvent(Long messageId, Long readerId, long timestamp) {
            this.messageId = messageId;
            this.readerId = readerId;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * 메시지 전달됨 이벤트
     */
    public static class MessageDeliveredEvent {
        public final Long messageId;
        public final Long receiverId;
        public final long timestamp;
        
        public MessageDeliveredEvent(Long messageId, Long receiverId, long timestamp) {
            this.messageId = messageId;
            this.receiverId = receiverId;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * 타이핑 상태 이벤트
     */
    public static class TypingStatusEvent {
        public final Long userId;
        public final String userNickname;
        public final boolean isTyping;
        public final long timestamp;
        
        public TypingStatusEvent(Long userId, String userNickname, boolean isTyping, long timestamp) {
            this.userId = userId;
            this.userNickname = userNickname;
            this.isTyping = isTyping;
            this.timestamp = timestamp;
        }
    }
}