-- 기존 경매 데이터에 seller_verified_region_at_creation 값 채우기
-- 기존 경매는 판매자의 현재 verifiedRegion으로 채움 (최선의 추정)

-- users 테이블에 verified_region 컬럼이 존재하는 경우에만 실행
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'users'
          AND column_name = 'verified_region'
    ) THEN
        UPDATE auctions a
        SET seller_verified_region_at_creation = (
            SELECT u.verified_region
            FROM users u
            WHERE u.id = a.seller_id
        )
        WHERE seller_verified_region_at_creation IS NULL
          AND EXISTS (
            SELECT 1
            FROM users u
            WHERE u.id = a.seller_id
              AND u.verified_region IS NOT NULL
          );
    END IF;
END $$;

-- 주석:
-- 기존 경매에 대해 판매자의 현재 verifiedRegion을 채워넣습니다.
-- 이는 완벽한 해결책은 아니지만, NULL 대신 유의미한 값을 제공합니다.
-- 새로운 경매부터는 정확한 등록 당시 주소가 저장됩니다.
-- users.verified_region 컬럼이 없으면 이 마이그레이션은 스킵됩니다.
