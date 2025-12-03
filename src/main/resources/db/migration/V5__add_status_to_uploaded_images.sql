-- uploaded_images 테이블에 status 컬럼 추가
ALTER TABLE uploaded_images ADD COLUMN status VARCHAR(20);

-- 기존 데이터는 모두 'PERMANENT' (영구 저장) 상태로 초기화
-- (기존 이미지는 이미 사용 중인 것으로 간주하여 삭제되지 않도록 보호)
UPDATE uploaded_images SET status = 'PERMANENT';

-- 컬럼 속성 변경: NOT NULL 및 기본값 설정
ALTER TABLE uploaded_images ALTER COLUMN status SET DEFAULT 'TEMP';
ALTER TABLE uploaded_images ALTER COLUMN status SET NOT NULL;
