package com.cherrypick.app.domain.chat.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 메시지 전송 상태 enum
 * 실시간 채팅에서 메시지 전송 상태를 추적하기 위한 열거형
 */
@Getter
@RequiredArgsConstructor
public enum MessageDeliveryStatus {
    
    /**
     * 발송됨 - 서버에서 메시지가 정상적으로 저장되고 전송됨
     */
    SENT("발송됨"),
    
    /**
     * 전달됨 - 수신자의 클라이언트가 메시지를 받았음 (WebSocket 연결 확인됨)
     */
    DELIVERED("전달됨"),
    
    /**
     * 읽음 - 수신자가 실제로 메시지를 읽었음
     */
    READ("읽음");
    
    private final String description;
    
    /**
     * 다음 상태로 전환 가능한지 확인
     * 
     * @param nextStatus 다음 상태
     * @return 전환 가능 여부
     */
    public boolean canTransitionTo(MessageDeliveryStatus nextStatus) {
        return switch (this) {
            case SENT -> nextStatus == DELIVERED || nextStatus == READ;
            case DELIVERED -> nextStatus == READ;
            case READ -> false; // 읽음 상태에서는 더 이상 변경 불가
        };
    }
    
    /**
     * 메시지 상태가 읽음인지 확인
     * 
     * @return 읽음 상태 여부
     */
    public boolean isRead() {
        return this == READ;
    }
    
    /**
     * 메시지 상태가 전달 이상인지 확인 (전달됨 또는 읽음)
     * 
     * @return 전달 이상 상태 여부
     */
    public boolean isDeliveredOrRead() {
        return this == DELIVERED || this == READ;
    }
}