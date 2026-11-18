# 기술 부채 정리 2차(debt-2) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 부채 정리 1차가 '창호 결정 필요' 로 남긴 여덟 항목(GitHub #4·#10·#13·#18·#19·#24-1)을 2026-11-18 결정대로 구현한다.

**Architecture:** 새 기능 없음. world-server 는 이펙트 문장 조사(도메인 `EffectLine`), 시전 발동 재확인(`AbilityCaster.fire`), 교전 중 막힌 결제 `확인`(`ShopCommandHandler`), 바뀐 방 상태를 조용히 보내기(`room` `quiet`, 새 `RoomRefresher`), HTTP 세션이 끝날 때 WebSocket 끊기(adapter). play-client 는 조용한 방 상태·여러 낱말 Tab·4401 안내·세션 늘리기, admin-console 은 지워진 시작 소지품 id 표시다.

**Tech Stack:** 1~6차·debt-1 과 같다 — Java 25 · Spring Boot 4.1.2 · Gradle 9.9.1 Kotlin DSL · PostgreSQL 18.6 / H2(테스트) · Next.js 16.4.6 · React 19.3.1 · TypeScript 7.0.2 · Vitest 5.1.0 · Docker Compose. **새 의존성 없음.**

**Spec:** 결정 요약 [index.md `### 부채 정리 2차 (debt-2)`](../../spec/system/index.md#부채-정리-2차-debt-2). 세부: [command.md `## 부채 정리 2차 바뀐 것`](../../spec/system/command.md#부채-정리-2차-바뀐-것)(조용한 방 상태·여러 낱말 Tab), [command.md `### 공격`](../../spec/system/command.md#공격)(막힌 `확인`), [command.md `### 시전과 중지`](../../spec/system/command.md#시전과-중지)(발동 때 영력 0), [admin-tool.md `### 이펙트 문장`](../../spec/system/admin-tool.md#이펙트-문장)(조사로 보는 때), [admin-tool.md `## 3차 화면`](../../spec/system/admin-tool.md#3차-화면)(지워진 물건 #id), [auth.md `### WebSocket 인증`](../../spec/system/auth.md#websocket-인증)(로그아웃·만료 끊기), [platform.md `### 부채 정리 2차 WebSocket 메시지 변경`](../../spec/system/platform.md#부채-정리-2차-websocket-메시지-변경)·[플레이 화면](../../spec/system/platform.md#플레이-화면-play-client). 이슈 본문·댓글(https://github.com/ch3224bin/mud-mk3/issues), [debt-1 계획](../debt-1/plan.md)·[진행](../debt-1/progress.md), `.claude/rules/*.md`.

---

## 이슈 분류

2026-11-18 main(`b5f5cd3`) 기준. **이슈의 모든 항목을 끝내는 태스크의 커밋만 `Fixes #N` 을 단다.**

| 이슈 | 이번에 하는 항목 | 태스크 | 닫기 |
|---|---|---|---|
| #19 | 조사로 보는 때(뒤 글자가 공백·문장 부호·끝, 허용 목록) | 1 | `Fixes #19` |
| #24 | (1) 시전 발동 때 영력 0 재확인 | 2 | 열어 둠 — (2) 대기 시작 시각은 '기록만'(리드 판정 후보 L-3) |
| #18 | 교전 중 막힌 결제 `확인` 은 대기를 지우지 않음(D86 바뀜) | 3 | `Fixes #18` |
| #13 | 처치·재등장·시체 사라짐·짐 뒤 조용한 방 상태(`room` `quiet: true`) | 4 | `Fixes #13` |
| #10 | 주운 뒤 후보 남음(#13 과 같은 방식) | 4 | — |
| #10 | 여러 낱말 이름 Tab 은 이름 전체 | 5 | — |
| #4 | 로그아웃·세션 만료 뒤 WebSocket 끊기 + 안내 | 6 | 열어 둠 — 쿠키 `secure`·세션 저장소 DB화 남음(debt-1 대로 미룸) |
| #10 | admin 시작 소지품의 지워진 물건 id 표시 | 7 | 열어 둠 — debt-1 최종 리뷰가 넘긴 Minor-defer 여섯 남음(`AbilityTable` 공백 이름, `CharacterRealmForm` 테스트, `ZoneForm` 목록 밖 소유, `Terminal` `isNearBottom` 창 줄임·2000줄 자르기 보정·경계 테스트) |

## 리드 판정 후보

구현을 막지 않고 아래대로 진행한다. 틀리면 리드가 바꾼다.

- **L-1 (#4, Task 6) 세션 늘리기.** 결정대로 세션이 끝나면 WebSocket 을 끊으면, 지금 play-client 는 접속 중 HTTP 요청이 없어(auth.md '열린 WebSocket 은 HTTP 세션을 늘리지 않는다') **명령만 치며 30분 놀면 끊긴다.** 그래서 스펙에 `(제안)` 으로 'play-client 가 명령을 보낼 때 마지막 세션 확인에서 5분이 넘었으면 `GET /api/auth/me` 를 한 번 부른다(입력하는 동안만 늘어남)' 를 적었고 Task 6 Step 6 이 구현한다. 리드가 받지 않으면 Task 6 Step 6 과 auth.md 그 문장을 뺀다.
- **L-2 (#4, Task 6) 안내 문구·코드.** 새 문구 `로그인이 끝났습니다. 다시 로그인하십시오.`(결정 예시 '다시 로그인해 주십시오.' 대신 기존 `AUTH_REQUIRED` 문구 `로그인이 필요합니다. 다시 로그인하십시오.` 의 '~하십시오' 문체), 코드는 기존 `error` `AUTH_REQUIRED` + 4401(새 코드·`code` 값 없음 — play-client 의 4401 → 로그인 화면이 그대로 쓰임).
- **L-3 (#24) 닫기.** (2) 재사용 대기 시작 시각은 debt-1 이 '스펙이 허용, 고칠 것이 아님(기록만)' 으로 정했다. Task 2 뒤 리드가 댓글로 (2)의 기록을 남기고 이슈를 닫을지 정한다(커밋은 `Fixes` 없이 `Refs #24`).
- **L-4 (#18, Task 3) 범인 막힘은 그대로.** 4차 `범인의 상점 막힘` 의 '결제 `확인` 은 대기를 먼저 지운 뒤 막는다' 는 결정 범위(교전 중) 밖이라 바꾸지 않았다(스펙에 그렇게 적음). 같은 규칙으로 맞추려면 리드가 말한다.
- **L-5 (#13, Task 4) 보내는 때의 범위.** 결정이 든 넷(처치·재등장·시체 사라짐·짐)에 #10 '주운 뒤' 를 따라 `줍기`·`버리기`, 그리고 관리 툴이 배치를 지워 몹이 사라질 때를 더했다. 버린 물건 5분 사라짐·바닥 배치 다시 놓임(읽을 때 계산이라 사건이 없음)과 교전 상태만 바뀔 때는 보내지 않는다.
- **L-6 (#13, Task 4) 기존 테스트 기대값.** 조용한 방 상태가 늘어 기존 테스트의 `containsExactly` 가 깨지지 않게, 테스트 도구 `RecordingGameMessageSender.messagesTo` 와 통합 테스트 `Client.next` 는 `quiet: true` 인 `room` 을 건너뛰고 새 도우미로 따로 본다(기대값 무변경). 스모크의 `room` 기다림도 같은 이유로 `quiet` 를 건너뛴다.
- **L-7 (#10) Minor-defer 여섯.** 결정 범위가 아니라 이번에 넣지 않았다. 넣으려면 Task 7 뒤에 태스크 하나(sonnet)로 묶을 수 있다.
- **L-8 (Task 8) client 이미지.** 스모크 때문에 `MUD_DEV_LOGIN_ENABLED=true` 로 빌드한 play-client·admin-console 이미지를 dev 로그인을 끄고 다시 빌드할지(debt-1 은 그대로 둠). 다시 빌드하면 Docker 를 한 번 더 쓴다.

**리드 판정 (확정, 2026-11-18)** — 위 후보보다 앞선다.
- L-1: 받는다(입력할 때 5분마다 `/api/auth/me` 로 세션 늘리기). 근거: 없으면 명령만 치며 노는 플레이어가 30분마다 끊긴다.
- L-2: 후보대로(문구 문체·4401 재사용).
- L-3: #24 는 Task 2 뒤에 닫는다. (2) 대기 시작 시각은 기록만 남긴다(닫기 댓글에 적음).
- L-4: 후보대로(범인 막힘의 `확인` 은 대기를 지운다 — 결정은 교전 중 막힘만 다룸).
- L-5: 받는다(줍기·버리기·배치 삭제까지 조용한 방 상태). 근거: #10 '주운 뒤 후보' 결정이 같은 방식을 요구한다.
- L-6: 후보대로.
- L-7: #10 의 Minor-defer 는 이번에 넣지 않는다(범위 밖, #10 은 열린 채 남김).
- L-8: 이번에 dev 로그인 끈 이미지를 다시 빌드하지 않는다(compose 빌드 한 번 규칙). 창호 할 일로 남긴다.

**사전 점검 판정 (리드, 2026-11-18)** — 태스크 본문보다 앞선다.
- F-1 (T3): `공격` 명령은 확인 대기를 먼저 지우므로, 막힌 `확인` 상태는 'QI_1 + 낙뢰술 익힘 → `낙뢰술 청모랑` → `사기` → `advanceAndTick(2)`(시전 발동으로 교전 시작)' 로 만든다. 세 테스트 모두 이 준비를 쓴다.
- F-2 (T8): 스모크 단계 52(#18)는 뺀다. #18 은 단위 테스트로만 본다.
- F-3 (T7): `GET /api/admin/world/starting-items` 는 지워진 물건 id 도 행으로 돌려준다(`itemName` null, 500 금지). WebMvc 테스트와 admin-tool.md (제안) 한 줄을 더한다.
- F-4 (T6): 로그아웃 버튼은 연결을 먼저 닫고(또는 로그아웃 중엔 `onUnauthorized` 무시) 서버 안내 줄이 보이지 않게 한다.
- F-5 (T6): 세션 id 는 sender 안의 동시성 안전한 구조(ConcurrentHashMap 등)에 둔다. 핸드셰이크~등록 사이 로그아웃 틈은 progress 에 한계로 적는다.
- F-6 (T6): 같은 브라우저 다른 계정 로그인 때 연결이 새 세션 id 를 따라가는 것은 스펙대로 두고 progress 에 기록한다.
- F-7 (T2): `learn` 에는 `TechniqueFixture.withCost`·`withCast` 를 쓴다.
- F-8 (T6): Files 에 `WebSocketGameMessageSenderTest` 를 넣고, id 가 바뀐 뒤 새 id 로 연결을 닫는지 보는 테스트를 더한다.
- F-9 (T4): '실패한 처리가 남긴 조용한 방 표시가 다음 처리로 새지 않음' 테스트를 더한다. 통합 테스트의 `next()`/`nextQuietRoom()` 버퍼 규칙은 구현자가 정해 보고한다.
- F-10·F-11·F-12: 구현자 재량/그대로. 새 스모크 단계는 `SMOKE OK` 출력 앞에 넣는다.

## Global Constraints

모든 태스크의 요구 사항에 이 절이 포함된다. 구현자는 이 절과 자기 태스크, `## 이슈 분류` 를 먼저 읽는다.

**범위**
- 위 결정과 스펙에 적힌 동작·문구만 바꾼다. 그 밖의 게임 규칙·화면 문구·서버 메시지·`(제안)` 결정은 바꾸지 않는다. 룰북(`docs/spec/game-rule/`)은 고치지 않는다(결정과 부딪치는 문장 없음). 스펙은 1단계에서 이미 고쳤다 — 구현이 스펙과 다르게 가야 하면 멈추고 리드에게 보고한다.
- 기존 테스트의 기대값(문구·수치)은 그 태스크가 '바뀌는 기존 테스트' 로 적은 것만 바꾼다. 그 밖에 바꿔야 통과하면 멈추고 보고한다.

**버전·의존성** — **새 라이브러리·플러그인·이미지를 넣지 않는다.** `build.gradle.kts`, `package.json`, `package-lock.json`, Dockerfile, `compose.yaml` 의 버전 줄을 바꾸지 않는다. HTTP 세션 끝 감지는 서블릿 표준 `jakarta.servlet.http.HttpSessionListener`·`HttpSessionIdListener` 빈(Spring Boot 가 등록)으로 한다.

**로컬 환경**
- macOS arm64, 기본 JDK Temurin 25. world-server 는 항상 `./gradlew`.
- 프론트 명령은 모듈의 `.nvmrc`(26.13.1)로: `cd <module> && source "$NVM_DIR/nvm.sh" && nvm use`.
- **디스크:** 태스크 시작 전 `df -h .` 를 본다. 남은 공간이 **10GB 미만이면 아무것도 지우지 말고 멈춰** 리드에게 보고한다. `ENOSPC` 도 같다. (계획 작성 때 54Gi.)
- **Docker 빌드·실행은 Task 8 에서 한 번만** 한다. Task 1~7·9 는 `docker build`, `docker compose build/up` 을 쓰지 않는다. **`pgdata` 볼륨은 어느 태스크에서도 지우지 않는다.**

**코드 규칙**
- world-server 코딩 규칙(`.claude/rules/*.md`), 루트 패키지 `mudmk3`. 아래 world-server 경로는 `world-server/src/{main,test}/java/mudmk3/` 기준이다.
- 문구는 한국어, 기존 상수·`Josa`·`Numbers` 를 그대로 쓴다. 새 문구는 스펙의 글자 그대로.
- **게임 락:** 명령·틱·접속·끊김은 모두 `GameService.serialized` 하나를 지난다. 메모리 상태는 그 안에서만 건드린다. WebSocket 어댑터의 보내기·닫기(`WebSocketGameMessageSender`)는 락 밖에서도 안전하다(연결마다 줄).
- **깎인 기혈·법력 열은 전용 네이티브 쿼리로만 쓴다.** 새 enum 열·값을 더하지 않는다.
- **AFTER_COMMIT 과 테스트 트랜잭션:** `@ApplicationServiceTest` 는 롤백되어 AFTER_COMMIT 리스너가 오지 않는다(`BaseGameTest.designateRealm` 관례).
- 자동 테스트는 Google 자격 증명·네트워크 없이 돈다. 게임 테스트의 시간은 `MutableClock` + 직접 `tick()`(`advance`·`advanceAndTick`), 난수는 `ScriptedRandom`. **실제로 기다리는 테스트(`Thread.sleep`, Awaitility)를 쓰지 않는다**(기존 `GameWebSocketIntegrationTest` 의 줄 기다림 `next()` 는 그대로 쓴다).
- Next.js 앱 규칙은 1~6차와 같다(정적 export, `"use client"`, 상대 경로 import, 쿼리스트링 id). admin-console 은 저장 전 클라이언트 검사를 하지 않고 서버 `detail` 을 보인다.
- 툴 호출 파라미터(JSON)의 한글은 리터럴 UTF-8로 쓰고 `\uXXXX` 이스케이프를 쓰지 않는다.

**저장소·커밋**
- **main 에서 일한다.** 태스크 하나 = 커밋 하나 이상. 구현자는 커밋까지 하고, 리뷰를 통과하면 리드가 `git push` 하고 이슈 댓글·닫기를 확인한다. 구현자는 서브에이전트를 띄우지 않고 GitHub 이슈에 쓰지 않는다.
- 커밋 메시지: `<type>(<module>): <한국어 요약>`(type: feat, test, build, docs, chore, fix, refactor / module: world-server, play-client, admin-console, infra, spec, impl). 태스크에 적힌 경우에만 본문에 `Fixes #N`(또는 `Refs #N`) 줄. 끝에 빈 줄 다음 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- 태스크를 끝내면 `docs/impl/debt-2/progress.md` 의 표에 한 줄(태스크 번호·제목, 커밋 해시는 리드가 채움, 메모)을 더하고 같은 커밋에 넣는다. 각 태스크 끝의 **progress.md 한 줄** 이 메모의 틀이다(실제로 바꾼 것·계획과 다른 점을 덧붙인다).

**검증 명령 (각 태스크 완료 조건, 고친 모듈만)**
- world-server: `cd world-server && ./gradlew build`(테스트 + SpotBugs + ArchUnit)
- admin-console / play-client: `cd <module> && source "$NVM_DIR/nvm.sh" && nvm use && npm ci && npm test && npx tsc --noEmit && npm run build`
- 스모크 스크립트를 고친 태스크: `node --check scripts/smoke-test.mjs`

**스모크 (Task 8만)** — README `### 스모크 테스트` 방법 그대로: `.env` 는 고치지 않고 `docker compose build` 한 번 → `up` 때만 셸 값으로 `MUD_DEV_LOGIN_ENABLED=true`, `MUD_ADMIN_EMAILS=<.env 의 기존 값>,smoke-admin@example.com` 을 덮어써 `docker compose up -d` → `node scripts/smoke-test.mjs`. **기존 pgdata 로 올린다.** DB 서비스 이름은 `postgres` 다(debt-1 Task 13 메모).

## Review Focus

테스트가 직접 겨누기 어려워 가장 깨지기 쉬운 것. 각 줄의 테스트는 담당 태스크에 있다.

1. **조용한 방 상태가 화면에 찍히는 일** — play-client 가 `quiet: true` 를 방 보기로 그리면 처치·줍기마다 방 설명이 쏟아진다. (Task 4 `render.test` `quietRoomRendersNothing`, `Terminal.test` 'quiet room 은 그리지 않고 Tab 후보만 바꾼다')
2. **한 처리에 여러 번** — 한 틱에 같은 방에서 몹 둘이 재등장·처치되면 조용한 방 상태가 한 번만 간다. 실패한 처리가 남긴 표시가 다음 처리로 새지 않는다. (Task 4 `quietRoomOncePerProcessing`)
3. **다른 브라우저의 로그아웃** — 같은 계정의 다른 HTTP 세션이 로그아웃해도 이 연결은 끊기지 않는다. 로그인 때 세션 id 가 바뀌어도 따라간다. (Task 6 `otherSessionLogoutKeepsSocket`, `HttpSessionEndListenerTest.sessionIdChangeIsFollowed`)
4. **막힌 확인과 만료** — 막힌 `확인` 이 대기를 남겨도 10초 만료는 그대로이고, 막힌 `사기`·`목록` 은 여전히 대기를 지운다. (Task 3 `blockedConfirmExpires`·`blockedShopCommandStillClearsPending`)
5. **조사 규칙의 경계** — 문장 끝·`!`·`,`·숫자·영문 다음 글자에서 조사로 보는지, 허용 목록 꼴만 이어서 보는지. 기존 `EffectLineTest` 기대값이 그대로다. (Task 1 `particleOnlyBeforeBoundary`·`allowedTails`)

---

## 파일 구조 (요약, 새로 생기거나 바뀌는 것)

```
world-server/src/main/java/mudmk3/
  domain/technique/EffectLine.java                                                  (T1)
  application/game/AbilityCaster.java                                               (T2)
  application/game/ShopCommandHandler.java                                          (T3)
  application/game/RoomRefresher.java (새), required/GameMessage.java(RoomMessage.quiet),
    RoomViewAssembler.java, GameService.java, CombatActions.java, MobSpawner.java,
    CorpseHandler.java, ItemCommandHandler.java                                     (T4)
  adapter/websocket/{GameHandshakeInterceptor,GameWebSocketHandler,WebSocketGameMessageSender}.java,
    HttpSessionEndListener.java (새)                                                (T6)
world-server/src/test/java/mudmk3/ ...                                              (태스크마다)
play-client/src/lib/{protocol,render,complete}.ts, sessionKeepAlive.ts (새),
  src/components/{Terminal,LoginScreen}.tsx, app/page.tsx                          (T4·T5·T6)
admin-console/src/components/StartItemsEditor.tsx                                   (T7)
scripts/smoke-test.mjs                                                              (T4 기다림 도우미, T8 단계)
```

---

### Task 1: 이펙트 문장 조사는 뒤가 끊길 때만 (#19)

**권장 모델:** opus

**스펙:** admin-tool.md `### 이펙트 문장` '조사로 보는 때'.

**Files:**
- Modify: `domain/technique/EffectLine.java`(`PARTICLES`·`render` — javadoc 도 새 규칙으로)
- Test: `domain/technique/EffectLineTest.java`

**Interfaces:**
- Produces: `EffectLine.render(String line, @Nullable String targetName, String casterName)` 시그니처 그대로. 규칙: 자리표시 바로 뒤에서 조사 글자 P(`으로` 를 `로` 보다 먼저)를 찾고, P 다음 위치 j 가 (a) 문장 끝이거나 `!Character.isLetterOrDigit(line.charAt(j))` 이면 조사, (b) 아니면 P 의 쌍에 허용된 꼬리 T 가 j 에서 시작하고 T 다음이 (a) 조건이면 조사(P 만 바꾸고 T 는 그대로 붙임), (c) 그 밖은 조사가 아니어서 아무것도 바꾸지 않는다(P 를 원문 그대로 둠). 허용 꼬리: `으로/로` → `부터`·`써`·`서`·`도`·`는`·`만`·`의`, `과/와` → `도`·`는`·`의`·`만`, 나머지 쌍은 없음. 표는 `Particle` record 에 `List<String> tails` 를 더해 둔다.

- [ ] **Step 1: 실패하는 테스트** — `EffectLineTest` 에 더한다(기존 `render`·`renderEveryPlaceholder`·`renderWithoutTargetKeepsPlaceholder` 는 그대로 통과해야 한다):

```java
@Test
void particleOnlyBeforeBoundary() {
    assertThat(EffectLine.render("{대상}가슴을 벤다", "청모랑", "한백")).isEqualTo("청모랑가슴을 벤다");
    assertThat(EffectLine.render("{대상}이마를 스친다!", "들토끼", "한백")).isEqualTo("들토끼이마를 스친다!");
    assertThat(EffectLine.render("{대상}은빛 털", "들토끼", "한백")).isEqualTo("들토끼은빛 털");
    assertThat(EffectLine.render("{대상}이나 {시전자}", "청모랑", "한백")).isEqualTo("청모랑이나 한백");
    assertThat(EffectLine.render("{대상}을, 다시", "들토끼", "한백")).isEqualTo("들토끼를, 다시");
    assertThat(EffectLine.render("{대상}이!", "들토끼", "한백")).isEqualTo("들토끼가!");
    assertThat(EffectLine.render("{대상}는…", "청모랑", "한백")).isEqualTo("청모랑은…");
    assertThat(EffectLine.render("{대상}가3", "청모랑", "한백")).isEqualTo("청모랑가3");
}

@Test
void allowedTails() {
    assertThat(EffectLine.render("{대상}로써 막는다", "청모랑", "한백")).isEqualTo("청모랑으로써 막는다");
    assertThat(EffectLine.render("{대상}으로서 선다", "들토끼", "한백")).isEqualTo("들토끼로서 선다");
    assertThat(EffectLine.render("{대상}로부터!", "청모랑", "한백")).isEqualTo("청모랑으로부터!");
    assertThat(EffectLine.render("{대상}와의 싸움", "청모랑", "한백")).isEqualTo("청모랑과의 싸움");
    assertThat(EffectLine.render("{시전자}과도 같다", "청모랑", "여소이")).isEqualTo("여소이와도 같다");
    assertThat(EffectLine.render("{대상}이도 운다", "청모랑", "한백")).isEqualTo("청모랑이도 운다"); // 이/가 꼬리 없음
}
```

- [ ] **Step 2: 실패 확인** — `cd world-server && ./gradlew test --tests '*EffectLineTest'` → `particleOnlyBeforeBoundary` FAIL(지금은 `청모랑이슴을 벤다`·`들토끼가마를 스친다!`). `allowedTails` 는 지금 코드로도 통과할 수 있다 — 새 규칙에서 허용 꼬리가 깨지지 않게 묶어 두는 회귀 테스트다.
- [ ] **Step 3: 구현** — Interfaces 규칙대로 `render` 의 조사 고르기만 바꾼다. 자리표시 찾기·`{대상}` 이 null 일 때 그대로 두기는 그대로.
- [ ] **Step 4: 통과 확인** — `./gradlew build`.
- [ ] **Step 5: 커밋** — `fix(world-server): 이펙트 문장 조사는 뒤가 공백·문장 부호·끝일 때만 고침` + 본문 `Fixes #19`.

**완료 조건:** 새 테스트·기존 `EffectLineTest` 통과, `./gradlew build` 통과.
**progress.md 한 줄:** 조사 판정 규칙(경계·허용 꼬리), 바뀐 메서드, 테스트.

---

### Task 2: 시전 발동 때 영력 0 재확인 (#24-1)

**권장 모델:** opus

**스펙:** command.md `### 시전과 중지` '발동' 의 영력 0 줄.

**Files:**
- Modify: `application/game/AbilityCaster.java`(`fire`, javadoc)
- Test: `application/game/provided/AbilityCastingTest.java`

**Interfaces:**
- Consumes: `AbilityTexts.NO_SPIRIT`(`영력이 없어 술법을 펼칠 수 없습니다.`), `CultivationFinder.stats(id).spirit()`, `Ability.isBodyRefining()`.
- Produces: `fire` 의 재확인 순서 = 시전 지움 → 세계에 없음 → 신통·대상(흩어짐) → **영력 0 이고 연체가 아님 → `SystemMessage.error(AbilityTexts.NO_SPIRIT)` 하고 끝** → 법력 → `cast`. 소모·재사용 대기·교전 시작 없음.

- [ ] **Step 1: 실패하는 테스트** — `AbilityCastingTest`:
  - `castingNoSpiritAtFire`: 기존 `castingManaShortAtFire` 처럼 `낙뢰술 청모랑` 시전 시작 → `setRealm(han, Realm.MORTAL)` → `sender.clear()` → `advanceAndTick(2)` → `sender.messagesTo("a")` 가 `containsExactly(SystemMessage.error("영력이 없어 술법을 펼칠 수 없습니다."))`(고치기 전에는 법력 거절 줄), `mobNamed("청모랑").isFighting()` false, `assertNoCooldown(han)`, `assertNotCasting(han)`.
  - `castingNoSpiritZeroCostElementalAtFire`: 소모 없음·시전 2초인 오행 신통(테스트 안에서 `TechniqueRequestFixture` 의 `withCost`·`withCast` 로 정의해 익힘)으로 같은 순서 → 같은 한 줄, 몹 기혈 그대로(고치기 전에는 발동해 피해).
  - `castingBodyRefiningAtMortalFires`: 소모 없음·시전 2초 연체 신통은 범인으로 내려도 발동한다(이펙트 줄 하나 + 몹이 교전 중).
- [ ] **Step 2: 실패 확인** — `./gradlew test --tests '*AbilityCastingTest'` → 앞 둘 FAIL.
- [ ] **Step 3: 구현** — Interfaces 순서대로 `fire` 에 검사 하나.
- [ ] **Step 4: 통과 확인** — `./gradlew build`.
- [ ] **Step 5: 커밋** — `fix(world-server): 시전 발동 때 영력 0 이면 오행 신통이 흩어짐` + 본문 `Refs #24`.

**완료 조건:** 위 테스트와 기존 `AbilityCastingTest` 통과, `./gradlew build` 통과.
**progress.md 한 줄:** 재확인 위치, 테스트 셋, #24(2) 는 기록만(L-3).

---

### Task 3: 교전 중 막힌 결제 `확인` 은 대기를 지우지 않음 (#18)

**권장 모델:** opus

**스펙:** command.md `### 공격` 교전 중 막힘 '막힌 결제 `확인` 과 확인 대기'.

**Files:**
- Modify: `application/game/ShopCommandHandler.java:79-91`(막힘 분기, javadoc 의 'D86 — 막힌 확인은 대기도 사라진다' 를 새 규칙으로)
- Test: `application/game/provided/GameConnectorTest.java`(`// ---- 교전 중 막힘(D86)` 절)

**Interfaces:**
- Produces: 교전 중 막힘 분기에서 `session.clearPending()` 은 막힌 명령이 `Command.Confirm` 이 **아닐** 때만 부른다(`목록`·`사기`·`팔기`·`값` 은 '다른 명령' 이라 `GameService.handleCommand` 가 이미 지우므로 결과는 같음). 범인 막힘(4차)은 그대로.

- [ ] **Step 1: 실패하는 테스트** — `GameConnectorTest`, 준비는 `buyBreaksHighStoneNeedsConfirm`(상품 영석 하나·저물대, `setRealm(QI_1)`) + `fightingBlocksCommands`(`prepareShop`·`placeMob(map, wolf(), 1)`):
  - `blockedConfirmKeepsPending`: `사기 회춘단 40` → `BUY_CONFIRM` → `공격 청모랑` → `확인` → error `싸우는 중에는 할 수 없습니다.` → 다시 `확인` → 같은 error(고치기 전에는 `확인할 것이 없습니다.`) → 10초 안에 교전을 끝냄(그 몹 배치를 `MobPlacementEditor` 로 지우고 `advanceAndTick(1)` — 몹이 사라져 교전이 끝남, 다른 방법이면 메모) → `확인` → item `회춘단(연기 초) 40개를 샀습니다. (하품 영석 -120)`.
  - `blockedConfirmExpires`: 막힌 `확인` 뒤 `clock.advance(Duration.ofSeconds(11))` → `확인` → error `확인할 것이 없습니다.`.
  - `blockedShopCommandStillClearsPending`: `사기 회춘단 40` → `공격 청모랑` → `목록`(막힘) → `확인` → error `확인할 것이 없습니다.`.
- [ ] **Step 2: 실패 확인** — `./gradlew test --tests '*GameConnectorTest'` → `blockedConfirmKeepsPending` FAIL.
- [ ] **Step 3: 구현** — Interfaces 대로.
- [ ] **Step 4: 통과 확인** — 기존 `confirmAfterDemotedToMortalFail`(범인 막힘은 그대로)·`fightingBlocksCommands` 포함 `./gradlew build`.
- [ ] **Step 5: 커밋** — `fix(world-server): 교전 중 막힌 확인은 결제 대기를 지우지 않음` + 본문 `Fixes #18`.

**완료 조건:** 위 테스트 통과, `./gradlew build` 통과.
**progress.md 한 줄:** 바뀐 분기, 교전을 끝낸 방법, 테스트 셋, 범인 막힘 그대로(L-4).

---

### Task 4: 바뀐 방 상태를 조용히 보내기 — 서버 + play-client (#13, #10 주운 뒤 후보)

**권장 모델:** opus

**스펙:** command.md `### 방 상태 조용히 보내기`, platform.md `### 부채 정리 2차 WebSocket 메시지 변경`·플레이 화면.

**Files (world-server):**
- Create: `application/game/RoomRefresher.java`(package-private `@Component`)
- Modify: `application/game/required/GameMessage.java`(`RoomMessage` 끝에 `boolean quiet`), `application/game/RoomViewAssembler.java`(`assemble` 에 `boolean quiet`), `application/game/GameService.java`(`serialized`·`sendRoom`·725행 근처 조립 도우미, `wakeAtStart` 옛 방 표시), `application/game/CombatActions.java`(`onMobKilled`), `application/game/MobSpawner.java`(`notifyAppeared`·`remove`), `application/game/CorpseHandler.java`(`removeExpired`·`collect` 에서 시체가 사라질 때), `application/game/ItemCommandHandler.java`(`pick`·`drop` 성공)
- Test: `application/game/RecordingGameMessageSender.java`, `application/game/provided/{CombatRewardTest,CombatSpawnTest,CombatDefeatTest,GameConnectorTest}.java`, `adapter/websocket/WebSocketGameMessageSenderTest.java`(JSON 에 `"quiet":false`), `adapter/websocket/GameWebSocketIntegrationTest.java`(`Client`)

**Files (play-client `src/` 기준):** Modify `lib/protocol.ts`(`RoomMessage.quiet: boolean`), `lib/render.ts`; Test `lib/render.test.ts`, `components/Terminal.test.tsx`(+ room 픽스처에 `quiet: false`).
**Files (스모크):** `scripts/smoke-test.mjs` — `isRoom` 과 그 밖에 `type === "room"` 을 기다리는 곳이 `quiet: true` 를 건너뛰게(L-6).

**Interfaces:**
- Produces: `RoomRefresher { void mark(Long roomId); Set<Long> drain(); void clear(); }` — 메모리 `LinkedHashSet`(표시한 순서), 게임 락 안에서만 쓴다. `GameService.serialized` 는 `outbox.clear()` 옆에서 `roomRefresher.clear()`, `work.run()` 뒤 **같은 트랜잭션 안에서** `drain()` 한 방마다 그 방이 있으면(지워졌으면 건너뜀) `presenceRepository.findAllInRoom(roomId)` 의 사람마다 그 사람 기준 `RoomMessage`(`zoneEntered=false`, `quiet=true`)를 `send`. 보통 방 보기는 `quiet=false`.
- 표시(mark)하는 곳: 몹 쓰러짐(그 몹의 방), 몹 나타남·배치가 지워져 몹을 뺌(그 방), 시체가 사라짐(만료·다 수습·빈 시체), 짐(`wakeAtStart` 의 옮기기 전 방), `줍기`·`버리기` 성공(그 방). 사람 이동·접속·끊김에는 표시하지 않는다.
- 테스트 도구(L-6): `RecordingGameMessageSender.messagesTo(sessionId)` 는 `quiet()` 인 `RoomMessage` 를 빼고 돌려주고, 새 `List<RoomMessage> quietRoomsTo(String sessionId)` 와 순서 확인용 `List<GameMessage> allMessagesTo(String sessionId)` 를 둔다. 통합 테스트 `Client.next()` 는 `quiet: true` room 을 건너뛰고 새 `JsonNode nextQuietRoom()` 이 그것만 기다린다.
- play-client: `renderMessage` 는 `room` 이 `quiet` 이면 `[]`. `Terminal` 의 `updateNames` 는 보통 `room` 과 같이 후보를 바꾼다(코드 변경 없음이 기대값).

- [ ] **Step 1: 실패하는 테스트 (world-server)**
  - `CombatRewardTest.killSendsQuietRoom`: 한백("a")·한립("b", 교전 안 함)이 같은 방, a 가 청모랑을 쓰러뜨림 → `allMessagesTo("a")` 의 마지막이 `quiet` room 이고 그 앞에 `청모랑을 쓰러뜨렸습니다.` 가 있다. `quietRoomsTo("a")`·`quietRoomsTo("b")` 가 각각 한 개, `mobs()` 에 청모랑 없음, `corpses()` 이름 `청모랑의 시체`, `zoneEntered()` false. b 의 `messagesTo` 는 고치기 전과 같다(`kill` notice 하나).
  - `CombatRewardTest.corpseExpirySendsQuietRoom`: 시체 3분 지남 틱 → 그 방 사람 `quietRoomsTo` 하나, `corpses()` 비어 있음. `collectAllSendsQuietRoom`: 다 수습하면 같은 것(수습한 사람 포함).
  - `CombatSpawnTest.respawnSendsQuietRoom`: 재등장 틱 → `청모랑이 나타난다.` 뒤에 quiet room 하나, `mobs()` 에 청모랑.
  - `CombatDefeatTest.defeatSendsQuietRoomToOldRoom`: 진 한백의 옛 방에 있던 b 는 `defeated` notice 뒤 quiet room 하나(`occupants()` 에 한백 없음), 새 notice 는 없다. 한백은 시작 방 보통 room(`quiet()` false).
  - `GameConnectorTest.pickSendsQuietRoom`·`dropSendsQuietRoom`: 줍기·버리기 한 사람과 같은 방 사람 모두 quiet room 하나(`items()` 가 바뀐 바닥).
  - `GameConnectorTest.quietRoomOncePerProcessing`: 같은 방 배치 하나에 몹 둘이 같은 틱에 재등장 → 그 방 사람 `quietRoomsTo` 정확히 하나. 이어 아무 일 없는 틱 → 더 없음.
  - `GameConnectorTest.lookSendsNormalRoom`: `보기` 의 room 은 `quiet()` false, 이동 뒤 room 도 false. `moveSendsNoQuietRoom`: 사람 이동만으로는 quiet room 없음.
  - `WebSocketGameMessageSenderTest`: 기존 room JSON 기대값 셋에 `"quiet":false` 를 더함(바뀌는 기존 테스트).
- [ ] **Step 2: 실패 확인** — `./gradlew test --tests '*CombatRewardTest' --tests '*CombatSpawnTest' --tests '*CombatDefeatTest' --tests '*GameConnectorTest'` → 새 테스트 FAIL(컴파일 실패 포함).
- [ ] **Step 3: 구현 (world-server)** — Interfaces 대로. `RoomMessage` 생성은 `RoomViewAssembler` 와 테스트뿐이다. `CombatMemoryReset` 은 저장소가 아닌 `RoomRefresher` 때문에 바꿀 필요 없음(처리마다 비움).
- [ ] **Step 4: 통과 확인** — `./gradlew build`. 기존 게임·통합 테스트 기대값은 L-6 도구 덕에 그대로여야 한다(바뀐 것이 있으면 멈추고 보고).
- [ ] **Step 5: 실패하는 테스트 (play-client)**
  - `render.test.ts` `quietRoomRendersNothing`: `renderMessage({...room, quiet: true})` 가 `[]`.
  - `Terminal.test.tsx` "quiet room 은 그리지 않고 Tab 후보만 바꾼다": 청모랑이 있는 room → `quiet: true` room(청모랑 없음, 시체 `청모랑의 시체`) → 로그 줄 수 그대로, `공격 청` + Tab → `공격 청모랑의 시체`.
  - "주운 뒤 quiet room 이 오면 바닥 물건 후보가 빠진다": 바닥 `늑대 가죽` room → `pick` notice(후보 그대로) → quiet room(items 빔) → `줍기 늑` + Tab 은 그대로 `줍기 늑`.
- [ ] **Step 6: 구현 (play-client)** — `protocol.ts` 에 `quiet: boolean`(주석: 부채 정리 2차, 조용한 방 상태), `render.ts` 의 `room` 분기. 기존 테스트의 room 픽스처에 `quiet: false` 를 더한다(바뀌는 기존 테스트, 기대값 무변경).
- [ ] **Step 7: 스모크 기다림** — `scripts/smoke-test.mjs` 의 `isRoom` 등 room 기다림이 `m.quiet !== true` 를 함께 보게 고치고 `node --check scripts/smoke-test.mjs`.
- [ ] **Step 8: 검증** — world-server `./gradlew build`, play-client 검증 명령.
- [ ] **Step 9: 커밋** — `feat(world-server): 처치·재등장·시체 사라짐·짐·줍기 뒤 바뀐 방 상태를 조용히 보냄`(play-client·스모크 포함, 또는 모듈마다 나눠도 됨) + 마지막 커밋 본문 `Fixes #13`.

**완료 조건:** 위 테스트 통과, 세 검증 통과, 기존 게임 테스트 기대값 무변경.
**progress.md 한 줄:** 표시하는 곳 목록, `RoomRefresher` 와 보내는 자리, 테스트 도구 변경(L-6), play-client·스모크 변경.

---

### Task 5: 여러 낱말 이름 Tab 은 이름 전체 (#10)

**권장 모델:** sonnet

**스펙:** command.md `### 여러 낱말 이름 자동완성`.

**Files (play-client `src/` 기준):** Modify `lib/complete.ts`(`Completer.complete`, javadoc); Test `lib/complete.test.ts`.

**Interfaces:**
- Produces: `Completer.complete(input: string, names: string[]): string` 시그니처 그대로. 첫 낱말(공백 전)이면 지금처럼 `COMMANDS`. 아니면 명령어 다음 낱말 시작부터 마지막 낱말 시작까지의 각 낱말 시작 위치를 **앞에서부터** 보며 꼬리 `input.slice(pos)` 가 비어 있지 않고 `names` 가운데 그 꼬리로 시작하는 것이 있으면 그 위치를 `head` 경계로 쓴다(가장 긴 꼬리 우선). 없으면 입력 그대로. 거듭 부르기·`reset` 은 지금 그대로(`cycle.head` 가 그 경계까지).

- [ ] **Step 1: 실패하는 테스트** — `complete.test.ts` 에 더한다(기존 테스트, 특히 '물건 이름은 이름 앞부분으로 맞춘다'·'채울 낱말이 비어 있으면 입력 그대로' 는 그대로 통과):
  - "여러 낱말 이름은 앞 낱말까지 친 것으로 이름 전체를 채운다": `new Completer().complete("줍기 늑대 가", ["늑대 가죽"])` → `"줍기 늑대 가죽"`.
  - "대상 뒤 여러 낱말 물건도 채운다": `complete("주기 한립 늑대 가", ["한립", "늑대 가죽"])` → `"주기 한립 늑대 가죽"`.
  - "가장 긴 꼬리가 먼저다": `complete("보기 청모랑의 시", ["시약", "청모랑의 시체"])` → `"보기 청모랑의 시체"`.
  - "공백으로 끝나도 앞 낱말과 이어 맞으면 채운다": `complete("줍기 늑대 ", ["늑대 가죽"])` → `"줍기 늑대 가죽"`, `complete("줍기 늑대 ", ["가죽"])` → `"줍기 늑대 "`.
  - "여러 낱말 후보도 거듭 부르면 다음 후보로 돈다": names `["늑대 가죽", "늑대 이빨"]`, `"줍기 늑대 "` → `"줍기 늑대 가죽"` → `"줍기 늑대 이빨"` → `"줍기 늑대 가죽"`.
- [ ] **Step 2: 실패 확인** — `npm test -- complete` → 새 테스트 FAIL.
- [ ] **Step 3: 구현** — Interfaces 대로.
- [ ] **Step 4: 검증** — play-client 검증 명령.
- [ ] **Step 5: 커밋** — `fix(play-client): 여러 낱말 이름의 Tab 은 이름 전체를 채움`(Fixes 없음).

**완료 조건:** 테스트 통과, 검증 명령 통과.
**progress.md 한 줄:** 꼬리 찾기 규칙, 더한 테스트.

---

### Task 6: 로그아웃·세션 만료 뒤 WebSocket 끊기 — 서버 + play-client (#4)

**권장 모델:** opus

**스펙:** auth.md `### WebSocket 인증` '로그아웃·세션 만료 뒤 열린 연결'·세션 유휴 줄, platform.md 닫기 코드 4401·플레이 화면 부채 정리 2차 줄. 리드 판정 후보 L-1·L-2.

**Files (world-server):**
- Create: `adapter/websocket/HttpSessionEndListener.java`(`@Adapter`, `HttpSessionListener`·`HttpSessionIdListener`)
- Modify: `adapter/websocket/GameHandshakeInterceptor.java`(속성 `HTTP_SESSION_ID`), `adapter/websocket/WebSocketGameMessageSender.java`, `adapter/websocket/GameWebSocketHandler.java`(문구 상수, javadoc 의 '1차엔 … 끊지 않는다' 줄)
- Test: `adapter/websocket/HttpSessionEndListenerTest.java`(새), `adapter/websocket/GameWebSocketIntegrationTest.java`

**Files (play-client):** Create `src/lib/sessionKeepAlive.ts`; Modify `src/components/Terminal.tsx`, `src/components/LoginScreen.tsx`, `app/page.tsx`; Test `src/lib/sessionKeepAlive.test.ts`(새), `src/components/Terminal.test.tsx`, `src/components/LoginScreen.test.tsx`(새).

**Interfaces:**
- world-server: `GameHandshakeInterceptor.HTTP_SESSION_ID = "httpSessionId"`(로그인한 세션일 때만 넣음). `GameWebSocketHandler.SESSION_ENDED_TEXT = "로그인이 끝났습니다. 다시 로그인하십시오."`. `WebSocketGameMessageSender`: `void endHttpSession(String httpSessionId)` — 그 속성이 같은 열린 연결마다 `sendError(id, "AUTH_REQUIRED", SESSION_ENDED_TEXT)` 뒤 `close(id, UNAUTHORIZED)`; `void renameHttpSession(String oldId, String newId)` — 속성을 새 id 로. `HttpSessionEndListener.sessionDestroyed` → `endHttpSession`, `sessionIdChanged` → `renameHttpSession`. 닫힘 뒤 처리는 기존 `afterConnectionClosed`(→ `GameConnector.disconnect`)가 한다.
- play-client: `TerminalProps.onUnauthorized: (notice: string | null) => void` — 닫히기 전 마지막으로 받은 `error` `AUTH_REQUIRED` 의 `text`(없으면 null). `TerminalProps.onCommandSent?: () => void` — 명령을 보낼 때마다(빈 줄 빼고). `createSessionKeepAlive(check: () => Promise<unknown>, now: () => number = Date.now, intervalMs = 5 * 60_000): { onCommand(): void }` — 만들 때 시각을 마지막 확인으로 보고, `onCommand` 때 `intervalMs` 보다 많이 지났으면 `check()` 를 부르고(그때를 마지막 확인으로) 실패는 삼킨다. `page.tsx` 가 `fetchMe` 로 만들어 넘긴다. `LoginScreen` props 에 `notice?: string | null` — 있으면 버튼 위 `<p role="status" className="muted">`.

- [ ] **Step 1: 실패하는 테스트 (world-server)**
  - `GameWebSocketIntegrationTest.logoutClosesGameSocket`: 한백·한립 접속 → 한백의 쿠키로 `POST /api/auth/logout`(헤더 `X-MUD-Client: 1`) 204 → 한백 `next()` 가 `{"type":"error","code":"AUTH_REQUIRED","text":"로그인이 끝났습니다. 다시 로그인하십시오."}`, `closed` 가 4401, 한립이 `한백이 사라진다.` disappear.
  - `GameWebSocketIntegrationTest.otherSessionLogoutKeepsSocket`: 같은 이메일로 두 번 dev 로그인(쿠키 둘) → 첫 쿠키로 접속 → 둘째 쿠키로 로그아웃 → 첫 연결에 `누구` 가 응답하고 닫히지 않음.
  - `HttpSessionEndListenerTest`(Mockito 로 `HttpSessionEvent`·`HttpSession`·sender 또는 실제 sender + 가짜 `WebSocketSession`): `sessionDestroyedEndsThatSession`(그 id 로만 `endHttpSession`), `sessionIdChangeIsFollowed`(`sessionIdChanged(event, "old")` → `renameHttpSession("old", 새 id)`). `WebSocketGameMessageSenderTest.endHttpSessionClosesOnlyMatching`: 속성이 다른 연결은 보내지도 닫지도 않음, 맞는 연결은 error JSON 뒤 4401.
  - 만료(30분)는 서블릿 컨테이너 동작이라 자동 테스트하지 않는다(destroyed 이벤트로 덮음).
- [ ] **Step 2: 실패 확인** — `./gradlew test --tests '*GameWebSocketIntegrationTest' --tests '*HttpSessionEndListenerTest' --tests '*WebSocketGameMessageSenderTest'` → FAIL.
- [ ] **Step 3: 구현 (world-server)** — Interfaces 대로. 리스너 빈은 Spring Boot 가 서블릿 리스너로 등록한다(등록이 안 되면 `ServletListenerRegistrationBean` 을 `support/config` 에 두고 메모). ArchUnit 규칙 그대로 통과.
- [ ] **Step 4: 통과 확인** — `./gradlew build`.
- [ ] **Step 5: 실패하는 테스트 (play-client)**
  - `Terminal.test.tsx` "4401 로 닫히기 전에 받은 AUTH_REQUIRED 문구를 넘긴다": error `AUTH_REQUIRED` `로그인이 끝났습니다. 다시 로그인하십시오.` 를 받고 4401 → `onUnauthorized` 가 그 문구로 불림. 기존 '4401 로 닫히면 로그인 화면으로 돌아간다' 는 `toHaveBeenCalledWith(null)` 로(바뀌는 기존 테스트).
  - `Terminal.test.tsx` "명령을 보낼 때 onCommandSent 를 부른다(빈 줄은 빼고)".
  - `sessionKeepAlive.test.ts`: "5분이 지나기 전에는 부르지 않는다", "5분이 넘게 지나 명령을 보내면 한 번 부르고 다시 5분을 센다", "check 가 실패해도 던지지 않는다".
  - `LoginScreen.test.tsx`: "notice 가 있으면 role=status 로 보인다", "없으면 보이지 않는다"(`useSearchParams` 는 `vi.mock("next/navigation")`).
- [ ] **Step 6: 구현 (play-client)** — Interfaces 대로. `page.tsx` 의 `anonymous` 상태에 `notice: string | null`(로그아웃 버튼·처음 화면은 null). **L-1 을 리드가 받지 않으면 `sessionKeepAlive`·`onCommandSent` 와 그 테스트를 빼고 auth.md 의 세션 늘리기 문장을 지운다.**
- [ ] **Step 7: 검증** — world-server `./gradlew build`, play-client 검증 명령.
- [ ] **Step 8: 커밋** — `feat(world-server): 로그아웃·세션 만료 때 열린 게임 연결을 안내 뒤 4401 로 끊음`, `feat(play-client): 세션이 끝나 끊기면 로그인 화면에 안내, 입력 중 세션 늘리기`(Fixes 없음 — #4 의 `secure`·세션 DB화 남음).

**완료 조건:** 위 테스트 통과, 두 모듈 검증 통과.
**progress.md 한 줄:** 끊는 경로(속성·리스너·sender), id 바뀜 처리, play-client 안내·세션 늘리기(L-1 결과), 테스트.

---

### Task 7: admin-console 시작 소지품의 지워진 물건 id 표시 (#10)

**권장 모델:** sonnet

**스펙:** admin-tool.md `## 3차 화면` `/start-items/` 아래 '목록에 없는(지워진) 물건 id'.

**Files (admin-console `src/` 기준):** Modify `components/StartItemsEditor.tsx`; Test `components/StartItemsEditor.test.tsx`.

**Interfaces:**
- Produces: 행의 `itemTypeId` 가 `itemTypes` 에 없으면 그 행 `select` 맨 앞에 `<option value={id}>(지워진 물건 #{id})</option>` 을 두어 골라 둔 채 보인다. 그런 행이 하나라도 있으면 표 위에 `<p className="error">목록에 없는 물건이 있습니다. 저장하기 전에 고치십시오.</p>`. 저장은 그대로 보낸다(서버 400 detail 은 기존 오류 줄).

- [ ] **Step 1: 실패하는 테스트** — `StartItemsEditor.test.tsx`:
  - "목록에 없는 물건 id 는 (지워진 물건 #9) 로 골라 두고 안내 줄을 보인다": `initial=[{itemTypeId: 9, count: 2}]`, 목록에 9 없음 → 선택 상자 값 `"9"`, 선택된 글자 `(지워진 물건 #9)`, `목록에 없는 물건이 있습니다. 저장하기 전에 고치십시오.` 가 보임.
  - "다른 물건을 고르면 그 선택지와 안내 줄이 사라진다".
  - "그대로 저장하면 그 id 를 보내고 서버 detail 을 보인다": `onSave` 가 `[{itemTypeId: 9, count: 2}]` 로 불리고 `ApiError` detail `물건 종류를 찾을 수 없습니다. id: 9` 가 `role="alert"` 로 보임.
- [ ] **Step 2: 실패 확인** — `npm test -- StartItemsEditor` → FAIL.
- [ ] **Step 3: 구현** — Interfaces 대로.
- [ ] **Step 4: 검증** — admin-console 검증 명령.
- [ ] **Step 5: 커밋** — `fix(admin-console): 시작 소지품의 지워진 물건 id 를 표시하고 고치라고 알림`(Fixes 없음 — #10 Minor-defer 남음, L-7).

**완료 조건:** 테스트 통과, 검증 명령 통과.
**progress.md 한 줄:** 표시·안내 방식, 테스트 셋.

---

### Task 8: 통합 확인 — 세 모듈, compose 한 번, 스모크 전체(기존 pgdata)

**권장 모델:** opus

**목적:** debt-2 전체가 기존 pgdata 에서 뜨고 1~6차·debt-1 시나리오가 그대로이며 새 동작이 실제 서버·브라우저 없는 클라이언트(스모크)에서 보이는지 확인한다. **이 계획에서 Docker 를 쓰는 유일한 태스크다.**

**Files:** Modify `scripts/smoke-test.mjs`, `docs/impl/debt-2/progress.md`.

**스모크 더하기 (기존 단계 기대값은 그대로):**
- #13: 단계 21 — C 가 줍기한 뒤 D 가 `quiet: true` room 을 받고 그 `items` 에 주운 물건 수가 줄어 있음. 단계 38(처치) 뒤 E 가 quiet room(`mobs` 에 ${rabbitName} 없음, `corpses` 에 시체)을, 단계 49 뒤 E 가 quiet room(`mobs` 에 ${ghostName} 없음)을 받음. quiet room 이 화면 줄 대신 오는 것이므로 `waitFor((m) => m.type === "room" && m.quiet === true …)` 로 본다.
- #19: 단계 43 의 `emberLines` 에 `{대상}이마를 스친다!`·`{대상}으로부터 불티가 튄다!` 를 더해 다섯으로(이름이 받침 없는 ${ghostName} 이므로 렌더는 `${ghostName}이마를 스친다!`(고치기 전 `…가마를`)·`${ghostName}로부터 불티가 튄다!`), `emberRenders` 에 같은 둘을 더함. 6개 거절 단언은 서로 다른 6줄(다섯 + 한 줄)로 만들어 detail 이 그대로 `이펙트 문장은 1~5개로 정하십시오: …` 이게 한다. 무작위라 이 실행에 새 줄이 안 나올 수 있다 — 나온 이펙트 줄이 모두 `emberRenders` 가운데 하나인지(기존 `isEffectLine`)로 본다.
- #18: 새 단계 52 — 관리자가 F(새 계정) 시작 소지품을 상품 영석 1·하급 저물대 1 로 바꿔 F 생성 → 연기 1층 지정 → 광장에 ${rabbitName} 배치(단계 51 이 지운 뒤라 새로) → F `사기 <단약>` 으로 확인 질문 → `공격 ${rabbitName}` → `확인` 두 번 모두 `싸우는 중에는 할 수 없습니다.`(두 번째가 `확인할 것이 없습니다.` 이면 실패) → 배치를 지우고 교전이 끝나면(≤ 10초 안에 안 되면 이 단계는 '대기 유지' 까지만 확인하고 메모) `확인` 으로 삼 → 시작 소지품·배치 되돌림. 너무 불안정하면 '확인 두 번' 까지만 하고 메모.
- #4: 새 단계 53 — G(새 계정) dev 로그인 → WebSocket 접속(생성 prompt 까지) → 같은 쿠키로 `POST /api/auth/logout` → G 가 error `AUTH_REQUIRED` `로그인이 끝났습니다. 다시 로그인하십시오.` 를 받고 4401 로 닫힘.
- #24-1·#10 Tab·admin 표시는 스모크로 보지 않는다(단위 테스트).

- [ ] **Step 1: 스크립트 수정** — `node --check scripts/smoke-test.mjs`.
- [ ] **Step 2: 세 모듈 검증** — world-server `./gradlew cleanTest build`, admin-console·play-client 검증 명령. 테스트 수를 메모에.
- [ ] **Step 3: 디스크 전** — `df -h .`·`docker system df` 를 메모에(10GB 미만이면 멈춤).
- [ ] **Step 4: compose 한 번** — `docker compose build` **한 번** → `up -d`(셸 값 덮어쓰기, **기존 pgdata**) → 기동 로그에 스키마·틱 ERROR/Exception 없음 → `source "$NVM_DIR/nvm.sh" && nvm use 26.13.1 && node scripts/smoke-test.mjs` → **단계 1~끝 `SMOKE OK`**. 실패하면 원인을 고치고(해당 모듈 테스트 통과) 바뀐 서비스만 `docker compose up -d --build <서비스>`(전체 재빌드 반복 금지). 고친 것은 따로 커밋.
- [ ] **Step 5: 되돌리기 확인** — 관리 API로 시작 방 id 1(태초 객잔)·시작 소지품 `[]`, psql 로 이 실행의 몹 배치 없음.
- [ ] **Step 6: 정리** — `docker compose down`(**볼륨 남김**) → `docker image prune -f` → `docker builder prune -f` → `world-server/build`, `play-client/{out,.next}`, `admin-console/{out,.next}` 삭제(`git check-ignore` 로 먼저 확인). `mud-mk3-*` 이미지·`pgdata`·`node_modules` 는 지우지 않는다. client 이미지를 dev 로그인 끄고 다시 빌드할지는 리드 판정(L-8) — 이 태스크에서는 하지 않고 메모만. 정리 뒤 `df -h .`·`docker system df` 를 메모에.
- [ ] **Step 7: 커밋** — `test(infra): debt-2 통합 확인과 스모크 조용한 방 상태·조사·막힌 확인·로그아웃 끊김 단계`.

**완료 조건:** 세 모듈 검증 통과, 기존 pgdata 로 스모크 1~끝 `SMOKE OK`, 시작 방 id 1·시작 소지품 `[]`·이 실행 몹 배치 0, pgdata 남음, 정리 뒤 dangling 이미지 없음.
**progress.md 한 줄:** 더한 단언·단계, 테스트 수, compose 빌드 한 번, 디스크 전후, 되돌림 확인, L-8 메모.

---

### Task 9: 최종 전체 리뷰

**권장 모델:** opus

**목적:** debt-2 전체(`b5f5cd3..HEAD`)를 이 계획·스펙·이슈와 대조한다. 코드를 고치면 따로 커밋한다.

- [ ] **Step 1: 리뷰** — `superpowers:requesting-code-review` 로 `b5f5cd3..HEAD`. 볼 것: Review Focus 다섯 줄이 테스트로 덮였는지, 결정·스펙 밖의 동작·문구가 바뀐 곳이 없는지(`git diff b5f5cd3 -- '*Test.java' '*.test.ts*'` 로 기존 기대값이 바뀐 곳이 Task 4 의 `"quiet":false`·픽스처, Task 6 의 `toHaveBeenCalledWith(null)` 뿐인지), quiet room 이 게임 락·트랜잭션 안에서 만들어지는지, 세션 끝 리스너가 게임 락을 잡지 않는지(닫힘 처리가 잡음), `Fixes #N` 이 #13·#18·#19 커밋에만 있는지, 스펙 `(제안)`·출처 표기가 구현과 맞는지.
- [ ] **Step 2: Critical·Important 고침** — 해당 모듈 검증 통과 후 커밋 `fix(<module>): …`. Docker 는 다시 빌드하지 않는다(다시 스모크가 필요하면 리드에게 묻는다).
- [ ] **Step 3: progress.md** — 리뷰 결과, 고친 것, 이슈별 결과 표(닫힘 / 열어 둠 — 남은 항목), 다음 할 일. 상태를 "부채 정리 2차 완료" 로.
- [ ] **Step 4: 커밋** — `docs(impl): 부채 정리 2차 완료 기록`.

**완료 조건:** Critical·Important 0, progress.md 완료 기록.
**progress.md 한 줄:** 최종 리뷰 결과와 고친 것.

---

## 태스크 의존 순서

```
1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9
```

- world-server 1~4·6 은 순서대로 한다(2·3·4 는 `application/game` 을, 4·6 은 `GameWebSocketIntegrationTest` 를 함께 고친다).
- 5(play-client `complete.ts`)·7(admin-console)은 다른 파일이라 아무 때나 되지만 리드가 하나씩 리뷰하므로 번호 순서로 한다. 5 는 4 의 `Terminal.test.tsx` 픽스처 변경 뒤라야 충돌이 없다.
- 8 은 모든 코드가 들어간 뒤 compose 한 번, 마지막 9.
- 각 태스크는 앞 태스크가 커밋된 상태에서 혼자 검증 명령이 통과한다.
