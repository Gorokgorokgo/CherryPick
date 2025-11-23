-- 경매 등록 당시 판매자의 GPS 인증 주소를 저장하는 컬럼 추가
-- 판매자가 이사를 가더라도 경매 등록 당시의 주소를 보존

ALTER TABLE auctions
ADD COLUMN seller_verified_region_at_creation VARCHAR(100);

-- 컬럼 설명:
-- seller_verified_region_at_creation: 경매 등록 당시 판매자의 verifiedRegion (스냅샷)
-- 예: "서울특별시 강남구 역삼1동"
-- NULL 허용: 경매 등록 당시 GPS 인증을 하지 않은 경우

CREATE INDEX idx_auctions_seller_verified_region ON auctions(seller_verified_region_at_creation);
