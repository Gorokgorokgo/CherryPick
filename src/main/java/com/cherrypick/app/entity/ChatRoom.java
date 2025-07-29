package com.cherrypick.app.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 채팅방 엔티티
 */
@Entity
@Table(name = "chat_rooms",
       uniqueConstraints = @UniqueConstraint(columnNames = {"auction_id", "buyer_id", "seller_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_message", length = 500)
    private String lastMessage;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Builder.Default
    @Column(name = "buyer_unread_count", nullable = false)
    private Integer buyerUnreadCount = 0;

    @Builder.Default
    @Column(name = "seller_unread_count", nullable = false)
    private Integer sellerUnreadCount = 0;

    /**
     * 마지막 메시지 업데이트
     */
    public void updateLastMessage(String message, User sender) {
        this.lastMessage = message;
        this.lastMessageAt = LocalDateTime.now();
        
        // 상대방의 읽지 않은 메시지 수 증가
        if (sender.equals(buyer)) {
            this.sellerUnreadCount++;
        } else {
            this.buyerUnreadCount++;
        }
    }

    /**
     * 읽지 않은 메시지 수 초기화
     */
    public void resetUnreadCount(User user) {
        if (user.equals(buyer)) {
            this.buyerUnreadCount = 0;
        } else if (user.equals(seller)) {
            this.sellerUnreadCount = 0;
        }
    }

    /**
     * 사용자의 읽지 않은 메시지 수 조회
     */
    public Integer getUnreadCount(User user) {
        if (user.equals(buyer)) {
            return this.buyerUnreadCount;
        } else if (user.equals(seller)) {
            return this.sellerUnreadCount;
        }
        return 0;
    }

    /**
     * 채팅방 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }
}