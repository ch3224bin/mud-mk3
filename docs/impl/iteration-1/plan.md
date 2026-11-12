# 1차 이터레이션 구현 계획 — 인증 + 맵 만들기 + 맵 돌아다니기

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Google 로그인으로 들어온 관리자가 admin-console에서 존(지역)·방·출구를 만들고, 플레이어가 play-client(터미널형 화면)에서 이름만 정해 캐릭터를 만든 뒤 그 맵을 돌아다니며 같은 방의 다른 플레이어를 보고 말을 주고받는다.

**Architecture:** `world-server`(Spring Boot, 헥사고날)가 맵·계정·캐릭터를 PostgreSQL에 두고, 접속 중인 플레이어의 위치(누가 어느 방에 있나)는 메모리에 둔다. admin-console은 REST(`/api/admin/**`), play-client는 WebSocket(`/ws/game`, JSON `{type, ...}`)으로 붙는다. 두 클라이언트는 Next.js 정적 export이고, Google 로그인은 world-server가 Spring Security OAuth2 Login(서버 측, 클라이언트 시크릿 사용)으로 하며, 로그인 상태는 world-server의 HTTP 세션 쿠키(`JSESSIONID`)로 유지한다. 두 클라이언트(`localhost:3000`, `localhost:3001`)와 world-server(`localhost:8080`)는 포트만 다른 같은 사이트라 쿠키가 REST(`credentials: "include"`)와 WebSocket 핸드셰이크에 함께 간다.

**Tech Stack:** Java 25 · Spring Boot 4.1.2 · Spring Security 7.1.2(BOM) · Gradle 9.9.1 Kotlin DSL · PostgreSQL 18.6 / H2(테스트) · Next.js 16.4.6 · React 19.3.1 · TypeScript 7.0.2 · Vitest 5.1.0 · Docker Compose

**Spec:** 아래 `## Spec` 참고. 계획은 스펙을 근거로 하고, 스펙이 정하지 않은 것은 `## 제안 결정 목록` 에서 정했다.

---

## Spec

- 시스템 스펙: `docs/spec/system/index.md`(`## 이터레이션 계획` 의 1차 범위), `platform.md`, `auth.md`, `admin-tool.md`, `world-structure.md`, `command.md`
- 룰북(게임 규칙): `docs/spec/game-rule/commands.md`(공통 규칙, 이동 · 보기, 말하기 · 계정), `docs/spec/game-rule/world.md`(장소, 방 보기, 지역, 출구, 일반 이동, 말하기와 누구), `docs/spec/game-rule/character.md`(캐릭터 생성 — 1차는 이름만), `docs/spec/game-rule/combat.md` 57행(접속 종료하면 세계에서 사라지고 다시 접속하면 끊은 방에서 시작)
- 용어: `docs/spec/glossary.md`
- world-server 코딩 규칙: `.claude/rules/*.md` (README, build, architecture, domain-model, application, adapter, persistence, testing, code-style). **world-server 태스크는 시작 전에 모두 읽는다.**
- 1차 범위 밖: 성·종문, 전투, NPC·몹, 퀘스트, 아이템, 스탯·영근·경지, 튜토리얼, 이름 있는 출구(`들어가기`)·비밀문, 영기 농도, 안전 장소, 귓속말·외치기, `보기 <대상>` 의 자세한 보기, `!` 반복, 금지어.

## Global Constraints

모든 태스크의 요구 사항에 이 절과 `## 제안 결정 목록` 이 포함된다. 태스크를 받은 구현자는 이 두 절을 먼저 읽는다.

**버전 (추측 금지 — 이 값을 그대로 쓴다)**

| 대상 | 버전 | 출처 (2026-11-12 조회) |
|---|---|---|
| Java | 25 (toolchain) | platform.md |
| Spring Boot | 4.1.2 (BOM: Spring Framework 7.0.10, Spring Security 7.1.2, Hibernate 7.4.13.Final, Jackson 3.1.7, JUnit 6.0.3, Mockito 5.23.0, PostgreSQL 드라이버 42.7.14, H2 2.4.240) | platform.md, Maven Central `spring-boot-dependencies-4.1.2.pom` |
| Gradle | 9.9.1 (wrapper), Kotlin DSL | platform.md |
| Gradle 플러그인 | `io.spring.dependency-management` 1.1.7, `com.github.spotbugs` 6.5.13 | platform.md |
| BOM 밖 테스트 라이브러리 | ArchUnit(`com.tngtech.archunit:archunit-junit5`) 1.5.1, Instancio(`org.instancio:instancio-core`) 6.2.1 | platform.md |
| 보안 스타터 | `spring-boot-starter-security-oauth2-client`(버전은 BOM. 옛 이름 `spring-boot-starter-oauth2-client` 는 4.1.2에서 deprecated), 테스트 `spring-boot-starter-security-test` | Maven Central 4.1.2 pom description |
| Node.js | 26.13.1 (LTS "Lithium") | `https://nodejs.org/dist/index.json` |
| next | 16.4.6 (engines node >=20.9.0) | `https://registry.npmjs.org/next/latest` |
| react, react-dom | 19.3.1 | registry.npmjs.org `react/latest`, `react-dom/latest` |
| typescript | 7.0.2 — next 16.4.6 `next build` 가 TS 7.0.2로 타입 검사를 실제로 하는 것을 임시 프로젝트에서 확인했다(일부러 넣은 타입 오류에서 `Failed to type check.`) | registry.npmjs.org `typescript/latest` |
| @types/react, @types/react-dom | 19.3.0 | registry.npmjs.org |
| @types/node | 26.6.6 | registry.npmjs.org |
| vitest | 5.1.0 (engines node ^22.12.0 \|\| ^24 \|\| >=26) | registry.npmjs.org |
| vite | 8.3.8 | registry.npmjs.org |
| @vitejs/plugin-react | 6.3.0 | registry.npmjs.org |
| jsdom | 31.0.0 (engines node ^22.22.2 \|\| ^24.15.0 \|\| >=26) | registry.npmjs.org |
| @testing-library/react | 16.3.3 (+ peer `@testing-library/dom` 10.4.2) | registry.npmjs.org |
| @testing-library/jest-dom | 7.0.1 | registry.npmjs.org |
| @testing-library/user-event | 14.6.10 | registry.npmjs.org |
| Docker 이미지 | `postgres:18.6-alpine`, `eclipse-temurin:25.0.5_8-jdk`(빌드), `eclipse-temurin:25.0.5_8-jre`(실행), `node:26.13.1-alpine`(프론트 빌드), `nginx:1.30.6-alpine`(정적 파일, stable 줄) | Docker Hub `library/*` 태그 목록 |

- npm 패키지 버전은 `package.json` 에 `^` 없이 정확한 버전으로 적고 `package-lock.json` 을 커밋한다. 패키지 매니저는 npm.
- 위 표에 없는 라이브러리를 새로 넣지 않는다. 꼭 필요하면 레지스트리에서 최신 안정판을 조회해 넣고 커밋 메시지 본문에 출처를 적는다.

**로컬 환경 (2026-11-12 확인)**

- macOS arm64. `java -version` → Temurin 25.0.3 (기본 JDK). Gradle toolchain이 로컬 JDK 25를 찾으므로 foojay 자동 다운로드 플러그인은 넣지 않는다.
- `gradle` 9.4.1 이 Homebrew로 깔려 있다 — wrapper를 만들 때만 쓴다(`gradle wrapper --gradle-version 9.9.1`). 그 뒤로는 항상 `./gradlew`.
- `node -v` → **v22.15.1** (nvm, `NVM_DIR=~/.config/nvm`). jsdom 31이 node ^22.22.2를 요구하므로 프론트 태스크는 Node 26.13.1이 필요하다. 프론트 모듈에 `.nvmrc`(`26.13.1`)를 두고, 명령 앞에 `source "$NVM_DIR/nvm.sh" && nvm install && nvm use` 를 붙여 실행한다(설치가 막히면 멈추고 리드에게 보고).
- `docker --version` → 29.3.1, `docker compose version` → v5.1.2.

**저장소·규칙**

