package com.cherrypick.app.domain.common.controller;

import com.cherrypick.app.domain.common.dto.AuctionUpdateMessage;
import com.cherrypick.app.domain.common.service.WebSocketMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket 실시간 메시징 컨트롤러
 * STOMP 프로토콜 기반 실시간 통신 처리
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {
    
    private final WebSocketMessagingService webSocketMessagingService;
    
    /**
     * 특정 경매 구독
     * 클라이언트가 /topic/auctions/{auctionId}를 구독할 때 호출
     * 
     * @param auctionId 경매 ID
     * @param principal 인증된 사용자 정보
     */
    @SubscribeMapping("/topic/auctions/{auctionId}")
    public void subscribeToAuction(@DestinationVariable Long auctionId, Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        log.info("사용자 {}가 경매 {} 구독 시작", userId, auctionId);
        
        // TODO: 경매 구독 통계 업데이트 (추후 구현)
        // auctionViewerService.addViewer(auctionId, userId);
    }
    
    /**
     * 클라이언트가 연결을 끊을 때 호출 (선택적 구현)
     * 현재 경매 참여자 수 업데이트 등에 활용
     */
    @MessageMapping("/auctions/{auctionId}/leave")
    public void leaveAuction(@DestinationVariable Long auctionId, Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        log.info("사용자 {}가 경매 {} 구독 해제", userId, auctionId);
        
        // TODO: 경매 구독 해제 통계 업데이트 (추후 구현)
        // auctionViewerService.removeViewer(auctionId, userId);
    }
    
    /**
     * 클라이언트에서 서버로 메시지 전송 처리
     * (현재는 입찰이 REST API로 처리되므로 제한적 사용)
     * 
     * @param auctionId 경매 ID
     * @param message 클라이언트가 보낸 메시지
     * @param principal 인증된 사용자 정보
     */
    @MessageMapping("/auctions/{auctionId}/message")
    @SendTo("/topic/auctions/{auctionId}")
    public AuctionUpdateMessage handleAuctionMessage(
            @DestinationVariable Long auctionId,
            @Payload AuctionUpdateMessage message,
            Principal principal) {
        
        String userId = principal != null ? principal.getName() : "anonymous";
        log.debug("경매 {} 메시지 수신: {} from {}", auctionId, message.getMessageType(), userId);
        
        // 메시지 타입에 따른 추가 처리 (필요시)
        switch (message.getMessageType()) {
            case BID_COUNT_UPDATE:
                // 입찰자 수 업데이트 등 특별한 처리
                break;
            default:
                log.debug("기본 메시지 처리: {}", message.getMessageType());
                break;
        }
        
        return message;
    }
    
    /**
     * 글로벌 알림 구독 (시스템 점검, 공지사항 등)
     */
    @SubscribeMapping("/topic/global")
    public void subscribeToGlobal(Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        log.info("사용자 {}가 글로벌 알림 구독", userId);
    }
    
    /**
     * 개인 알림 구독 (낙찰 알림, 개인 메시지 등)
     * 
     * @param userId 사용자 ID
     * @param principal 인증된 사용자 정보
     */
    @SubscribeMapping("/queue/users/{userId}")
    public void subscribeToUserNotifications(@DestinationVariable Long userId, Principal principal) {
        String authenticatedUserId = principal != null ? principal.getName() : null;
        
        // 본인만 개인 알림을 구독할 수 있도록 보안 검증
        if (authenticatedUserId == null || !authenticatedUserId.equals(userId.toString())) {
            log.warn("권한 없는 개인 알림 구독 시도: {} -> {}", authenticatedUserId, userId);
            return;
        }
        
        log.info("사용자 {}가 개인 알림 구독", userId);
    }
}