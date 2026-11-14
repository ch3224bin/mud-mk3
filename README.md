# mud-mk3

범인수선전 풍 수선(修仙) 텍스트 MUD. 지금은 3차 이터레이션까지 했다.

- 1차: 관리자가 맵을 만들고, 플레이어가 캐릭터를 만들어 돌아다니며 말한다.
- 2차: 관리자가 성·종문을 정의하고 지역 소유를 정하고, NPC를 만들어 방에 배치한다. 플레이어는 방 머리에서 소유(`[청석성]`)를, 방에서 NPC를 보고 `대화` 로 NPC에게 말을 건다.
- 3차: 아이템 종류(영석·저물대·단약·재료·법기)를 만들고 방 바닥에 물건을 두고 시작 소지품을 정하고 NPC를 상점으로 만든다. 플레이어는 소지품을 보고, 줍고 버리고, 상점에서 사고 팔고(영석으로 값을 치른다), 저물대에 담고 귀속시키고, 다른 사람에게 주고, 버린 물건은 5분 뒤 사라진다.

| 모듈 | 하는 일 | 주소 |
|---|---|---|
| `world-server/` | 게임 서버. Java 25 · Spring Boot 4. Google 로그인, 관리 REST API, 게임 WebSocket(`/ws/game`), PostgreSQL | http://localhost:8080 |
| `play-client/` | 플레이 화면(터미널형). Next.js 정적 export | http://localhost:3000 |
| `admin-console/` | 관리 툴(지역·방·출구·시작 방, 성·종문·지역 소유, NPC 편집, 아이템 종류·시작 소지품·방 바닥 물건·NPC 상점). Next.js 정적 export | http://localhost:3001 |

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

## 2차 시나리오 — 성·종문, 지역 소유, NPC, 대화

1차 시나리오로 맵과 시작 방이 있는 상태에서 한다.

1. admin-console 위쪽 바의 `성·종문`(`/factions/`)에서 성을 만든다. 예: 종류 `성`, 이름 `청석성`.
2. `지역 목록`(`/zones/`)에서 지역(예: 1차에서 만든 `청석숲`)을 `수정` 해 `소유` 를 `청석성 (성)` 으로 고른다. 목록의 `소유` 열에 `청석성 (성)` 이 보인다.
3. 위쪽 바의 `NPC`(`/npcs/`) → `NPC 만들기`(`/npc/`)에서 NPC를 만든다.
   - 이름(띄어쓰기 없이 1~12자) 예: `왕씨`, 인사말 예: `어서 오시오.`
   - 기본 대답(모르는 낱말에) — 새 NPC는 `글쎄, 잘 모르겠구려.` 가 들어 있다.
   - `낱말 추가` 로 낱말과 대답을 넣는다. 예: `단약` → `단약이라면 저기 약방에 가 보시오.`, `일거리` → `요즘 성 밖에 늑대가 많다오.`
   - `LLM 대화` 는 켜도 지금은 정해진 대사로 답한다.
   - 배치: 지역 → 방을 고른다(소유를 정한 지역의 방. 예: `청석숲` → `청석숲 오솔길`). 지역 선택을 `배치 안 함` 으로 두거나 방을 고르지 않으면 배치되지 않는다.
4. 방 화면(`/room/?id=`)의 `이 방의 NPC` 에 그 NPC가 보인다.
5. http://localhost:3000 에서 그 방으로 간다. 방 이름 줄에 `청석숲 오솔길  [청석성]` 처럼 소유가 붙고, 사람 줄 다음에 NPC 줄(`왕씨`)이 다른 색으로 보인다.
6. `대화 왕씨` → `왕씨: "어서 오시오."` 와 `물어볼 수 있는 낱말: [단약] [일거리]`.
7. `대화 왕씨 일거리`(또는 `대화 왕씨 [일거리]`) → `왕씨: "요즘 성 밖에 늑대가 많다오."`.
8. `대화 왕씨 날씨`(모르는 낱말) → `왕씨: "글쎄, 잘 모르겠구려."`.

이미 1차를 띄워 쓰던 compose 볼륨(`pgdata`)에서 그대로 올려도 된다. `ddl-auto=update` 가 새 테이블(`faction`·`npc`·`npc_topic`)과 `zone.owner_id` 열을 더한다(마이그레이션 도구는 아직 없다).

## 3차 시나리오 — 아이템, 영석, 상점, 저물대·귀속, 주기

