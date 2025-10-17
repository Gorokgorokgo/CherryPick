package com.cherrypick.app.domain.websocket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 실시간 경매 업데이트 메시지 DTO
 * WebSocket을 통해 클라이언트에게 전송되는 실시간 정보
 */
@Getter
@Builder
@AllArgsConstructor 
@NoArgsConstructor
public class AuctionUpdateMessage {
    
    /**
     * 메시지 타입
     */
    public enum MessageType {
        NEW_BID,           // 새로운 입찰
        AUCTION_ENDED,     // 경매 종료
        AUCTION_EXTENDED,  // 스나이핑 방지 연장11
        BIDDER_COUNT_CHANGED, // 입찰자 수 변경 (별칭)
        AUTO_BID_COMPETING, // 자동입찰 경쟁 진행 중
        AUTO_BID_RESULT,    // 자동입찰 경쟁 최종 결과
        // 알림 타입
        AUCTION_WON,       // 낙찰 알림 (구매자용)
        AUCTION_SOLD,      // 판매 완료 알림 (판매자용)
        AUCTION_NOT_SOLD,  // 유찰 알림
        CONNECTION_PAYMENT_REQUEST, // 연결 서비스 결제 요청
        CHAT_ACTIVATED,    // 채팅 활성화
        TRANSACTION_COMPLETED, // 거래 완료
        NEW_MESSAGE,       // 새 메시지
        PROMOTION          // 프로모션
    }
    
    /**
     * 메시지 타입
     */
    private MessageType messageType;
    
    /**
     * 경매 ID
     */
    private Long auctionId;
    
    /**
     * 현재 최고 입찰가
     */
    private BigDecimal currentPrice;
    
    /**
     * 입찰 참여자 수
     */
    private Integer bidCount;
    
    /**
     * 현재 최고 입찰자 닉네임 (개인정보 보호)
     */
    private String highestBidderNickname;
    
    /**
     * 경매 종료 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endAt;
    
    /**
     * 메시지 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * 추가 메시지 (선택사항)
     */
    private String message;
    
    // === 정적 팩토리 메서드 ===
    
    /**
     * 새로운 입찰 메시지 생성
     */
    public static AuctionUpdateMessage newBid(Long auctionId, BigDecimal currentPrice, 
                                            Integer bidCount, String bidderNickname) {
        return AuctionUpdateMessage.builder()
                .messageType(MessageType.NEW_BID)
                .auctionId(auctionId)
                .currentPrice(currentPrice)
                .bidCount(bidCount)
                .highestBidderNickname(bidderNickname)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 경매 종료 메시지 생성
     */
    public static AuctionUpdateMessage auctionEnded(Long auctionId, BigDecimal finalPrice,
                                                  String winnerNickname) {
        return AuctionUpdateMessage.builder()
                .messageType(MessageType.AUCTION_ENDED)
                .auctionId(auctionId)
                .currentPrice(finalPrice)
                .highestBidderNickname(winnerNickname)
                .timestamp(LocalDateTime.now())
                .message("경매가 종료되었습니다.")
                .build();
    }
    
    /**
     * 입찰 참여자 수 업데이트 메시지 생성
     */
    public static AuctionUpdateMessage bidCountUpdate(Long auctionId, Integer bidCount) {
        return AuctionUpdateMessage.builder()
                .messageType(MessageType.BIDDER_COUNT_CHANGED)
                .auctionId(auctionId)
                .bidCount(bidCount)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 입찰자 수 변경 메시지 생성 (별칭)
     */
    public static AuctionUpdateMessage bidderCountChanged(Long auctionId, Integer bidderCount) {
        return AuctionUpdateMessage.builder()
                .messageType(MessageType.BIDDER_COUNT_CHANGED)
                .auctionId(auctionId)
                .bidCount(bidderCount)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 자동입찰 경쟁 진행 메시지
     */
    public static AuctionUpdateMessage autoBidCompeting(Long auctionId, BigDecimal currentPrice, Integer bidCount) {
        return AuctionUpdateMessage.builder()
                .messageType(MessageType.AUTO_BID_COMPETING)
                .auctionId(auctionId)
                .currentPrice(currentPrice)
                .bidCount(bidCount)
                .timestamp(LocalDateTime.now())
                .message("자동입찰 경쟁 중...")
                .build();
    }

    /**
     * 자동입찰 경쟁 결과 메시지
     */
    public static AuctionUpdateMessage autoBidResult(Long auctionId, BigDecimal currentPrice, Integer bidCount, String winnerNickname) {
        return AuctionUpdateMessage.builder()
                .messageType(MessageType.AUTO_BID_RESULT)
                .auctionId(auctionId)
                .currentPrice(currentPrice)
                .bidCount(bidCount)
                .highestBidderNickname(winnerNickname)
                .timestamp(LocalDateTime.now())
                .message("자동입찰 경쟁 완료")
                .build();
    }

    // Lombok Builder 확장: 문자열 타입을 enum으로 변환하는 헬퍼 추가 (테스트 호환)
    public static class AuctionUpdateMessageBuilder {
        public AuctionUpdateMessageBuilder messageType(String type) {
            if (type != null) {
                this.messageType = MessageType.valueOf(type);
            }
            return this;
        }
        public AuctionUpdateMessageBuilder messageType(MessageType type) {
            this.messageType = type;
            return this;
        }
    }
}
