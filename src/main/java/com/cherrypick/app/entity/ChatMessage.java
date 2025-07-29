package com.cherrypick.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 엔티티
 */
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

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @NotBlank
    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @NotNull
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @PrePersist
    protected void onCreate() {
        super.onCreate();
        if (this.sentAt == null) {
            this.sentAt = LocalDateTime.now();
        }
    }

    /**
     * 메시지 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
    }

    /**
     * 발신자가 구매자인지 확인
     */
    public boolean isSentByBuyer() {
        return this.sender.equals(this.chatRoom.getBuyer());
    }

    /**
     * 발신자가 판매자인지 확인
     */
    public boolean isSentBySeller() {
        return this.sender.equals(this.chatRoom.getSeller());
    }
}