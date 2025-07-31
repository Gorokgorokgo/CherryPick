#!/bin/bash

# CherryPick 배포 스크립트

set -e

echo "🚀 CherryPick 배포 시작..."

# 환경 설정
ENV_FILE=".env"
COMPOSE_FILE="docker-compose.prod.yml"

# .env 파일 존재 확인
if [ ! -f "$ENV_FILE" ]; then
    echo "❌ .env 파일이 없습니다. 환경 변수를 설정해주세요."
    exit 1
fi

# Docker Compose 파일 존재 확인
if [ ! -f "$COMPOSE_FILE" ]; then
    echo "❌ $COMPOSE_FILE 파일이 없습니다."
    exit 1
fi

# Git 최신 코드 가져오기
echo "📥 최신 코드 가져오는 중..."
git pull origin main

# 기존 컨테이너 중지 및 제거
echo "🛑 기존 서비스 중지 중..."
docker-compose -f $COMPOSE_FILE down

# 이미지 빌드
echo "🔨 Docker 이미지 빌드 중..."
docker-compose -f $COMPOSE_FILE build --no-cache

# 서비스 시작
echo "▶️ 서비스 시작 중..."
docker-compose -f $COMPOSE_FILE up -d

# 서비스 상태 확인
echo "🔍 서비스 상태 확인 중..."
sleep 30

# Health check
echo "🏥 Health check 수행 중..."
max_attempts=30
attempt=0

while [ $attempt -lt $max_attempts ]; do
    if curl -f http://localhost/health > /dev/null 2>&1; then
        echo "✅ 애플리케이션이 정상적으로 시작되었습니다!"
        break
    fi
    
    attempt=$((attempt + 1))
    echo "⏳ Health check 시도 $attempt/$max_attempts..."
    sleep 5
done

if [ $attempt -eq $max_attempts ]; then
    echo "❌ 애플리케이션 시작에 실패했습니다."
    echo "📋 컨테이너 로그:"
    docker-compose -f $COMPOSE_FILE logs app
    exit 1
fi

# 오래된 이미지 정리
echo "🧹 오래된 Docker 이미지 정리 중..."
docker image prune -f

# 배포 완료
echo "🎉 배포가 성공적으로 완료되었습니다!"
echo "🌐 애플리케이션 URL: http://$(curl -s ifconfig.me)"
echo "📊 서비스 상태:"
docker-compose -f $COMPOSE_FILE ps