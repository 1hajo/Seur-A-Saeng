FROM node:18-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:alpine

# 빌드된 파일 복사
COPY --from=builder /app/dist /usr/share/nginx/html

# nginx.conf 복사
COPY --from=builder /app/nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]