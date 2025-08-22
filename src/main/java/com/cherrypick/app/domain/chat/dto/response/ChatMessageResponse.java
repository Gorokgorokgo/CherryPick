package com.cherrypick.app.domain.chat.dto.response;

import com.cherrypick.app.domain.chat.entity.ChatMessage;
import com.cherrypick.app.domain.chat.enums.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 응답 DTO
 */
@Getter
@Builder
@Schema(description = "채팅 메시지 응답")
public class ChatMessageResponse {

    @Schema(description = "메시지 ID", example = "1")
    private Long id;

    @Schema(description = "채팅방 ID", example = "1")
    private Long chatRoomId;

    @Schema(description = "발신자 ID", example = "1")
    private Long senderId;

    @Schema(description = "발신자 이름", example = "김판매자")
    private String senderName;

    @Schema(description = "메시지 내용", example = "안녕하세요! 거래 관련 문의드립니다.")
    private String content;

    @Schema(description = "메시지 타입", example = "TEXT")
    private MessageType messageType;

    @Schema(description = "읽음 여부", example = "true")
    private boolean isRead;

    @Schema(description = "읽은 시간")
    private LocalDateTime readAt;

    @Schema(description = "작성 시간")
    private LocalDateTime createdAt;

    @Schema(description = "답장할 메시지 ID")
    private Long replyToMessageId;

    @Schema(description = "답장할 메시지 내용")
    private String replyToContent;

    /**
     * ChatMessage 엔티티를 응답 DTO로 변환
     * 
     * @param message 채팅 메시지 엔티티
     * @return ChatMessageResponse
     */
    public static ChatMessageResponse from(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom().getId())
                .senderId(message.getSender() != null ? message.getSender().getId() : null)
                .senderName(message.getSender() != null ? message.getSender().getNickname() : "System")
                .content(message.getContent())
                .messageType(message.getMessageType())
                .isRead(message.isRead())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .build();
    }

    /**
     * 시스템 메시지인지 확인
     * 
     * @return 시스템 메시지 여부
     */
    public boolean isSystemMessage() {
        return messageType == MessageType.SYSTEM;
    }

    /**
     * 내 메시지인지 확인
     * 
     * @param userId 사용자 ID
     * @return 내 메시지 여부
     */
    public boolean isMyMessage(Long userId) {
        return senderId != null && senderId.equals(userId);
    }
}