package com.cherrypick.app.domain.connection.enums;

/**
 * 연결 서비스 상태
 */
public enum ConnectionStatus {
    /**
     * 대기 중 - 수수료 결제 대기
     */
    PENDING("대기 중"),
    
    /**
     * 활성화 - 채팅 연결 가능
     */
    ACTIVE("활성화"),
    
    /**
     * 완료 - 거래 완료
     */
    COMPLETED("완료"),
    
    /**
     * 취소 - 연결 서비스 취소
     */
    CANCELLED("취소");

    private final String description;

    ConnectionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}