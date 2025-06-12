#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 로그 함수들
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 에러 발생시 스크립트 종료
set -e

log_info "🚀 프론트엔드 배포를 시작합니다..."

# 현재 디렉토리 확인
cd /home/ubuntu

# 환경변수 파일 생성
create_env_file() {
    log_info "환경변수 파일을 생성합니다..."
    
    cat > seurasaeng_fe/.env << EOF
# API 서버 설정
VITE_API_BASE_URL=https://seurasaeng.site/api
VITE_SOCKET_URL=wss://seurasaeng.site/ws

# 외부 API 키들
VITE_MOBILITY_API_KEY=2868494a3053c4014954615d4dcfafc1
VITE_KAKAOMAP_API_KEY=d079914b9511e06b410311be64216366
VITE_PERPLEXITY_API_KEY=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

# 외부 API URL들
VITE_MOBILITY_API_BASE_URL=https://apis-navi.kakaomobility.com/v1/directions
VITE_KAKAOMAP_API_BASE_URL=//dapi.kakao.com/v2/maps/sdk.js
EOF
    
    chmod 600 seurasaeng_fe/.env
    log_success "✅ 환경변수 파일 생성 완료"
}

# 환경변수 파일 생성
create_env_file

# Docker 이미지 로드
if [ -f "seurasaeng_fe-image.tar.gz" ]; then
    log_info "Docker 이미지를 로드합니다..."
    if docker load < seurasaeng_fe-image.tar.gz; then
        log_success "✅ Docker 이미지 로드 완료"
        rm -f seurasaeng_fe-image.tar.gz
    else
        log_error "❌ Docker 이미지 로드 실패"
        exit 1
    fi
else
    log_warning "⚠️ Docker 이미지 파일이 없습니다."
fi

# 기존 컨테이너 중지
log_info "기존 컨테이너를 중지합니다..."
cd seurasaeng_fe
docker-compose down --timeout 30 2>/dev/null || true

# 새 컨테이너 시작
log_info "새로운 컨테이너를 시작합니다..."
docker-compose up -d

cd /home/ubuntu

# 헬스체크
log_info "서비스 준비 대기 중..."
MAX_ATTEMPTS=24
ATTEMPT=1

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    if curl -f -s http://localhost/health >/dev/null 2>&1; then
        log_success "✅ 프론트엔드 서비스 준비 완료"
        break
    fi
    
    if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
        log_error "❌ 헬스체크 시간 초과"
        cd seurasaeng_fe
        docker-compose logs --tail=20 2>/dev/null || true
        cd /home/ubuntu
        exit 1
    fi
    
    log_info "서비스 준비 대기 중... ($ATTEMPT/$MAX_ATTEMPTS)"
    sleep 5
    ((ATTEMPT++))
done

# 백엔드 연결 테스트
log_info "백엔드 서버 연결을 테스트합니다..."
if curl -f -s http://10.0.2.166:8080/ >/dev/null 2>&1; then
    log_success "✅ 백엔드 서버 연결 정상"
    
    # API 프록시 테스트
    if curl -f -s http://localhost/api/ >/dev/null 2>&1; then
        log_success "✅ API 프록시 정상 작동"
    else
        log_warning "⚠️ API 프록시 연결에 문제가 있을 수 있습니다."
    fi
else
    log_warning "⚠️ 백엔드 서버에 연결할 수 없습니다."
fi

# 배포 완료
log_success "🎉 프론트엔드 배포가 완료되었습니다!"
echo
log_info "=== 🌐 서비스 접근 정보 ==="
log_info "🌐 웹사이트: http://13.125.200.221"
log_info "🔒 HTTPS 웹사이트: https://seurasaeng.site"
log_info "🔍 헬스체크: http://13.125.200.221/health"
log_info "🔗 API 프록시: http://13.125.200.221/api/"
echo

log_success "🚀 프론트엔드 배포가 완료되었습니다!"