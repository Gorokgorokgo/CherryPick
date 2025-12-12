-- V8: notification_history 테이블의 type CHECK CONSTRAINT 업데이트
-- 새 알림 타입 추가: OUTBID, AUCTION_ENDING_SOON_15M, AUCTION_ENDING_SOON_5M, KEYWORD_ALERT, TRANSACTION_PENDING, TRANSACTION_CANCELLED

-- 기존 CHECK CONSTRAINT 삭제
ALTER TABLE notification_history DROP CONSTRAINT IF EXISTS notification_history_type_check;

-- 새 CHECK CONSTRAINT 추가 (모든 NotificationType 포함)
ALTER TABLE notification_history ADD CONSTRAINT notification_history_type_check CHECK (
    type IN (
        -- 기존 타입들
        'NEW_BID',
        'AUCTION_WON',
        'AUCTION_SOLD',
        'AUCTION_NOT_SOLD',
        'AUCTION_NOT_SOLD_HIGHEST_BIDDER',
        'AUCTION_ENDED',
        'CONNECTION_PAYMENT_REQUEST',
        'CHAT_ACTIVATED',
        'NEW_MESSAGE',
        'TRANSACTION_COMPLETED',
        'PROMOTION',
        'AUCTION_EXTENDED',
        -- 새 타입들 (Push Notification Type A, B, C)
        'OUTBID',
        'AUCTION_ENDING_SOON_15M',
        'AUCTION_ENDING_SOON_5M',
        'KEYWORD_ALERT',
        -- 거래 관련 새 타입들
        'TRANSACTION_PENDING',
        'TRANSACTION_CANCELLED'
    )
);

COMMENT ON CONSTRAINT notification_history_type_check ON notification_history IS '알림 타입 유효성 검증 (NotificationType enum 값들)';
