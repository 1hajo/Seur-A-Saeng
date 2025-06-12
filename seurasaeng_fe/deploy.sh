#!/bin/bash
set -e

echo "🚀 프론트엔드 간단 배포 시작..."

# 환경변수 파일 생성
cat > .env << EOF
VITE_API_BASE_URL=http://13.125.200.221/api
VITE_SOCKET_URL=ws://13.125.200.221/ws
VITE_MOBILITY_API_KEY=2868494a3053c4014954615d4dcfafc1
VITE_KAKAOMAP_API_KEY=d079914b9511e06b410311be64216366
VITE_PERPLEXITY_API_KEY=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
VITE_MOBILITY_API_BASE_URL=https://apis-navi.kakaomobility.com/v1/directions
VITE_KAKAOMAP_API_BASE_URL=//dapi.kakao.com/v2/maps/sdk.js
EOF

# 이미지 로드
if [ -f "../seurasaeng_fe-image.tar.gz" ]; then
    echo "📦 이미지 로드..."
    docker load < ../seurasaeng_fe-image.tar.gz
    rm -f ../seurasaeng_fe-image.tar.gz
fi

# 컨테이너 재시작
echo "🔄 컨테이너 재시작..."
docker-compose down 2>/dev/null || true
docker-compose up -d

# 헬스체크
echo "⏳ 서비스 준비 대기..."
for i in {1..20}; do
    sleep 3
    if curl -f http://localhost/health >/dev/null 2>&1; then
        echo "✅ 프론트엔드 준비 완료!"
        
        # 백엔드 연결 확인
        if curl -f http://10.0.2.166:8080 >/dev/null 2>&1; then
            echo "✅ 백엔드 연결 성공"
            echo "✅ API: http://localhost/api/"
        else
            echo "⚠️ 백엔드 연결 실패"
        fi
        
        echo "🎉 배포 완료!"
        echo "🌐 접속: http://13.125.200.221"
        exit 0
    fi
done

echo "❌ 배포 실패"
docker-compose logs --tail=10
exit 1