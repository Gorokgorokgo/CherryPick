#!/bin/bash

# SSL 설정 스크립트
# 사용자의 입력을 받아 처리하도록 수정됨

DOMAIN="cherrypick-api-server.store"

echo "🔒 SSL 인증서 설정을 시작합니다."
echo "대상 도메인: $DOMAIN"

# 1. 사용자 이메일 입력 (하드코딩 제거)
echo ""
echo "❓ 인증서 만료 알림을 받을 이메일 주소를 입력해주세요."
read -p "이메일: " USER_EMAIL

if [ -z "$USER_EMAIL" ]; then
    echo "❌ 이메일이 입력되지 않았습니다. 스크립트를 종료합니다."
    exit 1
fi

# 2. Certbot 설치 확인
if ! command -v certbot &> /dev/null; then
    echo "📦 Certbot(인증서 발급 도구)을 설치합니다..."
    sudo apt-get update
    sudo apt-get install -y certbot
fi

# 3. 기존 Nginx 컨테이너 중지 (80 포트 확보)
# Standalone 모드는 80번 포트를 직접 사용해야 합니다.
echo "🛑 80번 포트 확보를 위해 Nginx 컨테이너를 잠시 중지합니다..."
docker-compose -f docker-compose.prod.yml stop nginx

# 4. 인증서 발급 (Standalone 모드)
echo "🎫 인증서 발급을 요청합니다..."
# --non-interactive: 사용자 개입 없이 진행
# --agree-tos: 이용약관 동의
# -m: 알림 받을 이메일 설정
sudo certbot certonly --standalone -d $DOMAIN --non-interactive --agree-tos -m "$USER_EMAIL"

if [ $? -ne 0 ]; then
    echo "❌ 인증서 발급에 실패했습니다."
    echo "   1. DNS 설정(A 레코드)이 이 서버의 IP로 되어있는지 확인해주세요."
    echo "   2. 방화벽(AWS Security Group 등)에서 80번 포트가 열려있는지 확인해주세요."
    # 실패 시에도 서비스는 다시 켜줌
    docker-compose -f docker-compose.prod.yml start nginx
    exit 1
fi

# 5. 서비스 재시작
echo "🚀 인증서 적용을 위해 서비스를 재배포합니다..."
./deploy.sh

echo "✅ SSL 설정이 완료되었습니다!"
echo "이제 https://$DOMAIN 으로 안전하게 접속할 수 있습니다."