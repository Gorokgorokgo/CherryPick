-- Soft Delete 기능 추가를 위한 마이그레이션
-- BaseEntity를 상속하는 모든 테이블에 deleted_at, deleted_by 컬럼 추가

-- 1. Questions 테이블에 soft delete 컬럼 추가
ALTER TABLE questions
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by BIGINT;

CREATE INDEX idx_questions_deleted_at ON questions(deleted_at);

-- 2. Answers 테이블에 soft delete 컬럼 추가
ALTER TABLE answers
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by BIGINT;

CREATE INDEX idx_answers_deleted_at ON answers(deleted_at);

-- 3. User Accounts 테이블에 soft delete 컬럼 추가
ALTER TABLE user_accounts
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by BIGINT;

CREATE INDEX idx_user_accounts_deleted_at ON user_accounts(deleted_at);

-- 4. Uploaded Images 테이블에 soft delete 컬럼 추가
ALTER TABLE uploaded_images
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by BIGINT;

CREATE INDEX idx_uploaded_images_deleted_at ON uploaded_images(deleted_at);

-- 5. Chat Messages 테이블에 soft delete 컬럼 추가
ALTER TABLE chat_messages
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by BIGINT;

CREATE INDEX idx_chat_messages_deleted_at ON chat_messages(deleted_at);

-- 6. Chat Rooms 테이블에 soft delete 컬럼 추가
ALTER TABLE chat_rooms
ADD COLUMN deleted_at TIMESTAMP,
ADD COLUMN deleted_by BIGINT;

CREATE INDEX idx_chat_rooms_deleted_at ON chat_rooms(deleted_at);

-- 주석:
-- - deleted_at이 NULL이면 활성 레코드
-- - deleted_at이 NOT NULL이면 삭제된 레코드
-- - deleted_by는 삭제를 수행한 사용자 ID (관리자 삭제 시 NULL 가능)
-- - 인덱스 추가로 삭제되지 않은 레코드 조회 성능 향상
