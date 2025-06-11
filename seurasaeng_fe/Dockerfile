# 멀티 스테이지 빌드 - Builder Stage
FROM node:18-alpine as builder

WORKDIR /app

# package.json과 package-lock.json 복사
COPY package*.json ./

# 의존성 설치
RUN npm ci

# 소스 코드 복사
COPY . .

# Vite 환경변수 설정
ARG VITE_API_BASE_URL=https://seurasaeng.site/api
ARG VITE_SOCKET_URL=wss://seurasaeng.site/ws
ARG VITE_MOBILITY_API_KEY
ARG VITE_KAKAOMAP_API_KEY
ARG VITE_PERPLEXITY_API_KEY
ARG VITE_MOBILITY_API_BASE_URL=https://apis-navi.kakaomobility.com/v1/directions
ARG VITE_KAKAOMAP_API_BASE_URL=//dapi.kakao.com/v2/maps/sdk.js

ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_SOCKET_URL=$VITE_SOCKET_URL
ENV VITE_MOBILITY_API_KEY=$VITE_MOBILITY_API_KEY
ENV VITE_KAKAOMAP_API_KEY=$VITE_KAKAOMAP_API_KEY
ENV VITE_PERPLEXITY_API_KEY=$VITE_PERPLEXITY_API_KEY
ENV VITE_MOBILITY_API_BASE_URL=$VITE_MOBILITY_API_BASE_URL
ENV VITE_KAKAOMAP_API_BASE_URL=$VITE_KAKAOMAP_API_BASE_URL

# React 앱 빌드
RUN npm run build

# Nginx 프로덕션 환경
FROM nginx:1.25-alpine

# 필수 패키지 설치
RUN apk add --no-cache curl tzdata openssl && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata

# SSL 디렉토리 생성
RUN mkdir -p /etc/ssl/certs/seurasaeng.site \
             /var/log/nginx

# Nginx 설정 복사
COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

# 빌드된 React 앱 복사
COPY --from=builder /app/dist /usr/share/nginx/html

# 권한 설정
RUN chown -R nginx:nginx /usr/share/nginx/html /var/log/nginx && \
    chmod -R 755 /usr/share/nginx/html

# 포트 노출
EXPOSE 80 443

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost/health || exit 1

# 자체 서명 인증서 생성 스크립트
RUN echo '#!/bin/sh' > /docker-entrypoint.d/10-create-ssl.sh && \
    echo 'if [ ! -f /etc/ssl/certs/seurasaeng.site/fullchain.pem ]; then' >> /docker-entrypoint.d/10-create-ssl.sh && \
    echo '  echo "Creating self-signed SSL certificate..."' >> /docker-entrypoint.d/10-create-ssl.sh && \
    echo '  openssl req -x509 -nodes -days 365 -newkey rsa:2048 \' >> /docker-entrypoint.d/10-create-ssl.sh && \
    echo '    -keyout /etc/ssl/certs/seurasaeng.site/privkey.pem \' >> /docker-entrypoint.d/10-create-ssl.sh && \
    echo '    -out /etc/ssl/certs/seurasaeng.site/fullchain.pem \' >> /docker-entrypoint.d/10-create-ssl.sh && \
    echo '    -subj "/C=KR/ST=Seoul/L=Seoul/O=Seurasaeng/CN=seurasaeng.site"' >> /docker-entrypoint.d/10-create-ssl.sh && \
    echo 'fi' >> /docker-entrypoint.d/10-create-ssl.sh && \
    chmod +x /docker-entrypoint.d/10-create-ssl.sh

# Nginx 시작
CMD ["nginx", "-g", "daemon off;"]