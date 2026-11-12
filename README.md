# mud-mk3

범인수선전 풍 수선(修仙) 텍스트 MUD. 지금은 1차 이터레이션(관리자가 맵을 만들고, 플레이어가 캐릭터를 만들어 돌아다니며 말하는 데까지)이다.

| 모듈 | 하는 일 | 주소 |
|---|---|---|
| `world-server/` | 게임 서버. Java 25 · Spring Boot 4. Google 로그인, 관리 REST API, 게임 WebSocket(`/ws/game`), PostgreSQL | http://localhost:8080 |
| `play-client/` | 플레이 화면(터미널형). Next.js 정적 export | http://localhost:3000 |
| `admin-console/` | 관리 툴(지역·방·출구·시작 방 편집). Next.js 정적 export | http://localhost:3001 |

스펙: [시스템](docs/spec/system/index.md) · [게임 규칙(룰북)](docs/spec/game-rule/index.md)

## 필요한 것

- Docker (Docker Desktop 또는 OrbStack, `docker compose` v2)
- 개별 개발 실행을 할 때만:
  - Java 25 (Gradle은 wrapper `./gradlew` 를 쓴다)
  - Node.js 26.13.1 — 프론트 모듈마다 `.nvmrc` 가 있다. nvm이면 모듈 폴더에서 `nvm install && nvm use`

## 주의할 점

- **브라우저 주소는 항상 `localhost` 로 쓴다.** `127.0.0.1` 을 섞으면 다른 사이트가 되어 world-server의 세션 쿠키가 가지 않는다(로그인이 안 된 것처럼 보인다).
- 로그인 세션은 world-server 메모리에 있다. **world-server를 다시 띄우면 다시 로그인해야 한다.**
- Google 값은 world-server만 쓴다. 두 클라이언트는 로그인 버튼으로 브라우저를 world-server에 보낼 뿐이다.

## Google OAuth 클라이언트

로그인은 world-server가 서버 측에서 한다(Spring Security OAuth2 Login). 리디렉션 URI는 하나다.

```
http://localhost:8080/login/oauth2/code/google
```

### 기존 클라이언트를 쓸 때 (권장)

