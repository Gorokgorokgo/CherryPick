package com.cherrypick.app.domain.common.service;

import com.cherrypick.app.domain.common.dto.AuctionUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket 실시간 메시징 서비스
 * 경매 관련 실시간 업데이트를 클라이언트에게 전송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketMessagingService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
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
            messagingTemplate.convertAndSend(destination, message);
            log.debug("WebSocket 메시지 전송 성공: {} -> {}", destination, message.getMessageType());
        } catch (Exception e) {
            log.error("WebSocket 메시지 전송 실패: {} -> {}", destination, message.getMessageType(), e);
            // TODO: 메시지 전송 실패시 재시도 로직 또는 데드 레터 큐 처리
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
            messagingTemplate.convertAndSend(destination, message);
            log.debug("개인 WebSocket 메시지 전송 성공: {} -> {}", destination, message.getMessageType());
        } catch (Exception e) {
            log.error("개인 WebSocket 메시지 전송 실패: {} -> {}", destination, message.getMessageType(), e);
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
            messagingTemplate.convertAndSend(destination, message);
            log.debug("글로벌 WebSocket 메시지 전송 성공: {}", message.getMessageType());
        } catch (Exception e) {
            log.error("글로벌 WebSocket 메시지 전송 실패: {}", message.getMessageType(), e);
        }
    }
    
    /**
     * 새로운 입찰 발생시 실시간 알림
     */
    public void notifyNewBid(Long auctionId, java.math.BigDecimal currentPrice, 
                           Integer bidCount, String bidderNickname) {
        AuctionUpdateMessage message = AuctionUpdateMessage.newBid(
            auctionId, currentPrice, bidCount, bidderNickname
        );
        broadcastToAuction(auctionId, message);
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
     * 입찰 참여자 수 변경시 실시간 알림
     */
    public void notifyBidCountUpdate(Long auctionId, Integer bidCount) {
        AuctionUpdateMessage message = AuctionUpdateMessage.bidCountUpdate(auctionId, bidCount);
        broadcastToAuction(auctionId, message);
    }
}