package com.cherrypick.app.domain.notification.entity;

import com.cherrypick.app.domain.common.entity.BaseEntity;
import com.cherrypick.app.domain.notification.enums.NotificationType;
import com.cherrypick.app.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 알림 히스토리 (발송 기록)
 */
@Entity
@Table(name = "notification_history")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    /**
     * 관련 리소스 ID (경매 ID, 연결 서비스 ID 등)
     */
    @Column(name = "resource_id")
    private Long resourceId;

    /**
     * 채팅방 ID (낙찰 알림 시 사용)
     */
    @Column(name = "chat_room_id")
    private Long chatRoomId;

    /**
     * 읽음 여부
     */
    @Builder.Default
    @Column(name = "is_read", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isRead = false;

    /**
     * FCM 발송 성공 여부
     */
    @Builder.Default
    @Column(name = "fcm_sent", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean fcmSent = false;

    /**
     * 읽은 시간
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    // === 정적 팩토리 메서드 ===
    
    /**
     * 알림 히스토리 생성
     */
    public static NotificationHistory createNotification(
            User user,
            NotificationType type,
            String title,
            String message,
            Long resourceId) {

        return NotificationHistory.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .resourceId(resourceId)
                .build();
    }

    /**
     * 알림 히스토리 생성 (채팅방 ID 포함)
     */
    public static NotificationHistory createNotificationWithChatRoom(
            User user,
            NotificationType type,
            String title,
            String message,
            Long resourceId,
            Long chatRoomId) {

        return NotificationHistory.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .resourceId(resourceId)
                .chatRoomId(chatRoomId)
                .build();
    }

    // === 비즈니스 메서드 ===
    
    /**
     * 알림 읽음 처리
     */
    public NotificationHistory markAsRead() {
        return NotificationHistory.builder()
                .id(this.id)
                .user(this.user)
                .type(this.type)
                .title(this.title)
                .message(this.message)
                .resourceId(this.resourceId)
                .chatRoomId(this.chatRoomId)
                .isRead(true)
                .fcmSent(this.fcmSent)
                .readAt(LocalDateTime.now())
                .build();
    }

    /**
     * FCM 발송 성공 처리
     */
    public NotificationHistory markFcmSent() {
        return NotificationHistory.builder()
                .id(this.id)
                .user(this.user)
                .type(this.type)
                .title(this.title)
                .message(this.message)
                .resourceId(this.resourceId)
                .chatRoomId(this.chatRoomId)
                .isRead(this.isRead)
                .fcmSent(true)
                .readAt(this.readAt)
                .build();
    }
}