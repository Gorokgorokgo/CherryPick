package com.cherrypick.app.domain.chat.dto.response;

import com.cherrypick.app.domain.chat.entity.ChatRoom;
import com.cherrypick.app.domain.chat.enums.ChatRoomStatus;
import com.cherrypick.app.domain.connection.enums.ConnectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 채팅방 상세 응답 DTO
 */
@Getter
@Builder
@Schema(description = "채팅방 상세 응답")
public class ChatRoomResponse {

    @Schema(description = "채팅방 ID", example = "1")
    private Long id;

    @Schema(description = "경매 ID", example = "1")
    private Long auctionId;

    @Schema(description = "경매 제목", example = "아이패드 Pro 12.9인치 M2 (256GB)")
    private String auctionTitle;

    @Schema(description = "경매 설명", example = "거의 새제품입니다. 박스, 충전기 모두 있어요.")
    private String auctionDescription;

    @Schema(description = "경매 카테고리", example = "태블릿")
    private String category;

    @Schema(description = "낙찰가", example = "850000")
    private Long finalPrice;

    @Schema(description = "연결 서비스 ID", example = "1")
    private Long connectionServiceId;

    @Schema(description = "연결 서비스 상태", example = "ACTIVE")
    private ConnectionStatus connectionStatus;

    @Schema(description = "판매자 ID", example = "1")
    private Long sellerId;

    @Schema(description = "판매자 이름", example = "김판매자")
    private String sellerName;

    @Schema(description = "구매자 ID", example = "2")
    private Long buyerId;

    @Schema(description = "구매자 이름", example = "박구매자")
    private String buyerName;

    @Schema(description = "상대방 ID", example = "2")
    private Long partnerId;

    @Schema(description = "상대방 이름", example = "김판매자")
    private String partnerName;

    @Schema(description = "상대방 타입", example = "seller")
    private String partnerType; // seller 또는 buyer

    @Schema(description = "상대방 온라인 상태", example = "true")
    private boolean partnerOnline;

    @Schema(description = "채팅방 상태", example = "ACTIVE")
    private ChatRoomStatus status;

    @Schema(description = "활성화 시간")
    private LocalDateTime activatedAt;

    @Schema(description = "마지막 메시지 시간")
    private LocalDateTime lastMessageAt;

    @Schema(description = "생성 시간")
    private LocalDateTime createdAt;

    @Schema(description = "읽지 않은 메시지 개수", example = "3")
    private int unreadCount;

    @Schema(description = "거래 상태", example = "PENDING")
    private String transactionStatus;

    /**
     * ChatRoom 엔티티와 추가 정보를 사용하여 응답 DTO 생성 (ConnectionService 포함)
     *
     * @param chatRoom 채팅방 엔티티
     * @param currentUserId 현재 사용자 ID
     * @param unreadCount 읽지 않은 메시지 개수
     * @param partnerOnline 상대방 온라인 상태
     * @return ChatRoomResponse
     */
    public static ChatRoomResponse from(
            ChatRoom chatRoom,
            Long currentUserId,
            int unreadCount,
            boolean partnerOnline) {

        // 현재 사용자가 판매자인지 구매자인지 확인
        boolean isCurrentUserSeller = chatRoom.getSeller().getId().equals(currentUserId);

        // ConnectionService가 있는 경우와 없는 경우 분기 처리
        ChatRoomResponseBuilder builder = ChatRoomResponse.builder()
                .id(chatRoom.getId())
                .auctionId(chatRoom.getAuction().getId())
                .auctionTitle(chatRoom.getAuction().getTitle())
                .auctionDescription(chatRoom.getAuction().getDescription())
                .category(chatRoom.getAuction().getCategory() != null ?
                         chatRoom.getAuction().getCategory().name() : "기타")
                .sellerId(chatRoom.getSeller().getId())
                .sellerName(chatRoom.getSeller().getNickname())
                .buyerId(chatRoom.getBuyer().getId())
                .buyerName(chatRoom.getBuyer().getNickname())
                .partnerId(isCurrentUserSeller ?
                          chatRoom.getBuyer().getId() : chatRoom.getSeller().getId())
                .partnerName(isCurrentUserSeller ?
                            chatRoom.getBuyer().getNickname() : chatRoom.getSeller().getNickname())
                .partnerType(isCurrentUserSeller ? "buyer" : "seller")
                .partnerOnline(partnerOnline)
                .status(chatRoom.getStatus())
                .activatedAt(chatRoom.getActivatedAt())
                .lastMessageAt(chatRoom.getLastMessageAt())
                .createdAt(chatRoom.getCreatedAt())
                .unreadCount(unreadCount);

        // ConnectionService가 있는 경우에만 관련 정보 설정
        if (chatRoom.getConnectionService() != null) {
            builder.finalPrice(chatRoom.getConnectionService().getFinalPrice().longValue())
                   .connectionServiceId(chatRoom.getConnectionService().getId())
                   .connectionStatus(chatRoom.getConnectionService().getStatus());
        }

        return builder.build();
    }

