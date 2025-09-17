-- ===================================================================
-- 경매 상품 메타정보 필드 추가 마이그레이션
-- 작성일: 2025-08-30
-- 목적: 상품 상태(1-10점)와 구매일 필드 추가
-- ===================================================================

-- 1. 새 컬럼 추가 (NULL 허용으로 기존 데이터 호환성 보장)
ALTER TABLE auctions 
ADD COLUMN product_condition INTEGER;

ALTER TABLE auctions 
ADD COLUMN purchase_date DATE;

-- 2. 컬럼에 대한 설명 추가 (PostgreSQL)
COMMENT ON COLUMN auctions.product_condition IS '상품 상태 (1-10점, 10점이 최상)';
COMMENT ON COLUMN auctions.purchase_date IS '상품 구매일';

-- 3. 체크 제약 조건 추가 (1-10점 범위 검증)
ALTER TABLE auctions 
ADD CONSTRAINT chk_product_condition 
CHECK (product_condition IS NULL OR (product_condition >= 1 AND product_condition <= 10));

-- 4. 인덱스 추가 (상품 상태별 조회 성능 향상)
CREATE INDEX idx_auctions_product_condition ON auctions(product_condition) 
WHERE product_condition IS NOT NULL;

-- ===================================================================
-- 주의사항:
-- 1. 기존 데이터는 NULL 값을 가지며, 애플리케이션에서 기본값(7점) 처리
-- 2. 새로운 경매 등록시에만 필수값으로 처리
-- 3. 운영 환경 배포시 트래픽이 적은 시간대에 실행 권장
-- ===================================================================