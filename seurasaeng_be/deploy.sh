#!/bin/bash

set -e

echo "🚀 Seurasaeng Backend 배포 시작..."

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warn() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

# .env 파일 확인
if [ ! -f ".env" ]; then
    log_error ".env 파일이 없습니다."
    exit 1
fi

log_info ".env 파일 확인 완료"

# 필요한 디렉토리 생성
mkdir -p init-scripts

# PostgreSQL 초기화 스크립트 생성
cat > init-scripts/01-init.sql << 'EOF'
CREATE SCHEMA IF NOT EXISTS seurasaeng_test;
CREATE SCHEMA IF NOT EXISTS seurasaeng_prod;

GRANT ALL PRIVILEGES ON SCHEMA seurasaeng_test TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA seurasaeng_prod TO postgres;

ALTER DEFAULT PRIVILEGES IN SCHEMA seurasaeng_test GRANT ALL PRIVILEGES ON TABLES TO postgres;
ALTER DEFAULT PRIVILEGES IN SCHEMA seurasaeng_prod GRANT ALL PRIVILEGES ON TABLES TO postgres;

ALTER USER postgres SET search_path TO seurasaeng_test,seurasaeng_prod,public;

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
EOF

log_info "초기화 스크립트 생성 완료"

# 기존 컨테이너 정리
log_info "기존 컨테이너 정리 중..."
docker-compose down -v --remove-orphans 2>/dev/null || true

# Docker 이미지 로드
if [ -f "../seurasaeng_be-image.tar.gz" ]; then
    log_info "Docker 이미지 로드 중..."
    docker load < ../seurasaeng_be-image.tar.gz
    rm -f ../seurasaeng_be-image.tar.gz
fi

# 컨테이너 시작
log_info "컨테이너 시작 중..."
docker-compose up -d

# 간단한 대기
log_info "서비스 초기화 대기..."
sleep 30

# 최종 상태 표시
echo ""
echo "======================================"
echo "🎉 배포 완료!"
echo "======================================"
echo ""
echo "🌐 서비스 접속 정보:"
echo "  - 백엔드 API: http://localhost:8080"
echo "  - Health Check: http://localhost:8080/actuator/health"
echo ""
echo "📊 컨테이너 상태:"
docker-compose ps

log_info "배포가 완료되었습니다! 🚀"