package com.cherrypick.app.domain.user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 온라인 상태 enum
 * 실시간 채팅에서 사용자의 온라인 상태를 나타냄
 */
@Getter
@RequiredArgsConstructor
public enum OnlineStatus {
    
    /**
     * 온라인 - 현재 활성 상태
     */
    ONLINE("온라인"),
    
    /**
     * 오프라인 - 비활성 상태
     */
    OFFLINE("오프라인"),
    
    /**
     * 자리비움 - 일정 시간 비활성 상태
     */
    AWAY("자리비움");
    
    private final String description;
    
    /**
     * 온라인 상태인지 확인
     * 
     * @return 온라인 상태 여부
     */
    public boolean isOnline() {
        return this == ONLINE;
    }
    
    /**
     * 활성 상태인지 확인 (온라인 또는 자리비움)
     * 
     * @return 활성 상태 여부
     */
    public boolean isActive() {
        return this == ONLINE || this == AWAY;
    }
}