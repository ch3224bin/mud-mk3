# 플랫폼

게임이 돌아가는 바탕. → [목차](index.md)

## 웹 기반

- 게임은 웹 기반이다. 플레이어는 웹에서 접속해 텍스트로 플레이한다.
- 관리 화면도 있다. → [관리 툴](admin-tool.md)
- 백엔드는 Java Spring으로 구현한다. → [기술 스택](#기술-스택)
- 프론트엔드는 Next.js로 구현한다. → [기술 스택](#기술-스택)

## 기술 스택

기술 선택의 단일 원본이다. 코드를 어떻게 쓰는지(관례·구조·스타일)는 [`.claude/rules`](../../../.claude/rules/README.md)에 둔다.

| 대상 | 선택 · 버전 |
|---|---|
| 언어 | Java 25 (최신 LTS) |
| 프레임워크 | Spring Boot 4.1.2 (BOM 기준 Spring Framework 7.0.10, Hibernate 7.4.13.Final, Jackson 3.1.7, JUnit Jupiter 6.0.3, Mockito 5.23.0) |
| 빌드 | Gradle 9.9.1, Kotlin DSL |
| Gradle 플러그인 | `io.spring.dependency-management` 1.1.7, `com.github.spotbugs` 6.5.13 |
| 테스트 라이브러리 (BOM 밖) | ArchUnit 1.5.1, Instancio 6.2.1 |
| 운영 DB | PostgreSQL (드라이버는 BOM 기준 42.7.14) |
| 테스트 DB | H2 메모리 DB (BOM 기준 2.4.240). 테스트용으로만 쓴다 |
| 프론트엔드 | Next.js 16.4.6, React 19.3.1, TypeScript 7.0.2 — play-client, admin-console. 둘 다 정적 export (제안) |
| 프론트 런타임·빌드 | Node.js 26.13.1, Vite 8.3.8(Vitest용) (제안) |
| 프론트 테스트 | Vitest 5.1.0 + Testing Library(`@testing-library/react` 16.3.3, `user-event` 14.6.10, `jest-dom` 7.0.1), jsdom 31.0.0 (제안) |
| 게임 접속 | WebSocket, 메시지는 JSON → [실시간 통신과 월드 상태](#실시간-통신과-월드-상태) |
| 인증 | Google 로그인 → [인증](auth.md) |
| 배포 | Docker(Dockerfile) + docker-compose → 추후 Kubernetes → [배포](#배포) |
| 이미지 (제안) | PostgreSQL `postgres:18.6-alpine`, 실행 `eclipse-temurin:25.0.5_8-jre`(빌드 `-jdk`), 프론트 빌드 `node:26.13.1-alpine`, 서빙 `nginx:1.30.6-alpine` |

- 버전은 "최신 버전으로" 라는 결정에 따라 2026-11-12 기준 최신 안정판으로 확정했다.
- NPC와 내러티브(NPC 장기 메모리 포함)도 PostgreSQL에 둔다. 저장 방식은 (미정). → [NPC](npc.md#데이터-관리)
- 프론트엔드 버전은 1차 구현에서 `package.json` 에 고정한 값이다. 프론트 모듈마다 `.nvmrc` 에 Node 버전을 둔다. (제안)
- 버전 출처 (2026-11-12 확인):
  - Java: Adoptium API `available_releases` — `most_recent_lts: 25`
  - Gradle: services.gradle.org `versions/current` — 9.9.1
  - Spring Boot: Maven Central `spring-boot` 메타데이터 — 정식 최신 4.1.2(4.2.0은 RC). Boot 4.1 요구 사항: Java 17 이상(27까지 호환), Gradle 8.14 이상 또는 9.x (spring-boot 레포 `system-requirements.adoc`, v4.1.2)
  - BOM 관리 버전: Maven Central `spring-boot-dependencies-4.1.2.pom`
  - Gradle 플러그인: Gradle Plugin Portal 메타데이터
  - ArchUnit, Instancio: Maven Central 메타데이터

## 실시간 통신과 월드 상태

- 게임 접속은 WebSocket으로 한다.
- 브라우저(play-client)가 world-server에 WebSocket으로 직접 연결한다.
- WebSocket으로 오가는 데이터는 JSON이다. 프론트엔드는 JSON을 받아 텍스트로 표현한다.
  - 메시지는 종류를 나타내는 `type` 을 가진 JSON이다: `{type, ...}` (예: `room`, `say`, `combat`, `system`, `prompt`).
  - 강조는 색 코드가 아니라 의미 정보로 보낸다(예: `tone`: `damage`, `npc`, `item`). 텍스트 모양과 테마는 프론트엔드가 정한다.
  - 1차의 메시지 목록과 스키마는 아래 [1차 WebSocket 메시지](#1차-websocket-메시지)에 (제안)으로 정했다. 2차·3차 변경은 [2차](#2차-websocket-메시지-변경)·[3차](#3차-websocket-메시지-변경) 절에 있다. 전투 등 이후 메시지는 (미정).
- 월드 상태(방에 누가 있는지, 전투 등)는 메모리에 두고, 주기적으로 또는 중요한 시점에 DB에 저장한다.
- 저장 주기, 어떤 시점이 "중요한 시점" 인지, 틱은 (미정). 1차 값은 아래 [1차 월드 상태와 저장](#1차-월드-상태와-저장)에 있다.

### 1차 WebSocket 메시지

- 엔드포인트는 `ws://localhost:8080/ws/game` 이다. Spring raw WebSocket이고 STOMP는 쓰지 않는다. 인증은 핸드셰이크의 세션 쿠키로 한다. → [인증](auth.md#websocket-인증) (제안)
- 모든 메시지는 JSON이고 `type` 을 가진다. (제안)

클라이언트 → 서버 (제안)

| type | 필드 | 뜻 |
|---|---|---|
| `command` | `text` (최대 500자) | 입력 한 줄 |

서버 → 클라이언트 (제안)

| type | 필드 | 뜻 |
|---|---|---|
| `session` | `state`: `"creating"` \| `"playing"`, `characterName`: 문자열 \| null | 접속 상태가 바뀔 때 |
| `prompt` | `text` | 캐릭터 생성 중 묻는 말 |
| `room` | `zone`: `{id, name}`, `zoneEntered`: boolean, `room`: `{id, name, description}`, `exits`: 방위 한글 이름 목록(순서 `동 서 남 북 북동 북서 남동 남서 위 아래`), `occupants`: `[{name, kind:"player"}]`(나 빼고, 들어온 순서) | 방 보기 |
| `system` | `text`, `tone`: `"info"` \| `"error"` | 안내·결과·거절. `text` 에 줄바꿈(`\n`)이 있을 수 있다 |
| `notice` | `text`, `tone`: `"movement"`, `actor`, `event`: `"arrive"` \| `"leave"` \| `"appear"` \| `"disappear"` | 같은 방에서 일어난 일(남이 들어옴·나감·접속·접속 끊김) |
| `say` | `speaker`, `message`, `self`: boolean, `text`(그린 한 줄), `tone`: `"speech"` | 말하기 |
| `error` | `code`: `"AUTH_REQUIRED"` \| `"BAD_MESSAGE"`, `text` | 프로토콜 오류 |

- `tone` 은 의미 값이다. 1차에 쓰는 값은 `info`, `error`, `movement`, `speech` 다. 클라이언트는 `room` 을 그릴 때 `zone`(지역 줄)·`room-name`·`exits`·`occupant` 같은 자체 스타일 이름을 쓴다. (제안)
- 알 수 없는 메시지(JSON이 아니거나 `type` 이 모르는 값)와 바이너리 프레임은 `error` `BAD_MESSAGE` 로 답한다. (제안)
- 닫기 코드 (제안):

| 코드 | 뜻 |
|---|---|
| `1000` | `종료` 명령으로 정상 종료 |
| `1013` | 시작 방이 없어 세계가 열리지 않음 |
| `4401` | 로그인 안 됨(세션 없음·만료) |
| `4409` | 같은 계정이 다른 곳에서 접속해 밀려남 |
| `4500` | 이 연결로 못 보냄: 보류한 보내기가 512KB를 넘었거나 보내기에 실패 |

- 서버는 연결마다 보내기 줄(큐)을 두고 한 줄씩 보낸다. 느린 연결 하나가 게임 전체를 막지 않게 하려는 것이다. 보류가 512KB를 넘거나 보내기가 실패하면 그 연결만 4500으로 닫는다. (제안)
- 명령이 일으키는 출력(방 보기, 같은 방 알림 등)과 문구는 [명령어 처리](command.md#1차-명령어)에 있다.

### 2차 WebSocket 메시지 변경

1차 메시지에 더한다. 다른 메시지는 그대로다. (제안)

| type | 필드 | 뜻 |
|---|---|---|
| `room` | 1차 필드 + `owner`: `{kind: "castle" \| "sect", name}` \| `null`(지역 소유 없음). `occupants[].kind`: `"player"` \| `"npc"` | 방 보기 |
| `talk` | `npc`(NPC 이름), `word`(물은 낱말, 인사면 `null`), `text`(그린 줄들, `\n` 으로 나뉨), `tone`: `"talk"` | NPC 대답(묻는 사람에게만) |

- `occupants` 는 나를 뺀 플레이어(들어온 순서) 다음에 그 방에 배치된 NPC(만든 순서)를 `{name, kind: "npc"}` 로 둔다. 룰북 대상 목록 순서(플레이어 → 유니크 NPC)를 따른다. (제안)
- 새 `tone` 값은 `talk` 다. 클라이언트는 방의 NPC 줄에 자체 스타일 이름 `npc` 를, `talk` 의 줄들에 `talk` 를 쓴다. 소유는 방 이름 줄에 붙으므로 `room-name` 스타일을 그대로 쓴다. (제안)
- NPC가 방에 생기거나 사라져도(관리 변경) 같은 방 사람에게 알림 메시지는 없다. 다음 방 보기에 보인다. (제안)
- `대화` 명령의 문구는 [명령어 처리: 2차 명령어](command.md#2차-명령어)에 있다.

### 1차 월드 상태와 저장

- 메모리에 두는 것은 접속 중인 플레이어(캐릭터 id, 이름, 지금 방 id, 세션 id)와 WebSocket 세션 상태다. 메모리 저장소 구현은 `adapter/persistence/memory` 에 둔다. (제안)
- 맵(지역·방·출구)은 메모리에 캐시하지 않고 명령을 처리할 때마다 DB에서 읽는다. 그래서 관리 툴에서 바꾼 맵이 실행 중인 게임에 바로 반영된다. (제안) → [관리 툴](admin-tool.md#관리-툴-변경의-반영)
- 2차의 성·종문·지역 소유·NPC(대사·배치)도 메모리에 두지 않고 명령마다 DB에서 읽는다. NPC는 방에 고정되고 혼자 움직이지 않으므로 메모리 월드 상태에 넣지 않는다. (제안)
- 캐릭터 위치는 이동할 때마다 DB에 저장한다. 그래서 접속을 끊을 때 따로 저장할 것이 없다. 다시 접속하면 저장된 방에서 시작하고, 그 방이 지워졌으면 시작 방에서 시작한다. (제안)
- 플레이어가 서 있는 방을 관리자가 지우면 그 방의 캐릭터를 시작 방으로 옮긴다. 이 반영은 방 삭제가 커밋된 뒤(AFTER_COMMIT)에 게임에 적용된다. 접속 중이면 `system/info` `서 있던 곳이 사라져 시작 장소로 옮겨졌습니다.` 와 시작 방 `room` 을 보내고, 시작 방 사람들에게 `appear` 알림을 보낸다. 반영 전에 명령이 와도 현재 방이 없으면 그 자리에서 시작 방으로 옮긴다. (제안)
- 게임 명령 처리는 서버 전체에서 한 번에 하나로 직렬화한다(공정한 `ReentrantLock` 하나). 1차 사용자 수(소수)에는 충분하다. (제안)
- 같은 계정이 새로 접속하면 앞 연결에 `system/info` `다른 곳에서 접속해 이 연결을 끊습니다.` 를 보내고 4409로 닫는다. 캐릭터는 세계에 그대로 있고(나타난다·사라진다 알림 없음) 새 연결이 이어받아 지금 방 `room` 을 받는다. (제안)
- 틱과 주기 저장은 1차에 없다. (미정)
- 2차에도 서버 틱은 생기지 않는다. NPC가 알아서 말하기를 2차에 넣지 않아 NPC는 플레이어가 말을 걸 때만 답하기 때문이다(창호, 2026-11-14). → [2차 범위](index.md#2차-범위-npc-배치--기본-대화--성종문-정의)

### 3차 WebSocket 메시지 변경

- 3차(아이템)에 메시지가 늘어난다. 창호가 정했다(2026-11-14). → [이터레이션 계획: 3차 범위](index.md#3차-범위-아이템--영석--상점--저물대귀속--주기)
  - `room` 에 바닥 물건이 더해진다(룰북 대상 목록의 맨 끝, 떨어진 물건). → [월드 구조: 방 바닥 물건](world-structure.md#방-바닥-물건)
  - 소지품·물건 보기·상점 목록·사기·팔기·주기·귀속의 결과와, 주기에서 받는 쪽 알림이 생긴다.
- 메시지 이름·필드·`tone` 값(예: 룰북 강조 `item`)은 3차 구현 때 정해 이 절에 `(제안)` 으로 옮긴다. 기존 `system` 으로 보낼지 새 `type` 을 둘지도 그때 정한다.

### 3차 월드 상태와 저장

- 3차에 캐릭터의 소지품(물건·영석·귀속 여부)과 방 바닥 물건이 상태로 생긴다. 창호가 정했다(2026-11-14).
- 소지품을 언제 DB에 저장하는지(바뀔 때마다 / 주기적으로), 바닥 물건을 메모리에만 둘지 DB에도 둘지, 버린 물건의 5분을 무엇으로 잴지(서버 틱·예약 작업)는 3차 구현 때 정한다. → [3차 이터레이션 열린 질문](index.md#3차-이터레이션-열린-질문), [룰북: 줍기와 버리기](../game-rule/item.md#줍기와-버리기)
- 사기·주기처럼 두 쪽(캐릭터와 상점, 두 캐릭터)이 함께 바뀌는 명령이 다 되거나 다 안 되게 하는 방법(1차의 명령 직렬화로 충분한지 등)은 구현 때 정한다.

## 저장소 구성

mud-mk3 저장소 아래 모듈 3개로 나뉜다. 모듈 이름은 역할이 드러나게 지었다(`backend`·`frontend` 같은 이름은 쓰지 않음).

| 모듈 | 역할 | 기술 |
|---|---|---|
| `world-server` | 게임 서버. 월드 상태, 명령 처리, 데이터 저장. play-client와는 WebSocket, admin-console과는 REST로 통신 | Java Spring |
| `play-client` | 플레이 화면 | Next.js |
| `admin-console` | 관리 툴. play-client와 따로 분리된 앱 → [관리 툴](admin-tool.md) | Next.js |

- world-server의 루트 패키지는 `mudmk3` 다. 애그리거트(슬라이스)는 `zone`, `room`, `world`, `account`, `character`, `game`(메모리 월드)이다. (제안)
- 슬라이스 의존은 `room → zone`, `world → room`, `character → world`, `character → account`, `game → character, room, world` 이고 순환은 없다. `character → account` 는 캐릭터를 만들 때 계정이 있는지 확인하려는 의존이다. (제안)
- 2차에 슬라이스 `faction`(성·종문), `npc` 를 더했다. 의존은 `zone → faction`, `room → zone`, `world → room`, `npc → room`, `character → world, account, npc`, `game → character, room, world, npc` 이고 순환은 없다. `character → npc` 는 캐릭터 이름이 NPC 이름과 겹치는지 보려는 의존이다. 성·종문의 소유 지역 수는 `faction` 쪽에서 세어 `faction → zone` 의존을 만들지 않는다. (제안)
- 이 구조와 메모리 저장소 구현 위치는 [코딩 규칙: 아키텍처](../../../.claude/rules/architecture.md)를 따른다.

## 플레이 화면 (play-client)

- 터미널형 화면이다: 스크롤되는 출력 로그 + 아래쪽 입력창.
- 입력창에서 ↑↓ 로 명령 히스토리를 오가고, 탭으로 자동완성한다(명령어, 대상 이름).
- 정적 클라이언트로 만든다. SSR 없이 Next.js 정적 export·클라이언트 위주로 쓴다.
- 방 보기 화면은 `room` 메시지를 아래 모양으로 그린다. 룰북 방 보기 순서에서 1차에 있는 것만 둔다. → [룰북: 방 보기](../game-rule/world.md#방-보기) (제안)

```
◇ 청석숲            ← zoneEntered일 때만
청석숲 오솔길
늙은 소나무 사이로 좁은 길이 굽어 있다.
출구: 북 남동        ← 출구 없으면 "출구: 없음"
한립                ← occupants 한 줄에 하나
```

- 2차 방 보기 (제안): 지역 소유가 있으면 방 이름 줄 뒤에 두 칸 띄우고 `[소유 이름]` 을 붙인다(룰북 `청석숲 오솔길  (영기: 보통)` 처럼 방 이름 줄에 덧붙이는 모양). NPC 줄은 플레이어 줄 다음에 오고 플레이어 줄과 다른 색(스타일 `npc`)으로 그린다. `talk` 메시지는 `text` 를 줄로 나눠 스타일 `talk` 로 그린다. (제안)

```
◇ 청석숲                 ← zoneEntered일 때만 (1차와 같음)
청석성 남문  [청석성]     ← owner 있을 때만 "  [이름]"
돌로 쌓은 성문이 우뚝 서 있다.
출구: 북 남
한립                     ← occupant player (스타일 occupant)
왕씨                     ← occupant npc (스타일 npc)
```
- world-server가 보낸 JSON 메시지를 `type`·`tone` 에 따라 텍스트로 그린다. → [실시간 통신과 월드 상태](#실시간-통신과-월드-상태)

## 관리 툴 (admin-console)

- play-client와 분리된 Next.js 앱이다.
- world-server와 REST로 통신한다.
- play-client처럼 정적 export로 만든다(SSR 없음). 경로는 id를 쿼리스트링으로 받는다. → [관리 툴: 1차 화면](admin-tool.md#1차-화면) (제안)

## 배포

- Dockerfile은 모듈마다 둔다. (제안)
  - world-server: JDK 이미지로 빌드하고 JRE 이미지로 실행한다.
  - play-client·admin-console: node 이미지로 정적 export를 빌드하고 nginx가 정적 파일만 서빙한다.
- 저장소 루트 `compose.yaml` 이 `postgres`, `world-server`, `play-client`, `admin-console` 을 띄운다. 처음에는 맥북에서 docker-compose로 운영해 본다. (제안)
  - 포트: world-server `8080`, play-client `3000`, admin-console `3001`(컨테이너 안은 nginx `80`), PostgreSQL `5432`(루트 compose에서는 밖으로 열지 않음). 브라우저가 world-server에 직접 붙고 프록시는 없다. (제안)
  - 프론트의 `NEXT_PUBLIC_*` 값은 빌드 때 박히므로 compose `build.args` 로 넘긴다. (제안)
  - `world-server/compose.yaml` 은 `./gradlew bootRun`(`local` 프로필) 개발 때 DB만 띄우는 용도다(`spring-boot-docker-compose`). 루트 compose와 별개다. (제안)
  - 1차 스키마는 `spring.jpa.hibernate.ddl-auto=update`(운영), 테스트는 기본 `create-drop` 이다. 마이그레이션 도구(Flyway)는 다음 이터레이션이다. (제안)
- 환경변수 (제안):

| 이름 | 쓰는 곳 | 기본값 · 예 |
|---|---|---|
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | world-server | 없으면 `not-configured` 로 떠서 앱은 뜨지만 Google 로그인은 실패한다(개발용 로그인은 됨). 시작할 때 경고 로그 |
| `MUD_ADMIN_EMAILS` | world-server | 쉼표로 나눈 목록(예: `a@example.com,b@example.com`) |
| `MUD_PLAY_CLIENT_URL`, `MUD_ADMIN_CONSOLE_URL` | world-server(로그인 뒤 돌아갈 곳) | `http://localhost:3000/`, `http://localhost:3001/` |
| `MUD_CORS_ALLOWED_ORIGINS` | world-server(REST CORS + WebSocket Origin) | `http://localhost:3000,http://localhost:3001` |
| `MUD_DEV_LOGIN_ENABLED` | world-server(`local` 프로필에서만 의미) | `false` |
| `SPRING_PROFILES_ACTIVE` | world-server | 비움(운영) / `local` |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | world-server | compose: `jdbc:postgresql://postgres:5432/mud`, `mud`, `${POSTGRES_PASSWORD}` |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | postgres | `mud`, `mud`, 루트 `.env` 에서 읽음(비면 compose가 실패) |
| `NEXT_PUBLIC_WORLD_SERVER_URL` | 두 클라이언트(빌드 때) | `http://localhost:8080` (WebSocket 주소는 `http` 를 `ws` 로 바꿔 `/ws/game` 을 붙임) |
| `NEXT_PUBLIC_DEV_LOGIN` | 두 클라이언트(빌드 때) | `false` (compose에서는 `MUD_DEV_LOGIN_ENABLED` 값을 넘김) |

- 추후 Kubernetes 환경으로 배포한다. 시기와 구성은 (미정).

## 멀티 유저

- 여러 플레이어가 같은 월드에 동시에 접속해 함께 플레이한다.
- 플레이어끼리의 대화(말·귓속말·외치기)는 [명령어 처리](command.md#대화형-명령어)에서 다룬다.

## 단일 월드

- 게임은 거의 창호 또는 소수의 사람만 할 것으로 보여, 월드는 하나만 운영한다. → [월드 구조](world-structure.md)

## 열린 질문

- 월드 상태를 DB에 얼마나 자주 저장하는가? 어떤 시점이 "중요한 시점" 인가? (예: 접속 종료, 돌파, 아이템 획득 등) — 1차는 캐릭터 위치를 이동할 때마다 저장한다(제안으로 정함 → [1차 월드 상태와 저장](#1차-월드-상태와-저장)). 이후 상태는 (미정)
- 게임 틱(전투·회복 등이 도는 주기)을 어떻게 돌리는가? (2차까지는 틱이 없다. 5차 기본 전투에 서버 틱 자동 교전이 들어간다 → [4차 이후 후보](index.md#4차-이후-후보-예정). 3차 버린 물건의 5분은 → [3차 월드 상태와 저장](#3차-월드-상태와-저장))
- 전투 등 1차 이후의 WebSocket 메시지 종류와 `tone` 값은? (1차 메시지는 제안으로 정함 → [1차 WebSocket 메시지](#1차-websocket-메시지))
