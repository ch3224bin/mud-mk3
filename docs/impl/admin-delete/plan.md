# 관리 툴 삭제(admin-delete) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 관리 툴에서 몹(배치와 함께)과 캐릭터(딸린 데이터 전부, 계정은 남김)를 한 건씩 지우고, 지운 것을 서버 로그에 남긴다.

**Architecture:** 몹 삭제는 관리자 트랜잭션에서 배치·유형을 지우고 메모리의 몹은 다음 틱의 배치 맞추기(5차 그대로)가 지운다. 캐릭터 삭제는 game 슬라이스의 새 provided 포트 `OfflineCharacterRemover` 를 `GameService` 가 구현해 **게임 락 안에서** 접속 판정 → DB 삭제(character 슬라이스 `CharacterRemover`, 익힌 공법·깨달음은 `CharacterDeletedEvent` 동기 리스너) → 메모리 정리(`CombatEngine.forget`)를 한 트랜잭션으로 한다. 삭제 로그는 웹 어댑터가 커밋 뒤 남긴다. admin-console 은 몹 확인 문구에 배치 수, 캐릭터 화면 맨 아래 이름 입력 확인 칸을 더한다.

**Tech Stack:** 1~6차·debt-2 와 같다 — Java 25 · Spring Boot 4.1.2 · Gradle 9.9.1 Kotlin DSL · PostgreSQL 18.6 / H2(테스트) · Next.js 16.4.6 · React 19.3.1 · TypeScript 7.0.2 · Vitest 5.1.0 · Docker Compose v5. **새 의존성 없음.**