2차 시나리오까지 해서 맵·시작 방·NPC(예: `왕씨`)가 있는 상태에서 한다. 영석 4종(하품·중품·상품·극품)은 world-server가 뜰 때 만들어 두므로 아이템 목록(`/items/`)에 이미 있다(영석은 고칠 수 없다).

1. admin-console 위쪽 바의 `아이템`(`/items/`)에서 아이템 종류를 만든다.
   - `하급 저물대` — 종류 저물대, 등급 하급, 기준가 30
   - `회춘단(연기 초)` — 단약, 기준가 3
   - `회기단(연기 초)` — 단약, 기준가 4
   - `늑대 가죽` — 재료, 기준가 2
   - `비검` — 법기, 기준가 40
2. 방 화면(`/room/?id=`)의 `바닥 물건` 에서 `늑대 가죽` 3개를 둔다(재등장 분은 기본값 그대로).
3. 위쪽 바의 `시작 소지품`(`/start-items/`)에 하급 저물대 1·하품 영석 30·회춘단(연기 초) 3·회기단(연기 초) 3 을 넣고 저장한다(룰북 시작 소지품. 영수대는 아직 없다). 새로 만든 캐릭터만 받는다.
4. NPC 화면(`/npc/?id=`)의 `상점` 에서 상점 종류를 `단약방` 으로 하고 `회춘단(연기 초)`·`회기단(연기 초)` 를 더한다(값은 빈칸 — 기준가로 판다).
5. http://localhost:3000 에서 새 캐릭터를 만들어 그 방으로 간다. 방에 `늑대 가죽 3개` 줄이 보인다.
6. `소지품` → `[소지품 3/22칸]  하품 영석 30` 과 가진 물건.
7. `줍기 가죽 2` → `버리기 가죽 1` → `보기 가죽`(물건 이름은 앞부분·이름 안 낱말 앞부분으로 고른다).
8. 상점 NPC가 있는 방에서 `목록` → `사기 회춘단 2` → `팔기 회춘단 2` → `값 회춘단`. 영석을 깨야 하는 값이면 `확인` 또는 `취소` 로 답한다(10초 안).
9. `비검` 을 `사기` 나 `줍기` 로 얻어 `귀속 비검` → `버리기 비검`(귀속한 물건은 거절) → `귀속 해제 비검`.
10. 다른 브라우저(또는 시크릿 창)로 두 번째 계정을 같은 방에 들인다. 첫 계정에서 `주기 <이름> 회춘단 1`, `주기 <이름> 하품 5` — 받는 사람과 같은 방 다른 사람에게도 알림이 간다.

기존 `pgdata` 에서 그대로 올려도 된다. `ddl-auto=update` 가 새 테이블(`item_type`·`inventory`·`inventory_entry`·`floor_placement`·`dropped_item`·`npc_shop_item`·`world_starting_item`)과 `npc.shop_kind` 열을 더하고, world-server가 뜰 때 영석 4종을 만든다. 이미 있던 캐릭터는 빈 소지품을 가진다.

## 되는 명령

| 입력 | 하는 일 |
|---|---|
| `보기`, `ㅂ` | 지금 방 다시 보기 |
| `보기 <무엇>` | 같은 방 바닥 물건·소지품의 물건을 본다(물건은 보임, 사람·NPC는 아직) |
| `동` `서` `남` `북` `북동` `북서` `남동` `남서` `위` `아래` / `e` `w` `s` `n` `ne` `nw` `se` `sw` `u` `d` | 그 방위로 이동 |
| `말하기 <내용>`, `'<내용>` | 같은 방 모두에게 말하기 (200자까지) |
| `대화 <NPC>` | 같은 방 NPC의 인사말과 물어볼 수 있는 낱말 |
| `대화 <NPC> <낱말>` | 그 낱말을 묻는다. 모르는 낱말이면 기본 대답. NPC 이름은 앞부분만 써도 된다(`대화 왕 단약`). 대답은 묻는 사람에게만 보인다 |
| `누구` | 접속한 사람과 있는 지역 |
| `소지품`, `ㅈ` | 소지품 칸과 가진 영석·물건 (`[소지품 3/22칸]  하품 영석 30`) |
| `줍기 <물건> [수량]` | 방 바닥에서 소지품으로 |
| `버리기 <물건> [수량]` | 소지품에서 방 바닥으로(5분 뒤 사라진다) |
| `주기 <대상> <물건> [수량]` | 같은 방 사람에게 물건을 준다 |
| `목록` | 같은 방 상점의 파는 물건과 값 |
| `사기 <물건> [수량]`, `팔기 <물건> [수량]`, `값 <물건>` | 상점에서 사기·팔기·값 보기 |
| `귀속 <물건>`, `귀속 해제 <물건>` | 물건을 내게 묶거나(주기·버리기 안 됨) 풀기 |
| `확인`, `취소` | 영석을 깨야 하는 거래의 확인 질문에 답(10초 안) |
| `도움말` | 명령 목록: `명령: 보기(ㅂ) · 동 서 남 북 북동 북서 남동 남서 위 아래 (e w s n ne nw se sw u d) · 말하기(') · 대화 <NPC> [낱말] · 소지품(ㅈ) · 보기 <물건> · 줍기 <물건> [수량] · 버리기 <물건> [수량] · 주기 <대상> <물건> [수량] · 목록 · 사기 <물건> [수량] · 팔기 <물건> [수량] · 값 <물건> · 귀속 <물건> · 귀속 해제 <물건> · 확인 · 취소 · 누구 · 도움말 · 종료` |
| `종료` | 접속 끊기 |

