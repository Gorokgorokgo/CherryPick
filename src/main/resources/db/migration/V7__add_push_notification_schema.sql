-- V7: Push Notification System Schema
-- Type A: Outbid (Higher Bid), Type B: Ending Soon, Type C: Keyword Alert

-- 1. notification_settings 테이블에 새 알림 설정 컬럼 추가
ALTER TABLE notification_settings ADD COLUMN IF NOT EXISTS outbid_notification BOOLEAN DEFAULT TRUE NOT NULL;
ALTER TABLE notification_settings ADD COLUMN IF NOT EXISTS ending_soon_notification BOOLEAN DEFAULT TRUE NOT NULL;
ALTER TABLE notification_settings ADD COLUMN IF NOT EXISTS keyword_notification BOOLEAN DEFAULT TRUE NOT NULL;

-- 2. user_keywords 테이블 생성 (사용자 키워드 알림 구독)
CREATE TABLE IF NOT EXISTS user_keywords (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    keyword VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,
    deleted_by BIGINT,

    CONSTRAINT fk_user_keywords_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_user_keywords_user_id ON user_keywords(user_id);
CREATE INDEX IF NOT EXISTS idx_user_keywords_keyword ON user_keywords(keyword);
CREATE INDEX IF NOT EXISTS idx_user_keywords_category ON user_keywords(category);
CREATE INDEX IF NOT EXISTS idx_user_keywords_is_active ON user_keywords(is_active);
CREATE INDEX IF NOT EXISTS idx_user_keywords_deleted_at ON user_keywords(deleted_at);

-- 복합 인덱스: 활성 키워드 검색 최적화
CREATE INDEX IF NOT EXISTS idx_user_keywords_active_keyword ON user_keywords(is_active, keyword) WHERE deleted_at IS NULL;

-- 3. 코멘트 추가 (PostgreSQL)
COMMENT ON TABLE user_keywords IS '사용자 키워드 알림 구독 테이블 (Type C: Keyword Alert)';
COMMENT ON COLUMN user_keywords.keyword IS '알림 받을 키워드 (소문자로 저장)';
COMMENT ON COLUMN user_keywords.category IS '카테고리 필터 (null이면 전체 카테고리)';
COMMENT ON COLUMN user_keywords.is_active IS '키워드 활성화 상태';

COMMENT ON COLUMN notification_settings.outbid_notification IS 'Type A: 더 높은 입찰 발생 알림 (Outbid)';
COMMENT ON COLUMN notification_settings.ending_soon_notification IS 'Type B: 경매 마감 임박 알림 (15분/5분 전)';
COMMENT ON COLUMN notification_settings.keyword_notification IS 'Type C: 키워드 매칭 알림';
