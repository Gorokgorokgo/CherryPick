package com.cherrypick.app.domain.chat.dto.response;

import com.cherrypick.app.domain.chat.entity.ChatRoom;
import com.cherrypick.app.domain.chat.enums.ChatRoomStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 채팅방 목록 응답 DTO
 */
@Getter
@Builder
@Schema(description = "채팅방 목록 응답")
public class ChatRoomListResponse {

    @Schema(description = "채팅방 ID", example = "1")
    private Long id;

    @Schema(description = "경매 ID", example = "1")
    private Long auctionId;

    @Schema(description = "경매 제목", example = "아이패드 Pro 12.9인치 M2 (256GB)")
    private String auctionTitle;

    @Schema(description = "경매 카테고리", example = "태블릿")
    private String category;

    @Schema(description = "낙찰가", example = "850000")
    private Long finalPrice;

    @Schema(description = "상대방 ID", example = "2")
    private Long partnerId;

    @Schema(description = "상대방 이름", example = "김판매자")
    private String partnerName;

    @Schema(description = "상대방 타입", example = "seller")
    private String partnerType; // seller 또는 buyer

    @Schema(description = "온라인 상태", example = "true")
    private boolean isOnline;

    @Schema(description = "채팅방 상태", example = "ACTIVE")
    private ChatRoomStatus status;

    @Schema(description = "마지막 메시지", example = "안녕하세요! 거래 관련 문의드립니다.")
    private String lastMessage;

    @Schema(description = "마지막 메시지 시간")
    private LocalDateTime lastMessageAt;

    @Schema(description = "읽지 않은 메시지 개수", example = "3")
    private int unreadCount;

    @Schema(description = "활성화 시간")
    private LocalDateTime activatedAt;

    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;

    /**
     * ChatRoom 엔티티와 추가 정보를 사용하여 응답 DTO 생성
     * 
     * @param chatRoom 채팅방 엔티티
     * @param currentUserId 현재 사용자 ID
     * @param lastMessage 마지막 메시지
     * @param unreadCount 읽지 않은 메시지 개수
     * @param partnerOnline 상대방 온라인 상태
     * @return ChatRoomListResponse
     */
    public static ChatRoomListResponse from(
            ChatRoom chatRoom, 
            Long currentUserId, 
            String lastMessage, 
            int unreadCount,
            boolean partnerOnline) {
        
        // 현재 사용자가 판매자인지 구매자인지 확인
        boolean isCurrentUserSeller = chatRoom.getSeller().getId().equals(currentUserId);
        
        return ChatRoomListResponse.builder()
                .id(chatRoom.getId())
                .auctionId(chatRoom.getAuction().getId())
                .auctionTitle(chatRoom.getAuction().getTitle())
                .category(chatRoom.getAuction().getCategory() != null ? 
                         chatRoom.getAuction().getCategory().name() : "기타")
                .finalPrice(chatRoom.getConnectionService() != null ? 
                           chatRoom.getConnectionService().getFinalPrice().longValue() : 
                           chatRoom.getAuction().getCurrentPrice().longValue())
                .partnerId(isCurrentUserSeller ? 
                          chatRoom.getBuyer().getId() : chatRoom.getSeller().getId())
                .partnerName(isCurrentUserSeller ? 
                            chatRoom.getBuyer().getNickname() : chatRoom.getSeller().getNickname())
                .partnerType(isCurrentUserSeller ? "buyer" : "seller")
                .isOnline(partnerOnline)
                .status(chatRoom.getStatus())
                .lastMessage(lastMessage != null ? lastMessage : "")
                .lastMessageAt(chatRoom.getLastMessageAt())
                .unreadCount(unreadCount)
                .activatedAt(chatRoom.getActivatedAt())
                .createdAt(chatRoom.getCreatedAt())
                .build();
    }

    /**
     * 활성화된 채팅방인지 확인
     * 
     * @return 활성화 여부
     */
    public boolean isActive() {
        return status == ChatRoomStatus.ACTIVE;
    }

    /**
     * 완료된 채팅방인지 확인
     * 
     * @return 완료 여부
     */
    public boolean isCompleted() {
        return status == ChatRoomStatus.CLOSED;
    }
}