수량은 명령 끝에 적는다: 마지막 낱말이 수면 그 수, `전부` 면 모두, 없으면 1개(`줍기 가죽 2`, `버리기 가죽 전부`, `주기 왕 하품 10`). 같은 이름이 여럿이면 `2.가죽` 처럼 번호로 고른다. 영석은 `하품 영석`·`하품` 처럼 등급 낱말로 부른다.

`말하기` 는 플레이어끼리 하는 채팅이다. NPC에게 말을 걸 때는 `대화` 를 쓴다(`말` 이라는 명령은 없다).

명령은 정해진 이름·줄임말로만 알아듣는다(앞부분만 맞춰 쓰기는 안 된다 — 앞부분으로 고를 수 있는 것은 `대화` 의 NPC 이름, 물건 이름(앞부분·이름 안 낱말 앞부분), `주기` 의 받는 사람 이름뿐이다). play-client에서 ↑↓ 로 입력 기록, Tab 으로 명령어·같은 방 사람·NPC·바닥 물건 이름 자동완성(`귀속 해제` 는 자동완성 목록에 없고 `귀속` 뒤에 `해제` 를 직접 쓴다).

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

`scripts/smoke-test.mjs` 가 떠 있는 world-server에 REST·WebSocket으로 붙어 1차·2차 시나리오를 자동으로 돈다(Node 26 내장 `fetch`·`WebSocket` 만 씀, 설치할 것 없음). 개발용 로그인이 켜져 있어야 한다.

- 관리자 로그인 → 지역·방 `광장`·`북쪽 길`·양방향 출구·시작 방 지정. 플레이어는 관리 API 403, 미로그인 401, `X-MUD-Client` 없는 변경 403.
- 미로그인 WebSocket → `AUTH_REQUIRED` 후 4401, 허용 밖 `Origin` → 핸드셰이크 거절.
- 플레이어 두 명 캐릭터 생성 → 나타난다·보기·이동(떠난다·다가온다)·말하기·누구·종료(1000)·사라진다.
- 관리자가 플레이어가 서 있는 방을 지움 → 시작 방으로 옮겨짐. 같은 계정 재접속 → 앞 연결 4409.
- (2차) 성·종문 만들기 → 지역 소유를 성으로 → 지역 목록 `ownerName`. NPC(인사말·낱말 둘·기본 대답·LLM 켬)를 `광장` 에 배치 → NPC 상세 값.
- (2차) 플레이어 보기 → 방 머리 `owner`(성)와 occupants 의 NPC(`kind: npc`). `대화 <NPC>` 인사말과 낱말, 낱말 대답, 모르는 낱말 → 기본 대답, `도움말` 에 `대화 <NPC> [낱말]`.
- (2차) 소유 지역이 있는 성 삭제 → 409. NPC가 있는 방을 지우면 NPC 배치가 풀림.

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

실패하면 어느 단계에서 무엇이 기대와 달랐는지(받은 메시지 포함) 출력하고 종료 코드 1. 실행마다 지역(`스모크 <실행 번호>`)·플레이어 계정·캐릭터(한글 4자)·성(`청석성<숫자 6자리>`)·종문(`황풍곡<숫자>`)·NPC(`왕씨<숫자>`·`점소이<숫자>`)를 새로 만들고 지우지 않는다. 시작 방도 그 실행의 `광장` 으로 바뀐다 — 다른 맵을 쓰고 있으면 끝난 뒤 admin-console에서 시작 방을 다시 지정한다.

### 수동 확인 — 브라우저

