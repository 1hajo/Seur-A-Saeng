#!/bin/bash

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${CYAN}========================================"
echo -e "🔍 백엔드 연결 진단 스크립트"
echo -e "========================================${NC}"

BACKEND_IP="10.0.2.166"
BACKEND_PORT="8080"
BACKEND_URL="http://$BACKEND_IP:$BACKEND_PORT"

echo -e "\n${BLUE}📋 진단 대상:${NC}"
echo "  - 백엔드 서버: $BACKEND_URL"
echo "  - 프론트엔드 프록시: http://localhost/api/"

echo -e "\n${BLUE}🔍 1. 기본 네트워크 연결성 테스트${NC}"
echo "----------------------------------------"

# 1-1. Ping 테스트
echo -n "📡 Ping 테스트... "
if ping -c 3 -W 3 $BACKEND_IP >/dev/null 2>&1; then
    echo -e "${GREEN}✅ 성공${NC}"
else
    echo -e "${RED}❌ 실패 - 네트워크 미연결${NC}"
fi

# 1-2. DNS 해결 테스트 (도메인 사용시)
if [ "$BACKEND_IP" != "10.0.2.166" ]; then
    echo -n "🌐 DNS 해결 테스트... "
    if nslookup $BACKEND_IP >/dev/null 2>&1; then
        echo -e "${GREEN}✅ 성공${NC}"
    else
        echo -e "${RED}❌ 실패 - DNS 해결 불가${NC}"
    fi
fi

# 1-3. 포트 접근성 테스트
echo -n "🔌 포트 접근성 테스트... "
if command -v nc >/dev/null 2>&1; then
    if timeout 10 nc -zv $BACKEND_IP $BACKEND_PORT >/dev/null 2>&1; then
        echo -e "${GREEN}✅ 포트 $BACKEND_PORT 접근 가능${NC}"
    else
        echo -e "${RED}❌ 포트 $BACKEND_PORT 접근 불가${NC}"
        echo -e "  ${YELLOW}→ 백엔드 서버가 실행되지 않았거나 방화벽에 의해 차단됨${NC}"
    fi
else
    echo -e "${YELLOW}⚠️ nc 명령어 없음 - 건너뜀${NC}"
fi

echo -e "\n${BLUE}🔍 2. HTTP 연결 테스트${NC}"
echo "----------------------------------------"

# 2-1. HTTP 응답 시간 테스트
echo -n "⏱️ HTTP 응답 시간 테스트... "
HTTP_TIME=$(curl -o /dev/null -s -w "%{time_total}" --connect-timeout 10 --max-time 30 $BACKEND_URL 2>/dev/null)
HTTP_CODE=$(curl -o /dev/null -s -w "%{http_code}" --connect-timeout 10 --max-time 30 $BACKEND_URL 2>/dev/null)

if [ "$HTTP_CODE" != "000" ]; then
    echo -e "${GREEN}✅ 응답 시간: ${HTTP_TIME}초, 상태코드: $HTTP_CODE${NC}"
else
    echo -e "${RED}❌ HTTP 연결 실패${NC}"
fi

# 2-2. 상세 HTTP 응답 분석
echo -e "\n📊 상세 HTTP 응답 분석:"
CURL_OUTPUT=$(curl -v --connect-timeout 10 --max-time 30 $BACKEND_URL 2>&1)
CURL_STATUS=$?

if [ $CURL_STATUS -eq 0 ]; then
    echo -e "${GREEN}✅ HTTP 연결 성공${NC}"
    echo "응답 헤더:"
    echo "$CURL_OUTPUT" | grep -E "(< HTTP|< [A-Za-z-]+:)" | head -10
else
    echo -e "${RED}❌ HTTP 연결 실패${NC}"
    echo "에러 정보:"
    echo "$CURL_OUTPUT" | tail -5
    
    # cURL 종료 코드별 원인 분석
    case $CURL_STATUS in
        6)  echo -e "  ${YELLOW}→ DNS 해결 실패${NC}" ;;
        7)  echo -e "  ${YELLOW}→ 서버에 연결할 수 없음${NC}" ;;
        28) echo -e "  ${YELLOW}→ 연결 시간 초과${NC}" ;;
        52) echo -e "  ${YELLOW}→ 서버에서 응답하지 않음${NC}" ;;
        56) echo -e "  ${YELLOW}→ 네트워크 데이터 수신 실패${NC}" ;;
        *)  echo -e "  ${YELLOW}→ cURL 종료 코드: $CURL_STATUS${NC}" ;;
    esac
