package com.cherrypick.app.domain.notification.dto.response;

import com.cherrypick.app.domain.notification.entity.NotificationHistory;
import com.cherrypick.app.domain.notification.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 알림 히스토리 응답 DTO
 */
@Getter
@Builder
public class NotificationHistoryResponse {

    private Long id;
    private NotificationType type;
    private String typeDescription;
    private String title;
    private String message;
    private Long resourceId;
    private Boolean isRead;
    private Boolean fcmSent;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    /**
     * Entity에서 DTO로 변환
     */
    public static NotificationHistoryResponse from(NotificationHistory notification) {
        return NotificationHistoryResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .typeDescription(notification.getType().getDescription())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .resourceId(notification.getResourceId())
                .isRead(notification.getIsRead())
                .fcmSent(notification.getFcmSent())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}