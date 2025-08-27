# 프론트엔드 Dockerfile (seurasaeng_fe/Dockerfile)
FROM node:18-alpine AS builder

WORKDIR /app

# 캐싱을 위해 package 파일들을 먼저 복사
COPY package*.json ./
RUN npm ci --only=production

COPY . .

# 빌드 실행
RUN npm run build

FROM nginx:alpine

# 불필요한 파일 제거
RUN rm -rf /usr/share/nginx/html/*

# 빌드된 파일들 복사
COPY --from=builder /app/dist /usr/share/nginx/html

# Nginx 설정은 docker-compose에서 볼륨으로 마운트할 예정
# 기본 nginx.conf 백업
RUN mv /etc/nginx/nginx.conf /etc/nginx/nginx.conf.backup

# 포트 노출
EXPOSE 80

# Nginx 실행
CMD ["nginx", "-g", "daemon off;"]