fi

echo -e "\n${BLUE}🔍 3. 시스템 상태 확인${NC}"
echo "----------------------------------------"

# 3-1. 백엔드 프로세스 확인
echo "🔍 백엔드 프로세스 확인:"
JAVA_PROCESSES=$(ps aux | grep -E "(java|spring)" | grep -v grep)
if [ -n "$JAVA_PROCESSES" ]; then
    echo -e "${GREEN}✅ Java/Spring 프로세스 발견:${NC}"
    echo "$JAVA_PROCESSES" | head -3
else
    echo -e "${RED}❌ Java/Spring 프로세스 미발견${NC}"
fi

# 3-2. 포트 사용 현황
echo -e "\n🔍 포트 $BACKEND_PORT 사용 현황:"
if command -v netstat >/dev/null 2>&1; then
    PORT_INFO=$(netstat -tulpn | grep :$BACKEND_PORT)
    if [ -n "$PORT_INFO" ]; then
        echo -e "${GREEN}✅ 포트 사용 중:${NC}"
        echo "$PORT_INFO"
    else
        echo -e "${RED}❌ 포트 $BACKEND_PORT 사용 중인 프로세스 없음${NC}"
    fi
else
    echo -e "${YELLOW}⚠️ netstat 명령어 없음 - ss 시도${NC}"
    if command -v ss >/dev/null 2>&1; then
        PORT_INFO=$(ss -tulpn | grep :$BACKEND_PORT)
        if [ -n "$PORT_INFO" ]; then
            echo -e "${GREEN}✅ 포트 사용 중:${NC}"
            echo "$PORT_INFO"
        else
            echo -e "${RED}❌ 포트 $BACKEND_PORT 사용 중인 프로세스 없음${NC}"
        fi
    fi
fi

# 3-3. 시스템 리소스
echo -e "\n🔍 시스템 리소스:"
echo "메모리 사용량:"
free -h 2>/dev/null || echo "메모리 정보 확인 불가"

echo -e "\n디스크 사용량:"
df -h / 2>/dev/null || echo "디스크 정보 확인 불가"

echo -e "\nCPU 부하:"
uptime 2>/dev/null || echo "시스템 부하 정보 확인 불가"

echo -e "\n${BLUE}🔍 4. 방화벽 및 네트워크 설정${NC}"
echo "----------------------------------------"

# 4-1. 방화벽 상태 확인
echo "🔥 방화벽 상태:"
if command -v ufw >/dev/null 2>&1; then
    UFW_STATUS=$(ufw status 2>/dev/null)
    echo "$UFW_STATUS"
elif command -v iptables >/dev/null 2>&1; then
    echo "iptables 규칙 (일부):"
    iptables -L INPUT | head -10 2>/dev/null || echo "iptables 정보 확인 불가"
else
    echo -e "${YELLOW}⚠️ 방화벽 상태 확인 불가${NC}"
fi

# 4-2. 네트워크 인터페이스
echo -e "\n🌐 네트워크 인터페이스:"
if command -v ip >/dev/null 2>&1; then
    ip addr show | grep -E "(inet |inet6)" | head -5
else
    ifconfig 2>/dev/null | grep -E "(inet|inet6)" | head -5 || echo "네트워크 정보 확인 불가"
fi

echo -e "\n${BLUE}🔍 5. 프록시 연결 테스트${NC}"
echo "----------------------------------------"

