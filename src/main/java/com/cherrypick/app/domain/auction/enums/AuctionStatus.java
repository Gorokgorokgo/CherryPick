package com.cherrypick.app.domain.auction.enums;

public enum AuctionStatus {
    ACTIVE,           // 진행 중
    ENDED,           // 정상 낙찰 완료
    NO_RESERVE_MET,  // Reserve Price 미달로 유찰
    CANCELLED,       // 취소됨
    DELETED          // 삭제됨 (소프트 삭제)
}