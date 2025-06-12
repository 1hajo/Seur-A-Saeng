#!/bin/bash

set -e

echo "🚀 프론트엔드 배포 시작..."

# 환경변수 파일 생성
cat > seurasaeng_fe/.env << EOF
VITE_API_BASE_URL=https://seurasaeng.site/api
VITE_SOCKET_URL=wss://seurasaeng.site/ws
VITE_MOBILITY_API_KEY=2868494a3053c4014954615d4dcfafc1
VITE_KAKAOMAP_API_KEY=d079914b9511e06b410311be64216366
VITE_PERPLEXITY_API_KEY=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
VITE_MOBILITY_API_BASE_URL=https://apis-navi.kakaomobility.com/v1/directions
VITE_KAKAOMAP_API_BASE_URL=//dapi.kakao.com/v2/maps/sdk.js
EOF

# Docker 이미지 로드
if [ -f "seurasaeng_fe-image.tar.gz" ]; then
    echo "📦 Docker 이미지 로드 중..."
    docker load < seurasaeng_fe-image.tar.gz
    rm -f seurasaeng_fe-image.tar.gz
fi

# 기존 컨테이너 중지 및 새 컨테이너 시작
echo "🔄 컨테이너 재시작 중..."
cd seurasaeng_fe
docker-compose down --timeout 30 2>/dev/null || true
docker-compose up -d

# 간단한 헬스체크
echo "⏳ 서비스 시작 대기 중..."
for i in {1..12}; do
    if curl -f -s http://localhost/health >/dev/null 2>&1; then
        echo "✅ 프론트엔드 서비스 정상"
        break
    fi
    if [ $i -eq 12 ]; then
        echo "❌ 헬스체크 실패"
        docker-compose logs --tail=20
        exit 1
    fi
    sleep 5
done

echo "🎉 배포 완료!"
echo "🌐 접속: https://seurasaeng.site"