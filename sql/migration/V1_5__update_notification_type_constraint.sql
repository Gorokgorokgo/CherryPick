-- notification_history 테이블의 type CHECK 제약조건에 AUCTION_SOLD, AUCTION_NOT_SOLD, AUCTION_ENDED 추가
-- 작성일: 2025-10-10

-- 기존 제약조건 삭제
ALTER TABLE notification_history
DROP CONSTRAINT IF EXISTS notification_history_type_check;

-- 새로운 제약조건 추가 (모든 NotificationType enum 값 포함)
ALTER TABLE notification_history
ADD CONSTRAINT notification_history_type_check
CHECK (type IN (
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
  'PROMOTION'
));