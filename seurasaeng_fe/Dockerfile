# Builder Stage
FROM node:18-alpine as builder

WORKDIR /app
COPY package*.json ./
RUN npm ci

COPY . .

# 환경변수 설정
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

RUN npm run build

# Production Stage
FROM nginx:1.25-alpine

# 필수 패키지만 설치
RUN apk add --no-cache curl openssl

# SSL 디렉토리 생성
RUN mkdir -p /etc/ssl/certs/seurasaeng.site

# Nginx 설정 및 React 앱 복사
COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

# 자체 서명 인증서 생성
RUN openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout /etc/ssl/certs/seurasaeng.site/privkey.pem \
    -out /etc/ssl/certs/seurasaeng.site/fullchain.pem \
    -subj "/C=KR/ST=Seoul/L=Seoul/O=Seurasaeng/CN=seurasaeng.site"

EXPOSE 80 443

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD curl -f http://13.125.200.221/health || exit 1

CMD ["nginx", "-g", "daemon off;"]