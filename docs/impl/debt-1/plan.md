# 기술 부채 정리 1차(debt-1) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 1~6차 동안 GitHub 이슈(#1~#24)와 6차 '이슈 없이 다음 정리 단계에서' 목록에 쌓인 기술 부채 가운데 게임 규칙·`(제안)` 결정을 바꾸지 않고 고칠 수 있는 것을 고친다. 게임 동작(플레이어·관리자가 보는 문구·규칙)은 그대로다.

**Architecture:** 새 기능 없음. 고치는 곳은 world-server 스키마 정리(`LegacySchemaCleaner`), 캐릭터 행 쓰기, 법력 회복·깨달음 도메인, 전투 컴포넌트 나누기(`application/game`), 전투 메모리 정리, 관리 조회 쿼리, 헥사고날 의존·죽은 코드·중복, 테스트, admin-console·play-client 의 오류 처리·테스트다. 태스크마다 그 태스크가 바꾼 사실만 시스템 스펙(기술 설명)에 맞춘다.

**Tech Stack:** 1~6차와 같다 — Java 25 · Spring Boot 4.1.2 · Gradle 9.9.1 Kotlin DSL · PostgreSQL 18.6 / H2(테스트) · Next.js 16.4.6 · React 19.3.1 · TypeScript 7.0.2 · Vitest 5.1.0 · Docker Compose. **새 의존성 없음**(Flyway 도 이번에 들이지 않는다 — 아래 `## #1 Flyway 판단`).

