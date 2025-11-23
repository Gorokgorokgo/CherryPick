-- Soft Delete 컬럼 추가 (긴급 수정)
-- 실행 방법: PostgreSQL에 접속하여 이 SQL을 실행하세요

-- 1. Questions 테이블
ALTER TABLE questions ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS deleted_by BIGINT;
CREATE INDEX IF NOT EXISTS idx_questions_deleted_at ON questions(deleted_at);

-- 2. Answers 테이블
ALTER TABLE answers ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE answers ADD COLUMN IF NOT EXISTS deleted_by BIGINT;
CREATE INDEX IF NOT EXISTS idx_answers_deleted_at ON answers(deleted_at);

-- 3. User Accounts 테이블
ALTER TABLE user_accounts ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE user_accounts ADD COLUMN IF NOT EXISTS deleted_by BIGINT;
CREATE INDEX IF NOT EXISTS idx_user_accounts_deleted_at ON user_accounts(deleted_at);

-- 4. Uploaded Images 테이블
ALTER TABLE uploaded_images ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE uploaded_images ADD COLUMN IF NOT EXISTS deleted_by BIGINT;
CREATE INDEX IF NOT EXISTS idx_uploaded_images_deleted_at ON uploaded_images(deleted_at);

-- 5. Chat Messages 테이블 (BaseEntity 상속)
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE chat_messages ADD COLUMN IF NOT EXISTS deleted_by BIGINT;
CREATE INDEX IF NOT EXISTS idx_chat_messages_deleted_at ON chat_messages(deleted_at);

-- 6. Chat Rooms 테이블 (BaseEntity 상속)
ALTER TABLE chat_rooms ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE chat_rooms ADD COLUMN IF NOT EXISTS deleted_by BIGINT;
CREATE INDEX IF NOT EXISTS idx_chat_rooms_deleted_at ON chat_rooms(deleted_at);

-- 7. Bids 테이블 (BaseEntity 상속)
ALTER TABLE bids ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE bids ADD COLUMN IF NOT EXISTS deleted_by BIGINT;
CREATE INDEX IF NOT EXISTS idx_bids_deleted_at ON bids(deleted_at);

-- 8. Points (PointTransaction) 테이블 (BaseEntity 상속)
ALTER TABLE points ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE points ADD COLUMN IF NOT EXISTS deleted_by BIGINT;
CREATE INDEX IF NOT EXISTS idx_points_deleted_at ON points(deleted_at);

-- 9. Transactions 테이블 (BaseEntity 상속)
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS deleted_by BIGINT;
CREATE INDEX IF NOT EXISTS idx_transactions_deleted_at ON transactions(deleted_at);

-- 확인
SELECT
    table_name,
    column_name,
    data_type
FROM information_schema.columns
WHERE column_name IN ('deleted_at', 'deleted_by')
ORDER BY table_name, column_name;
