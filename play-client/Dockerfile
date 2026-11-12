# 빌드 단계: 정적 export(out/)를 만든다. NEXT_PUBLIC_* 값은 빌드 때 박힌다
FROM node:26.13.1-alpine AS build
ARG NEXT_PUBLIC_WORLD_SERVER_URL
ARG NEXT_PUBLIC_DEV_LOGIN
ENV NEXT_PUBLIC_WORLD_SERVER_URL=$NEXT_PUBLIC_WORLD_SERVER_URL
ENV NEXT_PUBLIC_DEV_LOGIN=$NEXT_PUBLIC_DEV_LOGIN
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# 실행 단계: nginx로 정적 파일만 서빙한다
FROM nginx:1.30.6-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/out /usr/share/nginx/html
EXPOSE 80
