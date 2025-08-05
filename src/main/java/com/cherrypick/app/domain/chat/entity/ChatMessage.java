package com.cherrypick.app.domain.chat.entity;

import com.cherrypick.app.domain.chat.enums.MessageType;
import com.cherrypick.app.domain.common.entity.BaseEntity;
import com.cherrypick.app.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@Builder
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

    // === 정적 팩토리 메서드 ===
    
    /**
     * 일반 텍스트 메시지 생성
     */
    public static ChatMessage createTextMessage(ChatRoom chatRoom, User sender, String content) {
        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .messageType(MessageType.TEXT)
                .content(content)
                .isRead(false)
                .build();
    }
    
    /**
     * 시스템 메시지 생성
     */
    public static ChatMessage createSystemMessage(ChatRoom chatRoom, String content) {
        return ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(null) // 시스템 메시지는 발신자 없음
                .messageType(MessageType.SYSTEM)
                .content(content)
                .isRead(false)
                .build();
    }

    // === 비즈니스 메서드 ===
    
    /**
     * 메시지 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
    
    /**
     * 시스템 메시지인지 확인
     */
    public boolean isSystemMessage() {
        return messageType == MessageType.SYSTEM;
    }
}