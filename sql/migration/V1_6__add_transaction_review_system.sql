-- ================================================================
-- V1.6 거래 후기 시스템 추가
-- ================================================================
-- 작성일: 2025-11-01
-- 설명: 3단계 거래 후기 시스템 (좋았어요/평범해요/별로에요)
-- ================================================================

-- ----------------------------------------------------------------
-- 1. transaction_reviews 테이블 생성
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transaction_reviews (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    reviewee_id BIGINT NOT NULL,
    review_type VARCHAR(10) NOT NULL CHECK (review_type IN ('SELLER', 'BUYER')),
    rating_type VARCHAR(10) NOT NULL CHECK (rating_type IN ('GOOD', 'NORMAL', 'BAD')),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_review_transaction FOREIGN KEY (transaction_id)
        REFERENCES transactions(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_reviewee FOREIGN KEY (reviewee_id)
        REFERENCES users(id) ON DELETE CASCADE,

    -- 중복 후기 방지: 1인당 1거래에 1개 후기만 작성 가능
    CONSTRAINT uk_review_transaction_reviewer UNIQUE (transaction_id, reviewer_id)
);

-- 인덱스 추가 (조회 성능 최적화)
CREATE INDEX idx_review_reviewee_type ON transaction_reviews(reviewee_id, review_type);
CREATE INDEX idx_review_transaction ON transaction_reviews(transaction_id);
CREATE INDEX idx_review_created_at ON transaction_reviews(created_at DESC);

COMMENT ON TABLE transaction_reviews IS '거래 후기 테이블 - 3단계 평가 시스템';
COMMENT ON COLUMN transaction_reviews.review_type IS '후기 유형: SELLER(판매자 후기) / BUYER(구매자 후기)';
COMMENT ON COLUMN transaction_reviews.rating_type IS '평가 등급: GOOD(좋았어요) / NORMAL(평범해요) / BAD(별로에요)';

-- ----------------------------------------------------------------
-- 2. users 테이블에 후기 통계 필드 추가
-- ----------------------------------------------------------------
ALTER TABLE users
ADD COLUMN IF NOT EXISTS seller_review_good INT DEFAULT 0 NOT NULL,
ADD COLUMN IF NOT EXISTS seller_review_normal INT DEFAULT 0 NOT NULL,
ADD COLUMN IF NOT EXISTS seller_review_bad INT DEFAULT 0 NOT NULL,
ADD COLUMN IF NOT EXISTS buyer_review_good INT DEFAULT 0 NOT NULL,
ADD COLUMN IF NOT EXISTS buyer_review_normal INT DEFAULT 0 NOT NULL,
ADD COLUMN IF NOT EXISTS buyer_review_bad INT DEFAULT 0 NOT NULL;

COMMENT ON COLUMN users.seller_review_good IS '판매자로 받은 좋았어요 후기 수';
COMMENT ON COLUMN users.seller_review_normal IS '판매자로 받은 평범해요 후기 수';
COMMENT ON COLUMN users.seller_review_bad IS '판매자로 받은 별로에요 후기 수';
COMMENT ON COLUMN users.buyer_review_good IS '구매자로 받은 좋았어요 후기 수';
COMMENT ON COLUMN users.buyer_review_normal IS '구매자로 받은 평범해요 후기 수';
COMMENT ON COLUMN users.buyer_review_bad IS '구매자로 받은 별로에요 후기 수';

-- ----------------------------------------------------------------
-- 3. notification_history 테이블의 type CHECK 제약조건에 REVIEW_RECEIVED 추가
-- ----------------------------------------------------------------
-- 기존 제약조건 삭제
ALTER TABLE notification_history DROP CONSTRAINT IF EXISTS notification_history_type_check;

-- 새로운 제약조건 추가 (REVIEW_RECEIVED 포함)
ALTER TABLE notification_history
ADD CONSTRAINT notification_history_type_check CHECK (
    type IN (
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
        'REVIEW_RECEIVED'  -- 새로 추가: 후기 수신 알림
    )
);

COMMENT ON CONSTRAINT notification_history_type_check ON notification_history IS '알림 타입 제약조건 (후기 수신 알림 포함)';

-- ================================================================
-- 마이그레이션 완료
-- ================================================================
