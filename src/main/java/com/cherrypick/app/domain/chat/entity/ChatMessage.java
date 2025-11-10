package com.cherrypick.app.domain.chat.entity;

import com.cherrypick.app.domain.chat.enums.MessageType;
import com.cherrypick.app.domain.chat.enums.MessageDeliveryStatus;
import com.cherrypick.app.domain.common.entity.BaseEntity;
import com.cherrypick.app.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    @Builder.Default
    private MessageDeliveryStatus deliveryStatus = MessageDeliveryStatus.SENT;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    // === 정적 팩토리 메서드 ===
    
    /**
     * 일반 텍스트 메시지 생성
     */
    public static ChatMessage createTextMessage(ChatRoom chatRoom, User sender, String content) {
        LocalDateTime now = LocalDateTime.now();
        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .messageType(MessageType.TEXT)
                .content(content)
                .isRead(false)
                .deliveryStatus(MessageDeliveryStatus.SENT)
                .sentAt(now)
                .build();
    }

    /**
     * 이미지 메시지 생성
     */
    public static ChatMessage createImageMessage(ChatRoom chatRoom, User sender, String imageUrl) {
        LocalDateTime now = LocalDateTime.now();
        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .messageType(MessageType.IMAGE)
                .content(imageUrl)
                .isRead(false)
                .deliveryStatus(MessageDeliveryStatus.SENT)
                .sentAt(now)
                .build();
    }

    /**
     * 메시지 타입에 따라 적절한 메시지 생성
     */
    public static ChatMessage createMessage(ChatRoom chatRoom, User sender, String content, MessageType messageType) {
        if (messageType == MessageType.IMAGE) {
            return createImageMessage(chatRoom, sender, content);
        } else {
            return createTextMessage(chatRoom, sender, content);
        }
    }
    
    /**
     * 시스템 메시지 생성
     */
    public static ChatMessage createSystemMessage(ChatRoom chatRoom, String content) {
        LocalDateTime now = LocalDateTime.now();
        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(null) // 시스템 메시지는 발신자 없음
                .messageType(MessageType.SYSTEM)
                .content(content)
                .isRead(false)
                .deliveryStatus(MessageDeliveryStatus.SENT)
                .sentAt(now)
                .build();
    }

    // === 비즈니스 메서드 ===
    
    /**
     * 메시지 읽음 처리 (전송 상태 추적 포함)
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
        
        // 전송 상태도 READ로 변경
        if (!deliveryStatus.canTransitionTo(MessageDeliveryStatus.READ)) {
            throw new IllegalStateException(
                String.format("메시지 상태를 변경할 수 없습니다: %s -> READ", deliveryStatus)
            );
        }
        this.deliveryStatus = MessageDeliveryStatus.READ;
    }
    
    /**
     * 시스템 메시지인지 확인
     */
    public boolean isSystemMessage() {
        return messageType == MessageType.SYSTEM;
    }
    
    /**
     * 메시지를 전달됨 상태로 변경
     */
    public void markAsDelivered() {
        if (!deliveryStatus.canTransitionTo(MessageDeliveryStatus.DELIVERED)) {
            throw new IllegalStateException(
                String.format("메시지 상태를 변경할 수 없습니다: %s -> DELIVERED", deliveryStatus)
            );
        }
        this.deliveryStatus = MessageDeliveryStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }
    
    
    /**
     * 메시지 전송 상태 확인
     */
    public boolean isDelivered() {
        return deliveryStatus.isDeliveredOrRead();
    }
    
    /**
     * 메시지가 완전히 읽혀졌는지 확인 (기존 isRead와 중복을 피하기 위해 메서드명 변경)
     */
    public boolean hasBeenRead() {
        return deliveryStatus.isRead();
    }
}