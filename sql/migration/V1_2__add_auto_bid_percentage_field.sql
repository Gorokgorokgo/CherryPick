-- 자동입찰 퍼센티지 필드 추가
-- 비즈니스 요구사항: 사용자 설정 퍼센티지 저장 (실행시 최소 5% 보장)

ALTER TABLE bids 
ADD COLUMN auto_bid_percentage INTEGER DEFAULT 5 
COMMENT '자동입찰 증가율 (5-10% 범위)';

-- 기존 자동입찰 데이터에 대한 기본값 설정
UPDATE bids 
SET auto_bid_percentage = 5 
WHERE is_auto_bid = true AND auto_bid_percentage IS NULL;

-- 퍼센티지 범위 제약 조건 추가
ALTER TABLE bids 
ADD CONSTRAINT chk_auto_bid_percentage_range 
CHECK (auto_bid_percentage IS NULL OR (auto_bid_percentage >= 5 AND auto_bid_percentage <= 10));

-- 인덱스 추가: 자동입찰 조회 성능 향상
CREATE INDEX idx_bids_auction_auto_active 
ON bids(auction_id, is_auto_bid, status, max_auto_bid_amount DESC)
WHERE is_auto_bid = true AND status = 'ACTIVE';

-- 자동입찰 통계용 뷰 생성 (선택사항)
CREATE OR REPLACE VIEW v_auto_bid_stats AS
SELECT 
    auction_id,
    COUNT(*) as active_auto_bidders,
    AVG(auto_bid_percentage) as avg_percentage,
    MAX(max_auto_bid_amount) as highest_max_amount,
    MIN(max_auto_bid_amount) as lowest_max_amount
FROM bids 
WHERE is_auto_bid = true AND status = 'ACTIVE'
GROUP BY auction_id;