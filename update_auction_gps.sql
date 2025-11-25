-- 기존 유찰경매 물품에 판매자의 GPS 좌표 복사
-- 판매자가 위치 인증을 했다면, 판매자의 GPS 좌표를 경매 테이블에 업데이트

UPDATE auctions a
SET
    latitude = u.latitude,
    longitude = u.longitude
FROM users u
WHERE a.seller_id = u.id
  AND a.status IN ('NO_RESERVE_MET', 'ENDED')  -- 유찰 또는 종료된 경매만
  AND a.latitude IS NULL  -- GPS 좌표가 없는 경매만
  AND u.latitude IS NOT NULL  -- 판매자가 위치 인증을 한 경우만
  AND u.longitude IS NOT NULL;

-- 업데이트된 행 수 확인
SELECT COUNT(*) as updated_count
FROM auctions a
JOIN users u ON a.seller_id = u.id
WHERE a.status IN ('NO_RESERVE_MET', 'ENDED')
  AND a.latitude IS NOT NULL
  AND a.longitude IS NOT NULL;
