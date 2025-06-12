#!/bin/bash
set -e

echo "🚀 백엔드 간단 배포 시작..."

# .env 파일 확인
if [ ! -f ".env" ]; then
    echo "❌ .env 파일 없음"
    exit 1
fi

# 기존 컨테이너 정리
echo "🔄 컨테이너 재시작..."
docker-compose down -v 2>/dev/null || true

# Docker 이미지 로드
if [ -f "../seurasaeng_be-image.tar.gz" ]; then
    echo "📦 이미지 로드..."
    docker load < ../seurasaeng_be-image.tar.gz
    rm -f ../seurasaeng_be-image.tar.gz
fi

# 컨테이너 시작
echo "🚀 서비스 시작..."
docker-compose up -d

# 간단 헬스체크
echo "⏳ 서비스 준비 대기..."
for i in {1..24}; do
    sleep 5
    if curl -f http://localhost:8080 >/dev/null 2>&1; then
        echo "✅ 백엔드 준비 완료!"
        echo "🌐 백엔드: http://localhost:8080"
        docker-compose ps
        exit 0
    fi
    echo "대기 중... ($i/24)"
done

echo "❌ 배포 실패"
docker-compose logs backend
exit 1