    /**
     * ChatRoom 엔티티와 추가 정보를 사용하여 응답 DTO 생성 (거래 상태 포함)
     *
     * @param chatRoom 채팅방 엔티티
     * @param currentUserId 현재 사용자 ID
     * @param unreadCount 읽지 않은 메시지 개수
     * @param partnerOnline 상대방 온라인 상태
     * @param transactionStatus 거래 상태
     * @return ChatRoomResponse
     */
    public static ChatRoomResponse from(
            ChatRoom chatRoom,
            Long currentUserId,
            int unreadCount,
            boolean partnerOnline,
            String transactionStatus) {

        // 현재 사용자가 판매자인지 구매자인지 확인
        boolean isCurrentUserSeller = chatRoom.getSeller().getId().equals(currentUserId);

        // ConnectionService가 있는 경우와 없는 경우 분기 처리
        ChatRoomResponseBuilder builder = ChatRoomResponse.builder()
                .id(chatRoom.getId())
                .auctionId(chatRoom.getAuction().getId())
                .auctionTitle(chatRoom.getAuction().getTitle())
                .auctionDescription(chatRoom.getAuction().getDescription())
                .category(chatRoom.getAuction().getCategory() != null ?
                         chatRoom.getAuction().getCategory().name() : "기타")
                .sellerId(chatRoom.getSeller().getId())
                .sellerName(chatRoom.getSeller().getNickname())
                .buyerId(chatRoom.getBuyer().getId())
                .buyerName(chatRoom.getBuyer().getNickname())
                .partnerId(isCurrentUserSeller ?
                          chatRoom.getBuyer().getId() : chatRoom.getSeller().getId())
                .partnerName(isCurrentUserSeller ?
                            chatRoom.getBuyer().getNickname() : chatRoom.getSeller().getNickname())
                .partnerType(isCurrentUserSeller ? "buyer" : "seller")
                .partnerOnline(partnerOnline)
                .status(chatRoom.getStatus())
                .activatedAt(chatRoom.getActivatedAt())
                .lastMessageAt(chatRoom.getLastMessageAt())
                .createdAt(chatRoom.getCreatedAt())
                .unreadCount(unreadCount)
                .transactionStatus(transactionStatus);

        // ConnectionService가 있는 경우에만 관련 정보 설정
        if (chatRoom.getConnectionService() != null) {
            builder.finalPrice(chatRoom.getConnectionService().getFinalPrice().longValue())
                   .connectionServiceId(chatRoom.getConnectionService().getId())
                   .connectionStatus(chatRoom.getConnectionService().getStatus());
        }

        return builder.build();
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
     * 현재 사용자가 판매자인지 확인
     * 
     * @param userId 사용자 ID
     * @return 판매자 여부
     */
    public boolean isCurrentUserSeller(Long userId) {
        return sellerId.equals(userId);
    }

    /**
     * 현재 사용자가 구매자인지 확인
     * 
     * @param userId 사용자 ID
     * @return 구매자 여부
     */
    public boolean isCurrentUserBuyer(Long userId) {
        return buyerId.equals(userId);
    }

    /**
     * 연결 서비스가 활성화되었는지 확인
     * 
     * @return 연결 서비스 활성화 여부
     */
    public boolean isConnectionActive() {
        return connectionStatus == ConnectionStatus.ACTIVE;
    }
}