# 5-1. Nginx 프록시 테스트
echo "🔄 API 프록시 테스트:"
PROXY_RESPONSE=$(curl -v --connect-timeout 10 --max-time 30 http://localhost/api/ 2>&1)
PROXY_STATUS=$?

if [ $PROXY_STATUS -eq 0 ]; then
    echo -e "${GREEN}✅ API 프록시 정상${NC}"
    echo "프록시 응답 헤더:"
    echo "$PROXY_RESPONSE" | grep -E "(< HTTP|< [A-Za-z-]+:)" | head -5
else
    echo -e "${RED}❌ API 프록시 실패${NC}"
    echo "프록시 에러:"
    echo "$PROXY_RESPONSE" | tail -5
fi

# 5-2. Nginx 상태 확인
echo -e "\n🌐 Nginx 상태:"
if command -v nginx >/dev/null 2>&1; then
    nginx -t 2>&1 && echo -e "${GREEN}✅ Nginx 설정 유효${NC}" || echo -e "${RED}❌ Nginx 설정 오류${NC}"
else
    NGINX_CONTAINER=$(docker ps | grep nginx | head -1 | awk '{print $1}')
    if [ -n "$NGINX_CONTAINER" ]; then
        echo "Docker Nginx 컨테이너 발견: $NGINX_CONTAINER"
        docker exec $NGINX_CONTAINER nginx -t 2>&1 && echo -e "${GREEN}✅ Nginx 설정 유효${NC}" || echo -e "${RED}❌ Nginx 설정 오류${NC}"
    else
        echo -e "${YELLOW}⚠️ Nginx 상태 확인 불가${NC}"
    fi
fi

echo -e "\n${BLUE}🔍 6. 로그 분석${NC}"
echo "----------------------------------------"

# 6-1. Docker 컨테이너 로그
echo "📋 Docker 컨테이너 로그 (최근 10줄):"
if command -v docker >/dev/null 2>&1; then
    FRONTEND_CONTAINER=$(docker ps | grep -E "(frontend|nginx)" | head -1 | awk '{print $1}')
    if [ -n "$FRONTEND_CONTAINER" ]; then
        echo "프론트엔드 컨테이너 로그:"
        docker logs $FRONTEND_CONTAINER --tail=10 2>&1 | tail -10
    else
        echo -e "${YELLOW}⚠️ 프론트엔드 컨테이너 없음${NC}"
    fi
else
    echo -e "${YELLOW}⚠️ Docker 명령어 없음${NC}"
fi

# 6-2. 시스템 로그
echo -e "\n📋 시스템 로그 (최근 5줄):"
if [ -f "/var/log/syslog" ]; then
    tail -5 /var/log/syslog 2>/dev/null || echo "시스템 로그 확인 불가"
elif [ -f "/var/log/messages" ]; then
    tail -5 /var/log/messages 2>/dev/null || echo "시스템 로그 확인 불가"
else
    echo -e "${YELLOW}⚠️ 시스템 로그 파일 없음${NC}"
fi

echo -e "\n${CYAN}========================================"
echo -e "📊 진단 완료"
echo -e "========================================${NC}"

if [ $HTTP_CODE != "000" ] && [ $HTTP_CODE != "" ]; then
    echo -e "${GREEN}✅ 백엔드 서버 연결 가능${NC}"
    echo -e "   HTTP 상태코드: $HTTP_CODE"
    echo -e "   응답 시간: ${HTTP_TIME}초"
else
    echo -e "${RED}❌ 백엔드 서버 연결 불가${NC}"
    echo -e "\n${YELLOW}🔧 권장 해결 방법:${NC}"
    echo "1. 백엔드 서버가 실행 중인지 확인"
    echo "2. 포트 $BACKEND_PORT이 열려있는지 확인"
    echo "3. 방화벽 설정 확인"
    echo "4. 네트워크 연결 상태 확인"
    echo "5. 백엔드 서버 로그 확인"
fi

if [ $PROXY_STATUS -eq 0 ]; then
    echo -e "${GREEN}✅ API 프록시 정상 작동${NC}"
else
    echo -e "${RED}❌ API 프록시 연결 문제${NC}"
    echo -e "\n${YELLOW}🔧 프록시 문제 해결 방법:${NC}"
    echo "1. Nginx 설정 확인 (/etc/nginx/conf.d/default.conf)"
    echo "2. 프론트엔드 컨테이너 재시작"
    echo "3. Docker 네트워크 상태 확인"
fi

echo -e "\n${BLUE}💡 추가 진단이 필요한 경우:${NC}"
echo "1. journalctl -u nginx -n 20 (시스템 Nginx 로그)"
echo "2. docker-compose logs frontend (Docker 로그)"
echo "3. curl -v http://$BACKEND_IP:$BACKEND_PORT/actuator/health (헬스체크)"