**Spec:** 결정 요약 [index.md `### 관리 툴 삭제 (admin-delete)`](../../spec/system/index.md#관리-툴-삭제-admin-delete). 세부: [admin-tool.md `## 관리 툴 삭제 (admin-delete)`](../../spec/system/admin-tool.md#관리-툴-삭제-admin-delete)(몹 삭제·캐릭터 삭제·삭제 기록·화면·REST API), [platform.md `### 관리 툴 삭제 월드 상태`](../../spec/system/platform.md#관리-툴-삭제-월드-상태)(캐릭터 id 를 든 메모리 구조 표·순서), [auth.md](../../spec/system/auth.md)(계정 남음), [debt-2 계획](../debt-2/plan.md)·[진행](../debt-2/progress.md), `CLAUDE.md`(worktree 규칙), `.claude/rules/*.md`.

---

## 리드 판정 후보

구현을 막지 않고 아래대로 진행한다. 틀리면 리드가 바꾼다.

- **L-1 '접속 중' 의 범위.** 세계에 있는 것(presence)만이 아니라 **그 캐릭터 계정의 게임 연결이 하나라도 열려 있으면** 접속 중으로 거절한다(4차 전 캐릭터가 영근을 고르는 중인 `GameSession.rootingExisting` 도 캐릭터 id 를 들고 있어, 지우면 그 연결의 `확인` 이 없는 캐릭터를 고치다 실패한다). 목록의 `접속 중` 표시(presence)와 드물게 다를 수 있다.
- **L-2 몹이 사라지는 때.** 몹 삭제는 메모리를 바로 건드리지 않고 다음 틱(1초 안)의 배치 맞추기가 지운다(5차 배치 삭제와 같은 길, 관리자 트랜잭션은 게임 락을 잡지 않는 1차 관례). 교전 중 플레이어에게 따로 알리지 않는다(5차 그대로). '바로' 가 필요하면 AFTER_COMMIT 리스너로 게임 락을 잡아 맞추기를 한 번 부르는 태스크를 더한다.
- **L-3 로그 자리.** 관리자 계정은 HTTP 로그인 정보에만 있어 웹 어댑터(`MobApi`·`CharacterApi`)가 포트가 돌려준 값(`DeletedMobType`·`DeletedCharacter`)으로 커밋 뒤 `info` 한 줄을 남긴다. 거절은 남기지 않는다. DB 감사 표는 없다.
- **L-4 캐릭터 확인 칸.** 모달(`<dialog>`) 대신 화면 안에 열리는 칸이다(jsdom `showModal` 지원이 불완전하고 다른 화면은 `window.confirm` 뿐이라 새 모달 틀을 만들지 않음). 이름 비교는 글자 그대로(공백 자르기 없음).
- **L-5 캐릭터 삭제 UI 자리.** `CharacterDetail` 은 그대로 두고 `app/character/page.tsx` 가 그 아래에 새 `CharacterDeleteForm` 을 둔다(기존 `CharacterDetail.test` 무변경).
- **L-6 몹 확인 창의 배치 수.** 새 API 없이 화면을 열 때 읽은 `MobDetailResponse.placements.length` 를 쓴다(그 사이 바뀌어도 서버는 있는 배치를 모두 지움).
- **L-7 `MobTypeInUseException`.** 쓰는 곳이 없어져 지운다(`ApiControllerAdvice` 목록에서도). 문구 `배치된 몹은 지울 수 없습니다` 를 기대하던 기존 테스트 넷을 바꾼다(아래 '바뀌는 기존 테스트').
- **L-8 worktree compose.** 원래 디렉토리 서버와 겹치지 않게 project `mud-mk3-wt-admin-delete`, 호스트 포트 18080·13000·13001, 저장소 밖 override 파일 `../admin-delete.compose.yaml`(커밋하지 않음), 볼륨 `mud-mk3-wt-admin-delete_pgdata`(원래 `mud-mk3_pgdata` 의 사본)를 쓴다. 사본은 원래 postgres 가 꺼져 있을 때 파일 복사, 켜져 있으면 `pg_dump`/`pg_restore` 로 만든다(원래 서버를 끄지 않음). 원래 디렉토리 서버의 포트·볼륨과 겹칠 수밖에 없으면 진행하지 않고 리드에게 묻는다.
- **L-9 스모크 대상.** 몹 삭제는 새 몹 하나(그 실행 이름)로, 캐릭터 삭제는 플레이어 E 로 본다(마지막 단계라 이후 단계에 영향 없음). 기존 단계 36 의 '배치된 몹 삭제 409' 단언은 뺀다.

**리드 판정 (확정, 2026-11-20)** — 위 후보보다 앞선다.
- L-1~L-7, L-9: 후보대로 받는다.
- L-2 근거: 5차 배치 삭제와 같은 길이고 1초 안에 사라지므로 결정('살아 있는 개체는 사라짐')을 만족한다.
- L-4 근거: 결정은 '이름을 그대로 입력해야 삭제 버튼 활성' 이고, 모달이냐 화면 안 칸이냐는 결정 밖이다.
- L-8: 후보대로. project 이름은 루트 `CLAUDE.md` 규칙(`mud-mk3-wt-<작업이름>`)을 따른다.

**사전 점검 판정 (리드, 2026-11-20)** — 태스크 본문보다 앞선다. 세부 근거는 사전 점검 표에 있다.
- F-1 (T1): `MobApiWebMvcTest.delete()` 의 mock 이 `DeletedMobType` 을 돌려주게 stub 을 더한다(바뀌는 기존 테스트).
- F-2 (T1): `AdminApiIntegrationTest` 의 409 단언과 그 앞 '배치 DELETE 204' 줄을 함께 지워 배치가 있는 채로 몹 DELETE 가 204 임을 본다(바뀌는 기존 테스트).
- F-3 (T2): `forgetRemovesRecords` 는 `forget` 전에 `grantInsight` 로 상한 기록을 만든다.
- F-4 (T2): `deleteClearsMobRecords` 는 `CombatTest.killerKeptWhenTopDealerLeft` 수치(연기 13층·1층, 적안 독사)를 쓰고 delete 전에 처치자가 한백임을 단언한다.
- F-5 (T2): `deleteOfflineCharacter`·`deleteClearsCombatAndTimers` 는 지우기 전에 값이 있음을 먼저 단언한다.
- F-6 (T2): `forget` 없이 먼저 구현해 `deleteClearsMobRecords` 실패를 확인한 뒤 `forget` 을 더한다.
- F-7 (T2): `CharacterDeletionTest` 는 `@ApplicationServiceTest`, 생성자에 `MobInstanceRepository` 포함.
- F-8 (T2): `BaseGameTest`·`GameConnectorTest`(헬퍼 옮김)를 Files 에 더하고, `CharacterApiWebMvcTest` 의 `@MockitoBean` 에 `OfflineCharacterRemover` 를 넣는다(바뀌는 기존 테스트).
- F-9 (T4): 스모크에 `ratName` 상수를 더한다. 단계 53 은 E 가 지면 fail(또는 몹 경지를 낮춤). 단계 54 '목록에 없음' 은 목록을 직접 읽어 본다(`characterNamed` 는 없으면 fail).
- F-10 (T4): Step 7 이 비교할 `mob_placement` 수를 스모크 실행 전에 잰다.
- F-11 (T1·T2): `principalOf` 는 Optional 이라 `.orElseThrow()`. `serialized(Supplier)` 는 `transactionTemplate.execute` 결과를 `requireNonNull` 로 감싸고, 기존 Runnable 판은 이것에 위임해 null 을 돌려주지 않는다.
- F-12 (T5): 리뷰가 허용하는 기존 테스트 변경 목록에 F-1·F-2·F-8 을 넣는다.

## Global Constraints

모든 태스크의 요구 사항에 이 절이 포함된다. 구현자는 이 절과 자기 태스크를 먼저 읽는다.

**작업 위치 (worktree)**
- **모든 구현·테스트·커밋은 worktree `/Users/ch3224bin/dev/apps/ch3223bin/mud/mud-mk3-wt/admin-delete`(브랜치 `admin-delete`, origin/main 기준)에서 한다.** 원래 디렉토리 `/Users/ch3224bin/dev/apps/ch3223bin/mud/mud-mk3` 에는 쓰지 않고, merge·checkout·pull 도 하지 않는다(`CLAUDE.md`). 아래 경로는 worktree 기준이다.
- 반영은 리드가 worktree 에서 `git fetch && git rebase origin/main` 뒤 `git push origin HEAD:main` 으로 한다.

**범위**
- 스펙에 적힌 동작·문구만 바꾼다. 그 밖의 게임 규칙·화면 문구·서버 메시지·`(제안)` 결정은 바꾸지 않는다. 룰북(`docs/spec/game-rule/`)은 고치지 않는다. 스펙은 이미 고쳤다 — 구현이 스펙과 다르게 가야 하면 멈추고 리드에게 보고한다.
- 기존 테스트의 기대값은 태스크가 '바뀌는 기존 테스트' 로 적은 것만 바꾼다. 그 밖에 바꿔야 통과하면 멈추고 보고한다.

**버전·의존성** — 새 라이브러리·플러그인·이미지를 넣지 않는다. `build.gradle.kts`, `package.json`, `package-lock.json`, Dockerfile, `compose.yaml` 을 바꾸지 않는다.

**로컬 환경**
- macOS arm64, 기본 JDK Temurin 25. world-server 는 항상 `./gradlew`.
- 프론트 명령은 모듈의 `.nvmrc`(26.13.1)로: `cd <module> && source "$NVM_DIR/nvm.sh" && nvm use`.
- **디스크:** 태스크 시작 전 `df -h .` 를 본다. 10GB 미만이면 아무것도 지우지 말고 멈춰 보고한다. `ENOSPC` 도 같다.
- **Docker 는 Task 4 에서만** 쓴다. **`mud-mk3_pgdata`(원래 볼륨)는 어느 태스크에서도 쓰거나 지우지 않는다**(읽기 전용 복사만).

**코드 규칙**
- `.claude/rules/*.md`, 루트 패키지 `mudmk3`. 아래 world-server 경로는 `world-server/src/{main,test}/java/mudmk3/` 기준이다.
- 문구는 한국어, 스펙의 글자 그대로. 이름 조사는 기존 `Josa` 를 쓴다(아래 문구는 조사가 필요 없게 정했다).
- **게임 락:** 명령·틱·접속·끊김·방 삭제 반영·경지 알림, 그리고 이번의 캐릭터 삭제는 모두 `GameService.serialized` 를 지난다. 메모리 상태는 그 안에서만 건드린다. 관리자 트랜잭션(`@ApplicationService` 의 몹 삭제 등)에서는 게임 락을 잡지 않는다.
- 자동 테스트는 네트워크 없이 돈다. 게임 테스트의 시간은 `MutableClock` + `advanceAndTick`, 난수는 `ScriptedRandom`. 실제로 기다리는 테스트(`Thread.sleep`, Awaitility)를 쓰지 않는다.
- 로그 확인 테스트는 `@ExtendWith(OutputCaptureExtension.class)` + `CapturedOutput`(기존 `SecurityConfigTest` 관례).
- Next.js 규칙은 1~6차와 같다(정적 export, `"use client"`, 상대 경로 import, 쿼리스트링 id). 저장 전 클라이언트 검사 없이 서버 `detail` 을 보인다.
- 툴 호출 파라미터(JSON)의 한글은 리터럴 UTF-8로 쓰고 `\uXXXX` 이스케이프를 쓰지 않는다.

**문구 (스펙 그대로)**
- 접속 중 409: `접속 중인 캐릭터는 지울 수 없습니다. 접속을 끊은 뒤 다시 시도하십시오: {이름}`
- 없는 캐릭터 400: `캐릭터를 찾을 수 없습니다. id: {id}`(기존 `CharacterFinder.find`), 없는 몹 400: `몹을 찾을 수 없습니다. id: {id}`(기존)
- 몹 로그: `관리자 삭제 — 몹 id: {id}, 이름: {이름}, 함께 지운 배치: {n}, 관리자 계정 id: {id}, 이메일: {email}`
- 캐릭터 로그: `관리자 삭제 — 캐릭터 id: {id}, 이름: {이름}, 계정 id: {accountId}, 관리자 계정 id: {id}, 이메일: {email}`
- 몹 확인(배치 0): `'{이름}' 몹을 지울까요?` / (배치 n>0): `'{이름}' 몹을 지울까요? 이 몹의 배치 {n}곳도 함께 지워지고, 세계에 나와 있는 {이름}은 사라집니다.` — **'{이름}은'** 에서 조사는 받침에 맞춘다(admin-console 에 조사 도우미가 없으면 `은(는)` 대신 `lib` 에 작은 `withEunNeun(word)` 를 더한다: 마지막 글자가 한글이 아니면(숫자 등) 받침 없음으로 보고 `는`).
- 캐릭터 절: 제목 `캐릭터 삭제`, 안내 `캐릭터를 지우면 소지품·익힌 공법·깨달음이 모두 사라지고 되돌릴 수 없습니다. 계정은 남아 다음 접속 때 새 캐릭터를 만듭니다.`, 버튼 `캐릭터 삭제`, 확인 칸 문구 `지우려면 캐릭터 이름을 그대로 입력하십시오: {이름}`, 입력 라벨 `캐릭터 이름`, 버튼 `삭제`·`취소`.

**저장소·커밋**
- 태스크 하나 = 커밋 하나 이상. 구현자는 커밋까지 하고 push 하지 않는다. 구현자는 서브에이전트를 띄우지 않는다.
- 커밋 메시지: `<type>(<module>): <한국어 요약>`(module: world-server, admin-console, infra, spec, impl). 끝에 빈 줄 다음 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- 태스크를 끝내면 `docs/impl/admin-delete/progress.md`(Task 1 이 debt-2 progress 꼴로 만든다: 상태·계획 링크·`## 끝난 태스크` 표 `| 태스크 | 커밋 | 메모 |`) 표에 한 줄을 더해 같은 커밋에 넣는다. 커밋 해시는 리드가 채운다.

**검증 명령 (각 태스크 완료 조건, 고친 모듈만)**
- world-server: `cd world-server && ./gradlew build`(테스트 + SpotBugs + ArchUnit)
- admin-console: `cd admin-console && source "$NVM_DIR/nvm.sh" && nvm use && npm ci && npm test && npx tsc --noEmit && npm run build`
- 스모크 스크립트를 고친 태스크: `node --check scripts/smoke-test.mjs`

## Review Focus

테스트가 직접 겨누기 어려워 가장 깨지기 쉬운 것. 각 줄의 테스트는 담당 태스크에 있다.

1. **끊긴 뒤 남은 피해 기록** — 지운 캐릭터가 끊기기 전 몹에 피해를 가장 많이 줬으면, 정리하지 않을 때 그 몹이 쓰러지는 틱이 없는 캐릭터를 처치자로 찾다 실패하고 틱마다 되풀이된다. (Task 2 `deleteClearsMobRecords`, `MobInstanceTest.forgetRemovesRecords`)
2. **판정과 지우기 사이의 접속** — 접속 판정과 DB 삭제가 같은 게임 락·트랜잭션 안이어야 한다. 영근을 고르는 중인 연결도 접속 중이다. (Task 2 `deleteWhileChoosingRootFail`, `deleteOnlineFail`; 락 자리는 Task 5 리뷰)
3. **FK 순서** — `character_technique` 는 `player_character` 에 FK 가 있다. H2 테스트에서 통과해도 PostgreSQL 에서 순서가 틀리면 깨진다. (Task 2 `deleteOfflineCharacter` + Task 4 스모크(PostgreSQL) 단계 54)
4. **교전 중인 몹 삭제** — 교전 중인 플레이어가 다음 틱 뒤 교전에서 빠져 이동할 수 있어야 하고 틱이 실패하지 않는다. (Task 1 `mobTypeDeletedRemovesLivingMob`, Task 4 스모크 단계 53)
5. **지운 뒤 같은 계정 접속** — 계정이 남아 이름 prompt 부터 다시 만들고, 지운 이름을 다시 쓸 수 있다. (Task 2 `reconnectAfterDeleteCreatesNew`, Task 4 단계 54)

---

## 파일 구조 (요약)

```
world-server/src/main/java/mudmk3/
  application/mob/provided/{MobTypeEditor.java, DeletedMobType.java (새)}, MobTypeInUseException.java (지움)
  application/mob/MobTypeModifyService.java, required/MobPlacementRepository.java          (T1)
  adapter/webapi/{MobApi.java, ApiControllerAdvice.java}                                    (T1·T2)
  domain/combat/MobInstance.java                                                            (T2)
  application/character/provided/{CharacterRemover, DeletedCharacter, CharacterDeletedEvent}.java (새)
  application/character/{CharacterModifyService.java, required/CharacterRepository.java}
  application/inventory/{InventoryModifyService.java, provided/InventoryEditor.java, required/InventoryRepository.java}
  application/cultivation/{CultivationModifyService.java, required/CharacterTechniqueRepository.java, required/CharacterInsightRepository.java}
  application/game/provided/{OfflineCharacterRemover, CharacterOnlineException}.java (새)
  application/game/{GameService.java, CombatEngine.java}
  adapter/webapi/CharacterApi.java                                                          (T2)
admin-console/src/lib/api.ts, src/lib/<조사 도우미>, src/components/{MobForm, CharacterDeleteForm (새)}.tsx,
  app/character/page.tsx                                                                    (T3)
scripts/smoke-test.mjs, docs/impl/admin-delete/progress.md                                  (T4)
```

---

### Task 1: 몹 삭제 — 배치와 함께 지우고 로그 (world-server)

**권장 모델:** sonnet

**스펙:** admin-tool.md `### 몹 삭제`·`### 삭제 기록`·`### 관리 툴 삭제 REST API`.

**Files:**
- Create: `application/mob/provided/DeletedMobType.java`, `docs/impl/admin-delete/progress.md`
- Modify: `application/mob/provided/MobTypeEditor.java`(javadoc '배치된 몹은 409' → '배치도 함께 지운다'), `application/mob/MobTypeModifyService.java`(`delete`, `checkDeletable` 지움), `application/mob/required/MobPlacementRepository.java`, `adapter/webapi/MobApi.java`(`@Slf4j`), `adapter/webapi/ApiControllerAdvice.java`(목록에서 `MobTypeInUseException` 뺌)
- Delete: `application/mob/provided/MobTypeInUseException.java`
- Test: `application/mob/provided/MobTypeEditorTest.java`, `adapter/webapi/MobApiWebMvcTest.java`, `adapter/webapi/AdminApiIntegrationTest.java`, `application/game/provided/CombatSpawnTest.java`

**Interfaces:**
- Produces: `public record DeletedMobType(Long id, String name, int placementCount)`; `MobTypeEditor.delete(Long mobTypeId): DeletedMobType`(없으면 기존 400); `MobPlacementRepository.deleteByMobTypeId(Long mobTypeId): long`(Spring Data 파생 삭제, 지운 수). 순서: `find` → `deleteByMobTypeId` → `mobTypeRepository.delete`(드롭 표는 값 컬렉션이라 함께). 배치를 먼저 지워야 `mob_placement.mob_type_id` FK 에 걸리지 않는다(Hibernate 는 remove 를 부른 순서로 지운다).
- `MobApi.delete(@PathVariable Long mobId, Authentication authentication)` 은 204 뒤(포트가 돌아온 뒤 = 커밋 뒤) 로그 `관리자 삭제 — 몹 id: …, 이름: …, 함께 지운 배치: …, 관리자 계정 id: …, 이메일: …`. 관리자는 `SessionAuthenticator.principalOf(authentication)`(기존, `AccountPrincipal`)로 읽는다.

**바뀌는 기존 테스트:** `MobTypeEditorTest.deletePlacedFail` → `deletePlaced` 로 바꿈, `MobApiWebMvcTest.deletePlacedFail` → `deleteLogs` 로 바꿈, `AdminApiIntegrationTest` 의 몹 삭제 409 단언(451~453행) → 아래대로.

- [ ] **Step 1: 실패하는 테스트**
  - `MobTypeEditorTest.deletePlaced`: 방 `샘터` 에 청모랑 배치 둘(`count` 2·1) → `flushAndClear` → `mobTypeEditor.delete(wolf.id())` 가 `new DeletedMobType(wolf.id(), "청모랑", 2)` → `flushAndClear` → `mobTypeFinder.find` 는 400 `몹을 찾을 수 없습니다. id: …`, `mobPlacementFinder`(방의 배치)는 비어 있음. 기존 `delete`(배치 없음)는 반환값 `placementCount` 0 단언을 더한다.
  - `MobApiWebMvcTest.deleteLogs(CapturedOutput output)`: `when(mobTypeEditor.delete(1L)).thenReturn(new DeletedMobType(1L, "청모랑", 2))` → `DELETE /api/admin/mobs/1` 204 → `output` 에 `관리자 삭제 — 몹 id: 1, 이름: 청모랑, 함께 지운 배치: 2, 관리자 계정 id: 1, 이메일: admin@example.com`(`SecurityFixture.adminPrincipal()` 값).
  - `AdminApiIntegrationTest`: 드롭 표 물건 삭제 409 단언을 몹 삭제 **앞으로** 옮기고, 몹 삭제는 204 → 그 방 상세 `$.mobs` 가 빈 배열 → `GET /api/admin/mobs/{id}` 400. 그 뒤 배치 수정 단언(`PUT /api/admin/mob-placements/…`)이 이 몹을 쓰면 그 부분은 몹 삭제 앞에 둔다(순서만 바꿈, 기대값 그대로).
  - `CombatSpawnTest.mobTypeDeletedRemovesLivingMob`: 청모랑을 둔 방에서 한백이 `공격 청모랑`(교전 중) → `sender.clear()` → `mobTypeEditor.delete(청모랑 id)` → `advanceAndTick(1)` → 한백 `quietRoomsTo` 하나이고 그 `mobs()` 에 청모랑 없음, 교전 알림·error 없음(`messagesTo` 비어 있음), 다음 이동 명령이 `싸우는 중에는 할 수 없습니다.` 가 아니라 이동한다. 이어 `advanceAndTick(1)` 이 예외 없이 지나간다.
- [ ] **Step 2: 실패 확인** — `./gradlew test --tests '*MobTypeEditorTest' --tests '*MobApiWebMvcTest' --tests '*AdminApiIntegrationTest' --tests '*CombatSpawnTest'` → 새·바뀐 테스트 FAIL(컴파일 실패 포함).
- [ ] **Step 3: 구현** — Interfaces 대로. `MobTypeInUseException` 을 지우고 `grep -rn MobTypeInUseException world-server` 가 비었는지 본다.
- [ ] **Step 4: 통과 확인** — `./gradlew build`.
- [ ] **Step 5: 커밋** — `feat(world-server): 배치된 몹도 배치와 함께 지우고 삭제를 로그에 남김`(progress.md 생성·한 줄 포함).

**완료 조건:** 위 테스트 통과, `./gradlew build` 통과.
**progress.md 한 줄:** 지우는 순서, 바뀐 기존 테스트, 교전 중 몹이 다음 틱에 사라짐 확인.

---

### Task 2: 캐릭터 삭제 — 접속 거절, DB·메모리 정리, 로그 (world-server)

**권장 모델:** opus

**스펙:** admin-tool.md `### 캐릭터 삭제`·`### 삭제 기록`·REST API 표, platform.md `### 관리 툴 삭제 월드 상태`(메모리 표·순서).

**Files:**
- Create: `application/character/provided/{CharacterRemover,DeletedCharacter,CharacterDeletedEvent}.java`, `application/game/provided/{OfflineCharacterRemover,CharacterOnlineException}.java`
- Modify: `domain/combat/MobInstance.java`, `application/character/CharacterModifyService.java`, `application/character/required/CharacterRepository.java`, `application/inventory/{InventoryModifyService,provided/InventoryEditor,required/InventoryRepository}.java`, `application/cultivation/{CultivationModifyService,required/CharacterTechniqueRepository,required/CharacterInsightRepository}.java`, `application/game/{GameService,CombatEngine}.java`, `adapter/webapi/{CharacterApi,ApiControllerAdvice}.java`
- Test: `domain/combat/MobInstanceTest.java`, `application/character/provided/CharacterRemoverTest.java`(새), `application/game/provided/CharacterDeletionTest.java`(새, `BaseGameTest` 상속), `adapter/webapi/CharacterApiWebMvcTest.java`

**Interfaces:**
- domain: `MobInstance.forget(Long characterId, Instant now): void` — 교전 상대이면 `disengage(characterId, now)` 를 먼저 하고(빈 교전이 되면 기존대로 다시 차기 예약), `released`·`insightRemaining`·`insightCapNotified` 에서 그 id 를 지운다. 이미 쓰러진 몹의 `killerCharacterId` 는 그대로(시체는 이미 남음).
- character 슬라이스(게임 락 모름, `@ApplicationService` 트랜잭션에 참여):
  - `public record DeletedCharacter(Long id, Long accountId, String name)`
  - `public record CharacterDeletedEvent(Long characterId)` — javadoc: '캐릭터 행을 지우기 **전에**, 지우는 트랜잭션 안에서 동기로 알린다(FK 때문에)'.
  - `CharacterRemover.delete(Long characterId): DeletedCharacter` — `characterFinder.find`(없으면 400) → `publishEvent(new CharacterDeletedEvent(id))` → `inventoryEditor.deleteOf(id)` → `characterRepository.delete(character)`(영근 값 컬렉션 함께). `CharacterModifyService` 가 구현.
  - `CharacterRepository.delete(PlayerCharacter character): void`
- inventory: `InventoryEditor.deleteOf(Long characterId): void`(없으면 아무것도 안 함), `InventoryRepository.delete(Inventory inventory): void`(항목은 cascade·orphanRemoval).
- cultivation: `CultivationModifyService.onCharacterDeleted(CharacterDeletedEvent event)` `@EventListener` — `characterTechniqueRepository.deleteAllByCharacterId(id)`, `characterInsightRepository.deleteAllByCharacterId(id)`(둘 다 Spring Data 파생 삭제 `void`; 엔티티를 읽어 remove 하므로 영속성 컨텍스트가 맞는다. 벌크 JPQL 은 쓰지 않는다). 의존 방향 `cultivation → character` 는 기존과 같다(`character → cultivation` 을 만들지 않으려고 이벤트를 쓴다).
- game 슬라이스:
  - `OfflineCharacterRemover.delete(Long characterId): DeletedCharacter` — javadoc '접속하지 않은 캐릭터를 게임 락 안에서 DB와 메모리에서 지운다'.
  - `public class CharacterOnlineException extends RuntimeException`(provided) → `ApiControllerAdvice.handleConflict` 목록(409).
  - `GameService implements OfflineCharacterRemover`: `@Transactional(propagation = Propagation.SUPPORTS)`, 값을 돌려주는 `private <T> T serialized(Supplier<T> work)` 를 더하고 기존 `serialized(Runnable)` 은 그것에 맡긴다. 안: `characterFinder.find(id)` → `sessions` 에 그 캐릭터의 `accountId` 를 가진 `GameSession` 이 있거나 `presenceRepository.findByCharacterId(id)` 가 있으면 `CharacterOnlineException("접속 중인 캐릭터는 지울 수 없습니다. 접속을 끊은 뒤 다시 시도하십시오: " + 이름)` → `characterRemover.delete(id)` → `combatEngine.forget(id, clock.instant())` → 반환. 예외는 트랜잭션을 되돌리고 그대로 나간다.
  - `CombatEngine.forget(Long characterId, Instant now)`(package-private): `playerCombatRepository.delete(id)`, `abilityTimersRepository.delete(id)`, 모든 몹 `forget` 뒤 `save`.
- adapter: `CharacterApi.delete(@PathVariable Long characterId, Authentication authentication)` `@DeleteMapping("/{characterId}")` 204, 로그 `관리자 삭제 — 캐릭터 id: …, 이름: …, 계정 id: …, 관리자 계정 id: …, 이메일: …`.

- [ ] **Step 1: 실패하는 테스트 (domain)** — `MobInstanceTest`:
  - `forgetRemovesRecords`: 몹이 A·B 와 교전, A 가 B 보다 큰 피해 → A `disengage` → `forget(A)` → `killer()` 가 B, `engagementOf(A)` 비어 있음, A 의 `grantInsight` 는 상한이 새로 시작(처음 기혈 최대치까지 줌).
  - `forgetEngagedOpponent`: A 만 교전 중 → `forget(A, now)` → `opponentIds()` 비어 있음, `isFighting()` false, `now + 10초` 에 `refillIfDue` 로 기혈이 다 참.
- [ ] **Step 2: 실패하는 테스트 (character)** — `CharacterRemoverTest`(`@ApplicationServiceTest`, 시작 소지품이 있는 세계):
  - `delete`: 캐릭터 생성 → `delete` 가 `new DeletedCharacter(id, accountId, "한백")` → flush·clear → `characterFinder.findByAccountId(accountId)` 비어 있음, `inventoryFinder` 로 그 캐릭터 소지품 없음(없을 때의 기존 동작을 단언), 계정은 `accountFinder.find(accountId)` 로 남음, 같은 계정으로 이름 `한백` 다시 `create` 성공.
  - `deleteFail`: 없는 id 9 → `IllegalArgumentException` `캐릭터를 찾을 수 없습니다. id: 9`.
- [ ] **Step 3: 실패하는 테스트 (game)** — `CharacterDeletionTest extends BaseGameTest`(`OfflineCharacterRemover`, `PlayerCombatRepository`, `AbilityTimersRepository` 를 생성자로 받음):
  - `deleteOfflineCharacter`: 한백 생성·소지품 `give`·공법 `learn`(깨달음이 쌓이게 신통 한 번 쓰기, 또는 `InsightEditor` 로 직접 더하기)·접속 끊음 → `delete` → flush·clear 뒤 native 수 세기 `character_technique`·`character_insight`·`inventory`·`inventory_entry`(그 inventory)·`character_spirit_root`·`player_character` 가 그 id 로 모두 0, `account` 는 1.
  - `deleteOnlineFail`: 한백이 세계에 있음 → `CharacterOnlineException` 문구 `접속 중인 캐릭터는 지울 수 없습니다. 접속을 끊은 뒤 다시 시도하십시오: 한백` → 캐릭터 그대로, 한백의 다음 `보기` 가 정상.
  - `deleteWhileChoosingRootFail`: 4차 전 캐릭터(영근 없음, `GameConnectorTest.prepareLegacyCharacter` 를 `BaseGameTest` 로 올려 씀)로 접속해 영근 prompt 상태 → 같은 거절.
  - `reconnectAfterDeleteCreatesNew`: 지운 뒤 같은 계정 `connect` → `SessionMessage("creating", null)`·`PromptMessage("이름을 정하십시오. (한글 2~6자)")`, 이어 이름 `한백` 으로 다시 만들 수 있음.
  - `deleteClearsMobRecords`: 한백(연기 7층)·한립(연기 1층)이 청모랑과 교전, 한백이 한 번 친 뒤(피해 1위) 접속 끊음 → `delete(한백)` → 한립이 청모랑을 쓰러뜨릴 때까지 `advanceAndTick` → 예외 없음, 한립에게 `청모랑을 쓰러뜨렸습니다.`(처치자 한립). 고치기 전에는 틱이 `캐릭터를 찾을 수 없습니다` 로 실패해야 한다(실패 확인).
  - `deleteClearsCombatAndTimers`: 한백이 신통을 써 재사용 대기 중에 끊음 → `delete` → `playerCombatRepository.find(id)`·`abilityTimersRepository.find(id)` 비어 있음.
- [ ] **Step 4: 실패하는 테스트 (adapter)** — `CharacterApiWebMvcTest`: `delete(CapturedOutput)`(`when(offlineCharacterRemover.delete(3L)).thenReturn(new DeletedCharacter(3L, 7L, "한백"))` → 204, 로그 `관리자 삭제 — 캐릭터 id: 3, 이름: 한백, 계정 id: 7, 관리자 계정 id: 1, 이메일: admin@example.com`), `deleteOnlineFail`(예외 → 409 detail 그대로, 로그 없음), `deleteMissingFail`(`IllegalArgumentException` → 400), 플레이어 403.
- [ ] **Step 5: 실패 확인** — `./gradlew test --tests '*MobInstanceTest' --tests '*CharacterRemoverTest' --tests '*CharacterDeletionTest' --tests '*CharacterApiWebMvcTest'` → FAIL.
- [ ] **Step 6: 구현** — Interfaces 대로. `CharacterRemover` 와 `OfflineCharacterRemover` 둘 다 provided 포트지만 어댑터는 `OfflineCharacterRemover` 만 쓴다(직접 `CharacterRemover` 를 부르면 접속 판정을 건너뛴다 — `CharacterRemover` javadoc 에 '게임 락 밖에서 부르지 않는다, `OfflineCharacterRemover` 를 쓴다' 를 적는다).
- [ ] **Step 7: 통과 확인** — `./gradlew build`(ArchUnit 순환 검사 포함).
- [ ] **Step 8: 커밋** — `feat(world-server): 접속하지 않은 캐릭터를 딸린 데이터·메모리와 함께 지우고 로그에 남김`.

**완료 조건:** 위 테스트 통과, 기존 테스트 기대값 무변경(`prepareLegacyCharacter` 를 옮긴 것만), `./gradlew build` 통과.
**progress.md 한 줄:** 지우는 표와 순서, 접속 판정 조건, 메모리 정리 목록, 고치기 전 `deleteClearsMobRecords` 실패 모양.

---

### Task 3: admin-console — 몹 확인 문구, 캐릭터 삭제 칸

**권장 모델:** sonnet

**스펙:** admin-tool.md `### 관리 툴 삭제 화면`. 문구는 Global Constraints '문구'.

**Files (admin-console 기준):**
- Modify: `src/lib/api.ts`(`deleteCharacter`), `src/components/MobForm.tsx`(`handleDelete` 문구), `app/character/page.tsx`(상세 아래 `CharacterDeleteForm`, `useRouter` 로 `/characters/`)
- Create: `src/components/CharacterDeleteForm.tsx`, 조사 도우미가 없으면 `src/lib/josa.ts`(`withEunNeun(word: string): string`)
- Test: `src/components/MobForm.test.tsx`, `src/components/CharacterDeleteForm.test.tsx`(새), 도우미를 만들면 `src/lib/josa.test.ts`(새)

**Interfaces:**
- `deleteCharacter(characterId: number): Promise<void>` — `apiFetch<void>(\`/api/admin/characters/${characterId}\`, { method: "DELETE" })`(다른 delete 와 같은 꼴).
- `CharacterDeleteForm({ character, onDeleted }: { character: { id: number; name: string }; onDeleted: () => void })` — `<section>` 안 `<h2>캐릭터 삭제</h2>`, 안내 문단, `캐릭터 삭제` 버튼. 누르면 확인 칸(버튼은 숨김): 문구 줄, `<label>캐릭터 이름 <input/></label>`, `삭제`(`disabled={typed !== character.name || submitting}`), `취소`. 성공 → `onDeleted()`. 실패 → `<p role="alert" className="error">{errorMessage(e)}</p>`, 칸은 연 채.
- `withEunNeun("청모랑")` → `"청모랑은"`, `withEunNeun("들토끼")` → `"들토끼는"`, `withEunNeun("들토끼123")` → `"들토끼123는"`.

**바뀌는 기존 테스트:** `MobForm.test` "삭제 는 확인을 묻고, 지우면 몹 목록으로 간다" 의 기대 문구(픽스처 `wolf` 는 배치 둘) → `'청모랑' 몹을 지울까요? 이 몹의 배치 2곳도 함께 지워지고, 세계에 나와 있는 청모랑은 사라집니다.`, "…서버가 409 를 주면 detail 을 보인다" → 서버 오류를 `new api.ApiError(400, "몹을 찾을 수 없습니다. id: 2")` 로, 이름도 "…서버 오류면 detail 을 보인다" 로.

- [ ] **Step 1: 실패하는 테스트**
  - `MobForm.test` "배치가 없으면 5차 문구로 묻는다": `{...wolf, placements: []}` → `confirm` 이 `'청모랑' 몹을 지울까요?`. 위 '바뀌는 기존 테스트' 둘.
  - `CharacterDeleteForm.test`(`api.deleteCharacter` mock, `character={{id: 3, name: "한백"}}`):
    - "캐릭터 삭제 를 누르면 확인 칸이 열리고, 이름이 글자 그대로 같을 때만 삭제 가 켜진다": 열기 전에는 `캐릭터 이름` 입력 없음 → 누름 → `지우려면 캐릭터 이름을 그대로 입력하십시오: 한백` → `삭제` disabled → `한` disabled → `한백` enabled → `한백 ` disabled.
    - "삭제 하면 그 id 로 지우고 onDeleted 를 부른다": `deleteCharacter` 가 `3` 으로, `onDeleted` 한 번.
    - "서버가 409 를 주면 칸을 연 채 detail 을 보인다": `ApiError(409, "접속 중인 캐릭터는 지울 수 없습니다. 접속을 끊은 뒤 다시 시도하십시오: 한백")` → `role="alert"` 에 그 문구, 입력 칸 그대로, `onDeleted` 안 불림.
    - "취소 하면 칸을 닫고, 다시 열면 입력이 비어 있다".
  - 도우미를 만들면 `josa.test` 셋(위 Interfaces 값).
- [ ] **Step 2: 실패 확인** — `npm test -- MobForm CharacterDeleteForm josa` → FAIL.
- [ ] **Step 3: 구현** — Interfaces 대로. `app/character/page.tsx` 는 `<CharacterDetail …/>` 바로 뒤에 `<CharacterDeleteForm character={character} onDeleted={() => router.push("/characters/")} />`.
- [ ] **Step 4: 검증** — admin-console 검증 명령.
- [ ] **Step 5: 커밋** — `feat(admin-console): 몹 삭제 확인에 배치 수, 캐릭터 화면에 이름 입력 삭제`.

**완료 조건:** 테스트 통과, 검증 명령 통과.
**progress.md 한 줄:** 확인 문구, 확인 칸 동작, 조사 도우미 여부, 바뀐 기존 테스트.

---

### Task 4: 통합 확인 — 전체 테스트, worktree compose 한 번, 기존 데이터 사본으로 스모크 전체

**권장 모델:** opus

**목적:** 기존 데이터(원래 pgdata 의 사본)에서 서버가 뜨고 1~6차·debt-2 스모크가 그대로이며 새 삭제가 PostgreSQL 에서 동작하는지 본다. **이 계획에서 Docker 를 쓰는 유일한 태스크다.**

**Files:** Modify `scripts/smoke-test.mjs`, `README.md`(`### 스모크 테스트` 목록에 관리 툴 삭제 한 줄), `docs/impl/admin-delete/progress.md`. 저장소 밖: `/Users/ch3224bin/dev/apps/ch3223bin/mud/mud-mk3-wt/admin-delete.compose.yaml`(커밋 안 함).

**스모크 고치기 (기존 단계 기대값은 그대로):**
- 머리 주석에 `// 관리 툴 삭제: 단계 36 의 배치된 몹 삭제 409 는 없어짐. 단계 53 몹 삭제, 54 캐릭터 삭제.` 를 더한다.
- 단계 36: `DELETE /api/admin/mobs/${rabbit.id}` 409 줄을 뺀다(배치된 몹도 지워지므로 — 지우면 뒤 단계가 깨짐).
- 새 단계 53 `관리자 — 몹 ${ratName}(강적 연기 5층, 실행 번호) 을 광장에 두고 E 가 공격 → 몹 삭제 204 → 광장에서 사라지고 E 교전이 끝남`: 몹을 만들고 광장 배치(**그 배치 id 는 `mobPlacementIds` 에 넣지 않는다** — 몹 삭제가 지우므로 끝 정리에서 400 이 남) → `lookUntil` 로 나타남 → `공격 ${ratName}` 의 교전 시작 줄 → `DELETE /api/admin/mobs/{id}` 204 → E 가 `isQuietRoom` 으로 `mobs` 에 ${ratName} 없는 quiet room 을 받음(≤ 5초) → 광장 상세 `mobs` 에 없음 → `GET /api/admin/mobs/{id}` 400 `몹을 찾을 수 없습니다. id: …` → E `동` 이 막히지 않고 사냥터 room, `서` 로 돌아옴. E 가 지우기 전에 쓰러뜨리지 않게 기혈이 큰 체급(강적 이상)으로 둔다.
- 새 단계 54 `관리자 — 접속 중 E 삭제 409 → E 종료 → 삭제 204 → 상세 400·목록에 없음 → E 계정 다시 접속하면 이름 prompt`: `DELETE /api/admin/characters/${eId}` 409 detail `접속 중인 캐릭터는 지울 수 없습니다. 접속을 끊은 뒤 다시 시도하십시오: ${nameE}` → E `종료` 와 1000 닫힘 → 204 → `GET /api/admin/characters/${eId}` 400 `캐릭터를 찾을 수 없습니다. id: ${eId}` → 목록에 ${nameE} 없음 → 같은 쿠키로 새 연결 → `session` `creating` 과 `이름을 정하십시오. (한글 2~6자)` prompt → 닫음. `eId` 는 단계 32 처럼 목록에서 찾는다.
- 둘 다 단계 52 뒤, `SMOKE OK` 출력 앞.

- [ ] **Step 1: 스크립트 수정** — `node --check scripts/smoke-test.mjs`.
- [ ] **Step 2: 모듈 검증** — world-server `./gradlew cleanTest build`, admin-console 검증 명령, play-client 는 바뀐 것이 없지만 `npm test` 한 번(회귀). 테스트 수를 메모에.
- [ ] **Step 3: 겹침 확인과 디스크 전** — `df -h .`·`docker system df` 를 메모에(10GB 미만이면 멈춤). `docker ps --format '{{.Names}} {{.Ports}}'` 와 `lsof -nP -iTCP:18080 -iTCP:13000 -iTCP:13001 -sTCP:LISTEN` 으로 포트가 비었는지 본다(차 있으면 멈추고 리드에게 묻는다). 원래 project(`mud-mk3`)는 건드리지 않는다.
- [ ] **Step 4: 환경 준비** — 원래 디렉토리의 `.env` 를 worktree 루트로 복사한다(`.gitignore` 에 있음, 커밋 안 함 — 사본 pgdata 의 비밀번호가 같아야 한다). override 파일 `../admin-delete.compose.yaml`:

```yaml
services:
  world-server:
    ports: !override
      - "18080:8080"
    environment:
      MUD_PLAY_CLIENT_URL: http://localhost:13000/
      MUD_ADMIN_CONSOLE_URL: http://localhost:13001/
      # 스모크는 Origin 으로 localhost:3000/3001 을 보낸다(스크립트 상수)
      MUD_CORS_ALLOWED_ORIGINS: http://localhost:3000,http://localhost:3001,http://localhost:13000,http://localhost:13001
  play-client:
    build:
      args:
        NEXT_PUBLIC_WORLD_SERVER_URL: http://localhost:18080
    ports: !override
      - "13000:80"
  admin-console:
    build:
      args:
        NEXT_PUBLIC_WORLD_SERVER_URL: http://localhost:18080
    ports: !override
      - "13001:80"
```

  이하 `DC="docker compose -p mud-mk3-wt-admin-delete -f compose.yaml -f ../admin-delete.compose.yaml"`(worktree 루트에서). `$DC config` 로 포트가 18080·13000·13001 만인지, 볼륨이 `mud-mk3-wt-admin-delete_pgdata` 인지 확인한다.
- [ ] **Step 5: pgdata 사본** — compose 의 볼륨은 `pgdata:/var/lib/postgresql`(PostgreSQL 18 이미지는 그 아래 `18/docker` 에 데이터)이고 project 이름이 앞에 붙으므로 사본 이름은 `mud-mk3-wt-admin-delete_pgdata` 다.
  - 원래 postgres 가 꺼져 있으면(`docker ps` 에 `mud-mk3-postgres-1` 없음): `docker volume create mud-mk3-wt-admin-delete_pgdata` → `docker run --rm -v mud-mk3_pgdata:/from:ro -v mud-mk3-wt-admin-delete_pgdata:/to postgres:18.6-alpine sh -c 'cp -a /from/. /to/'`(이미 있는 이미지, 원래 볼륨은 읽기 전용).
  - 켜져 있으면 파일 복사를 하지 않는다(쓰는 중인 데이터 디렉토리는 깨질 수 있음): `$DC up -d postgres`(빈 사본) → `docker exec mud-mk3-postgres-1 pg_dump -U mud -Fc mud > "$TMPDIR/admin-delete.dump"` → `$DC exec -T postgres pg_restore -U mud -d mud --no-owner < "$TMPDIR/admin-delete.dump"` → 덤프 파일 삭제.
  - 어느 쪽이었는지 메모에. compose 가 '외부에서 만든 볼륨' 경고를 내면 그대로 둔다.
- [ ] **Step 6: compose 한 번** — `MUD_DEV_LOGIN_ENABLED=true $DC build` **한 번** → `SPRING_PROFILES_ACTIVE=local MUD_DEV_LOGIN_ENABLED=true MUD_ADMIN_EMAILS=<.env 의 기존 값>,smoke-admin@example.com $DC up -d` → `$DC logs world-server` 에 ERROR/Exception 없음 → `source "$NVM_DIR/nvm.sh" && nvm use 26.13.1 && node scripts/smoke-test.mjs http://localhost:18080` → **단계 1~54 `SMOKE OK`**. 실패하면 원인을 고치고(해당 모듈 테스트 통과, 따로 커밋) 바뀐 서비스만 `$DC up -d --build <서비스>`.
- [ ] **Step 7: 로그·되돌림 확인** — `$DC logs world-server | grep '관리자 삭제'` 가 몹 한 줄·캐릭터 한 줄(스펙 형식, 관리자 이메일 `smoke-admin@example.com`). 관리 API로 시작 방·시작 소지품이 스모크 끝의 값, `$DC exec postgres psql -U mud -d mud -c 'select count(*) from mob_placement'` 가 스모크 시작 전과 같음. 원래 볼륨 `mud-mk3_pgdata` 를 쓰지 않았는지(`docker volume inspect` 의 이 실행 시각 이후 변경 없음 — 최소한 마운트한 컨테이너가 사본 쪽인지 `$DC ps`·`docker inspect` 로) 메모에.
- [ ] **Step 8: 정리** — `$DC down -v`(**사본 볼륨만** 지움 — 명령 전에 `$DC config --volumes` 가 `pgdata` 하나이고 실제 이름이 `mud-mk3-wt-admin-delete_pgdata` 인지 다시 본다) → `docker image rm mud-mk3-wt-admin-delete-world-server mud-mk3-wt-admin-delete-play-client mud-mk3-wt-admin-delete-admin-console` → `docker image prune -f` → `docker builder prune -f` → worktree 의 `.env`·`../admin-delete.compose.yaml`·빌드 산출물(`world-server/build`, `admin-console/{out,.next}`, `play-client/{out,.next}` — `git check-ignore` 로 먼저 확인) 삭제. `mud-mk3-*` 원래 이미지·`mud-mk3_pgdata`·`node_modules` 는 지우지 않는다. 정리 뒤 `df -h .`·`docker system df`·`docker volume ls | grep mud` 를 메모에.
- [ ] **Step 9: 커밋** — `test(infra): 관리 툴 삭제 통합 확인과 스모크 몹·캐릭터 삭제 단계`(스모크·README·progress).

**완료 조건:** 모듈 검증 통과, 원래 pgdata 사본으로 스모크 1~54 `SMOKE OK`, 삭제 로그 두 줄, 사본 볼륨·worktree 이미지·override 파일 정리, 원래 볼륨·원래 디렉토리 무변경(`git -C <원래> status --short` 비어 있음).
**progress.md 한 줄:** 사본 방법(복사/덤프), 더한 단계, 테스트 수, compose 빌드 한 번, 로그 줄, 디스크 전후, 정리 결과.

---

### Task 5: 최종 전체 리뷰

**권장 모델:** opus

- [ ] **Step 1: 리뷰** — `superpowers:requesting-code-review` 로 `origin/main..HEAD`. 볼 것: Review Focus 다섯 줄이 테스트로 덮였는지, 접속 판정과 삭제가 한 `serialized` 호출 안인지(판정 뒤 락을 놓지 않는지), 관리자 트랜잭션(몹 삭제)이 게임 락을 잡지 않는지, `CharacterRemover` 를 `OfflineCharacterRemover` 밖에서 부르는 곳이 없는지, 슬라이스 순환이 없는지, 기존 테스트 기대값이 바뀐 곳이 Task 1·3 의 '바뀌는 기존 테스트' 뿐인지(`git diff origin/main -- '*Test.java' '*.test.ts*'`), 로그 문구·스펙 `(제안)`·출처 표기가 구현과 맞는지.
- [ ] **Step 2: Critical·Important 고침** — 해당 모듈 검증 통과 후 커밋 `fix(<module>): …`. Docker 는 다시 쓰지 않는다(다시 스모크가 필요하면 리드에게 묻는다).
- [ ] **Step 3: progress.md** — 리뷰 결과, 고친 것, 창호 확인 항목((제안) 목록, 관리자 연결 끊기 기능 없음), 상태 "관리 툴 삭제 완료", '원래 디렉토리는 git pull 필요'.
- [ ] **Step 4: 커밋** — `docs(impl): 관리 툴 삭제 완료 기록`.

**완료 조건:** Critical·Important 0, progress.md 완료 기록. worktree·브랜치 삭제는 리드가 push 뒤 `CLAUDE.md` 의 '끝' 대로 한다.

---

## 태스크 의존 순서

```
1 → 2 → 3 → 4 → 5
```

- 1·2 는 둘 다 `ApiControllerAdvice` 를 고치므로 순서대로 한다. 2 는 1 의 `CombatSpawnTest` 와 겹치지 않는다(새 테스트 클래스).
- 3 은 서버 API 모양(1·2)에 기대지만 mock 으로 테스트하므로 1·2 뒤 아무 때나 되나, 리드가 하나씩 리뷰하므로 번호 순서로 한다.
- 4 는 모든 코드가 들어간 뒤 compose 한 번, 마지막 5.
