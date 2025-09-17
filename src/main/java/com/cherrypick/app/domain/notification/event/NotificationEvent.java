package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.notification.enums.NotificationType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 알림 이벤트 기본 클래스
 */
@Getter
public abstract class NotificationEvent extends ApplicationEvent {

    private final NotificationType notificationType;
    private final Long targetUserId;
    private final String title;
    private final String message;
    private final Long resourceId;

    protected NotificationEvent(Object source, NotificationType notificationType,
                              Long targetUserId, String title, String message, Long resourceId) {
        super(source);
        this.notificationType = notificationType;
        this.targetUserId = targetUserId;
        this.title = title;
        this.message = message;
        this.resourceId = resourceId;
    }
}