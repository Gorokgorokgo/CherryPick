-- 기존 category check constraint 제거
ALTER TABLE auctions DROP CONSTRAINT IF EXISTS auctions_category_check;

-- 새로운 카테고리 값들을 허용하는 check constraint 추가
ALTER TABLE auctions 
ADD CONSTRAINT auctions_category_check 
CHECK (category IN (
    'MOBILE_DEVICES',     -- 스마트폰/태블릿
    'COMPUTERS',          -- 컴퓨터/노트북
    'AUDIO',              -- 오디오/이어폰
    'CAMERAS',            -- 카메라/캠코더
    'DISPLAYS',           -- TV/모니터
    'GAMING',             -- 게임/콘솔
    'ELECTRONICS_OTHER',  -- 전자제품 기타
    'CLOTHING',           -- 의류/패션
    'FASHION_ACCESSORIES', -- 신발/가방/액세서리
    'WATCHES_JEWELRY',    -- 시계/주얼리
    'FURNITURE_INTERIOR', -- 가구/인테리어
    'HOME_KITCHEN',       -- 생활용품/주방용품
    'VEHICLES',           -- 자동차/오토바이
    'BICYCLES',           -- 자전거
    'AUTO_PARTS',         -- 자동차용품
    'FITNESS_SPORTS',     -- 운동/헬스/피트니스
    'BALL_SPORTS',        -- 구기/레저스포츠
    'BOOKS',              -- 도서/교재
    'MUSIC_HOBBIES',      -- 악기/취미용품
    'TOYS_COLLECTIBLES',  -- 완구/수집품
    'BEAUTY',             -- 화장품/뷰티
    'HEALTH',             -- 건강/의료용품
    'FOOD',               -- 식품/건강식품
    'PET_SUPPLIES',       -- 반려동물용품
    'TICKETS',            -- 티켓/쿠폰
    'OTHER'               -- 기타
));