스모크 테스트가 못 보는 것(화면·입력·실제 쿠키). 개발용 로그인으로 한다.

- [ ] http://localhost:3001 개발용 로그인(관리자 이메일) → 지역·방·출구(방위 → 대상 지역 → 대상 방, `반대 방향도`)를 만들고 시작 방 지정. 위쪽 바에 시작 방이 보인다.
- [ ] 관리자 아닌 이메일로 admin-console 로그인 → `관리자만 쓸 수 있습니다.` 와 로그아웃 버튼.
- [ ] http://localhost:3000 을 두 창(일반 창 + 시크릿 창)에서 서로 다른 이메일로 로그인 → 이름 정하기(한글 입력기로 조합 중인 글자가 Enter에 잘리지 않는지) → `확인`.
- [ ] 이동·말하기가 다른 창에 보인다. ↑↓ 입력 기록, Tab 으로 명령어·같은 방 사람 이름 자동완성.
- [ ] 한 창에서 같은 계정으로 새 탭을 열면 앞 탭이 끊기고 "다시 연결" 버튼이 보인다. 탭을 그냥 닫으면 다른 창에 `사라진다`.
- [ ] 로그아웃 뒤 새로 고침 → 로그인 화면.

2차 (위 [2차 시나리오](#2차-시나리오--성종문-지역-소유-npc-대화)대로 만든 뒤):

- [ ] admin-console 위쪽 바에 `성·종문`·`NPC` 링크. 성 만들기 → 지역 `소유` 지정 → 지역 목록 `소유` 열. 소유 지역이 있는 성을 지우면 `소유한 지역이 있는 성·종문은 지울 수 없습니다`, 소유를 `없음` 으로 바꾼 뒤에는 지워진다.
- [ ] NPC 만들기(인사말·낱말·대답·기본 대답·LLM 값·배치) → `/npc/?id=` 로 넘어가고, `/npcs/` 목록에 배치(`지역 / 방`)·낱말 수·LLM `켬`/`끔`.
- [ ] play-client 그 방: 방 이름 줄 뒤 `  [청석성]`, 플레이어 줄 다음 NPC 줄이 플레이어 줄과 다른 색.
- [ ] `대화 왕씨` → 인사말과 `물어볼 수 있는 낱말: [단약] [일거리]`, `대화 왕씨 단약` → 그 대답, 모르는 낱말 → 기본 대답. 대답 줄의 색이 `말하기` 줄과 다르다. 같은 방 다른 창에는 대화가 보이지 않는다.
- [ ] `대화` 만 → `누구에게 말을 거시겠습니까?`, NPC 없는 방에서 `대화 왕씨` → `여기에 그런 상대는 없습니다.`
- [ ] 입력창에 `대` + Tab → `대화`, `대화 왕` + Tab → NPC 이름 자동완성.
- [ ] NPC의 `LLM 대화` 를 켜고 저장해도 같은 대사로 답한다.
- [ ] NPC가 있는 방(시작 방이 아닌 방)을 지운다 → `/npcs/` 에서 그 NPC가 `배치 안 됨`. 다시 다른 방에 배치할 수 있다.

3차 (위 [3차 시나리오](#3차-시나리오--아이템-영석-상점-저물대귀속-주기)대로 만든 뒤):

- [ ] admin-console 위쪽 바에 `아이템`·`시작 소지품` 링크. 영석 4종은 목록에 있고 고칠 수 없다. 방 화면 `바닥 물건`, NPC 화면 `상점`.
- [ ] play-client 그 방: NPC 줄 다음 `늑대 가죽 3개` 줄이 다른 색(바닥 물건 줄 색).
- [ ] `소지품` → `[소지품 N/22칸]` 과 영석·물건 줄. 저물대·귀속·퀘스트 표시.
- [ ] 상점 방에서 `목록` → 물건과 값. `사기`·`팔기`·`값` 메시지.
- [ ] 영석을 깨야 하는 값(예: 상위 영석으로 치르기) → 확인 질문, `확인` 으로 거래가 되고 `취소` 로 그만둔다.
- [ ] `귀속 비검` 뒤 소지품에 `(귀속)`, `버리기`·`주기` 는 거절, `귀속 해제` 뒤 다시 된다.
- [ ] `주기 <이름> 회춘단 1` → 주는 사람·받는 사람 메시지와 같은 방 제3자 알림.
- [ ] 버린 물건이 5분 뒤 방에서 사라진다(방을 다시 `보기`).
- [ ] 입력창에 `줍기 늑` + Tab → `줍기 늑대 가죽`.

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
