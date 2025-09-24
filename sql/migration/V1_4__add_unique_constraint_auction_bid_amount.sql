-- V1.4: 중복 입찰 금액 방지를 위한 UNIQUE 제약조건 추가
-- 동시성 이슈 해결: 같은 경매에서 같은 금액으로 중복 입찰 원천 차단

-- 중복 입찰 금액 방지용 부분 UNIQUE 인덱스 생성
-- bid_amount > 0 AND is_auto_bid = false 조건으로 수동입찰만 중복 방지
-- 자동입찰(is_auto_bid = true)은 경쟁 시뮬레이션에서 중복될 수 있으므로 제외
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_manual_bid_amount
ON bids (auction_id, bid_amount)
WHERE bid_amount > 0 AND is_auto_bid = false;

-- 인덱스 생성 확인 로그
-- 이 제약조건으로 수동입찰에서 A가 6200원 입찰 후 B가 6200원 입찰하는 것을 데이터베이스 레벨에서 차단
-- 자동입찰은 경쟁 로직에서 처리