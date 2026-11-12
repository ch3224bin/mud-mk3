# 인증

→ [목차](index.md)

- 인증은 Google 로그인 기능을 사용한다. → [플랫폼: 기술 스택](platform.md#기술-스택)
- 1차 이터레이션 범위에 인증이 들어간다. → [이터레이션 계획](index.md#이터레이션-계획)

## 계정

- play-client와 admin-console 둘 다 Google 로그인을 쓰고, 같은 계정 체계를 쓴다. → [플랫폼: 저장소 구성](platform.md#저장소-구성)
- Google 계정 하나에 캐릭터는 1개다. → [룰북: 캐릭터 생성](../game-rule/character.md#캐릭터-생성)

## 관리자

- 관리자는 서버 설정(환경변수 등)에 관리자 Google 이메일 목록을 적어 정한다. → [관리 툴](admin-tool.md)

## 1차 로그인 흐름

world-server가 서버 측에서 Spring Security OAuth2 Login으로 Google 로그인을 처리하고, 이후 HTTP 세션 쿠키로 인증한다. 두 클라이언트는 로그인 버튼으로 브라우저를 world-server에 보낼 뿐이다. (제안)

- 등록: 클라이언트 id·시크릿은 환경변수 `GOOGLE_CLIENT_ID`·`GOOGLE_CLIENT_SECRET` 으로만 받는다. 범위는 `openid,profile,email` 이라 OIDC로 동작하고, ID 토큰 서명·`iss`·`aud`·`exp` 는 Spring이 검증한다. 클라이언트 시크릿은 서버에만 있다. → [플랫폼: 환경변수](platform.md#배포) (제안)
- 리디렉션 URI는 Spring 기본값 하나다: `http://localhost:8080/login/oauth2/code/google`. 기존에 쓰던 Google OAuth 클라이언트에 이미 등록된 값이라 새로 추가할 것이 없다. (제안)
- 흐름 (제안):
  1. 클라이언트의 로그인 버튼이 브라우저를 `http://localhost:8080/api/auth/login?client=play`(admin-console은 `client=admin`)로 보낸다. 전체 페이지 이동이다.
  2. world-server가 `client` 값을 세션에 적고 `/oauth2/authorization/google` 로 리다이렉트한다. Google 로그인·동의 뒤 `/login/oauth2/code/google` 로 돌아온다.
  3. 성공하면 Google `sub`·`email` 로 계정을 찾거나 만든다. `email_verified` 가 true가 아니면 실패다. 그리고 세션의 `client` 값에 맞는 클라이언트 주소로 리다이렉트한다: `play` → `MUD_PLAY_CLIENT_URL`, `admin` → `MUD_ADMIN_CONSOLE_URL`. 값이 없거나 다르면 `play` 이고, 임의 URL로는 보내지 않는다(열린 리다이렉트 방지). 실패하면 같은 주소에 `?loginError=1` 을 붙인다.
  4. 클라이언트는 `GET /api/auth/me` 를 `credentials: "include"` 로 불러 로그인 여부를 안다. 401이면 로그인 버튼을 보인다.
  5. 로그아웃은 `POST /api/auth/logout` 이고 세션을 무효화한 뒤 204를 준다. 세션이 이미 끝났어도 204다(클라이언트도 401을 로그아웃 성공으로 본다). (제안)
- 인증 REST API (제안):

| 메서드 · 경로 | 응답 |
|---|---|
| `GET /api/auth/login?client=play\|admin` | 302 → `/oauth2/authorization/google` |
| `GET /api/auth/me` | 200 `{id, email, admin, characterName}`(`characterName` 은 없으면 null). 로그인 안 됨 401 |
| `POST /api/auth/logout` | 204 |
| `POST /api/auth/dev` | 개발용 로그인(아래). 조건이 아니면 404 |

- 세션 쿠키는 `JSESSIONID` 이고 `HttpOnly`, `SameSite=Lax` 다. `localhost:3000`·`3001` 과 `localhost:8080` 은 오리진은 다르지만 같은 사이트(쿠키는 포트를 가리지 않음)라 Lax 쿠키가 간다. 그래서 브라우저 주소는 항상 `localhost` 로 쓴다. (제안)
- 세션 유휴 시간은 Spring 기본 30분이다. 열린 WebSocket은 HTTP 세션을 늘리지 않는다. (제안)
- 세션은 world-server 메모리에 둔다. 서버를 다시 시작하면 다시 로그인해야 한다. 1차에는 이를 허용하고, 세션 저장소를 DB로 옮기는 것(Spring Session JDBC)은 다음 이터레이션이다. (제안)
- 인증 안 된 `/api/**` 요청은 Google로 리다이렉트하지 않고 401을 준다. 관리자가 아니면 `/api/admin/**` 는 403이다. 401·403 본문은 `ProblemDetail` JSON이고 `detail` 은 각각 `로그인이 필요합니다.`·`권한이 없습니다.` 다. (제안)
- 세션의 인증 정보(principal)는 계정 id·이메일·관리자 여부 하나로 통일한다(Google 로그인·개발용 로그인 공통). (제안)

### 관리자 반영 시점

- 관리자 여부는 로그인 성공 시 서버 설정 `MUD_ADMIN_EMAILS`(쉼표로 나눈 목록, 대소문자 무시)로 정해 세션의 인증 정보에 권한 `ROLE_ADMIN` 으로 넣는다. 모든 로그인 계정은 `ROLE_PLAYER` 다. (제안)
- 목록을 바꾸면 다음 로그인부터 반영된다. (제안)

### 오리진·CORS·CSRF

- 오리진: world-server `8080`, play-client `3000`, admin-console `3001`. 브라우저가 world-server에 직접 붙는다. (제안)
- CORS: `MUD_CORS_ALLOWED_ORIGINS`(기본 `http://localhost:3000,http://localhost:3001`)에 있는 오리진만 허용하고 쿠키를 함께 보내게 한다(`allowCredentials=true`). 허용 헤더는 `Content-Type`·`X-MUD-Client` 다. WebSocket 핸드셰이크의 `Origin` 도 같은 목록으로 검사한다. (제안)
- CSRF: 쿠키 인증이라 막아야 한다. Spring 기본 CSRF 토큰은 끄고, `/api/**` 의 `POST`·`PUT`·`DELETE` 는 헤더 `X-MUD-Client: 1` 이 없으면 403으로 거절한다. 사용자 정의 헤더는 CORS 사전 요청을 일으키므로 허용 목록 밖 오리진은 보낼 수 없다. (제안)

### 개발용 로그인

- `POST /api/auth/dev {email}`(헤더 `X-MUD-Client: 1`)이 Google 없이 그 이메일 계정으로 세션을 만든다. 관리자 판정은 Google 로그인과 같다. (제안)
- `local` 프로필이고 `MUD_DEV_LOGIN_ENABLED=true` 일 때만 켜지고, 그 밖에는 404다. 기본은 꺼짐이다. 이 계정의 `googleSubject` 는 `dev:<email>` 이다. 클라이언트는 빌드 변수 `NEXT_PUBLIC_DEV_LOGIN=true` 일 때만 이메일 입력 폼을 보인다. (제안)
- Google 자격 증명 없이 하는 로컬 수동 확인·스모크 테스트용이다. (제안)

### WebSocket 인증

- 게임 WebSocket은 핸드셰이크에 실려 오는 세션 쿠키로 인증한다. 별도의 `auth` 메시지는 없다. (제안)
- 핸드셰이크 인터셉터가 `Origin` 을 허용 목록으로 검사하고(아니면 403), 세션의 인증 정보에서 계정 id를 꺼내 WebSocket 세션 속성으로 넘긴다. (제안)
- 연결은 받아 준 뒤, 계정 id가 없으면 `error` `AUTH_REQUIRED` 를 보내고 4401로 닫는다. 핸드셰이크에서 거절하면 클라이언트가 까닭을 알 수 없어서다. (제안) → [플랫폼: 1차 WebSocket 메시지](platform.md#1차-websocket-메시지)

## 열린 질문

- WebSocket 연결 시 인증 정보를 어떻게 전달하는가? → (제안) 세션 쿠키로 전달한다. → [WebSocket 인증](#websocket-인증)
