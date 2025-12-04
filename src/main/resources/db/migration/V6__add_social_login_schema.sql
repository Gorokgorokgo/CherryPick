-- 1. users 테이블에 verified_region 컬럼 추가 (PostgreSQL)
ALTER TABLE users ADD COLUMN IF NOT EXISTS verified_region VARCHAR(100);

-- 2. social_accounts 테이블 생성 (PostgreSQL)
CREATE TABLE IF NOT EXISTS social_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    name VARCHAR(50),
    profile_image_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    last_login_at TIMESTAMP,
    login_attempt_count INTEGER DEFAULT 0 NOT NULL,
    is_locked BOOLEAN DEFAULT FALSE NOT NULL,
    locked_until TIMESTAMP,
    last_login_ip VARCHAR(45),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    
    CONSTRAINT fk_social_accounts_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_provider_provider_id UNIQUE (provider, provider_id)
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_social_accounts_user_id ON social_accounts(user_id);
