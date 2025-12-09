package com.cherrypick.app.domain.chat.entity;

import com.cherrypick.app.domain.common.entity.BaseEntity;
import com.cherrypick.app.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅방 참여자 엔티티
 * 각 사용자의 채팅방 참여 상태를 개별적으로 관리합니다.
 */
@Entity
@Table(name = "chat_room_participants",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"chat_room_id", "user_id"})
        })
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 참여자가 채팅방을 나갔는지 여부
     */
    @Builder.Default
    @Column(name = "is_left", nullable = false)
    private Boolean isLeft = false;

    /**
     * 채팅방을 나간 시간
     */
    @Column(name = "left_at")
    private LocalDateTime leftAt;

    /**
     * 마지막으로 읽은 메시지 ID (나간 시점의 마지막 메시지)
     */
    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    // === 정적 팩토리 메서드 ===

    /**
     * 새 참여자 생성
     */
    public static ChatRoomParticipant createParticipant(ChatRoom chatRoom, User user) {
        return ChatRoomParticipant.builder()
                .chatRoom(chatRoom)
                .user(user)
                .isLeft(false)
                .build();
    }

    // === 비즈니스 메서드 ===

    /**
     * 채팅방 나가기
     */
    public ChatRoomParticipant leave(Long lastMessageId) {
        return ChatRoomParticipant.builder()
                .id(this.id)
                .chatRoom(this.chatRoom)
                .user(this.user)
                .isLeft(true)
                .leftAt(LocalDateTime.now())
                .lastReadMessageId(lastMessageId)
                .build();
    }

    /**
     * 채팅방 재입장
     */
    public ChatRoomParticipant rejoin() {
        return ChatRoomParticipant.builder()
                .id(this.id)
                .chatRoom(this.chatRoom)
                .user(this.user)
                .isLeft(false)
                .leftAt(null)
                .lastReadMessageId(this.lastReadMessageId)
                .build();
    }

    /**
     * 참여 중인지 확인 (나가지 않은 상태)
     */
    public boolean isActive() {
        return !this.isLeft;
    }

    /**
     * 특정 사용자의 참여자인지 확인
     */
    public boolean isUser(Long userId) {
        return this.user.getId().equals(userId);
    }
}