이미 쓰던 OAuth 클라이언트(예: mud-mk2에서 쓰던 것)를 그대로 쓴다. [Google Cloud Console](https://console.cloud.google.com/) > API 및 서비스 > 사용자 인증 정보에서 그 클라이언트를 열고 확인한다.

- 애플리케이션 유형이 **웹 애플리케이션** 인가.
- **승인된 리디렉션 URI** 에 `http://localhost:8080/login/oauth2/code/google` 이 있는가. 이미 있으면 더 할 것이 없다(없으면 추가한다. 끝에 `/` 없이 정확히 같아야 한다).
- **승인된 JavaScript 원본** 은 필요 없다(브라우저 JS가 Google을 부르지 않는다).
- OAuth 동의 화면이 **테스트** 상태라면, 로그인할 Google 계정이 **테스트 사용자** 목록에 있어야 한다. 없으면 Google 화면에서 `액세스 차단됨` 이 뜬다.
- 범위(scope)는 `openid`, `profile`, `email` 만 쓴다(민감하지 않은 범위라 추가 검토가 필요 없다).

### 새로 만들 때

1. Google Cloud Console에서 프로젝트를 고르거나 만든다.
2. API 및 서비스 > OAuth 동의 화면: 사용자 유형 **외부**, 앱 이름·지원 이메일을 넣는다. 게시 상태는 **테스트** 로 두고 **테스트 사용자** 에 로그인할 Google 계정(관리자 계정 포함)을 넣는다.
3. API 및 서비스 > 사용자 인증 정보 > 사용자 인증 정보 만들기 > **OAuth 클라이언트 ID**.
4. 애플리케이션 유형 **웹 애플리케이션**, 승인된 리디렉션 URI에 `http://localhost:8080/login/oauth2/code/google` 를 넣고 만든다.
5. 나온 클라이언트 ID와 클라이언트 보안 비밀번호를 `.env` 의 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` 에 넣는다.

값이 없으면 world-server는 `not-configured` 로 떠서 시작 로그에 경고(`Google 로그인이 설정되지 않았습니다`)를 남긴다. 앱은 뜨지만 Google 로그인은 실패한다(아래 개발용 로그인은 된다).

## 한 번에 띄우기 (docker compose)

```sh
cp .env.example .env     # 값을 채운다 (아래 표). .env 는 커밋하지 않는다
docker compose up --build
```

| `.env` 값 | 뜻 |
|---|---|
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | 위 OAuth 클라이언트 값 |
| `MUD_ADMIN_EMAILS` | 관리자로 볼 Google 이메일. 쉼표로 나눈다(대소문자 무시). 예: `a@example.com,b@example.com` |
| `POSTGRES_PASSWORD` | compose 안 PostgreSQL 비밀번호. 꼭 채운다 |
| `SPRING_PROFILES_ACTIVE` | 비우면 운영. 개발용 로그인을 쓰려면 `local` |
| `MUD_DEV_LOGIN_ENABLED` | 개발용 로그인. 기본 `false` (아래 절) |

뜨는 것:

- play-client: http://localhost:3000
- admin-console: http://localhost:3001
- world-server: http://localhost:8080 (`/api/...`, `ws://localhost:8080/ws/game`)
- postgres: compose 안에서만 열린다(밖으로 포트를 열지 않는다). 데이터는 볼륨 `pgdata` 에 남는다.

멈추기: `docker compose down` (데이터를 지우려면 `docker compose down -v` — 맵이 모두 사라진다).

`MUD_DEV_LOGIN_ENABLED` 를 바꾸면 두 클라이언트를 다시 빌드해야 한다(`NEXT_PUBLIC_*` 는 빌드 때 박힌다): `docker compose up --build`.

### 관리자 지정

`MUD_ADMIN_EMAILS` 에 있는 이메일로 로그인한 계정이 관리자다. 로그인할 때 정해지므로, 목록을 바꾼 뒤에는 world-server를 다시 띄우고(`docker compose up -d world-server`) 다시 로그인한다.

## 처음 할 일 — 1차 시나리오

시작 방이 없으면 아무도 게임에 들어갈 수 없다(`아직 세계가 열리지 않았습니다. 관리자가 시작 방을 정해야 합니다.`).

1. http://localhost:3001 에서 관리자 이메일로 로그인한다(`Google로 로그인`, 또는 개발용 로그인).
2. 지역 목록(`/zones/`)에서 지역을 만든다. 예: `청석숲`.
3. 지역을 열어 방을 둘 이상 만든다. 예: `청석숲 오솔길`, `늙은 소나무 아래`.
4. 방을 열어 출구를 더한다(방위 → 대상 지역 → 대상 방, `반대 방향도` 를 켜 두면 돌아오는 출구도 생긴다).
5. 한 방에서 **시작 방으로 지정** 을 누른다.
6. http://localhost:3000 에서 로그인한다(다른 Google 계정이어도 된다).
7. 이름을 정한다(한글 2~6자) → `확인` 으로 캐릭터를 만든다. 시작 방이 보인다.
8. `보기`, 출구 방위(`북`, `n` …)로 움직이고 `'안녕하시오` 로 말해 본다. 다른 브라우저(또는 시크릿 창)로 두 번째 계정을 들이면 서로의 움직임과 말이 보인다.

### 1차에 되는 명령

| 입력 | 하는 일 |
|---|---|
| `보기`, `ㅂ` | 지금 방 다시 보기 (`보기 <무엇>` 은 아직 안 됨) |
| `동` `서` `남` `북` `북동` `북서` `남동` `남서` `위` `아래` / `e` `w` `s` `n` `ne` `nw` `se` `sw` `u` `d` | 그 방위로 이동 |
| `말하기 <내용>`, `'<내용>` | 같은 방 모두에게 말하기 (200자까지) |
| `누구` | 접속한 사람과 있는 지역 |
| `도움말` | 명령 목록 |
| `종료` | 접속 끊기 |

명령은 정해진 이름·줄임말로만 알아듣는다(앞부분만 맞춰 쓰기는 안 된다). play-client에서 ↑↓ 로 입력 기록, Tab 으로 명령어·같은 방 사람 이름 자동완성.

## 개발용 로그인

Google 자격 증명 없이 이메일만으로 로그인한다(로컬 수동 확인·스모크 테스트용).

- world-server: **`local` 프로필 그리고 `MUD_DEV_LOGIN_ENABLED=true` 일 때만** `POST /api/auth/dev {email}` 이 생긴다. 그 밖에는 404. 기본은 꺼짐.
- 클라이언트: `NEXT_PUBLIC_DEV_LOGIN=true` 로 빌드했을 때만 이메일 입력 폼이 보인다.
- 관리자 여부는 Google 로그인과 같이 `MUD_ADMIN_EMAILS` 로 정한다.
- **운영에서는 켜지 않는다.** 아무 이메일로나 그 계정이 될 수 있다.

compose로 켜기 — `.env`:

```sh
SPRING_PROFILES_ACTIVE=local
MUD_DEV_LOGIN_ENABLED=true
MUD_ADMIN_EMAILS=admin@example.com
```

(compose는 `MUD_DEV_LOGIN_ENABLED` 값을 두 클라이언트의 `NEXT_PUBLIC_DEV_LOGIN` 빌드 인자로도 넘긴다.)

## 개별 개발 실행

### world-server

```sh
cd world-server
GOOGLE_CLIENT_ID=... GOOGLE_CLIENT_SECRET=... MUD_ADMIN_EMAILS=you@example.com \
  ./gradlew bootRun --args='--spring.profiles.active=local'
```

- `local` 프로필은 `spring-boot-docker-compose` 로 `world-server/compose.yaml` 의 PostgreSQL(`localhost:5432`, DB·사용자·비밀번호 모두 `mud`)을 자동으로 띄우고 접속 정보를 넣는다. 이것은 개발 전용 DB이고 루트 `compose.yaml` 의 DB(볼륨 `pgdata`)와 별개다. 루트 compose와 함께 띄워도 서로 부딪치지 않는다(루트 compose의 postgres는 포트를 열지 않는다). 단 8080 포트는 하나만 쓸 수 있으니 루트 compose의 world-server는 멈춰 둔다.
- 개발용 로그인: 앞에 `MUD_DEV_LOGIN_ENABLED=true` 를 더한다.

### play-client / admin-console

```sh
cd play-client          # 또는 admin-console
nvm install && nvm use
npm ci
npm run dev             # play-client → :3000, admin-console → :3001
```

- world-server 주소는 기본 `http://localhost:8080`. 바꾸려면 `NEXT_PUBLIC_WORLD_SERVER_URL`.
- 개발용 로그인 폼: `NEXT_PUBLIC_DEV_LOGIN=true npm run dev`.

## 테스트

Google 자격 증명·네트워크 없이 돈다. 저장소 루트에서 한 줄씩(각각 따로) 실행한다.

```sh
cd world-server && ./gradlew build                      # 테스트 + SpotBugs (DB는 H2)
cd play-client && nvm use && npm ci && npm test && npm run build
cd admin-console && nvm use && npm ci && npm test && npm run build
```

### 스모크 테스트 (compose로 띄운 뒤)

`scripts/smoke-test.mjs` 가 떠 있는 world-server에 REST·WebSocket으로 붙어 1차 시나리오를 자동으로 돈다(Node 26 내장 `fetch`·`WebSocket` 만 씀, 설치할 것 없음). 개발용 로그인이 켜져 있어야 한다.

- 관리자 로그인 → 지역·방 `광장`·`북쪽 길`·양방향 출구·시작 방 지정. 플레이어는 관리 API 403, 미로그인 401, `X-MUD-Client` 없는 변경 403.
- 미로그인 WebSocket → `AUTH_REQUIRED` 후 4401, 허용 밖 `Origin` → 핸드셰이크 거절.
- 플레이어 두 명 캐릭터 생성 → 나타난다·보기·이동(떠난다·다가온다)·말하기·누구·종료(1000)·사라진다.
- 관리자가 플레이어가 서 있는 방을 지움 → 시작 방으로 옮겨짐. 같은 계정 재접속 → 앞 연결 4409.

`.env` 는 위 [개발용 로그인](#개발용-로그인) 값에 스모크 테스트의 관리자 이메일 `smoke-admin@example.com` 을 더한다.

```sh
# .env
SPRING_PROFILES_ACTIVE=local
MUD_DEV_LOGIN_ENABLED=true
MUD_ADMIN_EMAILS=admin@example.com,smoke-admin@example.com
```

```sh
docker compose up -d --build
source "$NVM_DIR/nvm.sh" && nvm use 26.13.1
node scripts/smoke-test.mjs                      # 기본 http://localhost:8080. 끝에 SMOKE OK, 종료 코드 0
SMOKE_ADMIN_EMAIL=admin@example.com node scripts/smoke-test.mjs   # .env 를 안 고치고 이미 있는 관리자 이메일로
docker compose down                              # 볼륨은 남는다
```

실패하면 어느 단계에서 무엇이 기대와 달랐는지(받은 메시지 포함) 출력하고 종료 코드 1. 실행마다 지역(`스모크 <실행 번호>`)·플레이어 계정·캐릭터(한글 4자)를 새로 만들고 지우지 않는다. 시작 방도 그 실행의 `광장` 으로 바뀐다 — 다른 맵을 쓰고 있으면 끝난 뒤 admin-console에서 시작 방을 다시 지정한다.

### 수동 확인 — 브라우저

스모크 테스트가 못 보는 것(화면·입력·실제 쿠키). 개발용 로그인으로 한다.

- [ ] http://localhost:3001 개발용 로그인(관리자 이메일) → 지역·방·출구(방위 → 대상 지역 → 대상 방, `반대 방향도`)를 만들고 시작 방 지정. 위쪽 바에 시작 방이 보인다.
- [ ] 관리자 아닌 이메일로 admin-console 로그인 → `관리자만 쓸 수 있습니다.` 와 로그아웃 버튼.
- [ ] http://localhost:3000 을 두 창(일반 창 + 시크릿 창)에서 서로 다른 이메일로 로그인 → 이름 정하기(한글 입력기로 조합 중인 글자가 Enter에 잘리지 않는지) → `확인`.
- [ ] 이동·말하기가 다른 창에 보인다. ↑↓ 입력 기록, Tab 으로 명령어·같은 방 사람 이름 자동완성.
- [ ] 한 창에서 같은 계정으로 새 탭을 열면 앞 탭이 끊기고 "다시 연결" 버튼이 보인다. 탭을 그냥 닫으면 다른 창에 `사라진다`.
- [ ] 로그아웃 뒤 새로 고침 → 로그인 화면.

### 수동 확인 — Google 로그인

실제 Google 자격 증명으로 한 번 확인한다(자동 테스트는 Google 없이 돈다).

- [ ] `.env` 에 실제 `GOOGLE_CLIENT_ID`·`GOOGLE_CLIENT_SECRET` 을 넣고, 개발용 로그인은 끈다(`MUD_DEV_LOGIN_ENABLED=false`, `SPRING_PROFILES_ACTIVE=` 비움). `MUD_ADMIN_EMAILS` 에 내 Google 이메일. `docker compose up -d --build` (클라이언트 다시 빌드).
- [ ] world-server 시작 로그에 `Google 로그인이 설정되지 않았습니다` 경고가 **없다**.
- [ ] Google Cloud Console에서 그 클라이언트의 승인된 리디렉션 URI가 `http://localhost:8080/login/oauth2/code/google` 인지 확인한다.
- [ ] 두 클라이언트에 개발용 로그인 폼이 보이지 않고, `POST /api/auth/dev` 가 404.
- [ ] http://localhost:3001 `Google로 로그인` → Google 동의 → admin-console로 돌아와 관리 화면이 보인다.
- [ ] `MUD_ADMIN_EMAILS` 에 없는 Google 계정으로 admin-console 로그인 → `관리자만 쓸 수 있습니다.`
- [ ] http://localhost:3000 `Google로 로그인` → 돌아와 캐릭터 생성 → 이동·말하기.
- [ ] Google 화면에서 취소하면 클라이언트로 돌아와 `로그인하지 못했습니다. 다시 시도하십시오.`
