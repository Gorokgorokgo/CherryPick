package com.cherrypick.app.domain.notification.event;

import com.cherrypick.app.domain.notification.enums.NotificationType;

public class AccountRestoredEvent extends NotificationEvent {

    public AccountRestoredEvent(Object source, Long targetUserId) {
        super(source, 
              NotificationType.PROMOTION, 
              targetUserId, 
              "계정 복구 완료", 
              "탈퇴한 계정이 성공적으로 복구되었습니다. 다시 오신 것을 환영합니다!", 
              null);
    }
}