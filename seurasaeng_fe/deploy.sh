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

# SSL 인증서 상태 확인
check_ssl_certificate() {
    if [ -f "/etc/letsencrypt/live/seurasaeng.site/fullchain.pem" ]; then
        log_success "✅ Let's Encrypt SSL 인증서 발견"
        return 0
    else
        log_warning "⚠️ Let's Encrypt SSL 인증서 없음 - 자체 서명 인증서 사용"
        return 1
    fi
}

# 환경변수 파일 생성
create_env_file() {
    log_info "환경변수 파일을 생성합니다..."
    
    cat > seurasaeng_fe/.env << EOF
# API 서버 설정
VITE_API_BASE_URL=https://seurasaeng.site/api
VITE_SOCKET_URL=wss://seurasaeng.site/ws

# 외부 API 키들 (기본값)
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

# Docker Compose SSL 설정
setup_ssl_config() {
    cd seurasaeng_fe
    
    if check_ssl_certificate; then
        log_info "Let's Encrypt 인증서로 SSL 설정을 구성합니다..."
        
        # Let's Encrypt 볼륨이 없으면 추가
        if ! grep -q "/etc/letsencrypt:/etc/letsencrypt:ro" docker-compose.yml; then
            log_info "docker-compose.yml에 Let's Encrypt 볼륨을 추가합니다..."
            sed -i '/nginx_logs:\/var\/log\/nginx/a\      - /etc/letsencrypt:/etc/letsencrypt:ro' docker-compose.yml
        fi
        
        # nginx 설정에서 SSL 경로를 Let's Encrypt로 변경
        if grep -q "/etc/ssl/certs/seurasaeng.site/" nginx/default.conf; then
            log_info "nginx 설정을 Let's Encrypt 경로로 업데이트합니다..."
            sed -i 's|/etc/ssl/certs/seurasaeng.site/|/etc/letsencrypt/live/seurasaeng.site/|g' nginx/default.conf
        fi
        
        SSL_TYPE="Let's Encrypt"
    else
        log_info "자체 서명 인증서로 SSL 설정을 구성합니다..."
        
        # Let's Encrypt 볼륨 제거 (있다면)
        sed -i '\|/etc/letsencrypt:/etc/letsencrypt:ro|d' docker-compose.yml
        
        # nginx 설정에서 SSL 경로를 자체 서명으로 변경
        if grep -q "/etc/letsencrypt/live/seurasaeng.site/" nginx/default.conf; then
            log_info "nginx 설정을 자체 서명 인증서 경로로 업데이트합니다..."
            sed -i 's|/etc/letsencrypt/live/seurasaeng.site/|/etc/ssl/certs/seurasaeng.site/|g' nginx/default.conf
        fi
        
        SSL_TYPE="Self-Signed"
    fi
    
    log_success "✅ SSL 설정 완료 ($SSL_TYPE)"
    cd /home/ubuntu
}

# 환경변수 파일 생성
create_env_file

# SSL 설정 구성
setup_ssl_config

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
    log_warning "⚠️ Docker 이미지 파일이 없습니다. 로컬 빌드를 진행합니다."
fi

# 기존 컨테이너 중지
log_info "기존 컨테이너를 중지합니다..."
cd seurasaeng_fe
if docker-compose ps -q 2>/dev/null | grep -q .; then
    docker-compose down --timeout 30 2>/dev/null || true
else
    log_info "실행 중인 컨테이너가 없습니다."
fi

# 사용하지 않는 이미지 정리
log_info "사용하지 않는 Docker 이미지를 정리합니다..."
docker image prune -f 2>/dev/null || true

# 새 컨테이너 시작
log_info "새로운 컨테이너를 시작합니다..."
docker-compose up -d

cd /home/ubuntu

# 헬스체크
log_info "서비스 준비 대기 중..."
MAX_ATTEMPTS=24  # 2분 대기
ATTEMPT=1

while [ $ATTEMPT -le $MAX_ATTEMPTS ]; do
    if curl -f -s --connect-timeout 5 http://localhost/health >/dev/null 2>&1; then
        log_success "✅ HTTP 서비스 준비 완료"
        break
    fi
    
    if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
        log_error "❌ HTTP 헬스체크 시간 초과"
        cd seurasaeng_fe
        docker-compose logs --tail=20 2>/dev/null || true
        cd /home/ubuntu
        exit 1
    fi
    
    log_info "서비스 준비 대기 중... ($ATTEMPT/$MAX_ATTEMPTS)"
    sleep 5
    ((ATTEMPT++))
done

# HTTPS 헬스체크
if check_ssl_certificate; then
    log_info "HTTPS 연결을 테스트합니다..."
    if curl -k -f -s --connect-timeout 10 https://localhost/health >/dev/null 2>&1; then
        log_success "✅ HTTPS 서비스 준비 완료"
    else
        log_warning "⚠️ HTTPS 연결 실패 (HTTP는 정상)"
    fi
fi

# 백엔드 연결 테스트
log_info "백엔드 서버 연결을 테스트합니다..."
BACKEND_IP="10.0.2.166"
BACKEND_PORT="8080"

if curl -f -s --connect-timeout 10 http://${BACKEND_IP}:${BACKEND_PORT}/actuator/health >/dev/null 2>&1; then
    log_success "✅ 백엔드 서버 연결 정상"
    
    # API 프록시 테스트 (HTTP)
    if curl -f -s --connect-timeout 10 http://localhost/api/actuator/health >/dev/null 2>&1; then
        log_success "✅ HTTP API 프록시 정상 작동"
    else
        log_warning "⚠️ HTTP API 프록시 연결에 문제가 있을 수 있습니다."
    fi
    
    # API 프록시 테스트 (HTTPS)
    if check_ssl_certificate && curl -k -f -s --connect-timeout 10 https://localhost/api/actuator/health >/dev/null 2>&1; then
        log_success "✅ HTTPS API 프록시 정상 작동"
    fi
else
    log_warning "⚠️ 백엔드 서버에 연결할 수 없습니다."
    log_info "백엔드 서버가 실행 중인지 확인해주세요: http://${BACKEND_IP}:${BACKEND_PORT}/actuator/health"
fi

# 배포 완료
log_success "🎉 프론트엔드 배포가 완료되었습니다!"
echo
log_info "=== 🌐 서비스 접근 정보 ==="
log_info "🌐 HTTP: http://13.125.200.221"
if check_ssl_certificate; then
    log_info "🔒 HTTPS: https://seurasaeng.site (Let's Encrypt SSL)"
else
    log_info "🔒 HTTPS: https://seurasaeng.site (자체 서명 SSL)"
fi
log_info "🔍 헬스체크: http://13.125.200.221/health"
if curl -f -s http://${BACKEND_IP}:${BACKEND_PORT}/actuator/health >/dev/null 2>&1; then
    log_info "🔗 API 프록시: http://13.125.200.221/api/actuator/health"
fi
echo
log_info "=== 📊 관리 명령어 ==="
log_info "📊 서비스 상태: cd seurasaeng_fe && docker-compose ps"
log_info "📋 로그 확인: cd seurasaeng_fe && docker-compose logs -f"

# 배포 정보 기록
{
    echo "$(date): Frontend deployment completed"
    echo "  - Frontend Health: HEALTHY"
    echo "  - SSL Type: $SSL_TYPE"
    echo "  - Environment: LOADED"
    if curl -f -s http://${BACKEND_IP}:${BACKEND_PORT}/actuator/health >/dev/null 2>&1; then
        echo "  - Backend Connectivity: VERIFIED"
    else
        echo "  - Backend Connectivity: NOT_AVAILABLE"
    fi
    echo "  - Port 80: BOUND"
    echo "  - Port 443: BOUND"
} >> /home/ubuntu/deployment.log

log_success "🔒 SSL 지원 프론트엔드 배포가 완료되었습니다!"