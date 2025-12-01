# Multi-stage build for CherryPick Spring Boot Application
FROM eclipse-temurin:17-jdk-jammy AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle wrapper와 설정 파일 복사
COPY gradlew .
COPY gradle gradle/
COPY build.gradle .

# 소스 코드 복사
COPY src src/

# 실행 권한 부여 및 빌드
RUN chmod +x gradlew
RUN ./gradlew build -x test

# Runtime stage
FROM eclipse-temurin:17-jre-jammy

# 작업 디렉토리 설정
WORKDIR /app

# 필요한 패키지 설치
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 애플리케이션 사용자 생성
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 빌드된 JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 소유자 변경
RUN chown appuser:appuser app.jar

# 애플리케이션 사용자로 전환
USER appuser

# 포트 노출
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-Dspring.profiles.active=prod", "-jar", "app.jar"]