- 모듈: `world-server/`(Gradle 단독 빌드, 루트에 Gradle 없음), `play-client/`, `admin-console/`. 루트에는 `compose.yaml`, `.env.example`, `README.md`, `scripts/` 만 둔다.
- world-server 코딩 규칙은 `.claude/rules/*.md` 를 따른다. 루트 패키지(`<base>`)는 **`mudmk3`** 이다.
- Next.js 앱은 정적 export(`output: "export"`, `trailingSlash: true`, `images.unoptimized: true`)이고 SSR·Route Handler·middleware·`next/headers` 를 쓰지 않는다. 화면 코드는 `"use client"`. 경로 별칭(`@/`) 없이 상대 경로 import.
- 화면 문구·서버 메시지·예외 메시지는 한국어. 문구는 룰북 문장을 그대로 쓰고, 룰북에 없는 문구는 이 계획에 적힌 문장을 그대로 쓴다.
- 자동 테스트는 Google 자격 증명·네트워크 없이 돈다. 가짜(대역)는 테스트 코드 안에만 둔다.
- 비밀 값(`GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, DB 비밀번호)은 환경변수로만 받는다. 실제 값은 저장소 어디에도 없으며 찾지 않는다. `.env` 는 커밋하지 않고 `.env.example` 에 이름만 둔다.
- 브라우저 주소는 항상 `localhost` 로 쓴다(`127.0.0.1` 을 섞으면 다른 사이트가 되어 세션 쿠키가 가지 않는다).

**커밋**

- 태스크 하나 = 커밋 하나 이상. 구현자는 커밋만 하고 **push 하지 않는다**(리드가 한다). 구현자는 서브에이전트를 띄우지 않는다.
- 커밋 메시지: `<type>(<module>): <한국어 요약>` (type: feat, test, build, docs, chore, fix / module: world-server, play-client, admin-console, infra, spec). 예: `feat(world-server): 존·방·출구 도메인과 영속 매핑`.
- 커밋 메시지 끝에 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` 를 붙인다.
- 태스크를 끝내면 `docs/impl/iteration-1/progress.md` 의 표에 한 줄(태스크 번호·제목, 커밋 해시는 리드가 채움, 메모)을 더하고 같은 커밋에 넣는다.

**검증 명령 (각 태스크 완료 조건에 포함)**

- world-server: `cd world-server && ./gradlew build` (테스트 + SpotBugs 통과)
- play-client / admin-console: `cd <module> && source "$NVM_DIR/nvm.sh" && nvm use && npm ci && npm test && npm run build`

## 제안 결정 목록

스펙이 정하지 않아 이 계획에서 정한 것. Task 11이 `docs/spec/system/` 해당 문서에 `(제안)` 표시로 옮긴다.

**D1. 인증 흐름 — world-server의 Spring Security OAuth2 Login(서버 측) + HTTP 세션 쿠키** (오케스트레이터 지시로 확정. 창호의 기존 Google OAuth 클라이언트를 그대로 쓴다)
- 등록: `spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID}`, `client-secret=${GOOGLE_CLIENT_SECRET}`, `scope=openid,profile,email`. 리디렉션 URI는 Spring 기본 `{baseUrl}/login/oauth2/code/{registrationId}` = **`http://localhost:8080/login/oauth2/code/google`**(기존 클라이언트에 이미 등록된 값. 새로 추가할 리디렉션 URI는 없다). `openid` 로 OIDC가 되어 Spring이 ID 토큰 서명·`iss`·`aud`·`exp` 를 검증한다.
- 흐름:
  1. 클라이언트의 로그인 버튼이 브라우저를 `http://localhost:8080/api/auth/login?client=play`(admin-console은 `client=admin`)로 보낸다(전체 페이지 이동).
  2. world-server가 `client` 값을 HTTP 세션에 적고 `/oauth2/authorization/google` 로 리다이렉트 → Google 로그인·동의 → `http://localhost:8080/login/oauth2/code/google` 로 돌아온다.
  3. 성공 처리기가 Google `sub`·`email`(`email_verified` 가 true가 아니면 실패)로 계정을 찾거나 만들고, 세션의 `client` 값에 맞는 클라이언트 주소로 리다이렉트한다: `play` → `MUD_PLAY_CLIENT_URL`(기본 `http://localhost:3000/`), `admin` → `MUD_ADMIN_CONSOLE_URL`(기본 `http://localhost:3001/`). 값이 없거나 다르면 `play`. 임의 URL로는 보내지 않는다(열린 리다이렉트 방지). 실패하면 같은 주소에 `?loginError=1` 을 붙인다.
  4. 이후 클라이언트는 `fetch(..., {credentials: "include"})` 로 `GET /api/auth/me` 를 불러 로그인 여부를 안다(401이면 로그인 버튼을 보인다). WebSocket 핸드셰이크에도 같은 쿠키가 간다(D4).
  5. 로그아웃: `POST /api/auth/logout` → 세션 무효화, 204.
- 쿠키·세션: `JSESSIONID`, `HttpOnly`, `SameSite=Lax`. `localhost:3000`/`3001` 과 `localhost:8080` 은 오리진은 다르지만 같은 사이트(쿠키는 포트를 가리지 않음)라 Lax 쿠키가 간다. 세션은 world-server 메모리(톰캣)에 둔다 — 서버를 다시 시작하면 다시 로그인해야 한다(1차 허용, Spring Session JDBC는 다음 이터레이션). (제안)
- 이 방식을 고른 이유(오케스트레이터 확정 사항): 창호의 기존 OAuth 클라이언트(리디렉션 URI `http://localhost:8080/login/oauth2/code/google`)를 그대로 쓴다. 클라이언트 시크릿은 서버에만 있다.
- 관리자 여부는 로그인 성공 시 `MUD_ADMIN_EMAILS`(쉼표로 나눈 목록, 대소문자 무시)로 정해 세션의 인증 정보에 권한 `ROLE_ADMIN` 으로 넣는다(모든 로그인 계정은 `ROLE_PLAYER`). 목록을 바꾸면 다음 로그인부터 반영된다. (제안)
- 세션 인증 정보(principal)는 `AccountPrincipal(Long accountId, String email, boolean admin)` 하나로 통일한다(Google 로그인·개발용 로그인 둘 다).

**D2. 오리진·포트·CORS·CSRF** — world-server `8080`, play-client `3000`, admin-console `3001`, PostgreSQL `5432`(루트 compose에서는 밖으로 열지 않음). 브라우저가 world-server에 직접 붙는다(compose도 같은 포트 구성, 프록시 없음).
- CORS: `MUD_CORS_ALLOWED_ORIGINS`(기본 `http://localhost:3000,http://localhost:3001`)만 허용, `allowCredentials=true`, 헤더 `Content-Type`·`X-MUD-Client`. WebSocket 핸드셰이크의 `Origin` 도 같은 목록으로 검사한다(다른 사이트의 WebSocket 가로채기 방지).
- CSRF(쿠키 인증이므로 필요): Spring CSRF 토큰 대신, `/api/**` 의 `POST`·`PUT`·`DELETE` 는 헤더 `X-MUD-Client: 1` 이 있어야 하고 없으면 403. 사용자 정의 헤더는 CORS 사전 요청을 일으키므로 허용 목록 밖 오리진은 보낼 수 없다. Spring Security의 기본 CSRF는 끈다. (제안)
- 인증 안 된 `/api/**` 요청은 Google로 리다이렉트하지 않고 401(`ProblemDetail`)을 준다.

**D3. 개발용 로그인(dev login) (제안)** — `POST /api/auth/dev {email}`(헤더 `X-MUD-Client: 1`)이 Google 없이 그 이메일 계정으로 세션을 만들고(D1과 같은 `AccountPrincipal`, `ROLE_ADMIN` 판정도 같음) `AuthMeResponse` 를 돌려준다. `local` 프로필 **그리고** `MUD_DEV_LOGIN_ENABLED=true` 일 때만 빈이 생기고(그 밖에는 404), 기본은 꺼짐. 계정의 `googleSubject` 는 `dev:<email>`. 클라이언트는 빌드 변수 `NEXT_PUBLIC_DEV_LOGIN=true` 일 때만 이메일 입력 폼을 보인다. README에 명시한다. Google 자격 증명 없이 하는 로컬 수동 확인·스모크 테스트용이다.

**D4. WebSocket 접속·인증** — 엔드포인트 `ws://localhost:8080/ws/game`(Spring raw WebSocket, STOMP 아님). 인증은 핸드셰이크에 실려 오는 세션 쿠키로 한다: 핸드셰이크 인터셉터가 `Origin` 을 허용 목록으로 검사(아니면 403으로 거절)하고, HTTP 세션의 `SecurityContext` 에서 `AccountPrincipal` 을 꺼내 WebSocket 세션 속성 `accountId` 로 넘긴다. 연결은 받아 준 뒤, `accountId` 가 없으면 `error`(`AUTH_REQUIRED`) 메시지를 보내고 4401로 닫는다(핸드셰이크에서 거절하면 클라이언트가 까닭을 알 수 없어서). 별도의 `auth` 메시지는 없다.

**D5. WebSocket 메시지 스키마** (JSON, 모든 메시지에 `type`)

클라이언트 → 서버

| type | 필드 | 뜻 |
|---|---|---|
| `command` | `text` (최대 500자) | 입력 한 줄 |

서버 → 클라이언트

| type | 필드 | 뜻 |
|---|---|---|
| `session` | `state`: `"creating"` \| `"playing"`, `characterName`: string \| null | 접속 상태가 바뀔 때 |
| `prompt` | `text` | 캐릭터 생성 중 묻는 말 |
| `room` | `zone`: `{id, name}`, `zoneEntered`: boolean, `room`: `{id, name, description}`, `exits`: string[] (방위 한글 이름, 순서 `동 서 남 북 북동 북서 남동 남서 위 아래`), `occupants`: `[{name, kind:"player"}]` (나 빼고, 들어온 순서) | 방 보기 |
| `system` | `text`, `tone`: `"info"` \| `"error"` | 안내·결과·거절. `text` 에 줄바꿈(`\n`)이 있을 수 있다 |
| `notice` | `text`, `tone`: `"movement"`, `actor`: string, `event`: `"arrive"` \| `"leave"` \| `"appear"` \| `"disappear"` | 같은 방에서 일어난 일(남이 들어옴·나감·접속·접속 끊김) |
| `say` | `speaker`, `message`, `self`: boolean, `text`(그린 한 줄), `tone`: `"speech"` | 말하기 |
| `error` | `code`: `"AUTH_REQUIRED"` \| `"BAD_MESSAGE"`, `text` | 프로토콜 오류 |

- 닫기 코드: `1000` 종료 명령, `4401` 로그인 안 됨(세션 없음·만료), `4409` 같은 계정이 다른 곳에서 접속해 밀려남, `1013` 시작 방이 없어 세계가 열리지 않음.
- `tone` 은 의미 값이다. 1차에 쓰는 값: `info`, `error`, `movement`, `speech`. 클라이언트는 `room` 을 그릴 때 `zone`(지역 줄)·`room-name`·`exits`·`occupant` 같은 자체 스타일 이름을 쓴다.

**D6. 1차 명령어** (룰북 `commands.md` 줄임말·문구를 따름. 명령어는 정해진 이름·줄임말로만 알아듣고 앞부분 일치는 받지 않음)

| 입력 | 하는 일 | 출력 |
|---|---|---|
| `보기`, `ㅂ` | 지금 방 다시 보기 | `room` (`zoneEntered=false`) |
| `보기 <무엇이든>` | 1차엔 자세히 보기 없음 (제안) | `system/info` `아직 자세히 볼 수 없습니다.` |
| `동` `서` `남` `북` `북동` `북서` `남동` `남서` `위` `아래`, `e` `w` `s` `n` `ne` `nw` `se` `sw` `u` `d`(대소문자 무시) | 그 방위 출구로 이동 | 성공: 새 방 `room`. 실패: `system/error` `그쪽으로는 길이 없습니다.` |
| `말하기 <내용>`, `'<내용>`(띄어 쓰지 않아도 됨) | 같은 방 모두에게 | 나: `say(self=true, text='한백: "길을 묻겠소."')`, 남: `say(self=false, text='한백이 말한다: "길을 묻겠소."')`. 내용 없음: `system/error` `무엇이라 말하시겠습니까?`. 200자 넘음: `system/error` `말은 200자 이내로 하십시오.` (제안) |
| `누구` | 접속한 사람과 있는 지역 | `system/info` 여러 줄: `접속한 사람 (2)` 다음 줄마다 `  한립    청석숲`(두 칸 들여쓰기, 이름 뒤 네 칸) |
| `도움말` | 1차 명령 목록 | `system/info` `명령: 보기(ㅂ) · 동 서 남 북 북동 북서 남동 남서 위 아래 (e w s n ne nw se sw u d) · 말하기(') · 누구 · 도움말 · 종료` |
| `종료` | 접속을 끊는다 | `system/info` `다음에 또 뵙겠습니다.` 후 닫기 1000 |
| 빈 줄 | 무시 | 없음 |
| 그 밖 | — | `system/error` `무슨 말인지 모르겠습니다. ('도움말' 로 명령을 볼 수 있습니다)` |

- `말` 은 룰북에서 NPC 말 걸기라 1차엔 알 수 없는 명령으로 처리한다.

**D7. 같은 방 알림 문구 (제안)** — 룰북 출력 문체(묘사는 "~다")를 따른다. 조사 `이/가` 는 이름 끝 글자 받침으로 고른다.
- 내가 북으로 떠날 때 원래 방 사람들: `한백이 북쪽으로 떠난다.` (`위로`, `아래로`; 8방위는 `동쪽으로`·`북동쪽으로` 식)
- 도착한 방 사람들: `한백이 남쪽에서 다가온다.` (도착 방향은 이동 방향의 반대. `위에서`, `아래에서`)
- 접속(세계에 들어옴): `한백이 나타난다.` / 접속 끊김: `한백이 사라진다.`
- 다른 지역의 방에 들어가면 `room.zoneEntered=true` 이고 클라이언트가 방 위에 `◇ 청석숲` 한 줄을 그린다(룰북 `◇ 지역 (권장: …)` 에서 권장 경지는 1차에 없음). 접속 직후 첫 방도 `zoneEntered=true`.

**D8. 방 보기 화면(클라이언트가 `room` 을 그리는 모양)** — 룰북 방 보기 순서에서 1차에 있는 것만:
```
◇ 청석숲            ← zoneEntered일 때만
청석숲 오솔길
늙은 소나무 사이로 좁은 길이 굽어 있다.
출구: 북 남동        ← 출구 없으면 "출구: 없음"
한립                ← occupants 한 줄에 하나 (경지 표시는 경지 도입 후)
```

**D9. 캐릭터 생성(1차 최소) (제안)** — 룰북 생성 절차에서 이름과 확인만 한다(성별·외모·출신·영근은 다음 이터레이션). 캐릭터가 없는 계정으로 접속하면 `session(creating)` 과 `prompt` `이름을 정하십시오. (한글 2~6자)`.
- 이름 규칙: `^[가-힣]{2,6}$`. 어기면 `system/error` `이름은 띄어쓰기 없이 한글 2~6자로 정하십시오.` 후 같은 prompt. 이미 있으면 `system/error` `이미 있는 이름입니다. 다른 이름을 정하십시오.` 후 같은 prompt.
- 맞으면 `prompt` `이름 한백\n이대로 만들려면 '확인', 고치려면 '뒤로'.` → `확인`: 만든다 / `뒤로`: 이름 prompt로 / 그 밖: 같은 확인 prompt 다시.
- 만들면 `system/info` `캐릭터를 만들었습니다.`, `session(playing, 한백)`, 시작 방 `room`, 시작 방 사람들에게 `appear` 알림.
- 계정당 캐릭터 1개(DB 유니크). 금지어·NPC 이름 검사는 NPC가 생기는 이터레이션에서.

**D10. 데이터 모델**
- 존(지역) `Zone`: 이름(1~30자, 유일), 분위기 묘사(0~500자).
- 방 `Room`: 지역 하나, 이름(1~30자), 묘사(1~1000자), 출구 목록. 출구 `Exit`: 방위(`Direction` 10개), 대상 방. 한 방에 같은 방위 출구는 하나. 자기 자신으로 가는 출구는 막는다. 이름 있는 출구·비밀문은 다음 이터레이션.
- 세계 설정 `WorldSettings`(행 하나): 시작 방. 관리자가 지정한다. 시작 방은 지울 수 없다. 시작 방이 없으면 게임에 들어갈 수 없다(`system/error` `아직 세계가 열리지 않았습니다. 관리자가 시작 방을 정해야 합니다.` 후 닫기 1013).
- 계정 `Account`: Google `sub`(유일), 이메일. 캐릭터 `PlayerCharacter`: 계정 id(유일 = 계정당 1개), 이름(유일), 현재 방 id.
- 스키마는 1차엔 `spring.jpa.hibernate.ddl-auto=update`(운영), 테스트는 기본 `create-drop`. 마이그레이션 도구(Flyway)는 다음 이터레이션. (제안)

**D11. 월드 상태·저장 시점·관리 변경 반영 (제안)**
- 메모리에 두는 것: 접속 중인 플레이어(캐릭터 id, 이름, 지금 방 id, 세션 id)와 WebSocket 세션 상태.
- 맵(지역·방·출구)은 메모리에 캐시하지 않고 명령을 처리할 때마다 DB에서 읽는다. 그래서 관리 툴에서 바꾼 맵은 **실행 중 게임에 바로 반영**된다.
- 캐릭터 위치는 **이동할 때마다** DB에 저장한다(접속 종료 때 따로 저장할 것이 없다). 다시 접속하면 저장된 방에서 시작하고, 그 방이 지워졌으면 시작 방에서 시작한다.
- 플레이어가 서 있는 방을 관리자가 지우면: DB의 그 방 캐릭터를 시작 방으로 옮기고, 접속 중이면 `system/info` `서 있던 곳이 사라져 시작 장소로 옮겨졌습니다.` 와 시작 방 `room` 을 보내며 시작 방 사람들에게 `appear` 알림.
- 게임 명령 처리는 서버 전체에서 한 번에 하나(공정한 `ReentrantLock` 하나)로 직렬화한다. 1차 사용자 수(소수)에는 충분하다.
- 같은 계정이 새로 접속하면 앞 연결에 `system/info` `다른 곳에서 접속해 이 연결을 끊습니다.` 를 보내고 4409로 닫는다. 캐릭터는 세계에 그대로 있고(나타난다/사라진다 알림 없음) 새 연결이 이어받아 지금 방 `room` 을 받는다.

**D12. 관리 REST API** — 모두 `/api/admin/**`, `ADMIN` 권한 필요(로그인 안 됨 401, 관리자 아님 403). 변경 요청에는 `X-MUD-Client: 1` 헤더(D2). 오류는 `ProblemDetail`(`title`, `status`, `detail`=한국어 메시지, `timestamp`, `exception`). 검증 실패·잘못된 값·없는 id = 400, 이름 중복·규칙 충돌 = 409.

| 메서드 · 경로 | 요청 | 응답 |
|---|---|---|
| `GET /api/admin/zones` | — | 200 `ZoneResponse[]` (이름순) |
| `POST /api/admin/zones` | `{name, description}` | 201 `ZoneResponse` |
| `PUT /api/admin/zones/{zoneId}` | `{name, description}` | 200 `ZoneResponse` |
| `DELETE /api/admin/zones/{zoneId}` | — | 204. 방이 있으면 409 |
| `GET /api/admin/zones/{zoneId}/rooms` | — | 200 `RoomSummaryResponse[]` (id순) |
| `POST /api/admin/rooms` | `{zoneId, name, description}` | 201 `RoomDetailResponse` |
| `GET /api/admin/rooms/{roomId}` | — | 200 `RoomDetailResponse` |
| `PUT /api/admin/rooms/{roomId}` | `{zoneId, name, description}` | 200 `RoomDetailResponse` |
| `DELETE /api/admin/rooms/{roomId}` | — | 204. 이 방으로 오는 출구도 지운다. 시작 방이면 409 |
| `PUT /api/admin/rooms/{roomId}/exits/{direction}` | `{targetRoomId, twoWay}` | 200 `RoomDetailResponse` |
| `DELETE /api/admin/rooms/{roomId}/exits/{direction}?twoWay=true` | — | 204 |
| `GET /api/admin/world` | — | 200 `WorldResponse` |
| `PUT /api/admin/world/start-room` | `{roomId}` | 200 `WorldResponse` |

- `ZoneResponse {id, name, description, roomCount}`, `RoomSummaryResponse {id, name, exitCount}`, `RoomDetailResponse {id, zoneId, zoneName, name, description, start, exits: [{direction, directionName, targetRoomId, targetRoomName, targetZoneName}]}`(exits는 방위 순서), `WorldResponse {startRoomId, startRoomName, startZoneName}`(없으면 셋 다 null).
- `{direction}` 경로 값은 `EAST WEST SOUTH NORTH NORTHEAST NORTHWEST SOUTHEAST SOUTHWEST UP DOWN`.
- **반대 방향 출구 자동 생성 (제안)**: `twoWay=true`(관리 화면 기본값)면 대상 방에 반대 방위 출구를 같이 만든다. 대상 방의 반대 방위 출구가 이미 이 방을 가리키면 그대로 두고, 다른 방을 가리키면 아무것도 바꾸지 않고 409. 삭제에서 `twoWay=true` 면 대상 방의 반대 출구가 이 방을 가리킬 때만 같이 지운다.

**D13. 인증 REST API**

| 메서드 · 경로 | 요청 | 응답 |
|---|---|---|
| `GET /api/auth/login?client=play\|admin` | — | 302 → `/oauth2/authorization/google` (세션에 `client` 저장) |
| `GET /oauth2/authorization/google`, `GET /login/oauth2/code/google` | — | Spring Security 기본 경로 (성공 시 302 → 클라이언트 주소, 실패 시 302 → 클라이언트 주소`?loginError=1`) |
| `GET /api/auth/me` | 세션 쿠키 | 200 `AuthMeResponse {id, email, admin, characterName}` (`characterName` 은 없으면 null). 로그인 안 됨 401 |
| `POST /api/auth/logout` | 세션 쿠키 | 204 (세션 무효화) |
| `POST /api/auth/dev` | `{email}` | 200 `AuthMeResponse` + 세션 쿠키 (D3 조건에서만 존재, 아니면 404) |

**D14. admin-console 화면** — 정적 export, 경로는 쿼리스트링으로 id를 받는다.
- `/` 로그인(`Google로 로그인` 버튼 = `${worldServerUrl}/api/auth/login?client=admin` 으로 이동, dev login 폼, `?loginError=1` 이면 `로그인하지 못했습니다. 다시 시도하십시오.`). 로그인 후 관리자가 아니면 `관리자만 쓸 수 있습니다.` 와 로그아웃 버튼.
- `/zones/` 지역 목록(이름·방 수) + 지역 만들기 폼 + 각 지역 수정·삭제.
- `/zone/?id=1` 지역 하나: 방 목록(이름·출구 수, 시작 방 표시) + 방 만들기 폼.
- `/room/?id=3` 방 하나: 이름·묘사·지역 수정, 삭제, "시작 방으로 지정" 버튼, 출구 표(방위·대상 방(지역)·삭제), 출구 추가 폼(방위 선택 → 대상 지역 선택 → 대상 방 선택, "반대 방향도" 체크박스 기본 켜짐).
- 위쪽 공통 바: 현재 시작 방 표시, 지역 목록 링크, 로그아웃.

**D15. 루트 패키지·헥사고날 배치** — `<base>` = `mudmk3`. 애그리거트(슬라이스): `zone`, `room`, `world`, `account`, `character`, `game`(메모리 월드). 슬라이스 의존: `room → zone`, `world → room`, `character → world`, `game → character, room, world` (순환 없음). 메모리 저장소 구현은 `adapter/persistence/memory` 에 둔다(architecture.md "메모리 구현은 adapter").

**D16. 배포** — 모듈마다 `Dockerfile`(world-server: JDK 빌드 → JRE 실행 / 프론트: node 빌드 → nginx 정적 서빙). 저장소 루트 `compose.yaml` 이 `postgres`, `world-server`, `play-client`, `admin-console` 을 띄운다. 로컬 개발용 DB만 띄우는 `world-server/compose.yaml`(build.md 규칙, `spring-boot-docker-compose`)도 둔다. 프론트의 `NEXT_PUBLIC_*` 값은 빌드 때 박히므로 compose `build.args` 로 넘긴다.

**D17. 환경변수**

| 이름 | 쓰는 곳 | 기본값 / 예 |
|---|---|---|
| `GOOGLE_CLIENT_ID` | world-server (`spring.security.oauth2.client.registration.google.client-id`) | 없으면 `not-configured` 로 떠서 앱은 뜨지만 Google 로그인은 실패한다(개발용 로그인은 됨). 시작할 때 경고 로그 |
| `GOOGLE_CLIENT_SECRET` | world-server (`...google.client-secret`) | 위와 같음 |
| `MUD_ADMIN_EMAILS` | world-server | `a@example.com,b@example.com` |
| `MUD_PLAY_CLIENT_URL` | world-server (로그인 뒤 돌아갈 곳) | `http://localhost:3000/` |
| `MUD_ADMIN_CONSOLE_URL` | world-server (로그인 뒤 돌아갈 곳) | `http://localhost:3001/` |
| `MUD_CORS_ALLOWED_ORIGINS` | world-server (REST CORS + WebSocket Origin) | `http://localhost:3000,http://localhost:3001` |
| `MUD_DEV_LOGIN_ENABLED` | world-server (`local` 프로필에서만 의미) | `false` |
| `SPRING_PROFILES_ACTIVE` | world-server | 비움(운영) / `local` |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | world-server | compose: `jdbc:postgresql://postgres:5432/mud`, `mud`, `${POSTGRES_PASSWORD}` |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | postgres | `mud`, `mud`, (.env) |
| `NEXT_PUBLIC_WORLD_SERVER_URL` | 두 클라이언트(빌드 때) | `http://localhost:8080` (WebSocket 주소는 `http`→`ws` 로 바꿔 `/ws/game` 을 붙임) |
| `NEXT_PUBLIC_DEV_LOGIN` | 두 클라이언트(빌드 때) | `false` |

- 클라이언트에는 Google 값이 필요 없다(로그인은 모두 world-server가 한다).

**D18. play-client 입력 보조** — ↑↓ 히스토리(최대 100개, 바로 앞과 같은 입력은 다시 넣지 않음, ↓ 끝에서 빈 입력). 탭 자동완성: 첫 낱말이면 1차 명령어 목록에서, 그 뒤 낱말이면 지금 방 사람 이름에서 앞부분이 맞는 후보를 고른다. 탭을 거듭 누르면 후보를 차례로 돈다. 이름 후보는 마지막 `room.occupants` 로 정하고 `notice` 의 `arrive`/`appear` 는 더하고 `leave`/`disappear` 는 뺀다. 연결이 끊기면 자동 재접속하지 않고 "다시 연결" 버튼을 보인다.

## Review Focus

스펙이 직접 말하지 않지만 실제로 부딪칠 만한 것. 각 줄의 테스트는 담당 태스크에 들어 있다.

1. 같은 계정으로 탭 두 개 접속 → 앞 연결은 4409로 닫히고, 방 목록에 같은 캐릭터가 둘 보이지 않으며, 앞 연결이 닫힐 때 `사라진다` 알림이 나가지 않는다. (Task 6 `reconnectSameAccountReplacesOldSession`, Task 7 `secondConnectionClosesFirstWith4409`)
2. 브라우저 탭을 그냥 닫음(종료 명령 없이 연결 끊김) → 같은 방 사람에게 `사라진다` 알림, `누구` 목록에서 빠짐. (Task 7 `abruptCloseRemovesPresence`)
3. 플레이어가 서 있는 방을 관리자가 지움 → 시작 방으로 옮겨지고 다음 명령이 정상 동작. (Task 6 `roomDeletedWhileOccupiedMovesPlayerToStartRoom`)
4. 반대 방향 출구 충돌·자기 자신으로 가는 출구 → 409/400이고 아무것도 바뀌지 않음. (Task 3 `putExitTwoWayConflictFail`, `putExitToSelfFail`)
5. 시작 방을 아직 안 정한 세계에 접속 → 안내 후 1013으로 닫히고 서버 오류가 나지 않음. (Task 6 `connectWithoutStartRoomFail`)

---

## 파일 구조 (요약)

```
compose.yaml  .env.example  README.md  scripts/smoke-test.mjs
world-server/
  build.gradle.kts settings.gradle.kts gradlew* gradle/ spotbugs-exclude-filter.xml compose.yaml Dockerfile .dockerignore .gitignore
  src/main/java/mudmk3/
    WorldServerApplication.java
    domain/{shared,zone,room,world,account,character,game}/
    application/{zone,room,world,account,character,game}/{provided,required}/
    adapter/{webapi,webapi/dto,websocket,security,persistence/memory}/
    support/{stereotype,exception,config}/
  src/main/resources/{application.yml,application-local.yml,META-INF/orm.xml,instancio.properties}
  src/test/java/mudmk3/... (패키지는 대상과 같게)  src/test/resources/{application.yml,junit-platform.properties}
play-client/  admin-console/
  package.json package-lock.json .nvmrc next.config.ts tsconfig.json vitest.config.ts vitest.setup.ts Dockerfile nginx.conf .dockerignore .gitignore
  app/  src/lib/  src/components/
```

---

### Task 1: world-server 골격 — 빌드·공통 지원 코드·ArchUnit

**권장 모델:** opus

**목적:** 이후 모든 world-server 태스크가 올라설 Gradle 프로젝트, 공통 스테레오타입·엔티티 기반·테스트 기반, 아키텍처 검사를 만든다.

**Files:**
- Create: `world-server/settings.gradle.kts` (`rootProject.name = "world-server"`), `world-server/build.gradle.kts`, Gradle wrapper(`gradlew`, `gradlew.bat`, `gradle/wrapper/*`), `world-server/.gitignore`(`build/`, `.gradle/`, `.idea/`, `*.iml`), `world-server/spotbugs-exclude-filter.xml`
- Create: `world-server/compose.yaml` — 로컬 개발 DB: `postgres:18.6-alpine`, `POSTGRES_DB=mud`, `POSTGRES_USER=mud`, `POSTGRES_PASSWORD=mud`, 포트 `5432:5432`, 볼륨은 `/var/lib/postgresql` 에 마운트(PostgreSQL 18 이미지의 데이터 경로 변경 때문)
- Create: `src/main/java/mudmk3/WorldServerApplication.java`
- Create: `src/main/java/mudmk3/support/stereotype/{Adapter,WebApiAdapter,ApplicationService,ValidatedApplicationService}.java`
- Create: `src/main/java/mudmk3/support/exception/ValidationException.java` (`List<String> errors` 를 가진 RuntimeException)
- Create: `src/main/java/mudmk3/domain/AbstractEntity.java`, `src/main/java/mudmk3/domain/package-info.java`, `src/main/java/mudmk3/application/package-info.java` (둘 다 `@NullMarked`)
- Create: `src/main/resources/application.yml`, `application-local.yml`, `META-INF/orm.xml`(빈 entity-mappings, `<access>FIELD</access>`), `instancio.properties`(`bean.validation.enabled=true`)
- Create: `src/test/resources/junit-platform.properties`(`spring.test.constructor.autowire.mode=all`), `src/test/resources/application.yml`
- Create: `src/test/java/mudmk3/ApplicationServiceTest.java`(메타 애노테이션 `@ApplicationServiceTest` = `@SpringBootTest` + `@Transactional` + `@Import(TestFakesConfiguration.class)`), `src/test/java/mudmk3/TestFakesConfiguration.java`(`@TestConfiguration`, 지금은 비어 있음 — 뒤 태스크가 가짜 빈을 더함)
- Test: `src/test/java/mudmk3/HexagonalArchitectureTest.java`, `src/test/java/mudmk3/WorldServerApplicationTest.java`

**Interfaces:**
- Produces: 패키지 `mudmk3.{domain,application,adapter,support}`; `@Adapter`(= `@Component`), `@WebApiAdapter`(= `@Adapter` + `@RestController`), `@ApplicationService`(= `@Service` + `@Transactional`), `@ValidatedApplicationService`(= `@ApplicationService` + `@Validated`); `AbstractEntity`(`@MappedSuperclass`, `@Id @GeneratedValue(IDENTITY) Long id`, `@Nullable getId()`, HibernateProxy 안전 `final equals/hashCode`); `@ApplicationServiceTest`; `TestFakesConfiguration`.

- [ ] **Step 1: wrapper 만들기** — `cd world-server && gradle wrapper --gradle-version 9.9.1 --distribution-type bin` 후 `./gradlew --version` 이 `Gradle 9.9.1` 을 출력하는지 확인.
- [ ] **Step 2: `build.gradle.kts`** — `.claude/rules/build.md` 그대로: 플러그인 `java`, `org.springframework.boot` 4.1.2, `io.spring.dependency-management` 1.1.7, `com.github.spotbugs` 6.5.13; toolchain 25; 메인 의존성 `spring-boot-starter-data-jpa`, `spring-boot-starter-webmvc`, `spring-boot-starter-websocket`, `spring-boot-starter-validation`, Lombok; `runtimeOnly("org.postgresql:postgresql")`; `developmentOnly("org.springframework.boot:spring-boot-docker-compose")`; 테스트 의존성(`spring-boot-starter-test`, `-webmvc-test`, `-data-jpa-test`, ArchUnit 1.5.1, Instancio 6.2.1, `testRuntimeOnly("com.h2database:h2")`, `junit-platform-launcher`); `mockitoAgent` configuration과 `-javaagent`; SpotBugs `excludeFilter`. 보안 의존성은 Task 4에서 더한다.
- [ ] **Step 3: 설정 파일**
  - `application.yml`: `spring.application.name=world-server`, `server.port=8080`, `spring.jpa.hibernate.ddl-auto=update`, `spring.jpa.open-in-view=false`, `spring.docker.compose.enabled=false`(기본 꺼짐), `mud.cors.allowed-origins=${MUD_CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:3001}`.
  - `application-local.yml`: `spring.docker.compose.enabled=true`, `spring.docker.compose.file=compose.yaml`(`./gradlew bootRun --args='--spring.profiles.active=local'` 로 DB를 같이 띄움).
  - 테스트 `application.yml`: H2(`jdbc:h2:mem:mud;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`), `ddl-auto=create-drop`.
- [ ] **Step 4: 실패하는 테스트** — `WorldServerApplicationTest.contextLoads()`(`@SpringBootTest`), `HexagonalArchitectureTest`: `@AnalyzeClasses(packages = "mudmk3", importOptions = DoNotIncludeTests.class)`, `@ArchTest hexagonalArchitecture`(`layeredArchitecture().consideringAllDependencies()` — Domain `mudmk3.domain..`, Application `mudmk3.application..`, Adapter `mudmk3.adapter..`; Domain은 Application·Adapter만 접근 가능, Application은 Adapter만, Adapter는 아무도 접근 불가. `support` 와 루트(`WorldServerApplication`)는 레이어 밖으로 두고 위반으로 보지 않게 한다), `@ArchTest domainSlicesFreeOfCycles`(`slices().matching("mudmk3.domain.(*)..")`), `@ArchTest applicationSlicesFreeOfCycles`. 클래스가 아직 없어 실패하는 것을 `./gradlew test` 로 확인(빈 레이어는 `withOptionalLayers(true)` 로 허용).
- [ ] **Step 5: 구현** — 위 파일들. `AbstractEntity` 의 equals/hashCode는 참고 레포 관례(HibernateProxy면 실제 클래스 비교, id가 null이면 다름).
- [ ] **Step 6: 검증** — `cd world-server && ./gradlew build` → `BUILD SUCCESSFUL`(테스트·SpotBugs 통과).
- [ ] **Step 7: 커밋** — `build(world-server): Gradle 골격·공통 스테레오타입·ArchUnit 검사` + progress.md 한 줄.

**완료 조건:** `./gradlew build` 통과, `./gradlew --version` 이 9.9.1, `HexagonalArchitectureTest` 3개 규칙이 돈다.

---

### Task 2: 맵 도메인과 영속 — 존(지역)·방·출구·세계 설정

**권장 모델:** opus

**목적:** 지역·방·출구·시작 방을 도메인 규칙과 JPA 매핑(orm.xml)·리포지토리로 만든다.

**Files:**
- Create: `domain/zone/{Zone,ZoneSaveInfo,DuplicateZoneNameException}.java`
- Create: `domain/room/{Room,Exit,Direction,RoomSaveInfo}.java`
- Create: `domain/world/WorldSettings.java`
- Modify: `src/main/resources/META-INF/orm.xml`
- Create: `application/zone/required/ZoneRepository.java`, `application/room/required/RoomRepository.java`, `application/world/required/WorldSettingsRepository.java`
- Test: `domain/zone/ZoneTest`, `domain/room/RoomTest`, `domain/room/DirectionTest`, `domain/world/WorldSettingsTest`, `domain/zone/ZoneFixture`, `domain/room/RoomFixture`, `application/zone/required/ZoneRepositoryTest`, `application/room/required/RoomRepositoryTest`, `application/world/required/WorldSettingsRepositoryTest` (모든 경로는 `world-server/src/{main,test}/java/mudmk3/` 아래)

**Interfaces:**
- Produces:
  - `enum Direction { EAST("동","e"), WEST("서","w"), SOUTH("남","s"), NORTH("북","n"), NORTHEAST("북동","ne"), NORTHWEST("북서","nw"), SOUTHEAST("남동","se"), SOUTHWEST("남서","sw"), UP("위","u"), DOWN("아래","d") }` — 선언 순서가 표시 순서. `String koreanName()`, `String abbreviation()`, `Direction opposite()`, `String towardPhrase()`(`동쪽으로`…`북동쪽으로`, `위로`, `아래로`), `String fromPhrase()`(`동쪽에서`…, `위에서`, `아래에서`), `static Optional<Direction> fromCommand(String word)`(한글 이름 또는 줄임말, 영문은 대소문자 무시).
  - `record ZoneSaveInfo(@NotBlank @Size(max=30) String name, @Size(max=500) @Nullable String description)` (null 묘사는 `""` 로).
  - `Zone`: `static Zone create(ZoneSaveInfo)`, `void update(ZoneSaveInfo)`, `getName()`, `getDescription()`. 이름은 앞뒤 공백을 잘라 저장.
  - `record RoomSaveInfo(@NotBlank @Size(max=30) String name, @NotBlank @Size(max=1000) String description)`.
  - `Room`: `static Room create(Zone zone, RoomSaveInfo)`, `void update(Zone zone, RoomSaveInfo)`, `void setExit(Direction, Room target)`(같은 방위가 있으면 대상 교체, `target == this` 면 `IllegalArgumentException("자기 자신으로 가는 출구는 만들 수 없습니다")`), `void removeExit(Direction)`, `Optional<Exit> findExit(Direction)`, `List<Exit> getExits()`(방위 선언 순서로 정렬된 불변 리스트), `getZone()`, `getName()`, `getDescription()`.
  - `Exit`(Room 애그리거트 내부 엔티티): `getDirection()`, `getTarget()`; 변경 메서드는 package-private.
  - `WorldSettings`: `static WorldSettings create()`, `void designateStartRoom(Room room)`, `Optional<Room> findStartRoom()`, `boolean isStartRoom(Room room)`.
  - `ZoneRepository extends Repository<Zone, Long>`: `Zone save(Zone)`, `Optional<Zone> findById(Long)`, `List<Zone> findAllByOrderByNameAsc()`, `boolean existsByName(String)`, `boolean existsByNameAndIdNot(String, Long)`, `void delete(Zone)`.
  - `RoomRepository extends Repository<Room, Long>`: `Room save(Room)`, `Optional<Room> findById(Long)`, `List<Room> findAllByZoneOrderByIdAsc(Zone)`, `long countByZone(Zone)`, `@Query List<Room> findAllWithExitTo(Room target)`(그 방으로 가는 출구를 가진 방들), `void delete(Room)`.
  - `WorldSettingsRepository extends Repository<WorldSettings, Long>`: `WorldSettings save(WorldSettings)`, `Optional<WorldSettings> findFirstByOrderByIdAsc()`.
- 매핑(orm.xml): 테이블 `zone`(`name` varchar(30) not null, `UK_ZONE_NAME`; `description` varchar(500) not null), `room`(`zone_id` FK not null, `name` varchar(30), `description` varchar(1000)), `room_exit`(`room_id`, `direction` STRING varchar(16), `target_room_id`, `UK_ROOM_EXIT_ROOM_ID_DIRECTION`), `world_settings`(`start_room_id` nullable). `Room.exits` 는 `@OneToMany(mappedBy="room")` cascade ALL + orphanRemoval.

- [ ] **Step 1: 도메인 테스트 작성** — `DirectionTest`: `opposite`(NORTH↔SOUTH, NORTHEAST↔SOUTHWEST, UP↔DOWN 등 10개 모두 서로 반대), `fromCommand`(`"북"`→NORTH, `"N"`→NORTH, `"ne"`→NORTHEAST, `"아래"`→DOWN, `"북쪽"`→empty, `"말"`→empty), `towardPhrase`(NORTH→`북쪽으로`, UP→`위로`), `fromPhrase`(SOUTH→`남쪽에서`, DOWN→`아래에서`). `RoomTest`: `setExit` 로 같은 방위 두 번 넣으면 하나로 교체, `setExitToSelfFail`, `removeExit`, `getExits` 순서가 `동 서 남 북 북동 북서 남동 남서 위 아래`. `ZoneTest`: create/update가 이름 공백을 자름, 묘사 null→`""`. `WorldSettingsTest`: designate 전 `findStartRoom` empty, 후 그 방.
- [ ] **Step 2: 실패 확인** — `./gradlew test --tests 'mudmk3.domain.*'` → 컴파일 실패.
- [ ] **Step 3: 도메인·orm.xml·리포지토리 구현.**
- [ ] **Step 4: 리포지토리 테스트(`@DataJpaTest`)** — `ZoneRepositoryTest.duplicateNameFail`(같은 이름 두 번 저장 후 flush → `DataIntegrityViolationException`), `findAllByOrderByNameAsc`; `RoomRepositoryTest.saveWithExits`(flush·clear 후 다시 읽어 출구·대상이 유지), `findAllWithExitTo`(A→B, C→B 출구가 있으면 B로 [A, C]), `countByZone`; `WorldSettingsRepositoryTest.startRoomPersisted`.
- [ ] **Step 5: 검증** — `cd world-server && ./gradlew build` 통과.
- [ ] **Step 6: 커밋** — `feat(world-server): 존(지역)·방·출구·시작 방 도메인과 영속 매핑`.

**완료 조건:** 위 테스트 통과, ArchUnit 통과, orm.xml에 테이블·컬럼·유니크 제약 이름이 위 값대로.

---

### Task 3: 맵 관리 서비스와 관리 REST API

**권장 모델:** opus

**목적:** admin-console이 부를 지역·방·출구·시작 방 관리 API를 만든다. (보안은 Task 4에서 씌운다 — 이 태스크 끝에서는 인증 없이 열려 있다.)

**Files:**
- Create: `application/zone/provided/{ZoneFinder,ZoneEditor}.java`, `application/zone/{ZoneQueryService,ZoneModifyService}.java`, `application/zone/provided/ZoneNotEmptyException.java`
- Create: `application/room/provided/{RoomFinder,RoomEditor,RoomDeletedEvent,ExitConflictException,StartRoomDeletionException,ExitSaveRequest,RoomSaveRequest}.java`, `application/room/required/StartRoomChecker.java`, `application/room/{RoomQueryService,RoomModifyService}.java`, `adapter/persistence/StartRoomCheckerAdapter.java`
- Create: `application/world/provided/{WorldSettingsFinder,WorldSettingsEditor}.java`, `application/world/{WorldSettingsQueryService,WorldSettingsModifyService}.java`
- Create: `application/zone/provided/ZoneSaveRequest.java`
- Create: `adapter/webapi/{ZoneApi,RoomApi,WorldApi,ApiControllerAdvice}.java`, `adapter/webapi/dto/{ZoneResponse,RoomSummaryResponse,RoomDetailResponse,ExitResponse,WorldResponse,StartRoomRequest}.java`
- Test: `application/zone/provided/ZoneEditorTest`, `application/room/provided/RoomEditorTest`, `application/world/provided/WorldSettingsEditorTest`, `adapter/webapi/{ZoneApiWebMvcTest,RoomApiWebMvcTest,WorldApiWebMvcTest}`

**Interfaces:**
- Consumes: Task 2의 도메인·리포지토리.
- Produces (provided 포트, 각 인터페이스에 한 줄 Javadoc):
  - `ZoneFinder`: `Zone find(Long zoneId)`, `List<Zone> findAll()`(이름순), `long countRooms(Long zoneId)`.
  - `ZoneEditor`: `Zone create(@Valid ZoneSaveRequest)`, `Zone update(Long zoneId, @Valid ZoneSaveRequest)`, `void delete(Long zoneId)`. 이름 중복 → `DuplicateZoneNameException("이미 있는 지역 이름입니다: " + name)`, 방이 있으면 `ZoneNotEmptyException("방이 남아 있는 지역은 지울 수 없습니다")`.
  - `RoomFinder`: `Room find(Long roomId)`, `Optional<Room> findOptional(Long roomId)`, `List<Room> findAllInZone(Long zoneId)`.
  - `RoomEditor`: `Room create(@Valid RoomSaveRequest)`, `Room update(Long roomId, @Valid RoomSaveRequest)`, `void delete(Long roomId)`, `Room putExit(Long roomId, Direction, @Valid ExitSaveRequest)`, `void deleteExit(Long roomId, Direction, boolean twoWay)`.
    - `delete`: 시작 방이면 `StartRoomDeletionException("시작 방은 지울 수 없습니다")`; 이 방으로 오는 출구를 모두 지운 뒤 방을 지우고, 같은 트랜잭션 안에서 `ApplicationEventPublisher.publishEvent(new RoomDeletedEvent(roomId))`(동기 `@EventListener` 가 Task 6에서 붙는다).
    - `putExit`: 대상 방이 없으면 `IllegalArgumentException`; `twoWay` 이고 대상 방의 반대 방위 출구가 다른 방을 가리키면 `ExitConflictException("반대 방향 출구가 이미 다른 방으로 이어져 있습니다: " + 대상방이름 + " " + 반대방위한글)` 로 아무것도 바꾸지 않는다.
  - `record RoomSaveRequest(@NotNull Long zoneId, @NotBlank @Size(max=30) String name, @NotBlank @Size(max=1000) String description)` + `toInfo()`; `record ExitSaveRequest(@NotNull Long targetRoomId, boolean twoWay)`; `record ZoneSaveRequest(@NotBlank @Size(max=30) String name, @Size(max=500) @Nullable String description)` + `toInfo()`; `record RoomDeletedEvent(Long roomId)`.
  - `WorldSettingsFinder`: `Optional<Room> findStartRoom()`, `boolean isStartRoom(Long roomId)`. `WorldSettingsEditor`: `WorldSettings designateStartRoom(Long roomId)`(행이 없으면 만든다).
  - 슬라이스 순환 주의: `RoomModifyService` 가 world 슬라이스를 직접 쓰면 `world → room → world` 순환이 된다. 그래서 `RoomModifyService.delete` 는 자기 슬라이스의 required 포트 `StartRoomChecker`(`boolean isStartRoom(Long roomId)`)를 쓰고, 구현 `adapter/persistence/StartRoomCheckerAdapter`(`@Adapter`)가 `WorldSettingsRepository` 로 답한다. 의존은 `world → room` 하나뿐이다.
- REST(D12 표 그대로). 응답 DTO는 `record` + `static of(...)`. `RoomDetailResponse.start` 는 `WorldSettingsFinder.isStartRoom` 으로 채운다. `ZoneResponse.roomCount` 는 `ZoneFinder.countRooms`.
- `ApiControllerAdvice extends ResponseEntityExceptionHandler`: `DuplicateZoneNameException`·`ZoneNotEmptyException`·`ExitConflictException`·`StartRoomDeletionException` → 409, `IllegalArgumentException`·`ValidationException`·`ConstraintViolationException` → 400, 그 밖 `Exception` → 500. `ProblemDetail` 에 `timestamp`, `exception`(단순 클래스 이름) 속성.

- [ ] **Step 1: 서비스 테스트(`@ApplicationServiceTest`)** — `ZoneEditorTest`: `create`, `duplicateNameFail`, `update`, `updateToOtherZoneNameFail`, `deleteNotEmptyFail`, `delete`. `RoomEditorTest`: `create`, `update`(지역 옮기기 포함), `putExitTwoWay`(A 북→B 이면 B 남→A 생김), `putExitTwoWayKeepsExistingBackExit`, `putExitTwoWayConflictFail`(B의 남이 C를 가리키면 409 예외, A의 출구도 생기지 않음), `putExitToSelfFail`, `putExitOneWay`(B에 반대 출구 안 생김), `deleteExitTwoWay`(B 남→A 도 지움), `deleteExitTwoWayKeepsUnrelatedBackExit`(B 남→C 는 그대로), `deleteRemovesIncomingExits`, `deleteStartRoomFail`, `deletePublishesRoomDeletedEvent`(`@RecordApplicationEvents` 로 확인). `WorldSettingsEditorTest`: `designateStartRoom`, `designateAgainReplaces`.
- [ ] **Step 2: 실패 확인 후 서비스 구현** — `./gradlew test` 실패 확인 → 구현 → 통과.
- [ ] **Step 3: 웹 어댑터 테스트(`@WebMvcTest` + `MockMvcTester` + `@MockitoBean` 포트)** — `ZoneApiWebMvcTest`: `POST /api/admin/zones` 201·본문 `{id,name,description,roomCount}`, 빈 이름 400, 중복 409 `ProblemDetail.detail` 한국어, `DELETE` 204. `RoomApiWebMvcTest`: `GET /api/admin/rooms/{id}` 의 `exits[0].directionName == "북"`, `PUT .../exits/NORTH` 200, 잘못된 방위 `.../exits/NORTHH` 400, 충돌 409. `WorldApiWebMvcTest`: `GET /api/admin/world` 시작 방 없으면 세 필드 null, `PUT /api/admin/world/start-room` 200.
- [ ] **Step 4: 컨트롤러·DTO·advice 구현 후 통과 확인.**
- [ ] **Step 5: 검증** — `./gradlew build` 통과.
- [ ] **Step 6: 커밋** — `feat(world-server): 지역·방·출구·시작 방 관리 REST API`.

**완료 조건:** D12 표의 13개 엔드포인트가 동작하고 위 테스트 통과, ArchUnit(슬라이스 순환 없음) 통과.

---

### Task 4: 인증 — 계정, Google OAuth2 Login, 세션, 관리자, 보안 설정

**권장 모델:** opus

**목적:** D1·D2·D3·D13을 구현하고 `/api/admin/**` 에 관리자 권한을 씌운다.

**Files:**
- Modify: `world-server/build.gradle.kts` — `implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")`, `testImplementation("org.springframework.boot:spring-boot-starter-security-test")`
- Create: `domain/shared/Email.java`(record, 형식 검증, 소문자로 정규화), `domain/account/{Account,AccountRegisterInfo}.java`
- Create: `application/account/provided/{AccountLogin,AccountFinder,AccountPrincipal,AccountLoginRequest}.java`, `application/account/required/AccountRepository.java`, `application/account/{AccountLoginService,AccountQueryService}.java`
- Create: `support/config/AuthProperties.java`(`@ConfigurationProperties("mud.auth")` record: `List<String> adminEmails`, `String playClientUrl`, `String adminConsoleUrl`, `DevLogin devLogin(boolean enabled)`), `support/config/CorsProperties.java`(`mud.cors.allowed-origins: List<String>`)
- Create: `adapter/security/{SecurityConfig,GoogleOidcUserService,LoginClientRedirect,LoginSuccessHandler,LoginFailureHandler,ClientHeaderCsrfFilter,SessionAuthenticator}.java`
- Create: `adapter/webapi/{AuthApi,DevAuthApi}.java`, `adapter/webapi/dto/{AuthMeResponse,DevLoginRequest}.java`
- Modify: `application.yml`, `application-local.yml`, 테스트 `application.yml`, `ApiControllerAdvice`(`AccessDeniedException` 은 Spring Security가 처리하도록 advice에서 잡지 않는다)
- Modify: Task 3의 세 `*ApiWebMvcTest` — 보안 필터를 켠 상태로 관리자 사용자(`AccountPrincipal(…, admin=true)` + `ROLE_ADMIN`)와 `X-MUD-Client: 1` 헤더를 주고, 권한 없는 경우 테스트를 더한다
- Test: `domain/shared/EmailTest`, `domain/account/AccountTest`, `application/account/provided/AccountLoginTest`, `application/account/required/AccountRepositoryTest`, `adapter/security/SecurityIntegrationTest`, `adapter/security/DevLoginIntegrationTest`, `adapter/security/LoginSuccessHandlerTest`

**Interfaces:**
- Produces:
  - `Account`: `static Account register(AccountRegisterInfo(String googleSubject, Email email))`, `void changeEmail(Email)`, getter. 테이블 `account`(`google_subject` varchar(255) `UK_ACCOUNT_GOOGLE_SUBJECT`, `email` embeddable → 컬럼 `email_address` varchar(320)).
  - `AccountRepository`: `Account save(Account)`, `Optional<Account> findById(Long)`, `Optional<Account> findByGoogleSubject(String)`.
  - `record AccountLoginRequest(@NotBlank String googleSubject, @NotBlank @jakarta.validation.constraints.Email String email)`.
  - `record AccountPrincipal(Long accountId, String email, boolean admin) implements java.security.Principal, java.io.Serializable` — `getName()` 은 `String.valueOf(accountId)`. 세션에 저장되므로 Serializable.
  - `AccountLogin`(provided, `/** Google 또는 개발용 로그인으로 계정을 찾거나 만든다 */`): `AccountPrincipal login(@Valid AccountLoginRequest)` — subject로 찾고 없으면 만들고, 이메일이 바뀌었으면 고치고, 관리자 여부는 `AuthProperties.adminEmails`(대소문자 무시)로 정한다.
  - `AccountFinder`(provided): `Account find(Long accountId)`.
  - `GoogleOidcUserService extends OidcUserService`(`@Adapter`): `loadUser` 에서 `email_verified` 가 true가 아니면 `OAuth2AuthenticationException`; `AccountLogin.login(new AccountLoginRequest(sub, email))` → 반환 `OidcUser` 는 `DefaultOidcUser` 를 감싸되 권한 `ROLE_PLAYER`(+ 관리자면 `ROLE_ADMIN`)와 속성 `accountPrincipal` 을 갖는 `MudOidcUser`(같은 파일 안 클래스 또는 `adapter/security/MudOidcUser.java`, `AccountPrincipal getAccountPrincipal()`).
  - `SessionAuthenticator`(`@Adapter`): `void signIn(AccountPrincipal, HttpServletRequest, HttpServletResponse)` — `UsernamePasswordAuthenticationToken.authenticated(principal, null, 권한)` 으로 `SecurityContext` 를 만들어 `HttpSessionSecurityContextRepository` 에 저장(세션 고정 방지로 세션 id 교체). 개발용 로그인이 쓴다. 그리고 `static Optional<AccountPrincipal> principalOf(Authentication)` — `AccountPrincipal` 이면 그대로, `MudOidcUser` 면 꺼낸다(REST·WebSocket이 공통으로 쓴다).
  - `LoginClientRedirect`: `GET /api/auth/login?client=` 처리(컨트롤러는 `AuthApi` 에 두고 이 클래스는 `client` → URL 결정 로직: `"admin"` → `adminConsoleUrl`, 그 밖 → `playClientUrl`, 세션 속성 이름 `MUD_LOGIN_CLIENT`).
  - `LoginSuccessHandler`/`LoginFailureHandler`: D1 3단계 리다이렉트(실패는 `?loginError=1`).
  - `ClientHeaderCsrfFilter`(`OncePerRequestFilter`): `/api/**` 의 `POST`/`PUT`/`DELETE`/`PATCH` 에 `X-MUD-Client: 1` 이 없으면 403 `ProblemDetail`(`detail`: `허용되지 않은 요청입니다.`).
  - `SecurityConfig`: `oauth2Login`(`userInfoEndpoint.oidcUserService(GoogleOidcUserService)`, 성공·실패 처리기), CORS(`CorsProperties` 오리진, `allowCredentials(true)`, 메서드 GET/POST/PUT/DELETE/OPTIONS, 헤더 `Content-Type`·`X-MUD-Client`), Spring 기본 CSRF 끔 + `ClientHeaderCsrfFilter` 추가, `/api/**` 의 인증 실패는 `HttpStatusEntryPoint(UNAUTHORIZED)`(Google로 리다이렉트하지 않음), 로그아웃은 `AuthApi` 가 직접 세션 무효화. 허용: `GET /api/auth/login`, `POST /api/auth/dev`, `/oauth2/**`, `/login/oauth2/**`, `/ws/**`(인증은 핸드셰이크에서 따로), `OPTIONS /**`. `/api/admin/**` → `hasRole("ADMIN")`. 나머지 `authenticated()`.
  - `AuthApi`: `GET /api/auth/login`, `GET /api/auth/me`(`AuthMeResponse {id, email, admin, characterName}` — `characterName` 은 이 태스크에서는 항상 null, Task 5가 채운다), `POST /api/auth/logout`(204). `DevAuthApi`: `POST /api/auth/dev` — 클래스에 `@Profile("local")` + `@ConditionalOnProperty(name = "mud.auth.dev-login.enabled", havingValue = "true")`.
- 설정(`application.yml`): `spring.security.oauth2.client.registration.google.client-id=${GOOGLE_CLIENT_ID:not-configured}`, `client-secret=${GOOGLE_CLIENT_SECRET:not-configured}`, `scope=openid,profile,email`; `server.servlet.session.cookie.same-site=lax`, `http-only=true`; `mud.auth.admin-emails=${MUD_ADMIN_EMAILS:}`, `mud.auth.play-client-url=${MUD_PLAY_CLIENT_URL:http://localhost:3000/}`, `mud.auth.admin-console-url=${MUD_ADMIN_CONSOLE_URL:http://localhost:3001/}`, `mud.auth.dev-login.enabled=false`. `GOOGLE_CLIENT_ID` 가 `not-configured` 면 시작 시 경고 로그 `Google 로그인이 설정되지 않았습니다 (GOOGLE_CLIENT_ID)`. `application-local.yml`: `mud.auth.dev-login.enabled=${MUD_DEV_LOGIN_ENABLED:false}`. 테스트 `application.yml`: `client-id=test-client-id`, `client-secret=test-client-secret`, `admin-emails=admin@example.com`.
- Google 대역: 실제 Google 호출은 테스트하지 않는다. 성공 경로는 `GoogleOidcUserService` 의 계정 처리 부분을 `OidcUserRequest` 대신 클레임 맵으로 부르는 package-private 메서드(`MudOidcUser toMudUser(OidcIdToken idToken)`)로 단위 테스트하고, 세션 이후 경로는 `spring-security-test` 의 `SecurityMockMvcRequestPostProcessors.oidcLogin()` / `authentication(...)` 으로 만든 인증으로 테스트한다.

- [ ] **Step 1: 도메인·리포지토리 테스트** — `EmailTest`(형식 오류 `IllegalArgumentException`, `Admin@Example.COM` → `admin@example.com`), `AccountTest`(register·changeEmail), `AccountRepositoryTest`(`findByGoogleSubject`, 같은 subject 두 번 → 유니크 위반).
- [ ] **Step 2: 서비스 테스트** — `AccountLoginTest`: `loginCreatesAccount`, `loginReusesAccountAndUpdatesEmail`, `adminEmailGetsAdmin`(`ADMIN@example.com` 도 관리자), `nonAdminEmail`, `devSubjectAccount`(`dev:a@example.com`).
- [ ] **Step 3: 보안 어댑터 테스트** — `GoogleOidcUserServiceTest`: `email_verified=false` 면 `OAuth2AuthenticationException`, 관리자 이메일이면 권한에 `ROLE_ADMIN`. `LoginSuccessHandlerTest`: 세션 `MUD_LOGIN_CLIENT=admin` → `http://localhost:3001/`, 없음 → `http://localhost:3000/`, `client=https://evil.example` → `http://localhost:3000/`.
- [ ] **Step 4: 보안 통합 테스트(`@SpringBootTest` + `MockMvcTester`)** — `SecurityIntegrationTest`: `GET /api/admin/zones` 인증 없음 401(리다이렉트 아님), 일반 계정 403, 관리자 200; `POST /api/admin/zones` 관리자라도 `X-MUD-Client` 없으면 403, 있으면 201; `GET /api/auth/login?client=admin` → 302 `Location` 이 `/oauth2/authorization/google` 로 끝나고 다시 따라가면 `accounts.google.com` 으로 302(쿼리에 `redirect_uri=http://localhost/login/oauth2/code/google` 류 — MockMvc 기본 호스트 기준, `client_id=test-client-id`, `scope=openid profile email` 확인); `GET /api/auth/me` 인증 → 200 `{id, email, admin, characterName:null}`; `POST /api/auth/logout` 후 같은 세션으로 `me` 401; `POST /api/auth/dev` 는 기본 프로필에서 404; CORS 사전 요청(`OPTIONS` + `Origin: http://localhost:3001` + `Access-Control-Request-Headers: x-mud-client`)에 `Access-Control-Allow-Origin: http://localhost:3001`·`Access-Control-Allow-Credentials: true`, `Origin: http://evil.example` 은 거절. `DevLoginIntegrationTest`(`@ActiveProfiles("local")`, `mud.auth.dev-login.enabled=true`): `POST /api/auth/dev {"email":"admin@example.com"}` → 200·`admin:true`, 같은 세션으로 `GET /api/admin/zones` 200.
- [ ] **Step 5: 구현 → 모든 테스트 통과.** Task 3 WebMvc 테스트가 보안 때문에 깨지면 권한·헤더를 주도록 고친다(+ `unauthenticatedFail` 하나씩 추가).
- [ ] **Step 6: 검증** — `./gradlew build` 통과.
- [ ] **Step 7: 커밋** — `feat(world-server): Google OAuth2 로그인·세션·관리자 권한`.

**완료 조건:** D13 엔드포인트 동작, 관리 API가 관리자만 열림, 모든 테스트가 Google 자격 증명·네트워크 없이 통과. `GOOGLE_CLIENT_ID` 없이도 앱이 뜬다.

---

### Task 5: 캐릭터 — 이름 규칙, 계정당 하나, 시작 방, 위치 저장

**권장 모델:** opus

**목적:** D9·D10의 캐릭터를 만들고, 게임(Task 6)이 쓸 생성·조회·이동 저장 포트를 둔다.

**Files:**
- Create: `domain/character/{PlayerCharacter,CharacterName,CharacterCreateInfo,DuplicateCharacterNameException,CharacterAlreadyExistsException}.java`
- Create: `application/character/provided/{CharacterFinder,CharacterCreator,CharacterMover,WorldNotReadyException}.java`, `application/character/required/CharacterRepository.java`, `application/character/{CharacterQueryService,CharacterModifyService}.java`
- Modify: `adapter/webapi/AuthApi.java`(`/api/auth/me` 의 `characterName` 을 `CharacterFinder` 로 채움), orm.xml
- Test: `domain/character/{CharacterNameTest,PlayerCharacterTest,CharacterFixture}`, `application/character/required/CharacterRepositoryTest`, `application/character/provided/{CharacterCreatorTest,CharacterMoverTest}`

**Interfaces:**
- Consumes: `WorldSettingsFinder.findStartRoom()`(Task 3), `AccountFinder`(Task 4).
- Produces:
  - `record CharacterName(String value)` — 컴팩트 생성자에서 `^[가-힣]{2,6}$` 아니면 `IllegalArgumentException("이름은 띄어쓰기 없이 한글 2~6자로 정하십시오.")`. `static boolean isValid(String)`.
  - `PlayerCharacter`(이름 `Character` 는 `java.lang.Character` 와 겹쳐 쓰지 않는다): `static PlayerCharacter create(CharacterCreateInfo(Long accountId, CharacterName name, Long roomId))`, `void moveTo(Long roomId)`, `getAccountId()`, `getName()`, `getRoomId()`. 테이블 `player_character`(`account_id` `UK_PLAYER_CHARACTER_ACCOUNT_ID`, `name` varchar(6) `UK_PLAYER_CHARACTER_NAME`, `room_id` bigint not null — 방 FK는 두지 않는다: 방을 지울 때 이벤트로 옮기기 때문).
  - `CharacterRepository`: `save`, `Optional<PlayerCharacter> findById(Long)`, `Optional<PlayerCharacter> findByAccountId(Long)`, `boolean existsByName(CharacterName)`, `List<PlayerCharacter> findAllByRoomId(Long)`.
  - `CharacterFinder`: `Optional<PlayerCharacter> findByAccountId(Long accountId)`, `boolean isNameTaken(String name)`.
  - `CharacterCreator`: `PlayerCharacter create(Long accountId, String name)` — 순서: 이름 형식(`IllegalArgumentException`), 계정에 이미 있음(`CharacterAlreadyExistsException("이미 캐릭터가 있습니다")`), 이름 중복(`DuplicateCharacterNameException("이미 있는 이름입니다. 다른 이름을 정하십시오.")`), 시작 방 없음(`WorldNotReadyException("아직 세계가 열리지 않았습니다. 관리자가 시작 방을 정해야 합니다.")`). 시작 방에 둔다.
  - `CharacterMover`: `void moveTo(Long characterId, Long roomId)`(이동할 때마다 저장), `List<Long> relocateAll(Long fromRoomId, Long toRoomId)`(옮긴 캐릭터 id들).

- [ ] **Step 1: 테스트 작성** — `CharacterNameTest`: `한백`·`여섯글자이름`(6자) 통과, `한`(1자)·`일곱글자인이름`(7자)·`한 백`·`Han`·`한백1`·`ㅎㅂ`(자모) 실패. `CharacterCreatorTest`: `create`(시작 방에 놓임), `duplicateNameFail`, `secondCharacterForAccountFail`, `invalidNameFail`, `noStartRoomFail`. `CharacterMoverTest`: `moveTo`(flush·clear 후 room_id 바뀜), `relocateAll`. `CharacterRepositoryTest`: 계정 유니크·이름 유니크 위반.
- [ ] **Step 2: 실패 확인 → 구현 → 통과.**
- [ ] **Step 3: `/api/auth/me`** — 캐릭터가 있으면 `characterName` 이 이름. `SecurityIntegrationTest` 에 `meIncludesCharacterName` 추가.
- [ ] **Step 4: 검증** — `./gradlew build` 통과.
- [ ] **Step 5: 커밋** — `feat(world-server): 캐릭터 생성(이름)·계정당 하나·위치 저장`.

**완료 조건:** 위 테스트 통과, 의존 방향 `character → world → room → zone` 에 순환 없음.

---

### Task 6: 게임 세션과 명령 처리 — 보기·이동·말하기·누구·멀티유저

**권장 모델:** opus

**목적:** 전송 방식과 무관한 게임 코어. WebSocket 없이 provided 포트와 가짜 sender로 전부 테스트한다. D4~D11을 구현한다.

**Files:**
- Create: `domain/shared/Josa.java` — `static String iGa(String word)`(끝 글자가 받침 있는 한글이면 `"이"`, 아니면 `"가"`), `static String withIGa(String word)`(`한백` → `한백이`, `한립` → `한립이`, `여소` → `여소가`)
- Create: `domain/game/{OnlinePlayer,Command,CommandParser}.java`
  - `OnlinePlayer`: `characterId`, `accountId`, `name`, `roomId`, `sessionId`, `enteredAt`(들어온 순서용 증가 번호); `void moveTo(Long roomId)`, `void rebind(String sessionId)`.
  - `sealed interface Command` + records: `Look(@Nullable String argument)`, `Move(Direction direction)`, `Say(String message)`, `Who()`, `Help()`, `Quit()`, `Empty()`, `Unknown(String word)`.
  - `CommandParser.parse(String line)`: 앞뒤 공백 자르고 여러 공백은 하나로. `'` 로 시작하면 `Say(나머지 trim)`. 첫 낱말이 `보기`/`ㅂ` → `Look(나머지 or null)`, `Direction.fromCommand` 성공 → `Move`, `말하기` → `Say(나머지 or "")`, `누구` → `Who`, `도움말` → `Help`, `종료` → `Quit`, 빈 줄 → `Empty`, 그 밖 → `Unknown`.
- Create: `application/game/provided/GameConnector.java`
- Create: `application/game/required/{GameMessageSender,GameMessage,GameCloseReason,PresenceRepository}.java`
- Create: `application/game/{GameService,GameSession,RoomViewAssembler}.java`
- Create: `adapter/persistence/memory/InMemoryPresenceRepository.java`
- Test: `domain/shared/JosaTest`, `domain/game/CommandParserTest`, `application/game/provided/GameConnectorTest`, `test/.../application/game/RecordingGameMessageSender.java`(TestFakesConfiguration에 `@Bean @Primary` 로 등록)

**Interfaces:**
- Consumes: `CharacterFinder`, `CharacterCreator`, `CharacterMover`(Task 5), `RoomFinder`(Task 3), `WorldSettingsFinder`(Task 3), `RoomDeletedEvent`(Task 3).
- Produces:
  - `GameConnector`(provided, `/** 게임 접속과 플레이어 입력 처리를 제공한다 */`): `void connect(String sessionId, Long accountId)`, `void handle(String sessionId, String text)`, `void disconnect(String sessionId)`.
  - `GameMessageSender`(required): `void send(String sessionId, GameMessage message)`, `void close(String sessionId, GameCloseReason reason)`.
  - `enum GameCloseReason { QUIT, REPLACED, WORLD_NOT_READY }` (어댑터가 1000 / 4409 / 1013 으로 바꾼다).
  - `sealed interface GameMessage` + records (JSON 필드 이름과 같게 — Task 7이 그대로 직렬화한다):
    `SessionMessage(String state, @Nullable String characterName)`, `PromptMessage(String text)`, `RoomMessage(ZoneView zone, boolean zoneEntered, RoomView room, List<String> exits, List<OccupantView> occupants)` (+ `record ZoneView(Long id, String name)`, `record RoomView(Long id, String name, String description)`, `record OccupantView(String name, String kind)`), `SystemMessage(String text, String tone)`, `NoticeMessage(String text, String tone, String actor, String event)`, `SayMessage(String speaker, String message, boolean self, String text, String tone)`. 각 record에 `String type()` 이 `"session"`, `"prompt"`, `"room"`, `"system"`, `"notice"`, `"say"` 를 돌려준다.
  - `PresenceRepository`(required): `void save(OnlinePlayer)`, `Optional<OnlinePlayer> findByCharacterId(Long)`, `Optional<OnlinePlayer> findBySessionId(String)`, `List<OnlinePlayer> findAllInRoom(Long roomId)`(들어온 순서), `List<OnlinePlayer> findAll()`(이름순), `void delete(Long characterId)`.
  - `GameService`(`@ApplicationService`, `GameConnector` 구현): 공정 `ReentrantLock` 으로 세 메서드와 이벤트 처리를 직렬화. 세션별 상태 `GameSession`(accountId, phase `NAMING | CONFIRMING | PLAYING`, pendingName, characterId, lastZoneId)을 `Map<String, GameSession>` 에 둔다. `@EventListener void onRoomDeleted(RoomDeletedEvent)` 가 `CharacterMover.relocateAll` 과 접속 중인 사람 옮기기(D11)를 한다.
  - 문구는 D6·D7·D9·D11의 문장을 글자 그대로 쓴다.
- 처리 규칙
  - `connect`: 시작 방이 없으면 `system/error`(D10 문구) + `close(WORLD_NOT_READY)`. 캐릭터 없으면 `session(creating, null)` + 이름 prompt. 있으면 — 같은 캐릭터가 이미 접속 중이면 앞 세션에 `system/info` `다른 곳에서 접속해 이 연결을 끊습니다.` + `close(REPLACED)` 하고 앞 세션 상태를 지운 뒤 `rebind`(알림 없음); 아니면 저장된 방(없으면 시작 방, 그때는 DB도 옮김)에 넣고 그 방 다른 사람들에게 `appear`. 그다음 `session(playing, 이름)` + `room(zoneEntered=true)`.
  - `handle` 이 PLAYING이 아닐 때는 D9 생성 흐름. 500자 넘는 입력은 `system/error` `명령이 너무 깁니다.` (제안).
  - `Move`: 출구 없음 → 오류 문구. 있으면 원래 방 다른 사람에게 `leave`(`towardPhrase`), `CharacterMover.moveTo`, presence 이동, 새 방 다른 사람에게 `arrive`(반대 방향 `fromPhrase`), 나에게 `room(zoneEntered = 새 방 지역 id != lastZoneId)`.
  - `Say`: 나에게 `self=true`, 같은 방 다른 사람에게 `self=false`.
  - `Who`: 첫 줄 `접속한 사람 (N)`, 다음 줄마다 `"  " + 이름 + "    " + 지역이름`(이름순).
  - `Quit`: `system/info` `다음에 또 뵙겠습니다.` → presence에서 빼고 같은 방에 `disappear` → `close(QUIT)` → 세션 상태 제거. 그 뒤 어댑터가 부르는 `disconnect` 는 아무 일도 하지 않는다.
  - `disconnect`: 그 세션이 지금 그 캐릭터의 세션일 때만 presence에서 빼고 `disappear` 알림. 밀려난 세션·생성 중 세션은 상태만 지운다.

- [ ] **Step 1: 순수 단위 테스트** — `JosaTest`(`한백이`, `여소가`, `한립이`, `청모랑이`, `소나무가`), `CommandParserTest`(`"  북  "`→Move NORTH, `"NE"`→Move NORTHEAST, `"ㅂ"`→Look(null), `"보기 한립"`→Look("한립"), `"'안녕하시오"`→Say("안녕하시오"), `"말하기   길을   묻겠소"`→Say("길을 묻겠소"), `"말하기"`→Say(""), `"말 안녕"`→Unknown("말"), `"보"`→Unknown, `""`→Empty, `"누구"`, `"도움말"`, `"종료"`).
- [ ] **Step 2: 포트 테스트(`@ApplicationServiceTest`, 메시지는 `RecordingGameMessageSender` 로 확인, 맵은 `ZoneEditor`/`RoomEditor`/`WorldSettingsEditor` 로 준비하는 `prepareMap()` 을 기반 클래스 `BaseGameTest` 에)** — 맵: 지역 `청우촌`(방 `마을 어귀`(시작) 북↔남 `우물가`), 지역 `청석숲`(방 `숲 입구`), `우물가` 동↔서 `숲 입구`.
  - `connectWithoutStartRoomFail` — 시작 방 지정 전: error 문구 + `close(WORLD_NOT_READY)`.
  - `createCharacterFlow` — 이름 prompt → `한 백` 형식 오류 → `한백` → 확인 prompt `이름 한백\n이대로 만들려면 '확인', 고치려면 '뒤로'.` → `뒤로` → 이름 prompt → `한백` → `확인` → `캐릭터를 만들었습니다.`, `session(playing, 한백)`, `room` 이 `마을 어귀`·`zoneEntered=true`·`exits==["북"]`.
  - `createCharacterDuplicateNameFail`.
  - `lookShowsOthersExcludingSelf` — A·B 같은 방: A `보기` → occupants `[B]`, `zoneEntered=false`.
  - `moveNotifiesBothRooms` — A `북` → A가 `우물가` room; 마을 어귀의 C에게 `한백이 북쪽으로 떠난다.`(`event=leave`); 우물가의 B에게 `한백이 남쪽에서 다가온다.`(`event=arrive`); DB room_id 갱신.
  - `moveNoExitFail` — `그쪽으로는 길이 없습니다.`
  - `moveIntoOtherZoneSetsZoneEntered` — `우물가` → `동` → `숲 입구` room `zoneEntered=true`, `zone.name=="청석숲"`.
  - `sayReachesSameRoomOnly` — 나 `한백: "길을 묻겠소."`, 같은 방 `한백이 말한다: "길을 묻겠소."`, 다른 방 사람은 못 받음; `말하기` 빈 내용 오류; 201자 오류.
  - `whoListsOnlinePlayers` — `접속한 사람 (2)\n  한백    청우촌\n  한립    청우촌` 형태(이름순).
  - `unknownCommand`, `lookWithArgument`(`아직 자세히 볼 수 없습니다.`), `help`.
  - `quitRemovesAndCloses` — 같은 방에 `한백이 사라진다.`, `close(QUIT)`, 이어진 `disconnect` 가 알림을 두 번 보내지 않음.
  - `disconnectNotifiesAppearDisappear` — 접속 시 같은 방에 `나타난다`, 끊김 시 `사라진다`.
  - `reconnectSameAccountReplacesOldSession` — 같은 계정 두 번째 `connect`: 첫 세션에 끊김 문구 + `close(REPLACED)`, 같은 방 사람에게 appear/disappear 알림 없음, 두 번째 세션이 room 받음, 이어서 첫 세션 `disconnect` 해도 presence 유지.
  - `reconnectStartsInSavedRoom` — 이동 후 끊고 다시 접속 → 저장된 방.
  - `savedRoomDeletedStartsInStartRoom`.
  - `roomDeletedWhileOccupiedMovesPlayerToStartRoom` — `우물가` 에 서 있을 때 관리자가 `우물가` 삭제 → `서 있던 곳이 사라져 시작 장소로 옮겨졌습니다.` + 시작 방 room, 그 뒤 `보기` 정상.
- [ ] **Step 3: 실패 확인 → 구현 → 통과.**
- [ ] **Step 4: 검증** — `./gradlew build` 통과(ArchUnit: game 슬라이스가 다른 슬라이스의 provided만 씀).
- [ ] **Step 5: 커밋** — `feat(world-server): 게임 세션·명령 처리(보기·이동·말하기·누구)와 같은 방 알림`.

**완료 조건:** 위 테스트 전부 통과. 문구가 D6·D7·D9·D11과 글자 하나까지 같다.

---

### Task 7: WebSocket 어댑터 — `/ws/game`, 세션 쿠키 인증, JSON 직렬화

**권장 모델:** opus

**목적:** D4·D5를 구현해 브라우저가 실제로 게임에 붙게 한다.

**Files:**
- Create: `adapter/websocket/{GameWebSocketConfig,GameWebSocketHandler,GameHandshakeInterceptor,WebSocketGameMessageSender,InboundMessage}.java`
- Test: `adapter/websocket/GameWebSocketIntegrationTest.java`, `adapter/websocket/WebSocketGameMessageSenderTest.java`

**Interfaces:**
- Consumes: `GameConnector`(Task 6), `GameMessageSender`/`GameMessage`/`GameCloseReason`(Task 6), `SessionAuthenticator.principalOf(Authentication)`·`AccountPrincipal`(Task 4), `CorsProperties`(Task 4).
- Produces:
  - `GameWebSocketConfig`(`@Configuration @EnableWebSocket`): `/ws/game` 에 `GameWebSocketHandler` 등록, `addInterceptors(GameHandshakeInterceptor)`, `setAllowedOrigins(CorsProperties 목록)`(목록 밖 Origin은 핸드셰이크 403).
  - `GameHandshakeInterceptor implements HandshakeInterceptor`: `ServletServerHttpRequest` 의 HTTP 세션(새로 만들지 않음)에서 `HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY` 의 `SecurityContext` → `principalOf` → 있으면 속성 `accountId` 에 넣는다. 없어도 핸드셰이크는 통과시킨다.
  - `WebSocketGameMessageSender`(`@Adapter`, `GameMessageSender` 구현): 세션 id → `ConcurrentWebSocketSessionDecorator(session, 5000ms, 512KB)`. `send` 는 Jackson 3 `tools.jackson.databind.json.JsonMapper` 로 `GameMessage` record를 `{"type": ..., ...record 필드}` JSON 하나로 직렬화(필드 이름은 D5 그대로, `type` 이 맨 앞). 없는 세션이면 무시. `close` 는 `QUIT→1000`, `REPLACED→4409`, `WORLD_NOT_READY→1013`.
  - `GameWebSocketHandler extends TextWebSocketHandler`(`@Adapter`): 연결되면 sender에 등록하고, 속성 `accountId` 가 없으면 `{"type":"error","code":"AUTH_REQUIRED","text":"로그인이 필요합니다. 다시 로그인하십시오."}` 후 4401로 닫는다. 있으면 `GameConnector.connect(sessionId, accountId)`. 받는 메시지는 `command` 만 → `GameConnector.handle`. JSON이 아니거나 type이 맞지 않으면 `BAD_MESSAGE` `"알 수 없는 메시지입니다."`(닫지 않음). 닫히면(어떤 이유든) sender에서 빼고 `connect` 했던 세션이면 `GameConnector.disconnect`.
  - `record InboundMessage(String type, @Nullable String text)`.
  - 주의: HTTP 세션이 로그아웃·만료돼도 이미 열린 WebSocket은 1차에서 끊지 않는다(다음 접속부터 막힘). (제안)

- [ ] **Step 1: 통합 테스트(`@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("local")` + `mud.auth.dev-login.enabled=true`, `StandardWebSocketClient`)** — 로그인은 `RestClient` 로 `POST /api/auth/dev`(헤더 `X-MUD-Client: 1`)를 불러 받은 `Set-Cookie` 의 `JSESSIONID` 를 핸드셰이크 헤더 `Cookie` 와 `Origin: http://localhost:3000` 에 넣는다. 맵은 포트로 준비. 받은 메시지는 `BlockingQueue<String>` 에 모아 JSON으로 확인.
  - `authAndCreateCharacter` — 연결 → `{"type":"session","state":"creating","characterName":null}` → `{"type":"prompt",...}` → `command 한백` → `command 확인` → `system`, `session playing`, `room` 의 `exits` 가 `["북"]`, `occupants` 가 `[]`.
  - `noSessionClosesWith4401` — 쿠키 없이 연결 → `error.code=="AUTH_REQUIRED"`, 닫기 코드 4401.
  - `foreignOriginRejected` — `Origin: http://evil.example` 이면 핸드셰이크 실패.
  - `malformedJsonIsBadMessage`(연결은 유지).
  - `twoPlayersSeeEachOther` — A·B 접속(캐릭터 미리 생성), B 접속 시 A가 `notice appear`, A `북` → B가 `notice leave`, A `'안녕` → 다른 방 B는 못 받음, B도 `북` → A가 `notice arrive` 를 받고 B `'안녕하시오` → A가 `say self=false text="한립이 말한다: \"안녕하시오\""`.
  - `secondConnectionClosesFirstWith4409`.
  - `abruptCloseRemovesPresence` — A가 종료 명령 없이 연결을 닫음 → B가 `한백이 사라진다.`, B `누구` → `접속한 사람 (1)`.
  - `quitClosesWith1000`.
- [ ] **Step 2: 실패 확인 → 구현 → 통과.** `WebSocketGameMessageSenderTest` 는 각 `GameMessage` 의 JSON 모양(필드 이름·`type` 값)을 단위로 확인.
- [ ] **Step 3: 검증** — `./gradlew build` 통과.
- [ ] **Step 4: 커밋** — `feat(world-server): 게임 WebSocket(/ws/game)·세션 쿠키 인증·JSON 메시지`.

**완료 조건:** 위 통합 테스트 통과. 실제 Google 없이 돈다.

---

### Task 8: admin-console — 로그인과 맵 편집 화면

**권장 모델:** opus

**목적:** D14 화면으로 D12 API를 쓰는 관리 툴을 만든다.

**Files:**
- Create: `admin-console/{package.json,package-lock.json,.nvmrc,next.config.ts,tsconfig.json,vitest.config.ts,vitest.setup.ts,.gitignore,next-env.d.ts}`
- Create: `admin-console/app/{layout.tsx,page.tsx,globals.css}`, `app/zones/page.tsx`, `app/zone/page.tsx`, `app/room/page.tsx`
- Create: `admin-console/src/lib/{config.ts,auth.ts,api.ts,directions.ts}`, `src/components/{LoginPanel.tsx,DevLoginForm.tsx,AdminGuard.tsx,TopBar.tsx,ZoneForm.tsx,RoomForm.tsx,ExitEditor.tsx}`
- Test: `src/lib/api.test.ts`, `src/lib/auth.test.ts`, `src/components/{ExitEditor.test.tsx,ZoneForm.test.tsx,AdminGuard.test.tsx}`

**Interfaces:**
- Consumes: D12·D13 API(Task 3·4). 오류 응답은 `ProblemDetail` 의 `detail` 을 화면에 보인다.
- Produces:
  - `package.json` scripts: `"dev": "next dev -p 3001"`, `"build": "next build"`, `"test": "vitest run"`. 의존성은 Global Constraints 표 버전 그대로(`next`, `react`, `react-dom` / dev: `typescript`, `@types/react`, `@types/react-dom`, `@types/node`, `vitest`, `vite`, `@vitejs/plugin-react`, `jsdom`, `@testing-library/react`, `@testing-library/dom`, `@testing-library/jest-dom`, `@testing-library/user-event`). `.nvmrc` = `26.13.1`.
  - `next.config.ts`: `output: "export"`, `trailingSlash: true`, `images: { unoptimized: true }`.
  - `src/lib/config.ts`: `worldServerUrl`(`process.env.NEXT_PUBLIC_WORLD_SERVER_URL ?? "http://localhost:8080"`), `devLoginEnabled`(`NEXT_PUBLIC_DEV_LOGIN === "true"`).
  - `src/lib/auth.ts`(토큰 저장 없음 — 로그인 상태는 world-server 세션 쿠키): `googleLoginUrl(): string`(`${worldServerUrl}/api/auth/login?client=admin`), `fetchMe(): Promise<AuthMeResponse | null>`(401이면 null), `loginForDevelopment(email): Promise<AuthMeResponse>`, `logout(): Promise<void>`.
  - `src/lib/api.ts`: `class ApiError extends Error { status: number; detail: string }`; `apiFetch<T>(path, init?)`(항상 `credentials: "include"`, `GET` 이 아니면 헤더 `X-MUD-Client: 1` 과 `Content-Type: application/json`, 실패 응답은 `ProblemDetail` 을 읽어 `ApiError`), 그리고 엔드포인트마다 함수: `listZones`, `createZone`, `updateZone`, `deleteZone`, `listRooms(zoneId)`, `getRoom`, `createRoom`, `updateRoom`, `deleteRoom`, `putExit(roomId, direction, {targetRoomId, twoWay})`, `deleteExit(roomId, direction, twoWay)`, `getWorld`, `setStartRoom(roomId)`. 타입은 D12 응답 이름 그대로(`ZoneResponse` 등).
  - `src/lib/directions.ts`: `DIRECTIONS: {code: "EAST"|..., name: "동"|...}[]`(순서 `동 서 남 북 북동 북서 남동 남서 위 아래`).
  - `LoginPanel`: `Google로 로그인` 링크 버튼(`href=googleLoginUrl()`, 전체 페이지 이동), `devLoginEnabled` 면 `DevLoginForm`, URL에 `loginError=1` 이 있으면 `로그인하지 못했습니다. 다시 시도하십시오.`.
  - `AdminGuard`: `fetchMe()` 가 null이면 로그인 화면(`/`)으로, `me.admin == false` 면 `관리자만 쓸 수 있습니다.` + 로그아웃.
  - 페이지는 `useSearchParams` 로 `id` 를 읽고 `<Suspense>` 로 감싼다(정적 export 요구).

- [ ] **Step 1: 프로젝트 만들기** — `cd admin-console && source "$NVM_DIR/nvm.sh" && nvm install && nvm use`, `npm install` 으로 정확한 버전 설치(lock 생성). `vitest.config.ts`: `environment: "jsdom"`, `setupFiles: ["./vitest.setup.ts"]`(jest-dom 확장), `plugins: [react()]`.
- [ ] **Step 2: 테스트 작성** — `api.test.ts`(`fetch` 를 `vi.fn` 으로): `credentials: "include"` 와 변경 요청의 `X-MUD-Client: 1` 헤더가 붙음(GET에는 없음), `putExit(3, "NORTH", {targetRoomId: 4, twoWay: true})` 가 `PUT {url}/api/admin/rooms/3/exits/NORTH` 본문 `{"targetRoomId":4,"twoWay":true}`, 409 응답이면 `ApiError.status==409` 이고 `detail` 이 본문의 detail. `auth.test.ts`: `fetchMe` 가 401에 null, 200에 본문; `googleLoginUrl()` 이 `http://localhost:8080/api/auth/login?client=admin`. `ExitEditor.test.tsx`: 방위 선택·대상 지역→대상 방 선택 후 추가 버튼이 `putExit` 를 `twoWay: true`(기본 체크) 로 부름, 체크를 끄면 false, 409면 detail 문구가 보임. `ZoneForm.test.tsx`: 빈 이름이면 제출 버튼이 막힘, 30자 넘게 입력 불가(`maxLength`). `AdminGuard.test.tsx`: `admin:false` 면 `관리자만 쓸 수 있습니다.`.
- [ ] **Step 3: 실패 확인 → 구현 → 통과.** 화면 문구 예: 버튼 `지역 만들기`, `방 만들기`, `저장`, `삭제`, `출구 추가`, `반대 방향도`, `시작 방으로 지정`, `로그아웃`; 시작 방 표시 `시작 방: 청우촌 / 마을 어귀`(없으면 `시작 방: 없음`). 삭제 전 `confirm()` 으로 묻는다.
- [ ] **Step 4: 검증** — `npm ci && npm test && npm run build` 통과, `out/zones/index.html`·`out/room/index.html` 생성 확인.
- [ ] **Step 5: 커밋** — `feat(admin-console): Google 로그인과 지역·방·출구·시작 방 편집 화면`.

**완료 조건:** 테스트·정적 빌드 통과. (실제 서버와의 수동 확인은 Task 12.)

---

### Task 9: play-client — 터미널형 플레이 화면

**권장 모델:** opus

**목적:** 로그인 → WebSocket 접속 → 출력 로그 + 입력창으로 D5 메시지를 그리고, D18 입력 보조를 한다.

**Files:**
- Create: `play-client/{package.json,package-lock.json,.nvmrc,next.config.ts,tsconfig.json,vitest.config.ts,vitest.setup.ts,.gitignore,next-env.d.ts}` (설정은 Task 8과 같은 값, dev 포트 3000)
- Create: `play-client/app/{layout.tsx,page.tsx,globals.css}`
- Create: `play-client/src/lib/{config.ts,auth.ts,protocol.ts,render.ts,history.ts,complete.ts,gameSocket.ts}`, `src/components/{Terminal.tsx,LoginScreen.tsx,DevLoginForm.tsx}`
- Test: `src/lib/{render.test.ts,history.test.ts,complete.test.ts,gameSocket.test.ts}`, `src/components/Terminal.test.tsx`

**Interfaces:**
- Consumes: D5 메시지, D13 `/api/auth/login?client=play`·`/api/auth/me`·`/api/auth/logout`·`/api/auth/dev`.
  - `config.ts`·`auth.ts` 는 admin-console과 같은 모양(`googleLoginUrl()` 은 `client=play`, `fetchMe`, `loginForDevelopment`, `logout`; 모든 요청 `credentials: "include"`, 변경 요청에 `X-MUD-Client: 1`). 토큰을 저장하지 않는다.
  - `LoginScreen`: `Google로 로그인` 링크 버튼, `devLoginEnabled` 면 `DevLoginForm`, `loginError=1` 이면 `로그인하지 못했습니다. 다시 시도하십시오.`. `app/page.tsx` 는 `fetchMe()` 가 null이면 `LoginScreen`, 아니면 `Terminal`(위쪽에 작은 `로그아웃` 버튼).
- Produces:
  - `protocol.ts`: D5의 서버→클라이언트 메시지를 `type` 으로 구분되는 TS 유니온 `ServerMessage` 로, 클라이언트 메시지 `AuthMessage`·`CommandMessage`.
  - `render.ts`: `type OutputLine = { text: string; style: string }`; `renderMessage(msg: ServerMessage): OutputLine[]`. 규칙: `room` → (zoneEntered면 `{text:"◇ "+zone.name, style:"zone"}`) + `{room.name, "room-name"}` + `{description, "description"}` + `{"출구: " + (exits.join(" ") || "없음"), "exits"}` + occupants마다 `{name, "occupant"}`. `system`·`prompt`·`notice`·`say`·`error` → `text` 를 `\n` 으로 나눈 줄들, style은 `tone`(prompt는 `"prompt"`, error는 `"error"`). `session` → 줄 없음. 입력 줄 메아리는 `{"> " + 입력, "echo"}`.
  - `history.ts`: `class CommandHistory { constructor(limit = 100); push(line: string): void; previous(current: string): string; next(): string; reset(): void }` — D18 규칙.
  - `complete.ts`: `COMMANDS = ["보기","말하기","누구","도움말","종료","동","서","남","북","북동","북서","남동","남서","위","아래"]`; `class Completer { complete(input: string, names: string[]): string; reset(): void }` — 첫 낱말이면 명령어에서, 아니면 마지막 낱말을 이름에서 앞부분 일치로 찾고, 거듭 부르면 다음 후보로 돈다(입력이 바뀌면 처음부터). 후보가 없으면 입력 그대로.
  - `gameSocket.ts`: `class GameSocket { constructor(url: string, handlers: { onMessage(m: ServerMessage): void; onClose(code: number): void }, socketFactory?: (url: string) => WebSocket); sendCommand(text: string): void; close(): void }` — 인증은 브라우저가 핸드셰이크에 세션 쿠키를 자동으로 싣는 것으로 끝난다(따로 보내는 인증 메시지 없음). URL은 `worldServerUrl` 의 `http`→`ws`(`https`→`wss`) + `/ws/game`.
  - `Terminal`: 출력 로그(새 줄이 오면 아래로 스크롤, 최대 2000줄 보관) + 아래 입력창(포커스 유지). Enter → 메아리 + `sendCommand`. ↑↓ 히스토리, Tab 자동완성(`preventDefault`). 이름 후보는 D18대로 유지. 닫기 코드 4401 → 로그인 화면, 4409 → `다른 곳에서 접속해 연결이 끊겼습니다.` 1000 → 아무 말 없이 "다시 연결" 버튼, 그 밖 → `연결이 끊겼습니다.` + "다시 연결" 버튼.
  - 화면은 고정폭 글꼴, 어두운 바탕. `style` 별 색은 CSS 클래스(`.line-zone`, `.line-room-name`, `.line-exits`, `.line-occupant`, `.line-movement`, `.line-speech`, `.line-error`, `.line-prompt`, `.line-echo`)로 정한다.

- [ ] **Step 1: 프로젝트 만들기** — Task 8 Step 1과 같게(`cd play-client`).
- [ ] **Step 2: 테스트 작성** — `render.test.ts`: D8 예시 그대로(zoneEntered true/false, 출구 없음 → `출구: 없음`, occupants 두 명), 여러 줄 system(`누구` 출력)을 줄별로, `say` 는 `text` 그대로 style `speech`. `history.test.ts`: 차례로 넣은 `북`,`남` 에서 ↑ 두 번 → `남`,`북`, ↓ → `남`, ↓ 끝 → `""`; 같은 입력 연속은 하나로; 101개 넣으면 가장 오래된 것이 빠짐. `complete.test.ts`: `"보"`→`"보기"`, `"북"` 반복 → `"북"`→`"북동"`→`"북서"`→`"북"`(앞부분 일치 후보 순서는 COMMANDS 순서), `"말하기 한"` + names `["한립","한백"]` → `"말하기 한립"` → 다시 → `"말하기 한백"`, 후보 없음은 그대로. `gameSocket.test.ts`(가짜 WebSocket): 만든 URL이 `ws://localhost:8080/ws/game`, `sendCommand("북")` → `{"type":"command","text":"북"}`, 받은 JSON을 `onMessage` 로, 닫기 코드를 `onClose` 로. `Terminal.test.tsx`: 입력 후 Enter → 메아리 줄과 sendCommand 호출, ↑ 로 이전 명령, Tab 으로 `"누"`→`"누구"`, `room` 메시지가 오면 방 이름 줄이 보임.
- [ ] **Step 3: 실패 확인 → 구현 → 통과.**
- [ ] **Step 4: 검증** — `npm ci && npm test && npm run build` 통과, `out/index.html` 생성.
- [ ] **Step 5: 커밋** — `feat(play-client): 터미널형 플레이 화면(로그인·WebSocket·히스토리·자동완성)`.

**완료 조건:** 테스트·정적 빌드 통과.

---

### Task 10: Docker·compose·README·.env.example

**권장 모델:** opus

**목적:** D16·D17대로 맥북에서 `docker compose up` 으로 네 서비스를 띄우고, 실행 방법을 README에 적는다.

**Files:**
- Create: `world-server/Dockerfile`, `world-server/.dockerignore`(`build/`, `.gradle/`)
- Create: `play-client/Dockerfile`, `play-client/nginx.conf`, `play-client/.dockerignore`(`node_modules/`, `.next/`, `out/`), 같은 셋을 `admin-console/` 에도
- Create: 루트 `compose.yaml`, `.env.example`, `README.md`; Modify: 루트 `.gitignore`(`.env` 추가)

**Interfaces:**
- world-server Dockerfile: 빌드 단계 `eclipse-temurin:25.0.5_8-jdk` 에서 `./gradlew bootJar -x test --no-daemon` → 실행 단계 `eclipse-temurin:25.0.5_8-jre`, 비루트 사용자, `EXPOSE 8080`, `ENTRYPOINT ["java","-jar","/app/app.jar"]`.
- 프론트 Dockerfile: `node:26.13.1-alpine` 에서 `ARG NEXT_PUBLIC_WORLD_SERVER_URL`, `ARG NEXT_PUBLIC_DEV_LOGIN` → `ENV` 로 넘기고 `npm ci && npm run build` → `nginx:1.30.6-alpine` 에 `out/` 복사. `nginx.conf`: `listen 80`, `try_files $uri $uri/ $uri.html /404.html`.
- 루트 `compose.yaml` 서비스:
  - `postgres`: `postgres:18.6-alpine`, env `POSTGRES_DB=mud`, `POSTGRES_USER=mud`, `POSTGRES_PASSWORD=${POSTGRES_PASSWORD}`, 볼륨 `pgdata:/var/lib/postgresql`, `healthcheck: pg_isready -U mud -d mud`. 포트를 밖으로 열지 않는다.
  - `world-server`: `build: ./world-server`, `ports: ["8080:8080"]`, `depends_on: postgres (service_healthy)`, env `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/mud`, `SPRING_DATASOURCE_USERNAME=mud`, `SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD}`, `GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID:-not-configured}`, `GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET:-not-configured}`, `MUD_ADMIN_EMAILS=${MUD_ADMIN_EMAILS:-}`, `MUD_PLAY_CLIENT_URL=http://localhost:3000/`, `MUD_ADMIN_CONSOLE_URL=http://localhost:3001/`, `MUD_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:3001`, `SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-}`, `MUD_DEV_LOGIN_ENABLED=${MUD_DEV_LOGIN_ENABLED:-false}`.
  - `play-client`: `build: {context: ./play-client, args: {NEXT_PUBLIC_WORLD_SERVER_URL: http://localhost:8080, NEXT_PUBLIC_DEV_LOGIN: ${MUD_DEV_LOGIN_ENABLED:-false}}}`, `ports: ["3000:80"]`.
  - `admin-console`: 같은 모양, `ports: ["3001:80"]`.
  - `volumes: {pgdata: {}}`.
- `.env.example`(이름만, 값 비움): `GOOGLE_CLIENT_ID=`, `GOOGLE_CLIENT_SECRET=`, `MUD_ADMIN_EMAILS=`, `POSTGRES_PASSWORD=`, `SPRING_PROFILES_ACTIVE=`, `MUD_DEV_LOGIN_ENABLED=false` 와 각 줄 한국어 주석.
- `README.md`(한국어): 모듈 3개 소개, 필요한 것(Docker, Java 25, Node 26.13.1/nvm), Google OAuth 클라이언트(기존 클라이언트를 그대로 쓴다: 유형 웹 애플리케이션, **승인된 리디렉션 URI `http://localhost:8080/login/oauth2/code/google`** — 이미 등록돼 있으면 추가할 것 없음. 로그인이 서버 측에서 일어나므로 승인된 JavaScript 원본은 필요 없다. 클라이언트 ID·시크릿을 `.env` 의 `GOOGLE_CLIENT_ID`·`GOOGLE_CLIENT_SECRET` 에), 브라우저 주소는 `localhost` 로만(127.0.0.1 금지 — 세션 쿠키), 로그인은 world-server 메모리 세션이라 서버를 다시 띄우면 다시 로그인, `.env` 만들기, `docker compose up --build`, 접속 주소 3개, 관리자 지정(`MUD_ADMIN_EMAILS`), 처음 할 일(관리 툴에서 지역·방·출구 만들고 시작 방 지정), 개별 개발 실행(`world-server`: `./gradlew bootRun --args='--spring.profiles.active=local'`, 프론트: `npm run dev`), **개발용 로그인** 절(D3: `local` 프로필 + `MUD_DEV_LOGIN_ENABLED=true` 일 때만, 운영에서 켜지 말 것), 테스트 명령, 1차에 되는 명령어 목록(D6).

- [ ] **Step 1: 파일 작성.**
- [ ] **Step 2: 검증** — `cp .env.example .env` 후 값 채움(`POSTGRES_PASSWORD=mud-local`, `SPRING_PROFILES_ACTIVE=local`, `MUD_DEV_LOGIN_ENABLED=true`, `MUD_ADMIN_EMAILS=admin@example.com`) → `docker compose config` 오류 없음 → `docker compose build` 성공 → `docker compose up -d` → `curl -s -o /dev/null -w '%{http_code}' localhost:8080/api/auth/me` 가 `401`, `curl -s localhost:3000/ | grep -q '<html'`, `curl -s localhost:3001/zones/ | grep -q '<html'`, `curl -s -o /dev/null -w '%{http_code} %{redirect_url}' 'localhost:8080/api/auth/login?client=play'` 가 `302 http://localhost:8080/oauth2/authorization/google` → `docker compose down`. `.env` 는 커밋하지 않는다(`git status` 로 확인).
- [ ] **Step 3: 커밋** — `build(infra): 모듈별 Dockerfile·루트 compose·README·.env.example`.

**완료 조건:** 위 검증 명령 전부 성공, `.env` 가 커밋되지 않음.

---

### Task 11: 스펙 반영 — 제안 결정을 시스템 스펙에 `(제안)` 으로

**권장 모델:** sonnet

**목적:** 이 계획의 `## 제안 결정 목록`(D1~D18)과 구현에서 실제로 된 값을 `docs/spec/system/` 에 옮긴다. 코드를 바꾸지 않는다.

**Files:**
- Modify: `docs/spec/system/platform.md` — 기술 스택 표의 프론트엔드 행(Next.js 16.4.6, React 19.3.1, TypeScript 7.0.2, Node 26.13.1, 프론트 테스트 Vitest 5.1.0 + Testing Library, 출처·조회일), 실시간 통신 절에 D4·D5(메시지 표, 닫기 코드, tone 값), 월드 상태 절에 D11(메모리 범위, 이동할 때마다 저장, 맵은 DB에서 읽어 바로 반영, 명령 직렬화), 저장소 구성에 루트 패키지 `mudmk3`(D15), 배포 절에 D16·D17(Dockerfile 구성, compose 위치·포트, 이미지 버전, 환경변수 표), admin-console도 정적 export임. `## 열린 질문` 에서 이로써 답이 생긴 항목은 지우거나 "(제안으로 정함 → 절 링크)" 로 바꾼다(틱·주기 저장은 여전히 열림).
- Modify: `docs/spec/system/auth.md` — D1(서버 측 OAuth2 Login 흐름, 리디렉션 URI, 세션 쿠키, 로그인 뒤 돌아가는 곳), D2(오리진·CORS·CSRF 헤더), D3(개발용 로그인), D4 링크, 세션이 메모리라 재시작 시 다시 로그인, 관리자 반영 시점. 열린 질문(WebSocket 인증 전달)에 답.
- Modify: `docs/spec/system/admin-tool.md` — 1차 화면(D14)과 REST API(D12), 반대 방향 출구 자동 생성, "관리 툴에서 바꾼 내용이 바로 반영되는가" 열린 질문에 1차 답(맵은 바로 반영).
- Modify: `docs/spec/system/world-structure.md` — D10 데이터 모델(지역·방·출구·시작 방, 출구 규칙, 지우기 규칙).
- Modify: `docs/spec/system/command.md` — 1차 명령어 표(D6)와 알림 문구(D7)를 룰북 링크와 함께.
- Modify: `docs/spec/system/index.md` — 1차 범위 절에 "구현 계획: `docs/impl/iteration-1/plan.md`" 링크, 열린 질문 갱신.
- Modify: `.claude/rules/README.md` — `<base>` 를 `mudmk3` 로(미정 문구 제거). `.claude/rules/architecture.md` 메모리 월드 상태 절에 "메모리 구현은 `adapter/persistence/memory`" 한 줄.

**규칙:**
- 새로 적는 결정마다 문장 끝에 `(제안)` 을 붙인다(기존 관례: `docs/spec/game-rule/*.md`). 이미 확정된 문장은 고치지 않는다.
- 룰북(`docs/spec/game-rule/`)은 고치지 않는다. 다만 D7 알림 문구·D9 생성 문구·`아직 자세히 볼 수 없습니다.`·`말은 200자 이내로 하십시오.`·`명령이 너무 깁니다.` 처럼 룰북에 없는 문구는 `command.md` 에 "룰북에 아직 없음" 으로 표시해 남긴다.
- 값은 이 계획이 아니라 **구현된 코드**에서 확인해 적는다(`world-server/src/main/resources/application.yml`, `compose.yaml`, `package.json`, `GameService` 문구). 다르면 코드 값을 적고 progress.md 메모에 차이를 남긴다.

- [ ] **Step 1: 각 파일 수정.**
- [ ] **Step 2: 검증** — 모든 상대 링크가 존재하는 파일·헤딩을 가리키는지 `grep -o '](\.\./[^)]*\|](\w[^)]*\.md[^)]*)'` 로 뽑아 확인, `git diff --stat` 에 `docs/spec/system/*` 와 `.claude/rules/{README,architecture}.md` 만 있음.
- [ ] **Step 3: 커밋** — `docs(spec): 1차 이터레이션 제안 결정(인증 흐름·WebSocket 메시지·맵 모델·배포) 반영`.

**완료 조건:** D1~D18 각각이 스펙 어딘가에 `(제안)` 으로 있고, 해당 열린 질문이 정리됨.

---

### Task 12: 통합 확인 — compose로 띄워 시나리오 점검

**권장 모델:** opus

**목적:** 실제 컨테이너에서 1차 범위 시나리오(관리자가 맵을 만들고 두 플레이어가 돌아다니며 서로 보고 말함)를 자동 스크립트로 확인하고, Google 로그인 수동 확인 절차를 남긴다.

**Files:**
- Create: `scripts/smoke-test.mjs` (Node 26 내장 `fetch`·`WebSocket` 만 사용, 의존성 없음)
- Modify: `README.md`(스모크 테스트 실행법과 수동 확인 체크리스트 절), `docs/impl/iteration-1/progress.md`

**Interfaces:**
- `node scripts/smoke-test.mjs [baseUrl=http://localhost:8080]` — 개발용 로그인이 켜진 서버(`SPRING_PROFILES_ACTIVE=local`, `MUD_DEV_LOGIN_ENABLED=true`, `MUD_ADMIN_EMAILS` 에 `smoke-admin@example.com`)를 전제로 한다. 실행마다 겹치지 않게 지역 이름·캐릭터 이름에 실행 번호를 섞는다(캐릭터 이름은 한글이어야 하므로 `가`~`힣` 범위의 임의 글자 2개를 붙여 4자).
- 시나리오(각 단계 실패 시 무엇이 기대와 달랐는지 출력하고 종료 코드 1, 다 맞으면 `SMOKE OK` 와 0):
  1. `POST /api/auth/dev`(헤더 `X-MUD-Client: 1`, `Origin: http://localhost:3001`)로 관리자·플레이어 A·B 세션을 만들고 응답 `Set-Cookie` 의 `JSESSIONID` 를 각각 보관한다. 이후 REST는 `Cookie` 헤더로, WebSocket은 `new WebSocket(url, { headers: { Cookie, Origin: "http://localhost:3000" } })`(Node 내장 WebSocket이 `headers` 를 지원함을 Node 22에서 확인)로 붙는다.
  2. 관리자 세션으로 지역 만들기 → 방 `광장`, `북쪽 길` → `광장` 북 출구(twoWay) → 시작 방 지정. 플레이어 세션으로 `GET /api/admin/zones` 가 403.
  3. A·B WebSocket 접속 → 각자 이름 정하고 `확인` → `room.room.name == "광장"`.
  4. A가 B의 `appear`(또는 먼저 들어간 쪽이 상대 appear)를 받음. A `보기` → occupants에 B.
  5. A `북` → B가 `leave` 알림, A의 room 이 `북쪽 길`, `exits` 에 `남`.
  6. B `n` → A가 `arrive`. B `'안녕하시오` → A가 `say` `self=false`.
  7. A `누구` → 두 이름. A `종료` → 닫기 1000, B가 `disappear`.
  8. 관리자가 `북쪽 길` 삭제 → B가 `서 있던 곳이 사라져 시작 장소로 옮겨졌습니다.` 와 `광장` room.

- [ ] **Step 1: 스크립트 작성.**
- [ ] **Step 2: 실행** — Task 10 Step 2의 `.env` 로 `docker compose up -d --build` → `source "$NVM_DIR/nvm.sh" && nvm use 26.13.1 && node scripts/smoke-test.mjs` → `SMOKE OK`. 실패하면 원인을 고쳐(필요하면 해당 모듈 코드 수정 + 그 모듈 테스트 통과) 다시 돈다. 고친 것은 progress.md 메모에 적는다.
- [ ] **Step 3: 브라우저 확인(가능하면)** — `http://localhost:3001` 에서 개발용 로그인으로 지역·방·출구를 만들고 시작 방 지정, `http://localhost:3000` 두 창에서 각각 다른 이메일로 들어가 이동·말하기·↑↓·탭 자동완성을 확인. 브라우저를 쓸 수 없으면 건너뛰고 README 체크리스트로 남긴다.
- [ ] **Step 4: README** — "스모크 테스트" 절(위 명령)과 "Google 로그인 수동 확인" 체크리스트(`.env` 에 실제 `GOOGLE_CLIENT_ID`·`GOOGLE_CLIENT_SECRET`, 개발용 로그인 끄기, Google 콘솔의 리디렉션 URI가 `http://localhost:8080/login/oauth2/code/google` 인지 확인, 관리자 이메일로 admin-console 로그인 성공, 목록에 없는 이메일은 `관리자만 쓸 수 있습니다.`, play-client 로그인 → 캐릭터 생성 → 이동).
- [ ] **Step 5: 정리** — `docker compose down`(볼륨은 남김). progress.md에 결과(통과 여부, 고친 것, 수동 확인이 남았으면 그 항목)를 적는다.
- [ ] **Step 6: 커밋** — `test(infra): compose 스모크 테스트와 수동 확인 절차`.

**완료 조건:** `node scripts/smoke-test.mjs` 가 compose 환경에서 `SMOKE OK`. 세 모듈의 검증 명령이 모두 다시 통과.

---

## 태스크 의존 순서

```
1 → 2 → 3 → 4 → 5 → 6 → 7 ─┐
                3,4 ───────→ 8 ─┼→ 10 → 12
                        7 → 9 ──┘
                    (12 다음) 11
```

- 순서대로 하면 된다: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 11. (Task 11은 구현된 값을 옮기므로 통합 확인 뒤에 한다. 리드가 원하면 10 다음에 해도 된다.)
- 8(admin-console)은 4가 끝나면 시작할 수 있고, 9(play-client)는 7이 끝나야 실제 메시지와 맞출 수 있다.
