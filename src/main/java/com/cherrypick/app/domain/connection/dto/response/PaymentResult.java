package com.cherrypick.app.domain.connection.dto.response;

import com.cherrypick.app.domain.connection.enums.ConnectionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 연결 서비스 결제 결과 응답 DTO
 */
@Getter
@Builder
public class PaymentResult {

    private Long connectionId;
    
    /**
     * 결제 성공 여부
     */
    private Boolean success;
    
    /**
     * 결제 완료 후 연결 서비스 상태
     */
    private ConnectionStatus status;
    
    /**
     * 채팅방 활성화 여부
     */
    private Boolean chatRoomActivated;
    
    /**
     * 연결 활성화 시간
     */
    private LocalDateTime connectedAt;
    
    /**
     * 결과 메시지
     */
    private String message;
    
    /**
     * 에러 코드 (실패시)
     */
    private String errorCode;
}