# CherryPick 개발 환경 설정 가이드

## 🚀 로컬 개발 환경 구성

### 1. 개발용 데이터베이스 실행

```bash
# 개발용 PostgreSQL + Redis 실행
docker-compose -f docker-compose.dev.yml up -d

# 상태 확인
docker-compose -f docker-compose.dev.yml ps

# 로그 확인
docker-compose -f docker-compose.dev.yml logs
```

### 2. 환경 변수 설정

```bash
# 로컬 개발용 환경 변수 파일 복사
cp .env.local .env

# 또는 직접 환경 변수 지정
export SPRING_PROFILES_ACTIVE=dev
```

### 3. 애플리케이션 실행

```bash
# Spring Boot 애플리케이션 실행
./gradlew bootRun

# 또는 IDE에서 실행
# CherrypickApplication.java → Run
```

### 4. 접속 확인

- **애플리케이션**: http://localhost:8080
- **API 문서**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **pgAdmin**: http://localhost:8081 (admin@cherrypick.com / admin123)

## 🛠️ 개발 도구

### 데이터베이스 접속 정보

```
Host: localhost
Port: 5432
Database: cherrypick
Username: postgres  
Password: password123
```

### Redis 접속

```bash
# Redis CLI 접속
docker exec -it cherrypick-redis-dev redis-cli
```

## 🔄 환경별 실행

### 로컬 개발
```bash
# 환경 변수: .env.local 사용
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

### 프로덕션 테스트
```bash
# 환경 변수: .env 사용  
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun
```

## 🧹 정리

```bash
# 개발 컨테이너 중지
docker-compose -f docker-compose.dev.yml down

# 데이터까지 삭제
docker-compose -f docker-compose.dev.yml down -v
```

## 📝 팁

- **데이터 초기화**: 컨테이너 재시작시 `sql/init/` 폴더의 스크립트 자동 실행
- **포트 충돌**: 다른 PostgreSQL이 실행 중이면 포트 변경
- **IDE 설정**: IntelliJ Database Tool로 PostgreSQL 연결 가능