**Spec:** 이슈 본문·댓글(https://github.com/ch3224bin/mud-mk3/issues), `docs/impl/iteration-5/progress.md`·`docs/impl/iteration-6/progress.md` 의 `## 다음 할 일`, 시스템 스펙 `docs/spec/system/*.md`(기술 선택의 단일 원본 `platform.md`), `.claude/rules/*.md`.

---

## 이슈 분류

열린 이슈 24개를 2026-11-16 main(`552c132`) 코드로 확인했다. 이슈 안의 항목은 항목 단위로 나눴다. **'고칠 것' 이 이슈의 모든 항목이면 마지막으로 고치는 태스크의 커밋이 `Fixes #N` 을 단다. 일부만 고치면 `Fixes` 없이 리드가 남은 항목을 댓글로 남기고 열어 둔다.**

### 이미 해결 — 닫음 (항목 단위)

| 이슈·항목 | 근거 |
|---|---|
| #7 댓글·#15: `CombatEngine`·`ItemCommandHandler` 의 `notifyOthers` 중복 가운데 `CombatEngine` 쪽 | 6차 Task 5 `1fc75a6` — 교전 공통 경로를 `application/game/CombatActions.java` 로 떼어 냄. `CombatEngine` 안에는 `notifyOthers` 가 없고 `combatActions.notifyOthers` 를 부른다(`CombatEngine.java:362`). `ItemCommandHandler.java:473` 쪽 중복은 남음 → Task 4 |
| #15: `CombatEngine` 약 486줄 | 같은 커밋으로 430줄. 아직 크므로 나누기는 Task 4 |
| #13: 진 사람이 옛 방에서 조용히 사라짐(같은 방 알림 없음) 가운데 '쓰러짐' 알림 | 5차 `CombatEngine.onPlayerDefeated` 가 같은 방에 `한립이 쓰러진다.`(event `defeated`, command.md `### 질 때`)를 보낸다. '사라진다' 알림 여부는 남음 → 창호 결정 |

이슈 전체가 이미 해결된 것은 없다.

### 고칠 것

| 이슈 | 항목 | 태스크 | 닫기 |
|---|---|---|---|
| #1 | 단기: enum 열의 PostgreSQL CHECK 가 값 추가를 막는 문제 — 열마다 이름으로 지우는 대신 서버가 뜰 때 enum CHECK 를 한꺼번에 정리 | 1 | 열어 둠(Flyway 미룸) |
| #2 | 관리 경지 지정·게임 이동 등 캐릭터 행 전체 UPDATE 경합 → 열 하나만 쓰는 쿼리 | 2 | `Fixes #2` |
| #20 | 깎인 법력 > 최대치 보정, 회복 틱의 깎인 법력 읽기 줄이기 | 3 | `Fixes #20` |
| #21 | `Insight` null 값, `CharacterInsight.add` 음수·NaN | 3 | `Fixes #21` |
| #15 | `CombatEngine` 나누기, `notifyOthers`(`ItemCommandHandler`)·묶음 키(`GroupKey`/`MobGroup`) 중복, `GameService` 의 `AbilityTimersRepository` 의존(T7-1), 시전 지우기 세 곳(T7-2) | 4 | `Fixes #15` |
| #11 | 쓰러질 때 몹 유형이 지워져 있으면 조회 예외로 명령·틱이 롤백됨 | 5 | `Fixes #11` |
| #14 | 돌아오지 않는 캐릭터의 메모리 교전 기록, 지워진 방의 시체·몹 | 5 | `Fixes #14` |
| #8 | `보기 <옥간>` 의 무거운 조회, `viewAll` N+1, entity graph 행 곱, NPC 목록 방 조회 N+1, 쿼리 수 회귀 테스트 | 6 | `Fixes #8` |
| #17 | 쿼리 셋(빈 `IN`, 방 이름 하나에 출구까지 읽음, 배치 목록 entity graph) | 6 | — |
| #17 | `MobSummaryResponse`↔`MobDetailResponse.StatsResponse` 결합, `MobTier` 0 센티널, `MobTypeSaveInfo` 묘사 strip(판단: 그대로 — `TechniqueSaveInfo` 도 묘사를 자르는 같은 관례, 바꾸면 저장 동작이 바뀜) | 7 | `Fixes #17` |
| #5 | `ScrollItem` 을 `technique/provided` 로, ArchUnit 규칙 | 7 | `Fixes #5` |
| #6 | 쓰이지 않는 코드·검증 애노테이션 넷 | 7 | `Fixes #6` |
| #7 | `braceletOpen`·경지 읽기 중복, 자질 버림 중복 | 7 | — |
| #7 | 테스트 도우미 중복(시계 되돌림, `id()`) | 8 | — |
| #7 | `CharacterRealmForm` 이중 상태 | 10 | `Fixes #7` |
| #9 | 테스트 위생(이름·정렬 의존·위치 단언·어색한 assert·javadoc·6차 T1-1·T1-4·T6-1·T6-2·N2) | 8 | — |
| #23 | `AbilityRequest` with 도우미 | 8 | `Fixes #23` |
| #9 | 테스트 빈틈(영근 확인 방 없음, 저물대 3개, 동시 줍기, 방 삭제 roomId, 1.1배 대상 전환, 기준가 0 드롭, 빈 시체 문구) + 아이템 종류 목록 대소문자 정렬 | 9 | — |
| #9 | `LegacySchemaCleaner` 기동 순서·`tradable` 201·`dropsStaleBagGradeCheck` 막힘 확인 | 1 | — |
| #9 | 스모크 `runNo` 중복, 스모크 39단계 빈칸 단언 | 13 | 열어 둠(실제 PostgreSQL 테스트 미룸) |
| #16 | admin-console 브라우저 `min` 검사가 서버 detail 을 가림 | 10 | `Fixes #16` |
| #22 | 신통 편집기 `삭제` 접근 이름 중복 | 10 | `Fixes #22` |
| #10 | admin-console: 중간 401, 저장 뒤 응답 무시, 빈 기준가 → 0, 지역 화면 부분 실패, `RoomPicker`·페이지 연결 테스트, `min=1`(#16 과 같음) | 11 | — |
| #10 | play-client: 다시 연결하면 후보 비움 테스트, 강제 자동 스크롤, Tab 순서·`kind` 완전성 테스트, kill/respawn 때 후보 불변 테스트 | 12 | 열어 둠(창호 항목 남음) |
| (이슈 없음) | 6차 T5-1·T5-2·T5-3·T6-3·T7-3(world-server), T10-4·T10-6(문서) | 8 | — |
| (이슈 없음) | 6차 T9-1·T9-2(play-client 테스트) | 12 | — |
| (이슈 없음) | 6차 T11-2(스모크 `repeatUntil`) | 13 | — |

### 지금은 미룸 (이유 — 리드가 그대로 이슈 댓글로 쓴다)

| 이슈·항목 | 이유 |
|---|---|
| #1 Flyway 도입 | debt-1 에서는 미룬다: 지금 반복되는 문제(enum CHECK)는 Task 1 의 일반 정리로 막았고, Flyway 를 들이면 테스트(H2)가 PostgreSQL 마이그레이션 스크립트를 검증할 방법이 새 의존성(Testcontainers) 없이 없다. 첫 실제 배포 전이나 열 삭제·이름 바꾸기가 필요해질 때 아래 baseline 절차로 들인다. |
| #3 동시 편집 경합 6건(409 대신 500·NPE) | 모두 관리자 둘이 같은 순간에 편집할 때만 나는 드문 경합이고 1~3차 판정·platform.md 에서 받아들였다. 경로마다 트랜잭션 설계를 따로 바꿔야 해 관리자가 여럿이 될 때 묶어 고친다. |
| #4 세션 쿠키 `secure` | HTTPS 배포가 아직 없다. 배포 방식을 정할 때 환경 변수로 켠다. |
| #4 세션 저장소 DB화 | Spring Session JDBC 는 새 의존성이고 platform.md 기술 선택을 바꾼다. 서버 한 대·재시작 때 재로그인을 받아들이는 지금 규모에선 필요 없다. |
| #9 `LegacySchemaCleaner` 의 실제 PostgreSQL 테스트(중복 묶음 합치기) | 자동 테스트에 PostgreSQL 이 없고(Testcontainers 는 새 의존성), debt-1 Task 13 스모크가 기존 pgdata 로 정리 결과를 확인한다. |
| #12 틱·명령 실패 때 메모리 불일치와 틱 전체 롤백(6차 `AbilityTimers`·깨달음 상한, N6 포함) | platform.md 의 '명령·틱마다 트랜잭션 하나, 커밋 실패 때 메모리는 되돌리지 않음' 설계를 바꾸는 큰 일(행위자별 트랜잭션 또는 메모리 스냅샷)이고, 1~6차 스모크·테스트에서 실제 실패가 없었다. 전투가 넓어질 때(상성·법기) 설계부터 다시 본다. N6(관리 경지 지정의 `mana_lost` 쓰기가 게임 락 밖)은 그 열 하나의 드문 경합이라 받아들인 한계 그대로다. |
| #24-2 재사용 대기 시작 시각이 틱 시각 | 스펙이 '1초 안쪽으로 늦을 수 있다' 고 허용하므로 고칠 것이 아니다(기록만). 필요해지면 `fire` 에 `casting.firesAt()` 을 넘긴다. |

### 창호 결정 필요

| 이슈·항목 | 무엇을 정해야 하나 |
|---|---|
| #4 로그아웃·세션 만료 뒤에도 열린 WebSocket 이 남음 | auth.md 는 '열린 WebSocket 은 HTTP 세션을 늘리지 않는다' 만 정했다. 로그아웃·만료 때 연결을 끊을지, 끊는다면 플레이어에게 보일 문구·닫기 코드(4401 + `AUTH_REQUIRED`?)를 정해야 한다. |
| #13 처치·재등장·시체 사라짐·짐 때 `room` 재전송 | world-structure.md·command.md `(제안)` 이 '바닥이 바뀌어도 방 보기를 다시 보내지 않고 알림만' 이라고 정했다. 다시 보내려면 이 결정을 바꿔야 하고, 진 사람이 옛 방에서 '사라진다' 알림을 더하면 새 문구다. |
| #10 play-client 여러 낱말 물건 Tab, 주운 뒤 후보 남음 | 주운 뒤 후보는 #13 과 같은 결정(방 보기 재전송)에 걸린다. 여러 낱말 이름의 Tab 동작(낱말 단위로 끊을지 이름 전체를 채울지)은 화면 동작 결정이다. |
| #10 admin-console 목록에 없는(지워진) 시작 소지품 id 표시 | 선택 상자에 없는 id 를 어떻게 보일지(문구)를 정해야 한다. |
| #18 교전 중 막힌 `확인` 이 상점 결제 대기를 지움 | 5차 D86 `(제안)` '막힌 확인은 대기도 사라진다' 를 그대로 둘지 바꿀지. |
| #19 이펙트 문장 조사 인식이 낱말 첫 음절과 겹침 | admin-tool.md `### 이펙트 문장` 의 조사 규칙(뒤 글자가 공백·문장 부호일 때만 조사로 볼지, 안내 문구를 더할지)을 정해야 한다. |
| #24-1 시전 발동 때 영력 0(범인) 재확인 | command.md `### 시전과 중지` 의 발동 재확인 목록에 없다. 넣을지와 그때 문구(흩어짐 또는 영력 거절)를 정해야 한다. |

## #1 Flyway 판단

- **이번에는 들이지 않는다.** 근거:
  1. 실제로 반복된 문제는 '이미 있는 enum 열에 값을 더하면 PostgreSQL CHECK 가 저장을 막음' 하나다. Task 1 이 서버가 뜰 때 Hibernate 가 만든 enum CHECK 를 한꺼번에 정리해 열·값마다 손으로 정리를 더하던 일을 없앤다. 열 삭제는 `LegacySchemaCleaner` 가 이미 하고, 지금까지 열 이름 바꾸기·형 바꾸기는 없었다.
  2. Flyway 를 들이면 스키마의 원본이 SQL 스크립트가 되는데, 테스트는 H2 `create-drop` 이다. PostgreSQL 용 `pg_dump` 스크립트는 H2 에서 그대로 돌지 않는다. 테스트에서 Flyway 를 끄면 스크립트는 compose 스모크에서만 검증되고(지금보다 약함), 테스트에서 켜려면 H2 와 PostgreSQL 둘 다에 맞는 SQL 을 손으로 써야 하거나 Testcontainers(새 의존성)가 필요하다.
  3. 데이터는 개발용 pgdata 하나뿐이고 운영 배포가 없다. 들이는 시점은 첫 실제 배포 전이 가장 싸다(그 뒤엔 운영 DB 마다 baseline 을 맞춰야 함).
- **들일 때의 절차(기록, 이번엔 하지 않음):** (1) debt-1 이 끝난 pgdata 에서 `pg_dump --schema-only --no-owner` 로 지금 스키마를 떠 `V1__baseline.sql` 로 둔다(정리된 CHECK·`LegacySchemaCleaner` 결과가 반영된 상태). (2) `spring-boot-starter-flyway`(Spring Boot 4.1.2 BOM 관리 버전)와 `flyway-database-postgresql` 을 넣고 `spring.flyway.baseline-on-migrate=true`·`baseline-version=1` 을 준다 — 기존 pgdata 는 `flyway_schema_history` 에 baseline 1 행만 생기고 V1 을 건너뛰며, 새 DB 는 V1 을 돈다. (3) `ddl-auto` 를 `validate` 로 바꾸고, `LegacySchemaCleaner` 와 기존 데이터 채우기(경지·신통)의 스키마 부분은 V2 이후 스크립트로 옮긴다. (4) 테스트 전략을 먼저 정한다(H2 용 스크립트를 따로 둘지, Testcontainers 를 들일지). (5) 옮기기 전 `pg_dump` 전체 백업을 남기고 compose 로 기존 pgdata 에서 기동·스모크 전체를 확인한다.

## 리드 판정 후보

구현을 막지 않고 아래대로 진행한다. 틀리면 리드가 바꾼다.

- L-D1 (#1, Task 1) enum CHECK 는 다시 만들지 않고 지운다. DB 단의 enum 값 검사는 없어지지만 값은 Java enum 매핑(`STRING`)만 쓴다. platform.md·admin-tool.md 의 '값을 더할 때 그 열의 CHECK 를 이름으로 지우는 정리를 더한다' 기술 설명을 '서버가 뜰 때 enum CHECK 를 지운다' 로 고친다(게임 동작 아님).
- L-D2 (#2, Task 2) 캐릭터의 방·경지 열은 엔티티 매핑에서 `updatable="false"` 로 두고 열마다 `@Modifying` JPQL 로 쓴다(5차 깎인 기혈과 같은 꼴, `@Version` 낙관적 락은 게임 이동을 실패시키므로 쓰지 않음). platform.md `관리 툴 경지 지정의 반영` 의 '캐릭터 행 전체를 쓰므로 … 옛 값으로 되돌릴 수 있다(드묾, 받아들임)' 문장을 '열 하나만 쓰므로 다른 쪽 열을 되돌리지 않는다' 로 고친다(받아들였던 한계가 없어지는 기술 설명).
- L-D3 (#11, Task 5) 스냅샷(이슈의 고치는 방향) 대신 '쓰러지는 순간 몹 유형이 없으면 드롭 없는 시체' 로 고친다. admin-tool.md `(제안)` '드롭 표는 몹이 쓰러지는 순간의 표를 쓴다' 를 지키기 위해서다(스냅샷은 '나타날 때의 표' 로 바꾸는 규칙 변경). 지워진 유형의 표는 지금 없으므로 '쓰러지는 순간의 표 = 없음' 이다.
- L-D4 (#14, Task 5) 접속하지 않은 캐릭터의 메모리 교전 기록은 '교전 상대 없음 + 다음 공격 시각이 지남' 일 때 틱이 지운다(5차 판정 '끊김 뒤에도 다음 공격 시각은 남김' 을 그 시각까지 지킴). 대기시간 기록은 '시전 없음 + 공통·재사용 대기가 모두 끝남' 일 때 지운다.

**리드 판정 (확정, 2026-11-16)** — L-D1~L-D4 모두 위 후보대로 한다. 근거: 모두 기술 처리이고 게임 규칙·창호 결정을 바꾸지 않는다. L-D3 은 기존 (제안) '드롭 표는 쓰러지는 순간의 표' 와 맞는 쪽이다(유형이 지워졌으면 표가 없음). Flyway 도입 시점은 창호 확인 항목으로 넘긴다.

## Global Constraints

모든 태스크의 요구 사항에 이 절이 포함된다. 구현자는 이 절과 자기 태스크, `## 이슈 분류` 를 먼저 읽는다.

**범위 — 기술 부채만**
- **게임 규칙·화면 문구·서버 메시지·`(제안)` 결정을 바꾸지 않는다.** 룰북(`docs/spec/game-rule/`)은 고치지 않는다. 시스템 스펙은 태스크가 바꾼 기술 사실(쿼리·정리 방식·한계 설명)만 고친다. 고치다가 사람이 보는 동작·문구가 바뀌어야 하면 멈추고 리드에게 보고한다(그 항목은 '창호 결정 필요' 로 옮긴다).
- 이 계획의 '창호 결정 필요'·'미룸' 항목은 손대지 않는다.
- 기존 테스트의 기대값(문구·수치)을 바꾸지 않는다. 바꿔야 통과한다면 동작이 바뀐 것이므로 멈추고 보고한다(테스트 위생 태스크 8 의 이름·단언 방식 바꾸기는 예외 — 기대하는 값은 같게).

**버전·의존성 (추측 금지)** — 1차 계획 버전 표를 그대로 쓴다. **새 라이브러리·플러그인·이미지를 넣지 않는다.** `build.gradle.kts`, `package.json`, `package-lock.json`, Dockerfile, `compose.yaml` 의 버전 줄을 바꾸지 않는다. 쿼리 수 테스트는 Hibernate 내장 `Statistics`(`hibernate.generate_statistics`) 로 한다. 꼭 필요해 보이면 멈추고 리드에게 보고한다. (Flyway 는 이번에 들이지 않으므로 예외 없음. 나중에 들일 때는 Spring Boot 4.1.2 BOM 관리 버전만 쓴다 — `## #1 Flyway 판단`.)

**로컬 환경**
- macOS arm64, 기본 JDK Temurin 25. world-server는 항상 `./gradlew`.
- 프론트 태스크는 Node 26.13.1: 명령 앞에 `source "$NVM_DIR/nvm.sh" && nvm use`(모듈의 `.nvmrc`).
- **디스크:** 태스크 시작 전 `df -h .` 를 본다. 남은 공간이 **10GB 미만이면 아무것도 지우지 말고 멈춰** 리드에게 보고한다. `ENOSPC` 도 같다. (계획 작성 때 54Gi.)
- **Docker 빌드·실행은 Task 13 에서 한 번만** 한다. Task 1~12·14 는 `docker build`, `docker compose build/up` 을 쓰지 않는다. **`pgdata` 볼륨은 어느 태스크에서도 지우지 않는다**(1~6차 `ddl-auto=update` 로 만들어진 스키마와 데이터가 그대로 있어야 Task 13 이 이전 안전성을 확인한다).

**코드 규칙**
- world-server 코딩 규칙(`.claude/rules/*.md`), 루트 패키지 `mudmk3`. 아래 world-server 경로는 `world-server/src/{main,test}/java/mudmk3/` 기준이다.
- 문구는 한국어, 기존 상수·`Josa`·`Numbers` 를 그대로 쓴다. 예외 메시지(내부 `IllegalArgumentException`·`IllegalStateException`)는 한국어로, 기존 꼴(`캐릭터를 찾을 수 없습니다. id: …`)을 따른다.
- **게임 락:** 명령·틱·접속·끊김은 모두 `GameService.serialized` 하나를 지난다. 메모리 상태는 그 안에서만 건드린다. 메모리 상태 객체는 엔티티를 들고 있지 않는다.
- **깎인 기혈·법력 열은 전용 네이티브 쿼리로만 쓴다**(5·6차). 엔티티 저장이 그 열을 쓰지 않는다.
- **새 enum 열·값을 더하지 않는다.** 기존 enum 에 값을 더하게 되면 멈추고 보고한다.
- **AFTER_COMMIT 과 테스트 트랜잭션:** `@ApplicationServiceTest` 는 롤백되어 AFTER_COMMIT 리스너가 오지 않는다. 커밋이 필요한 테스트는 `TransactionTemplate`(+`REQUIRES_NEW`)으로 쓰고 끝에 지운다(5차 `designateRealm` 도우미 관례).
- **메모리 상태와 테스트:** 메모리 저장소를 더하거나 지우면 `CombatMemoryReset` 을 맞춘다.
- 자동 테스트는 Google 자격 증명·네트워크 없이 돈다. 시간은 `MutableClock` + 직접 `tick()`(`advance`·`advanceAndTick`), 난수는 `ScriptedRandom`. **실제로 기다리는 테스트(`Thread.sleep`, Awaitility)를 쓰지 않는다.**
- Next.js 앱 규칙은 1~6차와 같다(정적 export, `"use client"`, 상대 경로 import, 쿼리스트링 id). 저장 전 클라이언트 검사는 하지 않고 서버 `detail` 을 보인다. 숫자 칸은 5차 `MobForm` 처럼 `step="any"` 이고 `min`/`max` 를 두지 않는다.
- 툴 호출 파라미터(JSON)의 한글은 리터럴 UTF-8로 쓰고 `\uXXXX` 이스케이프를 쓰지 않는다.

**저장소·커밋**
- **main 에서 일한다.** 태스크 하나 = 커밋 하나 이상. 구현자는 커밋까지 하고, 리뷰를 통과하면 리드가 태스크마다 `git push` 하고 이슈 댓글·닫기를 확인한다. 구현자는 서브에이전트를 띄우지 않고 GitHub 이슈에 쓰지 않는다.
- 커밋 메시지: `<type>(<module>): <한국어 요약>`(type: feat, test, build, docs, chore, fix, refactor / module: world-server, play-client, admin-console, infra, spec, impl). 태스크가 이슈의 모든 항목을 끝내는 커밋에만 본문에 `Fixes #N` 줄을 넣는다(태스크마다 적힘). 끝에 빈 줄 다음 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- 태스크를 끝내면 `docs/impl/debt-1/progress.md` 의 표에 한 줄(태스크 번호·제목, 커밋 해시는 리드가 채움, 메모)을 더하고 같은 커밋에 넣는다. 각 태스크 끝의 **progress.md 한 줄** 이 그 메모의 틀이다(실제로 바꾼 것·계획과 다른 점을 덧붙인다).

**검증 명령 (각 태스크 완료 조건에 포함, 고친 모듈만)**
- world-server: `cd world-server && ./gradlew build`(테스트 + SpotBugs + ArchUnit)
- admin-console / play-client: `cd <module> && source "$NVM_DIR/nvm.sh" && nvm use && npm ci && npm test && npx tsc --noEmit && npm run build`
- 스모크 스크립트만 고친 태스크: `node --check scripts/smoke-test.mjs`

**스모크 (Task 13만)** — README `### 스모크 테스트` 방법 그대로: `.env` 는 고치지 않고 `docker compose build` 한 번 → `up` 때만 셸 값으로 `MUD_DEV_LOGIN_ENABLED=true`, `MUD_ADMIN_EMAILS=<.env 의 기존 값>,smoke-admin@example.com` 을 덮어써 `docker compose up -d` → `node scripts/smoke-test.mjs`. **기존 pgdata 로 올린다.** 실제 서버 틱(1초)이 돌므로 스모크는 실제 시간으로 기다린다(`waitFor`·되풀이, 고정 대기 없음).

## Review Focus

테스트가 직접 겨누지 않지만 부채 정리에서 가장 깨지기 쉬운 것. 각 줄의 테스트는 담당 태스크에 들어 있다.

1. **기존 pgdata 이전** — 서버가 뜰 때 enum CHECK 정리가 기존 스키마(1~6차 `ddl-auto=update`)에서 오류 없이 돌고, 몇 번 다시 떠도 같으며, 숫자 CHECK·유니크·FK 는 남는다. (Task 1 `dropsEnumChecksOnly`·`cleanIsIdempotent`, Task 13 psql 확인)
2. **행 쓰기 경합** — 관리 경지 지정과 게임 이동이 같은 행을 거의 동시에 써도 둘 다 남고, 깎인 기혈·법력 열도 되돌려지지 않는다. (Task 2 `designateAndMoveBothSurvive`·`moveDoesNotTouchVitals`)
3. **나누기 뒤 동작 그대로** — `CombatEngine` 나누기·`RoomNotifier`·묶음 키를 옮긴 뒤 1~6차 게임 테스트(`Combat*Test`·`Ability*Test`·`GameConnectorTest`)가 기대값을 하나도 바꾸지 않고 통과한다. (Task 4 완료 조건)
4. **메모리 정리 경계** — 끊긴 캐릭터의 교전 기록을 너무 일찍 지워 재접속으로 공격 대기를 초기화하는 길이 생기지 않고, 재접속하면 기혈 다시 참·법력 회복 기준 시각이 1~6차 그대로다. (Task 5 `offlineRecordKeptUntilAttackReady`·`reconnectAfterSweepRefills`·기존 `noRegenWhileOffline`)
5. **admin 오류 처리** — 저장 중 세션이 만료되면(401) 로그인 화면으로 가고, 숫자 칸에 0·빈칸을 넣고 저장하면 서버 detail 이 보인다. (Task 10 `zeroCountShowsServerDetail`, Task 11 `midSession401ShowsLogin`)

---

## 파일 구조 (요약, 새로 생기거나 바뀌는 것)

```
world-server/src/main/java/mudmk3/
  adapter/persistence/LegacySchemaCleaner.java                                     (T1)
  application/character/{CharacterModifyService}.java, required/CharacterRepository.java, provided/CharacterVitalsEditor.java  (T2·T3)
  domain/character/Mana.java, domain/cultivation/{Insight,CharacterInsight}.java   (T3)
  application/game/{ManaRecovery}.java                                             (T3)
  application/game/{CombatEngine,CombatActions,ItemCommandHandler,RoomViewAssembler,GameService,AbilityCommandHandler,AbilityCaster}.java,
                   {MobSpawner,RoomNotifier,MobGroupKey}.java (새)                  (T4)
  application/game/{CorpseHandler,CombatEngine,GameService}.java, required/{PlayerCombatRepository,AbilityTimersRepository,CorpseRepository,MobInstanceRepository}.java, adapter/persistence/memory/*  (T5)
  application/technique/{TechniqueQueryService}.java, required/TechniqueRepository.java, application/mob/{MobTypeQueryService}.java, required/MobPlacementRepository.java, adapter/webapi/NpcApi.java, application/game/ItemCommandHandler.java  (T6)
  application/technique/provided/ScrollItem.java (옮김), application/game/CharacterRealms.java (새), domain/mob/MobTier.java, adapter/webapi/dto/Mob*Response.java, ...  (T7)
world-server/src/test/java/mudmk3/ ...                                             (T8·T9, 그리고 태스크마다)
admin-console/src/components/*.tsx, src/lib/api.ts                                 (T10·T11)
play-client/src/components/Terminal.tsx, src/lib/*.test.ts                         (T13)
scripts/smoke-test.mjs, README.md                                                  (T12)
docs/spec/system/{platform,admin-tool,command,index}.md                            (해당 태스크가 바꾼 기술 사실만)
```

---

### Task 1: enum CHECK 일반 정리와 `LegacySchemaCleaner` 테스트 보강

**권장 모델:** opus

**목적:** #1 단기 항목. PostgreSQL 에서 Hibernate 가 enum 열마다 만드는 `CHECK (col = ANY (ARRAY[…]))`(H2 는 `col IN (…)`)가 나중에 enum 값을 더하면 저장을 막는 문제를, 서버가 뜰 때 enum CHECK 를 모두 지워 없앤다(L-D1). #9 의 `LegacySchemaCleaner` 항목 셋을 함께 한다.

**Files:**
- Modify: `adapter/persistence/LegacySchemaCleaner.java`(이름 하나로 지우던 `item_type_bag_grade_check` 정리를 일반 정리로 갈음)
- Modify: `docs/spec/system/platform.md`(마이그레이션·CHECK 설명 줄들 — 3차~6차 테이블 줄의 'CHECK 정리' 표현, 377행 'Flyway 는 다음 이터레이션' → `## #1 Flyway 판단` 요약), `docs/spec/system/admin-tool.md` `### 관리 툴 변경의 반영`(84행 '나중에 값을 더할 때 위 규칙대로 정리한다')
- Test: `adapter/persistence/LegacySchemaCleanerTest.java`, `adapter/webapi/ItemTypeApiWebMvcTest.java`(또는 `tradable` 을 다루는 기존 관리 API 테스트)

**Interfaces:**
- Produces: `LegacySchemaCleaner.clean()` 이 (기존 셋에 더해) 현재 스키마의 enum CHECK 를 모두 지운다. enum CHECK 판정: 정의(`information_schema.check_constraints.check_clause`)가 한 열을 문자열 상수 목록과 비교하는 꼴 — PostgreSQL `= ANY (ARRAY[` 를 포함, H2 ` IN(` / ` IN (` 를 포함. 숫자 비교(`>=`, `<=`, `BETWEEN`)만 있는 CHECK 는 남긴다. 지운 개수를 `log.info("enum CHECK 제약 {}개를 지웠습니다.", n)` 로 남기고, 0 이면 로그 없음.

- [ ] **Step 1: 실패하는 테스트** — `LegacySchemaCleanerTest`(H2):
  - `dropsEnumChecksOnly`: 테스트에서 `CREATE TABLE legacy_check_probe (id bigint primary key, grade varchar(16) CHECK (grade IN ('A','B')), count int CHECK (count >= 1))` → `clean()` → `grade='C'` 넣기 성공, `count=0` 넣기는 여전히 `DataIntegrityViolationException`. 끝에 표를 지운다.
  - `dropsHibernateEnumCheck`: H2 에서 Hibernate 가 enum 열에 만든 CHECK(예: `item_type.category`)가 `clean()` 뒤 `information_schema.check_constraints` 에서 사라짐. H2 가 enum 을 CHECK 가 아닌 네이티브 `ENUM` 형으로 만들어 CHECK 가 없으면 이 테스트는 빼고 메모한다(PostgreSQL 은 Task 13 psql 로 확인).
  - `cleanIsIdempotent`: `clean()` 두 번 → 예외 없음.
  - `dropsStaleBagGradeCheck`(#9 보강): 옛 CHECK(`LOW·MIDDLE·HIGH`)를 만든 뒤 **clean 전에는 `BRACELET_LOW` 넣기가 막히고**, clean 뒤엔 된다.
  - `runsBeforeApplicationReady`(#9): `LegacySchemaCleaner` 는 `SmartInitializingSingleton` 이라 `ApplicationReadyEvent` 리스너(경지 채우기 `@Order(0)`·영석 심기 `@Order(10)`·신통 채우기 `@Order(20)`)보다 먼저 돈다 — 컨텍스트가 뜬 뒤 옛 CHECK 가 없고 영석 행이 있음을 본다(또는 리스너 순서를 기록하는 테스트 빈). 방식은 구현자가 고르되 '정리가 영석 심기 뒤에 돌면 실패' 해야 한다.
  - 관리 API(#9): 옛 클라이언트가 아이템 종류 만들기 본문에 `"tradable": true` 를 더해 보내도 201(모르는 칸 무시).
- [ ] **Step 2: 실패 확인** — `./gradlew test --tests '*LegacySchemaCleanerTest'` → `dropsEnumChecksOnly`·`dropsHibernateEnumCheck` FAIL.
- [ ] **Step 3: 구현** — `CHECK_CONSTRAINTS_NAMED` 를 스키마 안 모든 CHECK 를 `(table_name, constraint_name, check_clause)` 로 읽는 쿼리(`information_schema.table_constraints` ⋈ `check_constraints`, `constraint_schema` 기준)로 바꾸고, 위 판정에 맞는 것만 `ALTER TABLE <table> DROP CONSTRAINT "<name>"`. H2 의 NOT NULL 로 생긴 시스템 CHECK 가 있으면 건드리지 않는다(판정에 안 걸림을 테스트로 확인). javadoc 의 목록을 고친다.
- [ ] **Step 4: 통과 확인** — 위 테스트와 `./gradlew build`.
- [ ] **Step 5: 스펙** — platform.md·admin-tool.md 의 'enum 열에 값을 더하면 그 열의 CHECK 를 이름으로 지우는 정리를 더한다' 류 문장을 '서버가 뜰 때 `LegacySchemaCleaner` 가 Hibernate 가 만든 enum CHECK 를 모두 지운다(값 검사는 Java enum 매핑이 한다)' 로, 377행 Flyway 줄을 '마이그레이션 도구는 첫 실제 배포 전에 들인다(debt-1 판단)' 로 고친다. 각 줄 `(제안)` 표시는 그대로.
- [ ] **Step 6: 커밋** — `fix(world-server): 서버가 뜰 때 enum CHECK 제약을 한꺼번에 정리` (Fixes 없음).

**완료 조건:** 위 테스트 통과, `./gradlew build` 통과, 숫자 CHECK·유니크·FK 는 남음.
**progress.md 한 줄:** enum CHECK 일반 정리(판정 규칙·로그), `LegacySchemaCleaner` 테스트 보강(#9 셋), 스펙 기술 설명 고침. #1 은 Flyway 미룸으로 열어 둠.

---

### Task 2: 캐릭터 행을 열 하나씩 쓰기

**권장 모델:** opus

**목적:** #2. `CharacterModifyService` 의 `designate`·`initiate`·`moveTo`·`relocateAll`·`chooseSpiritRoot` 가 엔티티 행 전체를 `save` 해 다른 쪽(관리 지정 ↔ 게임 이동)의 열을 옛 값으로 되돌릴 수 있다. 만들기(`create`) 뒤의 쓰기를 열마다 쿼리로 바꾼다(L-D2).

**Files:**
- Modify: `resources/META-INF/orm.xml`(`PlayerCharacter` 의 바뀌는 열 `room_id`·`realm` 에 `updatable="false"`, `insertable` 은 그대로. 영근은 값 컬렉션 표 `character_spirit_root` 라 해당 없음), `application/character/CharacterModifyService.java`, `application/character/required/CharacterRepository.java`
- Modify: `docs/spec/system/platform.md` `관리 툴 경지 지정의 반영`(183행), 5차 깎인 기혈 줄(236행)의 '4차 행 전체 경합' 언급
- Test: `application/character/provided/CharacterRowWriteTest.java`(새, 커밋하는 테스트), 기존 `CharacterModifyService`·`CharacterRepository` 테스트

**Interfaces:**
- 방식(5차 깎인 기혈과 같은 꼴): 엔티티 매핑에서 바뀌는 열을 `updatable="false"` 로 두어 엔티티 flush 가 그 열을 쓰지 않게 하고, 쓰기는 열마다 전용 쿼리로 한다. 도메인 메서드(`moveTo`·`changeRealm`·`chooseSpiritRoot`)는 그대로 불러 검사하고 메모리의 엔티티 값도 맞춘다(같은 트랜잭션 안에서 뒤에 읽는 코드가 새 값을 봄). `clearAutomatically` 는 쓰지 않는다(게임 명령 트랜잭션의 다른 엔티티를 떼어 내면 지연 로딩이 깨짐).
- Produces: `CharacterRepository` 의 `@Modifying @Query` — `int writeRoomId(Long characterId, Long roomId)`, `int writeRealm(Long characterId, Realm realm)`, `int writeRoomIdForAll(Long fromRoomId, Long toRoomId)`(`relocateAll` 용 — 옮길 캐릭터 id 는 먼저 `findAllByRoomId` 로 읽어 돌려줌). 0 행이면 `IllegalArgumentException("캐릭터를 찾을 수 없습니다. id: " + id)`(기존 `writeQiBloodLost` 와 같음). `CharacterMover`·`CharacterRealmEditor` 포트 시그니처는 그대로. `fillMissingRealms` 는 이미 쿼리라 그대로.
- `characterRepository.save` 는 `create` 와 `chooseSpiritRoot`(영근 값 컬렉션 표만 씀 — `player_character` 의 바뀌는 열은 이제 쓰지 않음)에만 남기고, 이동·경지 쓰기 뒤의 `save` 는 지운다.

- [ ] **Step 1: 실패하는 테스트** — `CharacterRowWriteTest`(`@SpringBootTest`, 커밋함, 끝에 만든 캐릭터·방 지움):
  - `designateAndMoveBothSurvive`: 바깥 `TransactionTemplate` 에서 `characterFinder.find(id)` 로 엔티티를 먼저 읽어 영속성 컨텍스트에 둔 뒤, 안쪽 `REQUIRES_NEW` 로 `moveTo(id, 북쪽 길)` 커밋 → 바깥에서 `designate(id, QI_3)`(컨텍스트의 옛 방을 든 엔티티를 씀) 커밋 → 새 트랜잭션에서 방 = 북쪽 길, 경지 = 연기 3층. 반대(`moveAndDesignateBothSurvive` — 바깥이 읽고 안쪽이 `designate`, 바깥이 `moveTo`)도.
  - `sameTransactionSeesNewRoom`: 한 트랜잭션에서 `moveTo` 뒤 `characterFinder.find(id).getRoomId()` 가 새 방.
  - `relocateAllKeepsRealm`: 방 삭제 옮기기와 경지 지정이 겹쳐도 둘 다 남음.
  - `moveDoesNotTouchVitals`: `qi_blood_lost`·`mana_lost` 를 쓴 뒤 `moveTo`·`designate`(같은 경지 아님) → 깎인 값은 `lostAfterDemotion` 대로만 바뀌고 이동은 그대로.
  - `writeToMissingCharacterFails`: 없는 id 로 `moveTo` → `IllegalArgumentException`(`캐릭터를 찾을 수 없습니다. id: 999999`).
- [ ] **Step 2: 실패 확인** — `designateAndMoveBothSurvive` FAIL(한쪽 값이 되돌려짐).
- [ ] **Step 3: 구현** — 위 Interfaces 대로. `grep -n 'characterRepository.save' world-server/src/main` 결과가 `create`·`chooseSpiritRoot` 두 곳만 남게 한다. `RealmChangedEvent` 발행·깎인 기혈·법력 맞추기 순서는 그대로.
- [ ] **Step 4: 통과 확인** — 새 테스트, 1~6차 테스트(`designateDoesNotOverwriteLost`·`vitalsWriteDoesNotTouchRealm` 포함), `./gradlew build`.
- [ ] **Step 5: 스펙** — platform.md 183·236행의 행 전체 경합 설명을 '이동·경지는 열 하나만 쓰는 쿼리라(엔티티 저장은 그 열을 쓰지 않음) 서로 되돌리지 않는다. 같은 열(경지)을 관리 지정과 게임 `읽기` 가 함께 쓰면 나중 커밋이 이긴다(드묾, 받아들임)' 로 고친다(`(제안)` 그대로).
- [ ] **Step 6: 커밋** — `fix(world-server): 캐릭터 이동·경지 지정을 열 하나만 쓰는 쿼리로` + 본문 `Fixes #2`.

**완료 조건:** 위 테스트 통과, `characterRepository.save` 가 `create`·`chooseSpiritRoot` 에만 남음, `./gradlew build` 통과.
**progress.md 한 줄:** `updatable="false"` 열 둘, 열 단위 쓰기 메서드, 바뀐 서비스 메서드, 스펙 문장.

---

### Task 3: 법력 회복 보정과 깨달음 불변식

**권장 모델:** opus

**목적:** #20(깎인 법력 > 최대치일 때 회복이 멈춘 듯 보임, 회복 틱이 깎인 법력을 세 번 읽음), #21(`Insight` null 값, `CharacterInsight.add` 음수·NaN).

**Files:**
- Modify: `domain/character/Mana.java`, `application/character/CharacterModifyService.java`, `application/character/provided/CharacterVitalsEditor.java`, `application/game/ManaRecovery.java`, `domain/cultivation/{Insight,CharacterInsight}.java`
- Test: `domain/character/ManaTest.java`, `application/game/provided/ManaRecoveryTest.java`, `domain/cultivation/{InsightTest,CharacterInsightTest}.java`

**Interfaces:**
- Produces: `Mana.lostFor(double current, int max)`(= `max - clamp(current, 0, max)`, `QiBlood.lostFor` 와 같은 꼴), `CharacterVitalsEditor.setMana(Long characterId, double current, int maxMana)`(읽지 않고 그 열만 씀, `setQiBlood` 와 같은 꼴). `recoverMana(Long, double)` 는 다른 곳에서 쓰지 않으면 지운다(`grep -rn recoverMana`).
- `ManaRecovery.recover(now)` 순서: `manaRegenSeconds(now)` 를 먼저(F16 — 가득해도 기준 시각이 감) → `manaLost(id) <= 0` 이면 건너뜀(가득한 캐릭터는 읽기 1번) → `stats = cultivationFinder.stats(id)`, `max == 0` 이면 건너뜀 → `setMana(id, min(max, stats.manaCurrent() + max × REGEN_RATE × seconds), max)`. `stats.manaCurrent()` 는 `max(0, max − lost)` 라 lost > max 이면 0 에서 시작해 바로 찬다(보이지 않는 몫을 먹지 않음).

- [ ] **Step 1: 실패하는 테스트**
  - `ManaRecoveryTest.recoversFromZeroWhenLostAboveMax`: 깎인 법력 30, 최대 25(공법 단계 법력 증가를 낮춰 최대가 준 상황 — 테스트는 `mana_lost` 를 30 으로 직접 씀) → 1초 틱 뒤 `status` 법력 현재 0.125(보이는 값 `0`→ 틱 8번 뒤 `1`), 깎인 양 24.875.
  - `ManaRecoveryTest.recoveryCapsAtMax`(기존 `manaRegenCapsAtMax` 가 있으면 그대로 통과).
  - `ManaTest.lostForClamps`: `lostFor(30, 25) == 0`, `lostFor(-1, 25) == 25`, `lostFor(10, 25) == 15`.
  - `InsightTest.nullAmountFails`: `new Insight(map with FIRE → null)` → `NullPointerException`.
  - `CharacterInsightTest.addRejectsNegative`·`addRejectsNaN`·`addRejectsInfinity` → `IllegalArgumentException`, `addZeroOk`.
- [ ] **Step 2: 실패 확인.**
- [ ] **Step 3: 구현** — 위 Interfaces 대로. `Insight` 생성자: 값마다 `requireNonNull(value, "깨달음 값이 비어 있습니다: " + kind)`. `CharacterInsight.add`: `if (!Double.isFinite(gain) || gain < 0) throw new IllegalArgumentException("깨달음은 0 이상 유한한 값만 더합니다: " + gain)`.
- [ ] **Step 4: 통과 확인** — 위 테스트, 6차 `ManaRecoveryTest` 전부(`regenClockAdvancesWhileFull`·`noRegenWhileOffline`), `./gradlew build`.
- [ ] **Step 5: 커밋** — `fix(world-server): 깎인 법력이 최대치를 넘을 때 회복 보정과 깨달음 불변식` + 본문 `Fixes #20`·`Fixes #21`(두 줄).

**완료 조건:** 위 테스트 통과, 회복 틱이 깎인 법력이 있는 캐릭터마다 `mana_lost` 를 두 번(전엔 세 번) 읽음 — 메모에 적는다.
**progress.md 한 줄:** `Mana.lostFor`·`setMana`, 회복 틱 순서, `recoverMana` 처리, 깨달음 검사 두 곳.

---

### Task 4: 전투 컴포넌트 나누기와 중복 정리

**권장 모델:** opus

**목적:** #15 남은 것 전부. 동작은 바뀌지 않는다(Review Focus 3).

**Files:**
- Create: `application/game/MobSpawner.java`(package-private `@Component` — `CombatEngine` 의 `reconcile`·`matches`·`remove`·`refillMobs`·`respawnMobs`·`notifyAppeared`·몹 id 순번), `application/game/RoomNotifier.java`(package-private `@Component`), `application/game/MobGroupKey.java`(package-private record)
- Modify: `application/game/{CombatEngine,CombatActions,ItemCommandHandler,RoomViewAssembler,GameService,AbilityCommandHandler,AbilityCaster}.java`, `application/game/required/AbilityTimersRepository.java` 또는 `domain/combat/AbilityTimers.java`(시전 지우기 도우미)
- Test: 기존 게임 테스트 전부(기대값 그대로), `application/game/MobGroupKeyTest.java`(새, 단위)

**Interfaces:**
- Produces:
  - `MobSpawner`: `void reconcile(Map<Long, MobPlacement> placements, Instant now, BiConsumer<String, GameMessage> send)`, `void refill(Instant now)`, `void respawn(Map<Long, MobPlacement> placements, Instant now, BiConsumer<String, GameMessage> send)`. `CombatEngine.tick` 의 순서(reconcile → 법력 회복 → 행동 → 몹 다시 차기 → 플레이어 다시 차기 → 시체 정리 → 재등장)는 그대로다.
  - `RoomNotifier.notifyOthers(OnlinePlayer actor, GameMessage notice, BiConsumer<String, GameMessage> send)` — 같은 방(`actor.getRoomId()`)의 다른 사람에게. `CombatActions`·`ItemCommandHandler` 가 tone 에 맞는 `NoticeMessage` 를 만들어 넘긴다. `GameService` 의 이동 알림(`notifyOthers(actor, roomId, …)`, 방을 따로 받음)은 `RoomNotifier` 에 `notifyOthers(OnlinePlayer actor, Long roomId, GameMessage notice, BiConsumer…)` 겹이름을 두고 쓰게 해도 된다(구현자 판단, 메모).
  - `MobGroupKey(Long mobTypeId, boolean fighting)` + `static MobGroupKey of(MobInstance mob)` — `CombatActions.GroupKey`·`RoomViewAssembler.MobGroup` 을 갈음.
  - `AbilityCommandHandler.timers(Long characterId): Optional<AbilityTimers>` — `GameService` 의 `신통` 목록이 이것을 쓰고 `GameService` 는 `AbilityTimersRepository` 를 받지 않는다(T7-1).
  - 시전 지우기 한 곳: `AbilityTimersRepository` 에 default 메서드 `boolean clearCasting(Long characterId, Predicate<Casting> which)`(찾기 → `clearCasting` → 저장, 지웠으면 true) 또는 같은 뜻의 도우미. `CombatEngine.clearCasting`·`AbilityCommandHandler.stop`·`AbilityCaster.fire` 세 곳이 이것을 쓴다(T7-2).
- Consumes: 6차 `CombatActions`(`startEngagement`·`strike`·`onMobKilled`·`findTarget`).

- [ ] **Step 1: 단위 테스트** — `MobGroupKeyTest`: 같은 유형·같은 교전 여부면 같은 키, 교전 여부가 다르면 다른 키.
- [ ] **Step 2: 옮기기** — 위 Interfaces 대로 한 번에 하나씩 옮기고 그때마다 `./gradlew test --tests 'mudmk3.application.game.*'` 통과를 본다. 정렬 상수 위치(T7-3 — `CombatEngine` 의 정렬 상수를 쓰는 곳 가까이, 6차 리뷰 메모)도 이때 맞춘다. 순환 의존이 생기지 않게 `MobSpawner` 는 `CombatEngine` 을 모른다.
- [ ] **Step 3: 확인** — `CombatEngine.java` 가 300줄 아래, `grep -n 'record GroupKey\|record MobGroup\|private void notifyOthers' world-server/src/main` 결과 없음, `GameService` 가 `AbilityTimersRepository` 를 import 하지 않음.
- [ ] **Step 4: 검증** — `./gradlew build`(1~6차 테스트 기대값 무변경).
- [ ] **Step 5: 커밋** — `refactor(world-server): CombatEngine 에서 몹 나타나기를 떼고 같은 방 알림·묶음 키·시전 지우기를 한 곳으로` + 본문 `Fixes #15`.

**완료 조건:** 위 확인 셋, 기존 테스트 기대값을 바꾸지 않고 `./gradlew build` 통과.
**progress.md 한 줄:** 새 컴포넌트 셋과 옮긴 메서드, `CombatEngine` 줄 수 전후, 시전 지우기 도우미 위치, `GameService` 이동 알림을 어떻게 했는지.

---

### Task 5: 전투 메모리 정리와 지워진 몹 유형의 처치

**권장 모델:** opus

**목적:** #11(L-D3), #14(L-D4).

**Files:**
- Modify: `application/game/CorpseHandler.java`(`leaveCorpse`), `application/mob/provided/MobTypeFinder.java`(없으면 `Optional<MobType> findOptional(Long)` 더함), `application/game/CombatEngine.java`(틱 끝 정리 단계), `application/game/GameService.java`(`relocateFrom`), `application/game/required/{PlayerCombatRepository,AbilityTimersRepository,CorpseRepository,MobInstanceRepository}.java` 와 `adapter/persistence/memory/*` 구현(필요한 지우기 메서드), `domain/combat/{PlayerCombat,AbilityTimers}.java`(지워도 되는지 묻는 메서드)
- Modify: `docs/spec/system/platform.md` 5차 메모리 절(정리 규칙 한 줄 `(제안)`), admin-tool.md 84행 뒤에 '쓰러지는 순간 몹 유형이 없으면 드롭 없는 시체' 한 줄 `(제안)`
- Test: `application/game/provided/CombatRewardTest.java`(또는 시체 테스트), `application/game/provided/CombatDefeatTest.java`, `application/game/provided/CombatSpawnTest.java`, `domain/combat/{PlayerCombatTest,AbilityTimersTest}.java`

**Interfaces:**
- Produces: `PlayerCombat.isIdle(Instant now): boolean`(교전 상대 없음 && `readyAt(now)` && 다시 찰 예약 없음), `AbilityTimers.isIdle(Instant now): boolean`(시전 없음 && 공통·재사용 대기 모두 끝남), 저장소 `void delete(Long characterId)`(`PlayerCombatRepository`·`AbilityTimersRepository`), `void deleteAllInRoom(Long roomId)`(`CorpseRepository`·`MobInstanceRepository`). 틱 마지막 단계 `sweepOffline(now)`: 접속하지 않은 캐릭터(`presenceRepository.findByCharacterId` 없음)의 기록 가운데 `isIdle` 인 것을 지운다.
- `relocateFrom(deletedRoomId)`: 사람을 옮긴 뒤 그 방의 시체·살아 있는 몹을 메모리에서 지운다(교전 상대에서 빼는 것은 `endAll` 이 이미 함 — 다른 방 사람이 그 몹과 교전 중일 수 없음).
- `leaveCorpse`: `mobTypeFinder.findOptional(id)` 가 비면 드롭 없는 시체(`drops = List.of()`)를 남기고 경고 로그 없이 진행.

- [ ] **Step 1: 실패하는 테스트**
  - `killAfterMobTypeDeletedLeavesEmptyCorpse`: 몹이 나타난 뒤 배치·몹 유형을 지우고(틱 전) 같은 틱 안에 처치 → 예외 없음, `청모랑의 시체` 가 남고 수습하면 빈 시체 문구(5차 문구 그대로), 같은 명령의 다른 결과(처치 줄)도 나감.
  - `roomDeletionRemovesCorpsesAndMobs`: 시체·몹이 있는 방을 지움 → 메모리 저장소에 그 방 것이 없음(다음 틱을 기다리지 않음).
  - `idleOfflineCombatRecordRemoved`: 끊은 캐릭터, 교전 없음, 다음 공격 시각 지남 → 다음 틱 뒤 `PlayerCombatRepository.find` 비어 있음.
  - `offlineRecordKeptUntilAttackReady`: 끊은 직후(다음 공격 시각 전) 틱 → 기록 남음, 재접속해 곧바로 `공격` → 5차대로 대기(악용 막음 그대로).
  - `reconnectAfterSweepRefills`: 기록이 지워진 뒤 기혈이 깎인 채 재접속 → 10초 뒤 다시 참(5차 그대로), 법력 회복 기준 시각 새로 시작(6차 `noRegenWhileOffline` 그대로).
  - `expiredOfflineTimersRemoved`·`offlineCooldownKeptUntilExpired`: 대기시간 기록도 같은 규칙.
  - 도메인: `PlayerCombatTest.isIdle`(교전 중 false, 다음 공격 전 false, 다시 찰 예약 있음 false, 셋 다 아니면 true), `AbilityTimersTest.isIdle`.
- [ ] **Step 2: 실패 확인.**
- [ ] **Step 3: 구현** — 위 Interfaces 대로. 정리는 게임 락 안 틱에서만. `CombatMemoryReset` 은 저장소가 늘지 않으므로 그대로(확인).
- [ ] **Step 4: 통과 확인** — `./gradlew build`.
- [ ] **Step 5: 스펙** — platform.md 5차 '메모리에만 두는 것' 뒤에 '접속하지 않은 캐릭터의 교전·대기시간 기록은 교전 상대가 없고 다음 공격 시각·대기시간이 모두 지나면 틱이 지운다. 방을 지우면 그 방의 시체·몹도 바로 지운다. (제안)', admin-tool.md 84행 드롭 문장 뒤 '쓰러지는 순간 몹 유형이 지워져 있으면 드롭 없는 시체를 남긴다. (제안)'.
- [ ] **Step 6: 커밋** — `fix(world-server): 끊긴 캐릭터·지워진 방의 전투 메모리 정리와 지워진 몹 유형의 처치` + 본문 `Fixes #11`·`Fixes #14`.

**완료 조건:** 위 테스트 통과, 5·6차 테스트 기대값 무변경, `./gradlew build` 통과.
**progress.md 한 줄:** `isIdle` 규칙 둘, 틱 정리 단계 위치, 방 삭제 정리, 지워진 유형 처치(L-D3), 스펙 두 줄.

---

### Task 6: 관리·게임 조회 쿼리 수 줄이기

**권장 모델:** opus

**목적:** #8 전부, #17 쿼리 셋.

**Files:**
- Modify: `world-server/src/main/resources/application.yml`(배치 읽기 크기), `application/technique/TechniqueQueryService.java`, `application/technique/required/TechniqueRepository.java`, `application/technique/provided/TechniqueFinder.java`, `application/game/ItemCommandHandler.java`(`보기 <옥간>` 313행 부근), `adapter/webapi/NpcApi.java`(91행 방 조회), `application/room/provided/RoomFinder.java`·그 구현(이름만 읽기), `application/mob/MobTypeQueryService.java`, `application/mob/required/MobPlacementRepository.java`
- Create: `src/test/java/mudmk3/QueryCounter.java`(Hibernate `Statistics` 를 감싼 테스트 도우미 — `reset()`, `long prepared()`), 테스트 설정 `application-test.yml` 에 `spring.jpa.properties.hibernate.generate_statistics: true`
- Test: `application/technique/TechniqueQueryCountTest.java`, `application/mob/MobTypeQueryServiceTest.java`(추가), `adapter/webapi/NpcApiQueryCountTest.java`(또는 기존 NPC API 통합 테스트에 추가)

**Interfaces:**
- Produces: `TechniqueRepository` 의 묶음 조회 — `List<LearnerCount> countLearnersByTechnique()`(또는 `in` 목록 판), `List<ScrollItemRow> findScrollItemsIn(Collection<Long> techniqueIds)`. `viewAll` 은 공법 수와 상관없이 정해진 수의 쿼리. 공법 그래프는 컬렉션 여럿을 한 쿼리로 읽지 않는다 — 공법(+`elements`) 한 번, `stages`·`abilities`(+그 값 컬렉션 `elements`·`effectLines`·`stagePowers`)는 지연 로딩 + `application.yml` 의 `spring.jpa.properties.hibernate.default_batch_fetch_size: 100`(orm.xml 에는 batch-size 가 없음)으로 묶어 읽는다. 결과 순서(이름순·`position` 순)는 그대로.
  - `TechniqueFinder.viewContained(Long techniqueId): ContainedTechniqueView`(옥간 보기에 필요한 값만 — 이름·단계·신통 이름 등 `ItemCommandHandler.containedTechnique` 가 실제로 쓰는 칸, 익힌 수·담긴 물건은 세지 않음). 쓰는 칸은 구현자가 `containedTechnique` 를 읽어 정한다.
  - `RoomFinder.findNames(Collection<Long> roomIds): Map<Long, String>`(출구를 읽지 않음). NPC 목록·`MobTypeQueryService.placements` 가 쓴다.
  - `MobTypeQueryService.viewAll`: 몹 종류가 없으면 `IN` 쿼리를 내지 않고 빈 목록. `findAllByMobTypeIdInOrderByIdAsc` 에 `@EntityGraph(attributePaths = "mobType")`.

- [ ] **Step 1: 실패하는 쿼리 수 테스트** — 공법 3개·5개를 만든 두 경우에 `viewAll()` 쿼리 수가 같음(`techniqueViewAllQueryCountConstant`), 공법 하나의 `view()` 결과 행이 곱해지지 않음(신통 3·단계 9·이펙트 3 일 때 결과 컬렉션 크기가 정확하고 쿼리 수 ≤ 정해진 값 — 구현 뒤 값을 고정해 회귀 테스트로), NPC 3명을 서로 다른 방에 두고 목록 요청 쿼리 수 = NPC 1명일 때와 같음, 몹 종류 0개에서 `viewAll()` 이 쿼리 1개(목록)만.
- [ ] **Step 2: 실패 확인.**
- [ ] **Step 3: 구현** — 위 Interfaces 대로. 응답 JSON 모양·순서는 바뀌지 않는다(기존 WebMvc·API 테스트 그대로 통과).
- [ ] **Step 4: 통과 확인** — `./gradlew build`.
- [ ] **Step 5: 커밋** — `refactor(world-server): 공법·NPC·몹 관리 조회의 N+1 과 행 곱 줄이기` + 본문 `Fixes #8`.

**완료 조건:** 쿼리 수 테스트 통과(고정한 값을 메모에), 기존 API 테스트 기대값 무변경, `./gradlew build` 통과.
**progress.md 한 줄:** 바뀐 조회와 쿼리 수 전후, 행 곱을 푼 방식, `QueryCounter`.

---

### Task 7: 헥사고날 의존·죽은 코드·중복 로직 정리

**권장 모델:** sonnet

**목적:** #5, #6, #7(world-server 의 경지·`braceletOpen`·자질 버림), #17 나머지. 기계적 정리 — 동작 그대로.

**Files:**
- Move: `application/technique/required/ScrollItem.java` → `application/technique/provided/ScrollItem.java`(JPQL 생성자 식 경로 `TechniqueRepository` 34행도 고침 — Task 6 이 바꿨으면 그 쿼리)
- Modify: `HexagonalArchitectureTest.java`(규칙 더함), `application/cultivation/required/CharacterTechniqueRepository.java`(+테스트), `domain/floor/FloorPlacementSaveInfo.java`, `application/game/ShopCommandHandler.java`, `application/item/required/ShopPrice.java`, `application/game/{ItemCommandHandler,CorpseHandler}.java`, `test/.../BaseGameTest.java`(`give`), `domain/cultivation/StatSheet.java`, `application/game/required/GameMessage.java`(`StatusMessage.of`), `application/game/CultivationTexts.java`, `adapter/webapi/dto/{MobSummaryResponse,MobDetailResponse}.java`, `domain/mob/{MobTier,MobType}.java`
- Create: `application/game/CharacterRealms.java`(package-private `@Component`)

**Interfaces:**
- Produces:
  - ArchUnit `webAdapterUsesOnlyProvidedPorts`: `mudmk3.adapter.webapi..` 의 클래스는 `mudmk3.application..required..` 에 의존하지 않는다. (`adapter.persistence`·`adapter.websocket`·`adapter.scheduling` 은 required 포트를 구현하므로 대상 밖.)
  - `CharacterRealms`: `Realm realm(Long characterId)`, `boolean braceletOpen(Long characterId)` — `ItemCommandHandler.braceletOpen`·`ShopCommandHandler.realmOf/braceletOpen`·`CorpseHandler` 114행이 쓴다. `BaseGameTest.give` 도 같은 값을 쓰게(테스트에서 package-private 빈을 못 쓰면 `characterFinder.find(id).getRealm().opensBracelet()` 한 줄 도우미로 모음).
  - `StatSheet.aptitudeShown(Element): int`(소수점 버림) — `StatusMessage.of`·`CultivationTexts.status` 가 쓴다.
  - `MobSummaryResponse` 가 자기 `StatsResponse`(또는 공용 `MobStatsResponse`)를 쓴다 — JSON 모양 그대로.
  - `MobTier`: 0 센티널 필드를 `@Nullable Double attackFactor`·`@Nullable Integer defaultRespawnSeconds`(또는 `OptionalDouble`/`OptionalInt` 필드)로. 공개 메서드 시그니처(`attackFactor(): OptionalDouble` 등)는 그대로, `MobType.java:60` 의 0 비교를 그 메서드로.
- `#6` 각 항목은 지우기 전 `grep -rn` 으로 사용처를 확인한다: `existsByCharacterIdAndTechniqueId`(테스트만 → 메서드와 그 테스트 단언을 지움), `FloorPlacementSaveInfo` 의 `@Min/@Max`(검증은 도메인이 함 — 지운 뒤 바닥 배치 400 테스트가 그대로 통과), `ShopCommandHandler.confirmQuestion` 의 `change > 0` 분기(깨는 돌이 있으면 거스름이 늘 0 보다 큰지 `SpiritStonePayment` 로 확인 — 늘 크면 분기를 지우고 근거를 주석 한 줄로, 0 일 수 있으면 그대로 두고 메모), `ShopPrice.effectivePrice` 가 `Pricing.defaultSellPrice` 와 같은 규칙을 다시 씀 → `Pricing` 에 `long sellPrice(@Nullable Integer price, long baseValue)` 를 두고 `ShopItem`·`ShopPrice` 둘 다 그것을 부름.
- `MobTypeSaveInfo` 묘사 strip 은 바꾸지 않는다(분류 표 판단).

- [ ] **Step 1: ArchUnit 규칙 먼저** — 규칙을 더하고 `./gradlew test --tests '*HexagonalArchitectureTest'` → FAIL(`TechniqueDetailResponse` → `ScrollItem`) 확인.
- [ ] **Step 2: `ScrollItem` 옮기기** → 통과.
- [ ] **Step 3: #6·#7·#17 정리** — 위 대로. 테스트: `StatSheetTest.aptitudeShownFloors`(52.9 → 52), `PricingTest.sellPrice`(null → 기준가, 값 → 그 값), `MobTierTest`(두목 `attackFactor()` 비어 있음·재등장 기준 비어 있음, 일반 0.6·90) — 기존 테스트가 이미 덮으면 그대로.
- [ ] **Step 4: 검증** — `./gradlew build`.
- [ ] **Step 5: 커밋** — 두 개로: `refactor(world-server): ScrollItem 을 provided 로 옮기고 웹 어댑터의 required 의존을 ArchUnit 으로 막음`(본문 `Fixes #5`), `refactor(world-server): 쓰이지 않는 코드·중복 로직·몹 체급 센티널 정리`(본문 `Fixes #6`·`Fixes #17`).

**완료 조건:** ArchUnit 새 규칙 통과, 기존 테스트 기대값 무변경, `./gradlew build` 통과.
**progress.md 한 줄:** ArchUnit 규칙, 지운 것·남긴 것(`confirmQuestion` 판단 근거), `CharacterRealms`·`aptitudeShown`·`Pricing.sellPrice`, `MobTier` 모양.

---

### Task 8: world-server 테스트 위생과 6차 작은 정리

**권장 모델:** sonnet

**목적:** #9 테스트 위생, #23, #7 테스트 도우미 중복, 6차 T5-1·T5-2·T5-3·T6-3·T7-3(남았으면)·T10-4·T10-6. 기대하는 값은 그대로, 쓰는 방식만 고친다(T5-2 는 main 코드 검사 한 줄).

**Files (test 는 `world-server/src/test/java/mudmk3/` 기준):**
- `domain/inventory/SpiritStonePaymentTest.java`(`highUsedUpMiddleBroken` → 뜻에 맞는 이름, 본문을 읽고 '상품을 다 쓰고 중품을 깸' 처럼 실제 시나리오대로), `domain/item/PricingTest.java`(어색한 assert → AssertJ 한 줄), `$[0]` 정렬 의존 단언이 있는 WebMvc·API 테스트(`grep -rn '\$\[0\]'` — 정렬이 계약이면 그대로, 아니면 `extractingPath`/이름으로 찾기), `adapter/webapi/AdminApiIntegrationTest.java`(`manageCharacters` 위치 기반 단언 → 이름으로 찾기, '지우지 않는 캐릭터' 주석), `application/game/provided/BaseGameTest.java`(`designateRealm` javadoc 정리, `textsTo(String sessionId)` 를 올림, `id()` 하나로), `GameConnectorTest`·`GameConnectorConcurrencyTest`(`id()`·`textsTo` 중복 지움), `AbilityUseTest`·`AbilityInsightTest`·`AbilityCastingTest`(`textsTo` 지움), `MutableClockReset.java`(after 에서도 되돌림 — `AfterEachCallback` 더함)
- `application/technique/provided/TechniqueEditorTest.java`(344행 `java.util.Arrays.asList` 등 완전 수식 이름 → import), `adapter/webapi/TechniqueApiWebMvcTest.java`(`create` 55행 JSON 부분 문자열 비교 → `$.abilities[0].stagePowers[0].stage` 등 경로 단위)
- `application/game/provided/AbilityUseTest.java`(`singleEffectLineUsesNoRandom` — `ScriptedRandom` 이 한 번도 불리지 않았음을 직접 단언: 남은 수가 그대로이거나 호출 수 0), `AbilityInsightTest.insightCapResetsAfterReengage`(끊은 뒤 기다림을 `advanceAndTick(1)` 로 — `REFILL_DELAY` 10초 전 — 해서 `disengage` 의 `clearInsightCap` 만으로 상한이 지워짐을 봄; 그 줄을 지우면 실패함을 한 번 확인하고 되돌림)
- #23: `domain/technique/TechniqueFixture.java`(또는 `application/technique/provided/TechniqueRequestFixture.java`) 한 곳에 `AbilityRequest` with 도우미(`withEffectLines`·`withStagePowers`·`withCast`·`withCost`·`withCooldown`·`withLearnStage`)를 모으고 `TechniqueEditorTest.java:396`·`missingStagePowersFail`·`GameConnectorTest.java:2693·2700` 이 그것을 부름. 칸 14개를 다시 나열하는 곳은 한 곳만.
- 6차: T5-1 `CombatSpawnTest.java` import 순서(`Corpse` 가 `AttackNature` 앞), T5-2 `domain/combat/AbilityTimers.startCooldown` 에 `requireNonNull(seconds)`(+단위 테스트 `startCooldownNullFails`), T5-3 `MobInstance.grantInsight` Javadoc 을 실제 동작(한 상대 상한)에 맞게, T6-3 `AbilityTexts` 의 static·인스턴스 메서드 섞임을 한쪽으로(인스턴스 빈이면 상수만 static), T7-3 은 Task 4 에서 했으면 건너뜀.
- 문서: T10-6 `docs/spec/system/index.md` 6차 완료 시나리오 `이펙트 문장 0개·6개 저장 거절 확인` 뒤에 `(REST·스모크로 확인)`. T10-4 `docs/spec/system/command.md` 6차 신통 절에서 같은 내용을 두 번 적은 문장 하나를 지운다(6차 Task 10 리뷰가 가리킨 것 — 찾지 못하면 건너뛰고 메모).

- [ ] **Step 1: 테스트별로 고치고 그 테스트만 돌려 통과 확인**(이름 바꾼 것은 바뀐 이름으로).
- [ ] **Step 2: 검증** — `./gradlew build`, 테스트 수가 줄지 않음(바뀐 수를 메모에).
- [ ] **Step 3: 커밋** — `test(world-server): 테스트 위생 정리와 AbilityRequest with 도우미`(본문 `Fixes #23`), 문서는 따로 `docs(spec): 6차 리뷰 작은 문장 정리`.

**완료 조건:** 기대값 무변경, `./gradlew build` 통과.
**progress.md 한 줄:** 고친 테스트 목록, with 도우미 위치, `MutableClockReset` after, T10-4 처리 결과, 테스트 수 전후.

---

### Task 9: world-server 테스트 빈틈 메우기

**권장 모델:** opus

**목적:** #9 의 테스트 빈틈. 지금 동작을 고정하는 테스트를 더한다(기대값은 스펙·룰북·지금 코드에서). 테스트가 지금 코드와 다르게 나오면 버그일 수 있으므로 고치지 말고 멈춰 보고한다. 아이템 종류 목록 대소문자 정렬만 main 을 고친다.

**Files:**
- Test: `application/game/provided/GameConnectorTest.java`(영근 확인 두 경우), 상점·소지품 테스트(저물대 3개), `GameConnectorConcurrencyTest.java`(동시 줍기), 방 삭제 테스트, `CombatTest`(1.1배 대상 전환), `CombatRewardTest`(기준가 0 드롭, 지워진 종류만 든 시체), `application/item/...ItemTypeQueryServiceTest`(정렬)
- Modify: `application/item/required/ItemTypeRepository.java`(목록 정렬)

**테스트:**
- `existingCharacterRootConfirmWithoutStartRoom`·`existingCharacterRootConfirmWhenSavedRoomDeleted`: 영근을 정하지 않은 기존 캐릭터가 영근 확인할 때 시작 방이 없으면 / 저장된 방이 지워졌으면 — 지금 코드 경로의 결과(1차 `WORLD_NOT_READY` 닫기 / 시작 방으로) 그대로.
- `buyWithThreeBagsInUse`·`sellWithThreeBags`: 저물대 3개일 때 사기 `BAG_IN_USE`, 팔기 `NO_ROOM`(문구는 `ShopCommandHandler` 상수 그대로).
- 동시 줍기: `전부` 대신 정확한 수(3개 바닥에서 둘이 각각 `줍기 2 늑대 가죽`)로 — 한 사람 2개, 다른 사람 1개, 바닥 0.
- 방 삭제: 지워진 roomId 를 가진 캐릭터 행을 직접 만든 뒤 접속 → 시작 방(1차 동작).
- `targetSwitchTo110Percent`(1.1배 대상 전환 끝-끝): command.md 5차 위협·대상 전환 규칙의 예 그대로 두 플레이어가 몹 하나와 교전, 위협이 1.1배를 넘는 순간 몹 대상이 바뀜(넘지 않으면 그대로).
- `zeroBaseValueDropsOneLowStone`: 고정 재료 기준가 0 일 때 영석 1개(5차 드롭 규칙 — 지금 코드 확인 뒤 그 값).
- `corpseOfDeletedItemTypesShowsEmpty`: 드롭 줄이 모두 지워진 종류인 시체를 수습 → 5차 빈 시체 문구.
- `itemTypesSortedIgnoringCase`: `apple`, `Banana`, `cherry` → 그 순서(H2·PostgreSQL 같은 결과가 나오게 `lower(name)` 정렬 + id). admin-tool.md 의 목록 순서 정의와 맞는지 먼저 확인하고, 스펙이 다르게 정했으면 멈춤.
- [ ] **Step 1: 테스트 쓰기 → 돌림.** 정렬 테스트만 FAIL 이어야 한다(나머지가 FAIL 이면 멈추고 보고).
- [ ] **Step 2: 정렬 고침 → 통과.**
- [ ] **Step 3: 검증** — `./gradlew build`.
- [ ] **Step 4: 커밋** — `test(world-server): 1~5차 테스트 빈틈과 아이템 종류 목록 대소문자 정렬`.

**완료 조건:** 새 테스트 통과, 지금 동작과 다른 결과가 없음(있으면 보고됨), `./gradlew build` 통과.
**progress.md 한 줄:** 더한 테스트, 정렬 고침, 버그로 보고한 것(있으면).

---

### Task 10: admin-console 숫자 칸·접근 이름·경지 폼 상태

**권장 모델:** sonnet

**목적:** #16, #22, #7 `CharacterRealmForm` 이중 상태.

**Files (`admin-console/src/` 기준):**
- Modify: `components/{FloorItemsEditor,ShopEditor,StartItemsEditor,ItemForm,AbilityTable,StageTable}.tsx`(`min=` 지움, 정수 칸도 `step="any"`), `components/AbilityDetailEditor.tsx`(65행 `aria-label={`이펙트 문장 ${index + 1} 삭제`}`), `components/AbilityTable.tsx`(243행 `aria-label={`${name} 삭제`}` — 이름이 빈 새 행이면 `신통 ${index + 1} 삭제`), `components/CharacterRealmForm.tsx`
- Test: 각 컴포넌트 `*.test.tsx`

- [ ] **Step 1: 실패하는 테스트**
  - `zeroCountShowsServerDetail`(FloorItemsEditor·ShopEditor·StartItemsEditor·ItemForm 각각): 수/값에 `0` 을 넣고 저장 → fetch 가 불리고(브라우저 검사가 막지 않음) 모의 400 `detail` 이 화면에 보임.
  - `AbilityDetailEditor`/`AbilityTable`: `getByRole('button', { name: '이펙트 문장 1 삭제' })`, `getByRole('button', { name: '화탄술 삭제' })` 가 각각 하나. 기존 테스트의 `{ name: '삭제' }` 찾기를 새 이름으로 바꾼다(보이는 글자 `삭제` 는 그대로).
  - `CharacterRealmForm`: 저장 뒤 받은 상세로 다시 그릴 때 선택 상자 값과 스탯 표가 같은 상태 하나에서 나옴 — 저장 응답의 경지가 요청과 다를 때(모의) 선택 상자가 응답 값을 보임.
- [ ] **Step 2: 실패 확인 → 구현.** `CharacterRealmForm` 은 경지 상태를 하나로(받은 상세에서 파생, 편집 중 값만 따로) — 이중 상태가 무엇인지 컴포넌트를 읽고 메모.
- [ ] **Step 3: 검증** — admin-console 검증 명령.
- [ ] **Step 4: 커밋** — `fix(admin-console): 숫자 칸 브라우저 검사를 풀고 삭제 버튼 접근 이름을 나눔`(본문 `Fixes #16`·`Fixes #22`), `refactor(admin-console): 경지 지정 폼 상태 하나로`(본문 `Fixes #7` — Task 7·8 이 #7 의 나머지를 끝낸 뒤라야 한다; 아니면 Fixes 를 빼고 메모).

**완료 조건:** `grep -rn 'min={' admin-console/src/components` 결과 없음, 테스트 통과, 검증 명령 통과.
**progress.md 한 줄:** 고친 칸 목록, 접근 이름 둘, 경지 폼 상태.

---

### Task 11: admin-console 오류 처리와 테스트 빈틈

**권장 모델:** opus

**목적:** #10 admin-console 항목(목록에 없는 시작 소지품 id 표시는 창호 결정이라 뺌).

**Files (`admin-console/src/` 기준):**
- Modify: `lib/api.ts`(401 처리), `components/AdminGuard.tsx`, `components/ItemForm.tsx`(빈 기준가), 저장 응답을 버리는 페이지·폼(`onSubmit: … => Promise<void>` 인 `ZoneForm`·`ItemForm`·`NpcForm`·`FactionForm`·`RoomForm` 을 쓰는 페이지 — 저장 뒤 응답으로 화면 상태를 바꾸는지 하나씩 확인), 지역 화면(성·종문 목록 실패 처리)
- Test: `lib/api.test.ts`, `components/AdminGuard.test.tsx`, `components/ItemForm.test.tsx`, `components/RoomPicker.test.tsx`(새), 페이지 연결 테스트(새 — 페이지 하나: 지역 화면)

**Interfaces:**
- Produces: `lib/api.ts` 가 로그인 뒤 요청에서 401 을 받으면 `ApiError` 를 던지기 전에 등록된 처리기(`onUnauthorized(handler: () => void): () => void`)를 부른다. `AdminGuard` 가 처리기를 등록해 로그인 화면으로 바꾼다. `GET /api/auth/me` 의 401(아직 로그인 안 함)과 로그아웃의 401 은 지금 동작 그대로.
- 빈 기준가: 칸이 비면 요청에 `baseValue: null` 을 보낸다(지금은 `Number('') = 0`). 서버의 400 `detail` 을 그대로 보인다.
- 지역 화면: 성·종문 목록 요청이 실패해도 지역 목록·폼은 보이고 실패 detail 을 한 줄로 보인다(문구는 서버 detail 또는 기존 `errorMessage` 결과 — 새 문구를 만들지 않음).

- [ ] **Step 1: 실패하는 테스트** — `midSession401ShowsLogin`(로그인된 화면에서 저장 요청이 401 → 로그인 화면), `blankBaseValueSendsNull`(요청 본문 `baseValue: null`, 모의 400 detail 보임), 저장 응답 반영(`savedNameFromServer` — 서버가 이름을 잘라 돌려주면 화면이 그 이름), `zoneScreenShowsZonesWhenFactionsFail`, `RoomPicker` 단독(목록·고르기·빈 목록), 지역 페이지 연결(목록 → 고치기 링크 → 폼).
- [ ] **Step 2: 실패 확인 → 구현.**
- [ ] **Step 3: 검증** — admin-console 검증 명령.
- [ ] **Step 4: 커밋** — `fix(admin-console): 세션 만료 401·빈 기준가·저장 응답·지역 화면 부분 실패 처리`(Fixes 없음 — #10 은 play-client·창호 항목이 남음).

**완료 조건:** 테스트 통과, 검증 명령 통과.
**progress.md 한 줄:** 401 처리기, 응답을 반영하게 고친 화면 목록, 새 테스트.

---

### Task 12: play-client 테스트 빈틈과 자동 스크롤

**권장 모델:** sonnet

**목적:** #10 play-client 항목(여러 낱말 Tab·주운 뒤 후보는 창호 결정이라 뺌), 6차 T9-1·T9-2.

**Files (`play-client/src/` 기준):**
- Modify: `components/Terminal.tsx`(85행 `log.scrollTop = log.scrollHeight`)
- Test: `components/Terminal.test.tsx`, `lib/complete.test.ts`, `lib/render.test.ts`

- [ ] **Step 1: 실패하는 테스트**
  - `keepsScrollWhenReadingUp`: 사용자가 위로 올려 읽는 중(바닥에서 떨어짐)에 새 줄이 오면 `scrollTop` 이 그대로, 바닥에 붙어 있었으면 바닥으로(jsdom 에서 `scrollHeight`·`clientHeight` 를 정의해 시험).
  - `reconnectClearsInventoryCandidates`: 다시 연결하면 소지품 이름 후보가 비는 것(4차 동작 고정).
  - `candidatesUnchangedOnKillAndRespawn`: 처치·재등장 `notice` 가 와도 자동완성 후보가 바뀌지 않음(5차 `(제안)` 동작 고정).
  - Tab 순서·`kind` 완전성: `COMMANDS` 순서가 command.md `### 6차 도움말` 순서와 같고, `room` 의 occupants `kind` 값(`player`·`npc`)과 서버 tone 목록을 모두 다루는지(빠진 값이 있으면 타입 오류가 나게 `satisfies`/전수 검사).
  - T9-1: `insight` 를 복사해 쓰는 테스트를 공용 픽스처로, T9-2: '흩어짐' 줄 예를 서버 `AbilityTexts` 문구 그대로.
- [ ] **Step 2: 실패 확인 → 구현** — 자동 스크롤은 새 줄을 붙이기 전에 '바닥 근처(남은 거리 ≤ 한 줄 높이)' 였는지 기억해 그때만 내린다.
- [ ] **Step 3: 검증** — play-client 검증 명령.
- [ ] **Step 4: 커밋** — `fix(play-client): 위로 올려 읽는 중엔 자동 스크롤하지 않음과 자동완성 테스트 보강`(Fixes 없음).

**완료 조건:** 테스트 통과, 검증 명령 통과.
**progress.md 한 줄:** 자동 스크롤 규칙, 더한 테스트, T9-1·T9-2.

---

### Task 13: 통합 확인 — 세 모듈, compose 한 번, 스모크 전체(기존 pgdata)

**권장 모델:** opus

**목적:** debt-1 전체가 기존 pgdata(1~6차 `ddl-auto=update` 스키마, `LegacySchemaCleaner` 정리 이력)에서 안전하게 뜨고 1~6차 시나리오가 그대로인지 확인한다. **이 계획에서 Docker 를 쓰는 유일한 태스크다.**

**Files:**
- Modify: `scripts/smoke-test.mjs`(#9 둘, T11-2), `README.md`(스모크 절 한 줄), `docs/impl/debt-1/progress.md`

**스모크 고침:**
- #9 `runNo`: `Date.now() % 1_000_000` 은 약 17분마다 되풀이돼 기존 pgdata 에 같은 이름이 남아 있으면 409 가 난다. 실행 번호를 고른 뒤 관리 API 로 그 번호의 이름(성·종문 `청석성<n>`)이 이미 있는지 보고 있으면 1 더해 다시 본다(자릿수 6 그대로 — 이름 길이 제한).
- #9 단계 39: `수습` 전에 소지품 빈칸이 있음을 단언한다(수습이 자리 없음으로 실패하는 경우를 가려냄).
- T11-2 `repeatUntil`: 6차 Task 11 리뷰가 가리킨 되풀이 도우미 문제를 고친다(구현자가 리뷰 메모·코드를 보고, 찾지 못하면 메모만).
- 그 밖의 단계·기대값은 그대로.

- [ ] **Step 1: 스크립트 수정** — `node --check scripts/smoke-test.mjs`.
- [ ] **Step 2: 세 모듈 검증** — world-server `./gradlew build`, admin-console·play-client 검증 명령. 테스트 수를 메모에.
- [ ] **Step 3: 디스크 전** — `df -h .`·`docker system df` 를 메모에(10GB 미만이면 멈춤).
- [ ] **Step 4: compose 한 번** — 스모크 전 `docker compose exec db pg_dump`(또는 `docker run --rm -v pgdata…`) 대신 **psql 로 기준 상태만 기록**: enum CHECK 목록(`SELECT conrelid::regclass, conname, pg_get_constraintdef(oid) FROM pg_constraint WHERE contype='c'`) — DB 만 먼저 `docker compose up -d db` 로 띄워 확인. 그다음 `docker compose build` **한 번** → `up -d`(셸 값 덮어쓰기, **기존 pgdata**) → 기동 로그에 스키마 오류 없음·`enum CHECK 제약 N개를 지웠습니다.`(첫 기동, N ≥ 1)·틱 오류 없음 → psql 로 enum CHECK 가 없고 숫자 CHECK·유니크·FK 는 남음, 기존 캐릭터·공법 행 수가 기동 전과 같음 → `source "$NVM_DIR/nvm.sh" && nvm use 26.13.1 && node scripts/smoke-test.mjs` → **단계 1~끝 `SMOKE OK`**. 실패하면 원인을 고치고(해당 모듈 테스트 통과) 바뀐 서비스만 `docker compose up -d --build <서비스>`(전체 재빌드 반복 금지). 고친 것은 따로 커밋.
- [ ] **Step 5: 다시 뜨기** — `docker compose restart world-server` → 로그에 enum CHECK 정리 줄 없음(멱등), 스키마 오류 없음.
- [ ] **Step 6: 되돌리기 확인** — 관리 API로 시작 방 id 1(태초 객잔)·시작 소지품 `[]`, psql 로 이 실행의 몹 배치 없음.
- [ ] **Step 7: 정리** — `docker compose down`(**볼륨 남김**) → `docker image prune -f` → `docker builder prune -f` → `world-server/build`, `play-client/{out,.next}`, `admin-console/{out,.next}` 삭제(`git check-ignore` 로 먼저 확인). `mud-mk3-*` 이미지·`pgdata`·`node_modules` 는 지우지 않는다. 정리 뒤 `df -h .`·`docker system df` 를 메모에.
- [ ] **Step 8: README·progress** — README 스모크 절에 '실행 번호는 기존 pgdata 에 없는 번호로 고른다' 한 줄. progress.md 에 결과(통과 여부, 고친 것, enum CHECK 지운 수, 디스크 전후, 테스트 수).
- [ ] **Step 9: 커밋** — `test(infra): debt-1 통합 확인과 스모크 실행 번호·수습 전 빈칸 단언`.

**완료 조건:** 세 모듈 검증 통과, 기존 pgdata 로 스모크 1~끝 `SMOKE OK`, 다시 떠도 정리 멱등, 시작 방 id 1·시작 소지품 `[]`·이 실행 몹 배치 0, pgdata 남음, 정리 뒤 dangling 이미지 없음.
**progress.md 한 줄:** 스모크 고침 셋, compose 빌드 한 번, enum CHECK 정리 결과(지운 수·남은 제약), 디스크 전후, 되돌림 확인.

---

### Task 14: 최종 전체 리뷰

**권장 모델:** opus

**목적:** debt-1 전체(main 의 debt-1 커밋 전부, `552c132` 다음부터 HEAD)를 이 계획·이슈와 대조한다. 코드를 고치면 따로 커밋한다.

- [ ] **Step 1: 리뷰** — `superpowers:requesting-code-review` 로 `552c132..HEAD`. 볼 것: Review Focus 다섯 줄이 테스트로 덮였는지, **게임 동작·문구가 바뀐 곳이 없는지**(1~6차 테스트 기대값이 바뀐 diff 가 없는지 `git diff 552c132 -- '*Test.java' '*.test.ts*'` 로 확인 — 바뀐 것은 Task 8 의 이름·단언 방식뿐이어야 함), enum CHECK 판정이 숫자 CHECK 를 건드리지 않는지, `characterRepository.save` 가 `create`·`chooseSpiritRoot` 에만 남았는지, 게임 락 밖에서 메모리 상태를 건드리는 곳이 없는지, 메모리 정리가 5차 '다음 공격 시각 유지' 를 깨지 않는지, ArchUnit 새 규칙, 스펙 고친 줄이 기술 사실만인지(`(제안)` 표시 유지), `Fixes #N` 이 이슈의 모든 항목을 끝낸 커밋에만 있는지.
- [ ] **Step 2: Critical·Important 고침** — 해당 모듈 검증 통과 후 커밋 `fix(<module>): …`. Docker는 다시 빌드하지 않는다(다시 스모크가 필요하면 리드에게 묻는다).
- [ ] **Step 3: progress.md** — 리뷰 결과, 고친 것, 이슈별 결과 표(닫힘 / 열어 둠 — 남은 항목 / 미룸 이유 / 창호 결정 필요), 다음 할 일. 상태를 "debt-1 완료" 로.
- [ ] **Step 4: 커밋** — `docs(impl): debt-1 기술 부채 정리 완료 기록`.

**완료 조건:** Critical·Important 0, progress.md 완료 기록.
**progress.md 한 줄:** 최종 리뷰 결과와 고친 것.

---

## 태스크 의존 순서

```
1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10 → 11 → 12 → 13 → 14
```

- world-server 는 순서대로 1~9. 나란히 하지 않는다: 2·3 은 `CharacterModifyService`, 4·5·6·7 은 `CombatEngine`·`ItemCommandHandler`·`CorpseHandler`·`GameService` 를, 6·7 은 `TechniqueRepository` 를, 8 은 앞 태스크들이 고친 테스트 파일을 함께 고친다.
- 10·11(admin-console)은 world-server 와 파일이 겹치지 않아 1~9 사이 아무 때나 해도 되지만, 리드가 한 번에 하나씩 리뷰하므로 9 뒤에 한다. 10 의 `Fixes #7` 은 7·8 뒤라야 한다.
- 12(play-client) 다음 13(통합 — 모든 코드가 들어간 뒤 compose 한 번), 마지막 14.
- 실행 순서: 번호 그대로 **1~14**.
- 각 태스크는 앞 태스크가 커밋된 상태에서 혼자 검증 명령이 통과한다.
