-- 알림 히스토리 테이블에 chat_room_id 컬럼 추가
-- 경매 낙찰 시 채팅방 연결을 위한 필드

-- chat_room_id 컬럼 추가 (nullable, BIGINT)
ALTER TABLE notification_history
ADD COLUMN IF NOT EXISTS chat_room_id BIGINT;

-- 인덱스 추가 (채팅방 ID로 알림 조회 성능 개선)
CREATE INDEX IF NOT EXISTS idx_notification_history_chat_room_id
ON notification_history(chat_room_id);

-- 주석 추가
COMMENT ON COLUMN notification_history.chat_room_id IS '채팅방 ID (낙찰 알림 시 사용)';
