package com.cherrypick.app.domain.chat.entity;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.chat.enums.ChatRoomStatus;
import com.cherrypick.app.domain.common.entity.BaseEntity;
import com.cherrypick.app.domain.connection.entity.ConnectionService;
import com.cherrypick.app.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_service_id", nullable = false)
    private ConnectionService connectionService;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRoomStatus status = ChatRoomStatus.INACTIVE;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    // === 정적 팩토리 메서드 ===
    
    /**
     * 채팅방 생성 (연결 서비스 기반)
     */
    public static ChatRoom createChatRoom(
            Auction auction,
            User seller, 
            User buyer, 
            ConnectionService connectionService) {
        
        return ChatRoom.builder()
                .auction(auction)
                .seller(seller)
                .buyer(buyer)
                .connectionService(connectionService)
                .status(ChatRoomStatus.INACTIVE)
                .build();
    }

    // === 비즈니스 메서드 ===
    
    /**
     * 채팅방 활성화 (수수료 결제 완료 시)
     */
    public ChatRoom activateChatRoom() {
        return ChatRoom.builder()
                .id(this.id)
                .auction(this.auction)
                .seller(this.seller)
                .buyer(this.buyer)
                .connectionService(this.connectionService)
                .status(ChatRoomStatus.ACTIVE)
                .lastMessageAt(this.lastMessageAt)
                .activatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 채팅방 종료 (거래 완료 시)
     */
    public ChatRoom closeChatRoom() {
        return ChatRoom.builder()
                .id(this.id)
                .auction(this.auction)
                .seller(this.seller)
                .buyer(this.buyer)
                .connectionService(this.connectionService)
                .status(ChatRoomStatus.CLOSED)
                .lastMessageAt(this.lastMessageAt)
                .activatedAt(this.activatedAt)
                .build();
    }
    
    /**
     * 마지막 메시지 시간 업데이트
     */
    public ChatRoom updateLastMessageTime() {
        return ChatRoom.builder()
                .id(this.id)
                .auction(this.auction)
                .seller(this.seller)
                .buyer(this.buyer)
                .connectionService(this.connectionService)
                .status(this.status)
                .lastMessageAt(LocalDateTime.now())
                .activatedAt(this.activatedAt)
                .build();
    }
    
    /**
     * 채팅방이 활성화되었는지 확인
     */
    public boolean isActive() {
        return this.status == ChatRoomStatus.ACTIVE;
    }
    
    /**
     * 사용자가 채팅방 참여자인지 확인
     */
    public boolean isParticipant(Long userId) {
        return seller.getId().equals(userId) || buyer.getId().equals(userId);
    }
}