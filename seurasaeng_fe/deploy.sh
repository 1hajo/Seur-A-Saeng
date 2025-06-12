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
BACKEND_IP="10.0.2.166"
BACKEND_PORT="8080"

# 1. 네트워크 연결성 확인
log_info "📊 네트워크 연결성 검사..."
echo "1. 백엔드 서버 ping 테스트:"
if ping -c 3 $BACKEND_IP >/dev/null 2>&1; then
    log_success "✅ 백엔드 서버 ping 성공"
else
    log_error "❌ 백엔드 서버 ping 실패 - 네트워크 문제 가능성"
fi

# 2. 포트 접근성 테스트
echo "2. 백엔드 포트 접근성 테스트:"
if command -v nc >/dev/null 2>&1; then
    if nc -zv $BACKEND_IP $BACKEND_PORT 2>&1; then
        log_success "✅ 백엔드 포트 $BACKEND_PORT 접근 가능"
    else
        log_error "❌ 백엔드 포트 $BACKEND_PORT 접근 불가 - 서버 미실행 또는 방화벽 차단"
    fi
else
    log_warning "⚠️ nc 명령어가 없어 포트 테스트를 건너뜁니다."
fi

# 3. HTTP 응답 테스트
echo "3. 백엔드 HTTP 응답 테스트:"
BACKEND_RESPONSE=$(curl -v --connect-timeout 10 --max-time 30 http://$BACKEND_IP:$BACKEND_PORT/ 2>&1)
BACKEND_STATUS=$?

if [ $BACKEND_STATUS -eq 0 ]; then
    log_success "✅ 백엔드 서버 연결 정상"
    echo "응답 정보: $BACKEND_RESPONSE"
    
    # API 프록시 테스트
    echo "4. API 프록시 테스트:"
    PROXY_RESPONSE=$(curl -v --connect-timeout 10 --max-time 30 http://localhost/api/ 2>&1)
    PROXY_STATUS=$?
    
    if [ $PROXY_STATUS -eq 0 ]; then
        log_success "✅ API 프록시 정상 작동"
        echo "프록시 응답: $PROXY_RESPONSE"
    else
        log_error "❌ API 프록시 연결 실패"
        echo "프록시 에러: $PROXY_RESPONSE"
        log_warning "Nginx 설정을 확인해주세요."
    fi
else
    log_error "❌ 백엔드 서버 연결 실패"
    echo "에러 상세: $BACKEND_RESPONSE"
    log_warning "가능한 원인:"
    echo "  - 백엔드 서버가 실행되지 않음"
    echo "  - 포트 $BACKEND_PORT이 차단됨"
    echo "  - 백엔드 서버에서 오류 발생"
    echo "  - 네트워크 설정 문제"
    
    # 백엔드 서버 상태 추가 확인
    echo "5. 백엔드 서버 프로세스 확인:"
    if ps aux | grep -E "(java|spring|$BACKEND_PORT)" | grep -v grep; then
        log_info "백엔드 관련 프로세스 발견"
    else
        log_warning "백엔드 프로세스 미발견"
    fi
    
    echo "6. 포트 사용 현황:"
    if command -v netstat >/dev/null 2>&1; then
        NETSTAT_RESULT=$(netstat -tulpn | grep :$BACKEND_PORT)
        if [ -n "$NETSTAT_RESULT" ]; then
            echo "포트 $BACKEND_PORT 사용 현황: $NETSTAT_RESULT"
        else
            log_warning "포트 $BACKEND_PORT을 사용 중인 프로세스가 없습니다."
        fi
    else
        log_warning "netstat 명령어가 없어 포트 확인을 건너뜁니다."
    fi
    
    # 시스템 리소스 확인
    echo "7. 시스템 리소스 확인:"
    log_info "메모리 사용량:"
    free -h || echo "메모리 정보 확인 불가"
    
    log_info "디스크 사용량:"
    df -h / || echo "디스크 정보 확인 불가"
    
    # 네트워크 인터페이스 확인
    echo "8. 네트워크 인터페이스 확인:"
    if command -v ip >/dev/null 2>&1; then
        ip addr show | grep -E "(inet|inet6)" || echo "네트워크 인터페이스 정보 확인 불가"
    else
        ifconfig 2>/dev/null | grep -E "(inet|inet6)" || echo "네트워크 인터페이스 정보 확인 불가"
    fi
fi

# 배포 완료
log_success "🎉 프론트엔드 배포가 완료되었습니다!"
echo
log_info "=== 🌐 서비스 접근 정보 ==="
log_info "🌐 웹사이트: http://13.125.200.221"
log_info "🔒 HTTPS 웹사이트: https://seurasaeng.site"
log_info "🔍 헬스체크: http://13.125.200.221/health"
if [ $BACKEND_STATUS -eq 0 ]; then
    log_info "🔗 API 프록시: http://13.125.200.221/api/"
else
    log_warning "⚠️ 백엔드 미연결로 API 프록시 사용 불가"
fi
echo
log_info "=== 📊 관리 명령어 ==="
log_info "📊 서비스 상태: cd seurasaeng_fe && docker-compose ps"
log_info "📋 로그 확인: cd seurasaeng_fe && docker-compose logs -f"

# 배포 정보 기록
{
    echo "$(date): Frontend deployment completed"
    echo "  - Frontend Health: HEALTHY"
    echo "  - Environment: LOADED"
    if [ $BACKEND_STATUS -eq 0 ]; then
        echo "  - Backend Connectivity: VERIFIED"
    else
        echo "  - Backend Connectivity: FAILED"
        echo "  - Backend Error: $BACKEND_RESPONSE"
    fi
    echo "  - Port 80: BOUND"
    echo "  - Port 443: BOUND"
} >> /home/ubuntu/deployment.log

log_success "🚀 프론트엔드 배포가 완료되었습니다!"