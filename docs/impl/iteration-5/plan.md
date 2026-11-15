# 5차 이터레이션 구현 계획 — 기본 전투

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 관리자는 admin-console에서 몹(종류·기준 경지·체급·공격 이름·재등장·드롭 표)을 정의하고 방에 둔다. 플레이어는 play-client에서 방의 몹을 보고 `공격` 해 1초 서버 틱으로 자동 교전하고, 처치하면 시체를 `수습` 해 드롭을 얻는다. 몹은 재등장하고, 지면 기혈 1로 시작 방에서 깨어나며, 교전 뒤 10초에 기혈이 다 찬다.

**Architecture:** 4차 구조를 넓힌다. `world-server`(헥사고날)에 슬라이스 `mob`(몹 유형·드롭 표·배치, DB)과 도메인 슬라이스 `combat`(살아 있는 몹·플레이어 교전 상태·시체, 메모리)을 더한다. 메모리 저장 포트는 `application/game/required`, 구현은 `adapter/persistence/memory`. 처음으로 서버 틱이 생긴다: 어댑터 `adapter/scheduling` 의 `@Scheduled`(1초)가 provided 포트 `GameTicker.tick()` 을 부르고, `GameService` 가 명령과 같은 게임 락·트랜잭션·outbox 로 처리한다. 시계(`Clock`)와 난수(`RandomGenerator`)는 주입하고, 테스트는 틱을 끄고 `tick()` 을 직접 부른다. 깎인 기혈만 `player_character.qi_blood_lost` 에 저장한다(4차 L4). play-client에는 `room.mobs`·`room.corpses` 와 `tone: "combat"` 이 생긴다.

**Tech Stack:** 1~4차와 같다 — Java 25 · Spring Boot 4.1.2 · Gradle 9.9.1 Kotlin DSL · PostgreSQL 18.6 / H2(테스트) · Next.js 16.4.6 · React 19.3.1 · TypeScript 7.0.2 · Vitest 5.1.0 · Docker Compose. **새 의존성 없음**(스케줄링·`RandomGenerator` 는 Spring·JDK 기본).

**Spec:** 아래 `## Spec`. 계획은 스펙과 룰북을 근거로 하고, 스펙이 정하지 않은 구현 세부는 `## 제안 결정 목록`(D77~D96)에서 정했다.

---

## Spec

- 시스템 스펙(5차 결정의 원본, 모두 `(제안)`): `docs/spec/system/index.md`(`### 5차 범위: 기본 전투`, `### 5차 이터레이션 열린 질문`), `command.md`(`## 5차 명령어` 아래 전부), `admin-tool.md`(`## 5차 범위`·`## 5차 데이터`·`## 5차 화면`·`## 5차 REST API`, `### 관리 툴 변경의 반영` 5차 줄), `platform.md`(`### 5차 WebSocket 메시지 변경`, `### 5차 월드 상태와 저장`, `#### 서버 틱`, `## 저장소 구성` 5차 줄, `## 플레이 화면` 5차 줄), `npc.md`(`## 몹 (5차)`)
- 룰북(규칙·수치·문구의 원본): `docs/spec/game-rule/combat.md`(진행 방식, 전투 상태, 처리 순서, 몹과 파티원의 행동, 공격력·방어력, 피해 흐름), `npc.md`(몹, 몹이 가지는 것, 몹 스탯, 재등장, 처치 보상 귀속, 시체와 수습, 드롭 표, 요수·적대 수선자·귀물·괴뢰), `first-region.md`(`## 몹` 표 — 테스트·스모크 값), `companion.md`(급과 기준 경지 표), `stats.md`(소수점, 최대치가 바뀔 때, 기혈), `world.md`(방 보기 표기·기혈 상태 구간), `commands.md`(대상 지정, 공통 오류, 상태별로 쓸 수 없는 명령, 출력 문체), `death.md`(사망 순간 1과 첫 줄 문구 — 나머지는 5차가 아님), `item.md`(영석, 매입가)
- 1~4차 계획·진행: `docs/impl/iteration-4/plan.md`(D57~D76), `iteration-4/progress.md`(리드 판정 L1~L5, Task 14·15 기록 — PostgreSQL enum CHECK, AFTER_COMMIT 과 테스트 트랜잭션), 이전 계획의 버전 표(`iteration-1/plan.md`)
- world-server 코딩 규칙: `.claude/rules/*.md`(특히 architecture.md `## 메모리 월드 상태`, adapter.md WebSocket 어댑터, testing.md). **world-server 태스크는 시작 전에 모두 읽는다.**
- 용어: `docs/spec/glossary.md`
- 5차 범위 밖(만들지 않는다, index.md `5차에 넣지 않는 것` 그대로): 신통·법기 쓰기, 단약·영석 쓰기, 도주·막기·추격·정혈 태우기, 정식 사망·부활·페널티·유품, 명상·회복, 기절, 선공·배회·무리·몹 대사, 적대 수선자 법력·도주, 지속 피해, 속성 약점, 방어 층, `대상`·`중지`, 안전 장소·PvP, 파티·영수·괴뢰, 깨달음·수위, 퀘스트 처치 수, 권장 경지, LLM 챗.

## Global Constraints

모든 태스크의 요구 사항에 이 절과 `## 제안 결정 목록` 이 포함된다. 구현자는 이 두 절을 먼저 읽는다.

**버전·의존성 (추측 금지)** — 1차 계획 버전 표를 그대로 쓴다. **새 라이브러리·플러그인·이미지를 넣지 않는다.** `build.gradle.kts`, `package.json`, `package-lock.json`, Dockerfile, `compose.yaml` 의 버전 줄을 바꾸지 않는다. 꼭 필요해 보이면 멈추고 리드에게 보고한다.

**로컬 환경**
- macOS arm64, 기본 JDK Temurin 25. world-server는 항상 `./gradlew`.
- 프론트 태스크는 Node 26.13.1: 명령 앞에 `source "$NVM_DIR/nvm.sh" && nvm use`(모듈의 `.nvmrc`).
- **디스크:** 태스크 시작 전 `df -h .` 를 본다. 남은 공간이 **10GB 미만이면 아무것도 지우지 말고 멈춰** 리드에게 보고한다. `ENOSPC` 도 같다.
- **Docker 빌드·실행은 Task 11에서 한 번만** 한다. Task 1~10은 `docker build`, `docker compose build/up` 을 쓰지 않는다. `pgdata` 볼륨은 어느 태스크에서도 지우지 않는다.

**문구·규칙**
- 화면 문구·서버 메시지·예외 메시지는 한국어. **룰북 수치·문장은 글자 그대로, 룰북에 없는 문구는 시스템 스펙(이 계획이 가리키는 절)의 문장을 글자 그대로** 쓴다. 스펙에도 없는 문구는 이 계획에 적힌 문장을 글자 그대로 쓴다.
- 조사는 `Josa`(끝의 닫는 괄호·문장 부호를 건너뛰고, 숫자·영문으로 끝나면 받침 없음)로 고른다. 따옴표 안 입력(`이곳에 '곰'은 없습니다.`)은 따옴표를 건너뛰고 앞 낱말로 고른다. 수는 `Numbers.count`, 시간은 `Durations.korean`.
- `(제안)` 관례: 이 계획의 D는 대부분 이미 스펙에 있다(계획과 함께 썼음). 구현 중 달라진 것과 스펙에 없는 구현 세부만 Task 10이 해당 절에 `(제안)` 으로 옮긴다. 룰북 파일(`docs/spec/game-rule/`)은 고치지 않는다.
- 툴 호출 파라미터(JSON)의 한글은 리터럴 UTF-8로 쓰고 `\uXXXX` 이스케이프를 쓰지 않는다.
- 계획 문구의 `{이름}`·`{n}` 은 채워 넣는 자리, `{이름은}` 처럼 조사가 붙은 자리는 `Josa` 로 고른 조사를 붙인다는 뜻이다.
- world-server 코딩 규칙(`.claude/rules/*.md`), 루트 패키지 `mudmk3`. 아래 world-server 경로는 `world-server/src/{main,test}/java/mudmk3/` 기준이다. open-in-view가 꺼져 있으므로 REST 응답·틱에 쓰는 지연 연관은 트랜잭션 안에서 값 record로 바꾼다(4차 `TechniqueView` 관례). **메모리 상태 객체는 엔티티를 들고 있지 않는다**(트랜잭션 밖으로 나가면 detached) — 나타날 때 값 record(`MobProfile`, D84)로 복사한다.
- **새 enum 열은 orm.xml 에 `column-definition="varchar(N)"` 을 준다**(1~4차 관례). PostgreSQL 에서는 그래도 `<table>_<column>_check` 가 생긴다(4차 Task 14 교훈, admin-tool.md `### 관리 툴 변경의 반영`). **5차는 이미 있는 enum 열에 값을 더하지 않는다**(새 enum 은 새 표 `mob_type`·`mob_tier_drop` 에만) — 그래서 `LegacySchemaCleaner` 는 고치지 않는다. 구현 중 기존 enum(`ItemCategory`·`Realm`·`BagGrade` 등)에 값을 더하게 되면 멈추고 리드에게 보고한다. Task 11 이 기존 pgdata 로 몹 저장·`qi_blood_lost` 쓰기를 확인한다.
- **기존 행이 있는 표에 더하는 열은 nullable** 로 매핑한다(4차 `realm` 교훈): `player_character.qi_blood_lost`.
- **AFTER_COMMIT 과 테스트 트랜잭션(4차 교훈):** `@ApplicationServiceTest` 는 롤백되어 AFTER_COMMIT 리스너가 오지 않는다. 5차는 관리 변경을 이벤트가 아니라 **틱의 배치 맞추기(D85)** 로 반영하므로 테스트는 준비 뒤 `tick()` 을 부르면 된다. 새 AFTER_COMMIT 리스너를 만들지 않는다.
- **게임 락:** 틱·명령·접속·끊김·관리 반영은 모두 `GameService.serialized` 하나를 지난다. 틱 처리 코드는 락 밖에서 메모리 상태를 건드리지 않는다. 관리 REST 는 메모리 몹을 읽지 않는다(배치 수는 DB).
- **메모리 상태와 테스트:** 메모리 저장소는 스프링 싱글턴이라 테스트끼리 남는다. `@ApplicationServiceTest` 에 테스트마다 몹·교전·시체 메모리와 가짜 난수를 비우는 확장을 더한다(D93, `MutableClockReset` 과 같은 방식).
- Next.js 앱 규칙은 1~4차와 같다(정적 export, `"use client"`, 상대 경로 import, 쿼리스트링 id). 저장 전 클라이언트 검사는 하지 않고 서버 `detail` 을 보인다.
- 자동 테스트는 Google 자격 증명·네트워크 없이 돈다. 시간에 걸린 테스트는 `MutableClock` + 직접 `tick()`, 난수에 걸린 테스트는 `ScriptedRandom`(D93). **실제로 기다리는 테스트(`Thread.sleep`, Awaitility)를 쓰지 않는다**(스케줄러 어댑터 단위 테스트 하나만 예외 없이 직접 부름).

**저장소·커밋**
- **main 에서 일한다.** 태스크 하나 = 커밋 하나 이상. 구현자는 커밋까지 하고, 리뷰를 통과하면 리드가 태스크마다 `git push` 한다. 구현자는 서브에이전트를 띄우지 않는다.
- 커밋 메시지: `<type>(<module>): <한국어 요약>`(type: feat, test, build, docs, chore, fix, refactor / module: world-server, play-client, admin-console, infra, spec, impl). 끝에 빈 줄 다음 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- 태스크를 끝내면 `docs/impl/iteration-5/progress.md` 의 표에 한 줄(태스크 번호·제목, 커밋 해시는 리드가 채움, 메모)을 더하고 같은 커밋에 넣는다. 각 태스크 끝의 **progress.md 한 줄** 이 그 메모의 틀이다(실제로 바꾼 것·계획과 다른 점을 덧붙인다).

**검증 명령 (각 태스크 완료 조건에 포함, 고친 모듈만)**
- world-server: `cd world-server && ./gradlew build`(테스트 + SpotBugs + ArchUnit)
- admin-console / play-client: `cd <module> && source "$NVM_DIR/nvm.sh" && nvm use && npm ci && npm test && npx tsc --noEmit && npm run build`

**스모크 (Task 11만)** — README `### 스모크 테스트` 방법 그대로(4차 Global Constraints 와 같음): `.env` 는 고치지 않고 `docker compose build` 한 번 → `up` 때만 셸 값으로 `MUD_DEV_LOGIN_ENABLED=true`, `MUD_ADMIN_EMAILS=<.env 의 기존 값>,smoke-admin@example.com` 을 덮어써 `docker compose up -d` → `node scripts/smoke-test.mjs`. 끝나면 **시작 방을 id 1(태초 객잔)로, 시작 소지품을 `[]` 로 되돌리고** 확인한다. 실제 서버 틱(1초)이 돌므로 스모크는 몹 행동을 실제 시간으로 기다린다(단계마다 제한 시간).

## 제안 결정 목록

스펙이 정하지 않은 구현 세부. 번호는 4차(D57~D76)에 잇는다. 스펙이 이미 정한 것(문구·REST 표·화면 칸·스탯 식·틱 순서)은 다시 적지 않고 절을 가리킨다 — **그 절의 문장이 곧 요구 사항이다.**

**D77. 슬라이스·의존** — platform.md `## 저장소 구성` 5차 줄 그대로: `mob`(domain·application) → `item, room`; 도메인 슬라이스 `combat` → `mob`(application 슬라이스 없음 — 메모리 포트는 game 의 required); `game` → `mob`, `combat`(+4차). 드롭 표에 든 아이템 종류 수는 `ItemTypeRepository` JPQL(엔티티 이름 문자열)로 센다. 어댑터 패키지 `adapter/scheduling` 을 새로 둔다(`mudmk3.adapter..` 라 ArchUnit 계층 규칙 그대로). 순환 없음.

**D78. 경지 보조와 몹 enum** — `domain/shared/Realm` 에 `int majorIndex()`(범인 0 · 연기 1 · 축기 2 — 방 보기 `헤아릴 수 없음` 판정)와 `int beastGrade()`(연기 1~7층 1 · 8~13층 2 · 축기 3, 범인은 `IllegalStateException`)를 더한다. `domain/mob`:
- `MobKind { BEAST("요수", 1.2), CULTIVATOR("적대 수선자", 1.0), GHOST("귀물·음혼", 0.5), PUPPET("괴뢰", 1.5) }` — `koreanName()`, `double bodyFactor()`, `double qiBloodFactor()`(PUPPET 0.8, 나머지 1.0).
- `MobTier { NORMAL, STRONG, ELITE, BOSS }` — `koreanName()`(`일반`…`두목`), `double qiBloodFactor()`(0.7·1.0·2.0·10.0), `OptionalDouble attackFactor()`(0.6·0.8·0.6·빔), `double bodyFactor()`(BOSS 1.2, 나머지 1.0), `int rewardMultiplier()`(1·2·5·20), `OptionalInt defaultRespawnSeconds()`(90·180·600·빔), `@Nullable String badge()`(null·`강적`·`정예`·`두목`).
- `DropTier { ADVANCED("고급", 0.08), RARE("희귀", 0.005), TREASURE("진귀", 0.0004) }` — `koreanName()`, `double baseChance()`. 선언 순서 = 수습 표시 순서.

**D79. 몹 스탯 `MobStats`** — `domain/mob/MobStats(int qiBlood, int body, int power)` record, `static MobStats of(MobKind kind, Realm realm, MobTier tier, @Nullable BigDecimal bossAttackCoefficient)`. 식은 admin-tool.md `### 몹 MobType` 스탯 줄 그대로, 마지막에 `Math.round`(반올림, 룰북). 테스트 값은 그 절의 네 예와 들토끼(요수 일반 연기 1층 42·7·7)·흑풍랑왕(요수 정예 연기 7층 240·14·14).

**D80. 몹 유형 `MobType`** — `domain/mob/MobType`(`@Entity`), 입력 record `MobTypeSaveInfo(String name, @Nullable String description, MobKind kind, Realm realm, MobTier tier, String attackName, @Nullable BigDecimal attackCoefficient, @Nullable Integer respawnSeconds, List<MobFixedDrop> fixedDrops, List<MobTierDrop> tierDrops)`, embeddable record `MobFixedDrop(Long itemTypeId, int count)`·`MobTierDrop(DropTier tier, Long itemTypeId)`(3차 `ShopItem`·`StartingItem` 처럼 물건은 id만, FK 없음).
- `static create(MobTypeSaveInfo info, Map<Long, String> itemNames)`·`update(info, itemNames)`(물건 이름은 문구용, 서비스가 만듦) 가 admin-tool.md 예외 표 문구로 `IllegalArgumentException` 을 던진다. 검사 순서는 그 절 끝 문장 그대로. `update` 는 드롭 표를 통째로 바꾼다. 이름 유일(대소문자 무시)은 서비스가 `existsByNameIgnoreCase(AndIdNot)` → `DuplicateMobNameException`(`domain/mob`, 4차 technique 관례, 409).
- 메서드: `getName()`… getter, `MobStats stats()`, `String rankName()`(BEAST `{beastGrade}급`, 나머지 `realm.koreanName()`), `int effectiveRespawnSeconds()`, `List<MobFixedDrop> getFixedDrops()`, `List<Long> tierItemIds(DropTier)`.
- 표: `mob_type`(`name` varchar(20) `UK_MOB_TYPE_NAME`, `description` varchar(500) not null, `kind` varchar(16), `realm` varchar(32), `tier` varchar(16) — enum STRING, `attack_name` varchar(12), `attack_coefficient` numeric(2,1) null, `respawn_seconds` int null), `mob_fixed_drop`(`mob_type_id`, `position`, `item_type_id`, `count`), `mob_tier_drop`(`mob_type_id`, `position`, `tier` varchar(16), `item_type_id`).

**D81. 몹 배치 `MobPlacement`** — `domain/mob/MobPlacement`(`@Entity`): `Long roomId`, `@ManyToOne MobType mobType`(EAGER), `int count`. `static create(Long roomId, MobType, int count)`, `update(MobType, int count)`(수 1~10, 문구 admin-tool.md). 표 `mob_placement`(`room_id` FK 없음, `mob_type_id` FK). `RoomDeletedEvent` 동기 리스너가 그 방 배치를 지운다(3차 `FloorModifyService.onRoomDeleted` 와 같음).

**D82. 관리 서비스·REST** — `application/mob/provided`: `MobTypeFinder`(`/** 몹 유형을 찾는다 */` `MobType find(Long)` — 없으면 `IllegalArgumentException("몹을 찾을 수 없습니다. id: " + id)`, `MobTypeView view(Long)`, `List<MobTypeView> viewAll()` 이름순), `MobTypeEditor`(`/** 몹 유형을 만들고 고치고 지운다 */` `create`·`update`·`delete`), `MobPlacementFinder`(`/** 몹 배치를 찾는다 */` `List<MobPlacement> findAll()`(id 순, 몹 유형과 드롭 표까지 읽음 — 틱이 씀), `List<MobPlacementView> findAllInRoom(Long roomId)`), `MobPlacementEditor`(`create(Long roomId, @Valid MobPlacementSaveRequest)`·`update(Long, …)`·`delete(Long)`), 요청 `MobTypeSaveRequest`(+`MobFixedDropRequest`, `MobTierDropRequest`, `toInfo()`), `MobPlacementSaveRequest(Long mobId, int count)`, 값 record `MobTypeView`(admin-tool.md `MobDetailResponse` 칸 전부, 물건 이름은 `ItemTypeFinder.findAllById`, `List<MobTypeView.PlacementView> placements` — `PlacementView(Long id, Long roomId, String roomName, int count)`), `MobPlacementView(Long id, Long roomId, Long mobId, String mobName, int count)`. 필수 enum(`kind`·`realm`·`tier`) null·빠진 목록·빠진 묘사는 4차 D72 방식(컴팩트 생성자).
- 서비스 `MobTypeQueryService`·`MobTypeModifyService`·`MobPlacementQueryService`·`MobPlacementModifyService`. 물건 종류 확인은 `ItemTypeFinder.findAllById` 로 없는 id 첫째를 3차 문구로. 배치된 몹 삭제 409 `MobTypeInUseException`(provided). 배치 수는 `MobPlacementRepository.countByMobTypeId`.
- 어댑터 `adapter/webapi/{MobApi,MobPlacementApi}.java`, DTO `MobSummaryResponse`·`MobDetailResponse`·`MobPlacementResponse`(admin-tool.md 모양), `RoomApi` 상세에 `mobs`. 409 두 예외를 `ApiControllerAdvice.handleConflict` 에 더한다.
- 아이템 쪽: `ItemTypeRepository` + `@Query("select count(m) from MobType m join m.fixedDrops d where d.itemTypeId = :itemTypeId")`·같은 모양 `tierDrops`, `ItemTypeModifyService` 의 '쓰이는 물건' 검사에 더한다(문구는 3차 그대로).

**D83. 깎인 기혈** — platform.md `### 5차 월드 상태와 저장` 의 'DB에 두는 것' 그대로. **이 열은 전용 쿼리로만 읽고 쓴다**(리뷰 고침: 틱이 캐릭터 행 전체를 쓰면 관리 경지 지정을 옛 값으로 되돌릴 수 있음).
- 매핑: `PlayerCharacter.qiBloodLost`(`@Nullable Double`) — orm.xml `<column name="qi_blood_lost" nullable="true" insertable="false" updatable="false"/>`(ddl 이 열을 만들되 엔티티 저장은 이 열을 쓰지 않음). getter 는 `@Nullable Double getQiBloodLostAtLoad()`(읽은 때 값, 테스트만 씀 — 게임·관리 코드는 쓰지 않는다).
- `CharacterRepository` 에 `@Modifying(flushAutomatically = true) @Query(value = "update player_character set qi_blood_lost = :lost where id = :id", nativeQuery = true) int writeQiBloodLost(Long id, double lost)` 와 `@Query(value = "select coalesce(qi_blood_lost, 0) from player_character where id = :id", nativeQuery = true) Optional<Double> readQiBloodLost(Long id)`. 네이티브라 영속성 컨텍스트를 비우지 않고(`clearAutomatically` 없음) 다른 열을 건드리지 않는다. 구현 중 H2·PostgreSQL 에서 이 매핑이 열을 만들지 않거나 쓰기가 막히면 멈추고 리드에게 보고한다.
- 계산은 도메인 `domain/character/QiBlood`(static): `double lostAfterDamage(double lost, double damage, int max)`(min(max, lost + damage)), `double lostFor(double current, int max)`(max − current, 0~max), `double lostAfterDemotion(double lost, Realm from, Realm to)`(`to.body() < from.body()` 이면 `max(0, lost − (from.body() − to.body()) × 10)`, 아니면 그대로), `double current(double lost, int max)`(max(0, max − lost)).
- provided 포트 `application/character/provided/CharacterVitalsEditor`(`/** 캐릭터의 현재 기혈을 깎고 채운다 */`): `double qiBloodLost(Long characterId)`, `double takeDamage(Long characterId, double damage, int maxQiBlood)`(남은 현재 기혈을 돌려줌), `void setQiBlood(Long characterId, double current, int maxQiBlood)`, `boolean restoreQiBlood(Long characterId)`(깎인 게 있었으면 0으로 쓰고 true). 구현 `CharacterModifyService`, 없는 캐릭터 4차 문구. 관리 경지 지정 `designate` 는 `changeRealm` 뒤 `writeQiBloodLost(id, QiBlood.lostAfterDemotion(readQiBloodLost(id), from, to))`.
- `StatSheet` 에 `double qiBloodCurrent` 를 더한다: `StatSheet.of(Realm, @Nullable SpiritRoot, List<CharacterTechnique>, double qiBloodLost)` → `QiBlood.current`. `CultivationFinder.stats` 는 `CharacterVitalsEditor.qiBloodLost` 로 읽어 넘긴다(엔티티 값을 쓰지 않음). `StatusMessage.of` 의 `qiBlood` 게이지 current = `Damage.gauge(qiBloodCurrent)`, `CultivationTexts.status` 의 기혈 줄도 같다. 관리 REST 응답은 바꾸지 않는다.

**D84. 메모리 개념 (`domain/combat`)** — 모두 엔티티가 아닌 보통 클래스(lombok `@Getter`), 시각 인자는 `Instant now`.
- `MobProfile(Long mobTypeId, String name, String description, MobKind kind, Realm realm, MobTier tier, String attackName, MobStats stats, int respawnSeconds, String rankName)` record, `static MobProfile of(MobType)`.
- `MobInstance`: `long id`(전역 증가 번호), `Long placementId`, `int slot`(배치 안 자리 0~), `Long roomId`, `int placementCount`, `MobProfile profile`, `double qiBlood`, 살아 있음/쓰러짐, `@Nullable Instant respawnAt`, 교전 상대(`LinkedHashMap<Long characterId, Engagement(double threat, double dealt, long firstDealtSeq)>` 교전 순), `@Nullable Long targetCharacterId`, `@Nullable Instant nextActionAt`, `@Nullable Instant refillAt`. 메서드: `static spawn(long id, Long placementId, int slot, Long roomId, int placementCount, MobProfile, Instant now)`, `boolean engage(Long characterId, Instant now)`(새 상대면 true, 상대가 없던 몹이면 위협 1·`nextActionAt = now + 1.5초`·`refillAt = null`), `void disengage(Long characterId, Instant now)`(비면 `refillAt = now + 10초`, `nextActionAt = null`), `MobHit receive(Long characterId, double power, Instant now)`(`MobHit(double damage, boolean died)` — 피해 = `Damage.afterBody(power, body)`, 위협·피해 기록은 층 전 `power`, 쓰러지면 상대를 모두 비우고 `respawnAt = now + respawnSeconds`), `Optional<Long> target()`(위협 최고, 지금 대상보다 1.1배를 넘어야 바꿈, 대상이 빠졌으면 최고로 — 룰북), `Optional<Long> killer()`(dealt 최대, 같으면 `firstDealtSeq` 작은 쪽), `Set<Long> opponentIds()`, `boolean isAlive()`, `boolean isFighting()`, `Condition condition()`, `void refillIfDue(Instant now)`(기혈 최대·위협·피해 기록 지움), `boolean respawnDue(Instant now)`, `void respawn(MobProfile, Instant now)`, `void actedAt(Instant due, Instant now)`(다음 = `CombatRules.next(due, now)`).
- `PlayerCombat`: `Long characterId`, 교전 상대 `List<Long> mobIds`(교전 순), `@Nullable Long targetMobId`, `@Nullable Instant nextAttackAt`(3초 간격, 교전이 끝나도 남음), `@Nullable Instant refillAt`. 메서드 `engage(long mobId)`, `target(long mobId)`, `disengage(long mobId, Instant now)`(비면 `refillAt = now + 10초`), `endAll(Instant now)`, `Optional<Long> currentTarget()`(지정 대상, 없으면 첫 상대), `boolean readyAt(Instant now)`(`nextAttackAt` 이 없거나 지남), `attackedAt(Instant due, Instant now)`, `boolean isFighting()`, `void scheduleRefill(Instant now)`, `boolean refillDue(Instant now)`, `clearRefill()`.
- `Corpse`: `long id`, `Long roomId`, `String mobName`, `@Nullable Long killerCharacterId`, `Instant diedAt`, `List<DropLine> remaining`. `String name()`(`{몹}의 시체`), `boolean collectibleBy(Long characterId, Instant now)`(처치자이거나 `diedAt + 60초` 지남), `Duration lockedFor(Instant now)`, `boolean expired(Instant now)`(3분), `boolean isEmpty()`, `void takeFrom(int index, int count)`.
- `Condition { HEALTHY("멀쩡함", "멀쩡하다."), HURT("다침", "다쳤다."), BADLY_HURT("크게 다침", "크게 다쳤다."), DYING("빈사", "빈사 상태다.") }` — `static of(double current, int max)`: 비율 ≥ 0.75 HEALTHY, ≥ 0.40 HURT, ≥ 0.15 BADLY_HURT, 그 밖 DYING.
- `Damage`: `static double afterBody(double power, int body)`(`max(0, power − body × 0.5)`), `static String display(double)`(0 → `0`, 0 초과 0.5 미만 → `1 미만`, 그 밖 `Math.round`), `static int gauge(double current)`(0 초과 1 미만 → 1, 그 밖 `floor`), `static double playerPower(int spirit)`(`3 + 1.8 × spirit`).
- `CombatRules`: 상수 `ATTACK_INTERVAL = 3초`, `MOB_FIRST_ACTION = 1.5초`, `REFILL_DELAY = 10초`, `CORPSE_EXCLUSIVE = 60초`, `CORPSE_LIFETIME = 3분`, `static Instant next(Instant due, Instant now)`(`due + 3초`, 그래도 `≤ now` 면 `> now` 가 될 때까지 3초씩 더함).
- `domain/mob/DropLine(Long itemTypeId, String name, int count)`, `domain/mob/DropRoll.roll(MobType type, Map<Long, ItemType> itemTypes, ItemType lowSpiritStone, RandomGenerator random) → List<DropLine>`. 순서·난수 소비(테스트가 정하는 순서): 고정 재료(입력 순, 난수 없음) → 고정 재료가 있으면 `nextDouble() < 0.3` 이면 `nextDouble()` 하나로 개수 `max(1, floor(매입가 합 × (1 + u)))`(매입가 합 = Σ `Pricing.unitBuyPrice(item) × count`) → 계층마다(선언 순) 목록이 비지 않았으면 `p = baseChance × rewardMultiplier`, 확정 `floor(p)` 개 + `nextDouble() < p − floor(p)` 면 1개, 개수마다 `nextInt(목록 크기)` 로 고름. 없는 물건 종류 id 는 건너뛴다(드롭 표가 고쳐진 뒤 지워진 경우). 같은 물건 줄은 처음 자리에 수를 합친다.

**D85. 틱과 배치 맞추기** — `application/game/provided/GameTicker`(`/** 서버 틱 하나를 처리한다 */ void tick()`)를 `GameService` 가 구현한다(`@Transactional(propagation = SUPPORTS)` + `serialized`, 4차 `connect` 와 같음). 실제 처리는 package-private `@Component` `CombatEngine.tick(Instant now, BiConsumer<String, GameMessage> send)`(3차 핸들러의 `send` 콜백 방식). 순서는 platform.md `#### 서버 틱` 1~5 그대로.
- 배치 맞추기: `MobPlacementFinder.findAll()` 로 배치마다 '서명' `(roomId, mobTypeId, count)` 를 보고, 메모리 몹의 서명이 다르거나 배치가 없어졌으면 그 몹을 지우고(교전 중이던 플레이어는 `PlayerCombat.disengage`, 알림 없음) 없는 자리를 `MobProfile.of` 로 나타나게 한다(`청모랑이 나타난다.`, notice event `respawn`). 재등장 때도 배치에서 읽은 지금 정의로 `MobProfile` 을 다시 만든다.
- 스케줄러: `adapter/scheduling/GameTickScheduler`(`@Adapter`, `@Scheduled(fixedRate = 1000) void tick()` → `GameTicker.tick()`, 예외는 잡아 `log.error` 하고 다음 틱을 막지 않음), `support/config/SchedulingConfig`(`@EnableScheduling`, `@ConditionalOnProperty(name = "mud.game.tick-enabled", havingValue = "true", matchIfMissing = true)`). `GameTickScheduler` 도 같은 조건. `application.yml` 에 `mud.game.tick-enabled: true`, `application-test.yml` 에 `false`.
- 메모리 포트(`application/game/required`, 한 줄 Javadoc): `MobInstanceRepository`(`save`, `Optional<MobInstance> findById(long)`, `List<MobInstance> findAllInRoom(Long roomId)` — 살아 있는 것만, (placementId, slot) 순, `List<MobInstance> findAll()`, `void delete(long)`, `void deleteAll()`), `PlayerCombatRepository`(`save`, `Optional<PlayerCombat> find(Long characterId)`, `List<PlayerCombat> findAll()`, `delete`, `deleteAll`), `CorpseRepository`(`save`, `List<Corpse> findAllInRoom(Long roomId)` 쓰러진 순, `findAll`, `delete(long)`, `deleteAll`). 구현 `adapter/persistence/memory/InMemory{MobInstance,PlayerCombat,Corpse}Repository`(`ConcurrentHashMap`, 게임 락 안에서만 바뀜 — 1차 presence 와 같음).

**D86. 교전 처리** — command.md `### 공격`·`### 교전 메시지` 그대로. 이 계획이 정하는 것:
- `Command.Attack(@Nullable ItemQuery target)`(`공격`, 인자 전체가 이름·번호 허용, `ItemQuery.parse`), `Command.Collect(@Nullable ItemQuery corpse)`(`수습`). Task 6 에서는 `Collect` 를 `CombatCommandHandler` 로 보내되 그 자리는 4차 `UNKNOWN` 문구를 보내고, Task 7 이 수습으로 바꾼다(sealed switch 를 채우려는 임시 자리). `ReservedWords.ALL` 에 `공격` 을 더한다(D62 목록 끝).
- 명령 쪽은 package-private `@Component` `CombatCommandHandler.handle(Command, OnlinePlayer, BiConsumer<String, GameMessage>)`(공격·수습·보기 몹), 문구는 package-private `@Component` `CombatTexts`(4차 `CultivationTexts` 와 같음).
- 몹 찾기는 `ItemNames.select(방 보기 순서로 늘어놓은 몹, m -> m.getProfile().name(), query)` — 번호(`2.청모랑`)는 방 보기에 보이는 순서(D89 묶음 순서, 묶음 안에서 (placementId, slot) 순)와 같다, NPC 는 2차 `talk` 찾기 규칙, 플레이어는 presence 이름 전체·앞부분. 시체는 `Corpse.name()` 으로 `ItemNames.select`.
- 플레이어 행동(틱 또는 `공격` 즉시): 위력 = `Damage.playerPower(CultivationFinder.stats(id).spirit())` → `MobInstance.receive` → 줄 보냄 → 죽었으면 D87 처치. 몹 행동: `target()` 의 플레이어에게 `Damage.afterBody(profile.stats().power(), stats.body())` → `CharacterVitalsEditor.takeDamage(id, dmg, stats.qiBlood())` → 줄 → 0 이하면 D88 질 때. 같은 틱 행동 정렬 키는 `(dueAt, 교전 시작 순번)`(플레이어·몹 모두 교전에 들어온 순번을 `PlayerCombat`·`MobInstance` 에 둠). 행동 전 다시 확인: 행동자가 살아 있고/세계에 있고, 대상과 같은 방이며 교전 중.
- 막힘 검사는 `CombatEngine.isFighting(Long characterId)` 를 `GameService`(이동·`종료`·`대화`)·`ItemCommandHandler`(`줍기`·`주기`·`읽기`)·`ShopCommandHandler`(`목록`·`사기`·`팔기`·`값`·대기 있는 `확인`) 처음에 부른다. 범인 막힘(D68)보다 먼저다.
- **결과를 돌려주고 옮기기는 GameService 가 한다**(리뷰 고침: `CombatEngine` 이 `GameService` 를 주입받으면 순환, `enterZone`·`roomView` 는 GameService private). `CombatEngine.tick(Instant, send)` 은 `CombatOutcome(List<Long> defeatedCharacterIds)`(record, `application/game` package-private, 진 순서)를 돌려준다. `공격` 명령으로는 질 수 없어 `attack` 은 돌려주지 않는다. `GameService.tick()` 은 `serialized` 안에서 outcome 의 캐릭터마다 private `wakeAtStart(Long characterId)`(D88)를 부른다.
- 세계에서 빠질 때(`doDisconnect`·`quit`·`relocateFrom`) `CombatEngine.leave(characterId, now)`: 모든 교전을 끝내고 `PlayerCombat` 을 지운다(relocate 는 지우지 않고 `endAll` + 다시 차기 예약). 들어올 때(`enterWorld`) `CombatEngine.entered(characterId, now)`: 깎인 기혈이 있으면 다시 차기 예약.

**D87. 처치와 수습** — command.md `### 수습` 그대로. 처치 때: `DropRoll.roll(MobTypeFinder.find(profile.mobTypeId()), ItemTypeFinder.findAllById(드롭 id들), ItemTypeFinder.spiritStone(LOW), random)` → `Corpse` 저장(드롭이 비어도 시체는 남음) → 처치자·교전자·방 사람 줄(교전 메시지 표) → 각 교전 플레이어 `disengage`. 수습: `collectibleBy` 아니면 남은 시간 `ceil` 초(`Durations.korean(BigDecimal.valueOf(초))`), 줄마다 `InventoryEditor.addUpTo(id, itemTypeId, count, braceletOpen)` 로 들어간 만큼 빼고, 남은 게 없으면 시체를 지운다. 드롭 표가 고쳐진 뒤 지워진 물건 종류 id(시체를 만들 때는 있었음)는 수습 때 조용히 건너뛰고 시체에서 뺀다(`ItemTypeFinder.findAllById` 에 없는 id). 거둔 줄은 `{이름} {수}` 를 `, ` 로 잇는다.

**D88. 질 때와 다시 차기** — command.md `### 질 때 (5차 최소)`·`### 교전 뒤 기혈` 그대로. 나누어 맡는다:
- `CombatEngine`(몹 행동 안): `CharacterVitalsEditor.setQiBlood(id, 1, max)` → 그 플레이어의 모든 교전 끝(`MobInstance.disengage`, 몹은 상대가 없으면 다시 차기 예약) → 그 틱의 남은 행동 취소 → 첫 줄 `기혈이 다했습니다. 눈앞이 캄캄해진다…` 와 옛 방 notice `{이름이} 쓰러진다.`(event `defeated`) → `PlayerCombat.scheduleRefill(now)` → outcome 에 id 를 넣음.
- `GameService.wakeAtStart(Long characterId)`(private, 락 안): 시작 방(D96)이 지금 방과 다르면 `CharacterMover.moveTo` + presence `moveTo` + 시작 방 사람에게 1차 `appear` → 둘째 줄 `{시작 방 이름}에서 깨어났습니다. (기혈 1/{max})`(max 는 `CultivationFinder.stats`) → `sendRoom`(지금 방 `room`). 세션이 없으면(끊김 처리 중) 옮기기만 한다.
- 다시 차기: `CombatEngine.tick` 의 순서 3에서 `refillDue` 면 `CharacterVitalsEditor.restoreQiBlood` 가 true 일 때만 줄을 보낸다.

**D89. 방 보기·보기·상태** — command.md `### 교전 메시지` 의 방 보기·`보기 <몹>`·`상태` 줄 그대로. `RoomMessage` 에 `List<MobView> mobs`, `List<CorpseView> corpses` 를 `occupants` 다음, `items` 앞에 더한다(JSON 필드 순서 = 이 순서): `MobView(String name, String rank, @Nullable String tier, int count, boolean fighting)`, `CorpseView(String name, boolean collectible)`. `RoomViewAssembler.assemble` 에 `List<MobInstance> mobs, List<Corpse> corpses, Realm viewerRealm, Instant now` 를 더한다. 묶음 키 `(mobTypeId, fighting)`, 순서는 그 키가 처음 나온 자리. rank = `mob.realm().majorIndex() − viewerRealm.majorIndex() >= 2` 면 `헤아릴 수 없음`, 아니면 `profile.rankName()`. `보기 <대상>` 의 찾는 순서는 플레이어 → NPC → 몹 → 시체 → 바닥 물건 → 소지품(`ItemCommandHandler.look` 의 4차 순서에 몹·시체를 끼움, 시체는 `아직 자세히 볼 수 없습니다.`). `상태` 전투 줄은 `GameService.status` 가 `CultivationTexts.status` 뒤에 `CombatTexts.statusLine(PlayerCombat, mobs)` 가 비지 않으면 `\n` 으로 붙인다.

**D90. WebSocket·play-client** — platform.md `### 5차 WebSocket 메시지 변경`·플레이 화면 5차 줄 그대로. `SystemMessage.combat(String)`, `NoticeMessage.combat(String text, String actor, String event)`. play-client 스타일 `mob`·`combat`(색은 스펙). 줄 모양: 몹 `{name} ({rank}{fighting ? ", 전투 중" : ""})` + (tier 있으면 ` [{tier}]`) + (count ≥ 2 면 ` ×{count}`), 시체 `{name}` + (collectible 이면 ` (수습 가능)`).

**D91. admin-console** — admin-tool.md `## 5차 화면` 그대로. 파일: `app/{mobs,mob}/page.tsx`, `src/components/{MobForm,DropTableEditor,MobPlacementsEditor}.tsx`, `src/lib/mobs.ts`(종류·체급·계층 코드와 이름 상수, `formatRespawn(seconds)` — 4차 시간 문체: 60 미만 `N초`, 나누어떨어지는 분 `N분`, 그 밖 `N분 M초`), `src/lib/api.ts`(타입·함수), `src/components/TopBar.tsx`, `app/room/page.tsx`. 경지 선택은 `GET /api/admin/realms` 에서 `MORTAL` 을 뺀다.

**D92. 도움말** — command.md `### 5차 도움말` 문구를 `GameService.HELP` 에 글자 그대로. play-client `COMMANDS`·이름 후보 순서도 그 절 그대로.

**D93. 테스트 도우미** — `test/java/mudmk3/ScriptedRandom`(`RandomGenerator` 구현, `enqueueDoubles(double...)`·`enqueueInts(int...)`, 비면 `nextDouble()` 0.999·`nextInt(bound)` 0 — 영석·계층 드롭이 없는 쪽), `TestFakesConfiguration` 에 `@Primary` 빈. 운영 빈은 `support/config/RandomConfig`(`RandomGenerator randomGenerator()` = `RandomGenerator.getDefault()`, 게임 락 안에서만 씀). `test/java/mudmk3/CombatMemoryReset`(BeforeEach: 메모리 저장소 셋 `deleteAll`, `ScriptedRandom.reset()`)을 `@ApplicationServiceTest` 에 `@ExtendWith` 로 더한다. `BaseGameTest` 에 `tick()`(=`GameTicker.tick()`), `advance(double seconds)`(시계만), `advanceAndTick(double seconds)`(0.5초 단위로 나눠 시계를 움직이며 틱마다 `tick()` — 실제 1초 틱보다 촘촘해 줄이 나오는 시각은 조금 이를 수 있으나 최종 상태(기혈·처치·시체)는 같아야 함, 테스트는 '그 시간까지 나온 줄' 로만 단언), `placeMob(GameMap, MobTypeSaveInfo, int count)`(만들고 배치하고 `tick()` 한 번 — 몹이 나타난 상태를 돌려줌. 틱 전 상태를 보는 테스트는 `placeMob` 대신 편집기를 직접 부름). 픽스처 `domain/mob/MobFixture`: `rabbit()`(들토끼, BEAST·QI_1·NORMAL·`뒷발차기`), `wolf()`(청모랑, QI_2), `boss()`(쌍두 흑망, QI_12·BOSS·0.6·2,700초), `request(...)`.

**D94. 1~4차 테스트·스모크 고치기** — `RoomMessage` 필드가 늘어 생성자를 쓰는 테스트(`RoomViewAssembler`·play-client 픽스처)는 빈 `mobs`·`corpses` 를 넣는다. `StatusMessage` 기혈 current 는 깎이지 않은 캐릭터에서 최대치라 4차 기대값 그대로다. 스모크 단계 24·35 의 `도움말` 기대값을 D92 문구로 바꾼다. `ReservedWords` 개수 기대(104)는 105로.

**D95. 경합·순서** — 관리 배치 변경은 다음 틱에 보인다(커밋 뒤라야 틱이 읽음 — 트랜잭션 격리). 깎인 기혈은 전용 네이티브 쿼리로만 쓰고 엔티티 저장은 그 열을 쓰지 않으므로(D83), 틱의 기혈 쓰기와 관리 경지 지정·게임 이동이 서로의 열을 되돌리지 않는다. 관리 경지 지정의 깎인 기혈 조정(읽고 쓰기)과 같은 순간의 틱 기혈 쓰기가 겹치면 나중 커밋이 이긴다(그 열 하나, 드묾, 받아들임). 몹 정의 고치기와 그 몹 처치가 겹치면 드롭은 처치 트랜잭션이 읽은 정의다.

**D96. 시작 방** — 질 때 옮길 시작 방은 `WorldSettingsFinder.findStartRoom()`. 비어 있으면(실제로는 생기지 않음) 옮기지 않고 그 방에서 기혈 1로 깨어나며 둘째 줄의 방 이름은 지금 방이다.

### 5차 열린 질문 대응

index.md `### 5차 이터레이션 열린 질문` 은 모두 `→ (제안)` 답이 있다. 이 계획은 답을 바꾸지 않고 구현 세부만 더한다: 몹 데이터 D78~D80, 배치·반영 D81·D85, 공격·위력 D84(`Damage.playerPower`)·D86, 틱·간격 D84(`CombatRules`)·D85, 피해·위협·처치자 D84, 막힘 D86, 끊김 D86, 시체·드롭 D84(`DropRoll`)·D87, 재등장·다시 차기 D84·D85, 질 때·회복 D88, 화면 D89·D90.

## Review Focus

스펙이 직접 말하지 않지만 실제로 부딪칠 만한 것. 각 줄의 테스트는 담당 태스크에 들어 있다.

1. 두 플레이어가 한 몹과 싸우다 한 사람이 지거나 연결을 끊음 → 몹은 남은 사람을 계속 치고(위협 규칙), 처치자는 피해를 더 준 쪽, 다른 사람은 60초 동안 수습 못 함. (Task 6 `mobSwitchesToRemainingPlayer`·`disconnectEndsEngagement`, Task 7 `killerIsTopDamageDealer`·`otherPlayerWaitsForCorpse`)
2. 교전 중에 관리자가 배치를 지우거나 수를 바꾸거나 방을 지움 → 다음 틱에 몹이 사라지고 플레이어 교전이 끝나며(이동 가능, 10초 뒤 기혈 참) 틱이 예외 없이 돈다. (Task 5 `placementChangeRespawnsFresh`, Task 6 `placementRemovedMidCombatEndsCombat`·`roomDeletedMidCombatRelocatesAndEnds`)
3. 기존 pgdata 와 재시작 → `qi_blood_lost` 가 null 인 캐릭터는 가득, 깎인 채 접속을 끊은 캐릭터는 다음 접속 10초 뒤 참, 다시 시작 뒤 첫 틱에 몹이 다 찬 채 나타남. (Task 3 `legacyNullIsFull`, Task 7 `hurtPlayerRefillsAfterEnter`, Task 5 `firstTickSpawnsAll`, Task 11 단계 확인)
4. 저물대가 차서 수습이 일부만 됨 → 거둔 줄 + `저물대에 자리가 없습니다. 남은 것: …`, 남은 것은 시체에 남아 칸을 비우면 다시 수습, 3분이 지나면 남은 것과 함께 사라짐. (Task 7 `collectPartialWhenBagFull`·`corpseExpiresWithLeftovers`)
5. 같은 틱에 행동이 겹침 → 플레이어의 일격으로 몹이 쓰러지면 그 틱의 몹 행동은 취소, 몹이 플레이어를 쓰러뜨리면 그 플레이어의 행동은 취소, 틱이 몇 초 밀려도 밀린 공격을 몰아서 하지 않음. (Task 6 `deadMobActionCancelled`·`defeatedPlayerActionCancelled`·`lateTickDoesNotBurst`)

---

## 파일 구조 (요약, 새로 생기거나 바뀌는 것)

```
world-server/src/main/java/mudmk3/
  domain/shared/Realm.java                                                   (majorIndex, beastGrade)
  domain/mob/{MobKind,MobTier,DropTier,MobStats,MobType,MobTypeSaveInfo,MobFixedDrop,MobTierDrop,MobPlacement,DropLine,DropRoll,DuplicateMobNameException}.java   (새)
  domain/combat/{MobProfile,MobInstance,MobHit,Engagement,PlayerCombat,Corpse,Condition,Damage,CombatRules}.java   (새)
  domain/character/PlayerCharacter.java, domain/cultivation/StatSheet.java, domain/game/{Command,CommandParser}.java, domain/technique/ReservedWords.java
  application/mob/{MobTypeQueryService,MobTypeModifyService,MobPlacementQueryService,MobPlacementModifyService}.java, provided/*, required/{MobTypeRepository,MobPlacementRepository}.java   (새)
  application/character/provided/CharacterVitalsEditor.java (새), CharacterModifyService.java
  application/cultivation/CultivationQueryService.java, application/item/{ItemTypeModifyService,required/ItemTypeRepository}.java
  application/game/{GameService,ItemCommandHandler,ShopCommandHandler,RoomViewAssembler,CombatEngine,CombatCommandHandler,CombatTexts}.java, provided/GameTicker.java, required/{GameMessage,MobInstanceRepository,PlayerCombatRepository,CorpseRepository}.java
  adapter/persistence/memory/InMemory{MobInstance,PlayerCombat,Corpse}Repository.java   (새)
  adapter/scheduling/GameTickScheduler.java (새), adapter/webapi/{MobApi,MobPlacementApi,RoomApi,ApiControllerAdvice}.java, dto/{MobSummaryResponse,MobDetailResponse,MobPlacementResponse,RoomDetailResponse}.java
  support/config/{SchedulingConfig,RandomConfig}.java (새)
  resources/{application.yml, META-INF/orm.xml}
world-server/src/test/java/mudmk3/{ScriptedRandom,CombatMemoryReset,TestFakesConfiguration,ApplicationServiceTest}.java, domain/mob/MobFixture.java, application/game/provided/BaseGameTest.java
admin-console/  app/{mobs,mob}/page.tsx, app/room/page.tsx, src/lib/{api,mobs}.ts, src/components/{TopBar,MobForm,DropTableEditor,MobPlacementsEditor}.tsx
play-client/    src/lib/{protocol,render,complete}.ts, app/globals.css
docs/spec/system/*.md  README.md  scripts/smoke-test.mjs
```

---

### Task 1: 몹 유형 — 도메인·영속·관리 서비스, 드롭 표의 아이템 사용 검사

**권장 모델:** opus

**목적:** D78·D79·D80, `MobTypeFinder`·`MobTypeEditor`(D82 앞부분), 아이템 쪽 쓰이는 검사. 배치·REST는 Task 2.

**Files:**
- Create: `domain/mob/{MobKind,MobTier,DropTier,MobStats,MobType,MobTypeSaveInfo,MobFixedDrop,MobTierDrop,DuplicateMobNameException}.java`, `application/mob/{MobTypeQueryService,MobTypeModifyService}.java`, `application/mob/provided/{MobTypeFinder,MobTypeEditor,MobTypeSaveRequest,MobFixedDropRequest,MobTierDropRequest,MobTypeView,MobTypeInUseException}.java`, `application/mob/required/MobTypeRepository.java`
- Modify: `domain/shared/Realm.java`, `orm.xml`, `application/item/required/ItemTypeRepository.java`, `application/item/ItemTypeModifyService.java`
- Test: `domain/shared/RealmTest`(추가), `domain/mob/{MobStatsTest,MobTypeTest,MobFixture}`, `application/mob/required/MobTypeRepositoryTest`, `application/mob/provided/MobTypeEditorTest`, `application/item/provided/ItemTypeEditorTest`(추가)

**Interfaces:**
- Consumes: 4차 `Realm`, 3차 `ItemTypeFinder.findAllById`.
- Produces: D78 enum·`Realm.majorIndex()/beastGrade()`, D79 `MobStats.of`, D80 `MobType`·`MobTypeSaveInfo`·`MobFixedDrop`·`MobTierDrop`, `MobTypeFinder`(`find`, `view`, `viewAll`), `MobTypeEditor`(`MobTypeView create(@Valid MobTypeSaveRequest)`, `MobTypeView update(Long, @Valid MobTypeSaveRequest)`, `void delete(Long)`), `MobTypeView`(admin-tool.md `MobDetailResponse` 칸 — `placements` 는 Task 2가 채우고 여기서는 빈 목록), `MobTypeModifyService` 의 `private void checkDeletable(MobType)` 자리(Task 2가 배치 검사), `MobFixture`(D93).

- [ ] **Step 1: 도메인 테스트** — `RealmTest`: `majorIndex`(MORTAL 0, QI_13 1, FOUNDATION_EARLY 2), `beastGrade`(QI_7 1, QI_8 2, FOUNDATION_PEAK 3, MORTAL 예외). `MobStatsTest`: 표 테스트 — 청모랑(BEAST QI_2 NORMAL) `(49, 8, 8)`, 들토끼(BEAST QI_1 NORMAL) `(42, 7, 7)`, 적안 독사(BEAST QI_5 STRONG) `(100, 12, 15)`, 흑풍랑왕(BEAST QI_7 ELITE) `(240, 14, 14)`, 석괴뢰(PUPPET QI_11 STRONG) `(128, 24, 24)`, 쌍두 흑망(BEAST QI_12 BOSS 0.6) `(1700, 24, 19)`, 떠도는 음혼(GHOST QI_10 NORMAL) `(105, 8, 17)`, 산적 수선자(CULTIVATOR QI_6 NORMAL) `(77, 11, 13)`. `MobTypeTest`: `rankName`(청모랑 `1급`, 음혼 `연기 10층`), `effectiveRespawnSeconds`(일반 빈칸 90, 강적 180, 입력 30), 실패 문구 admin-tool.md 그대로 — `nameBlankFail`, `nameTooLongFail`(21자), `nameOrdinalFail`(`2.늑대`), `descriptionTooLongFail`, `mortalRealmFail`, `attackNameFail`(빈칸, 13자), `bossCoefficientMissingFail`·`bossCoefficientRangeFail`(0.3, 0.9), `coefficientOnNonBossFail`, `coefficientScaleFail`(0.55 → `공격 계수는 소수 첫째 자리까지 적으십시오.`), `bossRespawnFail`(빈칸, 1,799, 3,601), `respawnRangeFail`(9, 86,401), `fixedDropsTooManyFail`(6종), `fixedDropCountFail`(0, 100 → `고정 재료의 수는 1~99로 정하십시오: 토끼 가죽`), `duplicateFixedDropFail`, `tierDropsTooManyFail`(고급 11개 → `(고급)`), `duplicateTierDropFail`, 검사 순서(이름과 재등장이 함께 틀리면 이름 문구).
- [ ] **Step 2: 실패 확인 → 구현.** 없는 물건 id 는 도메인 전에 서비스가 3차 문구로 400(D80 `itemNames`).
- [ ] **Step 3: 리포지토리·서비스 테스트** — `MobTypeRepositoryTest`: `saveWithDrops`(flush·clear 뒤 고정 재료·계층 드롭 순서), `replaceDrops`(update 뒤 flush 오류 없음), `duplicateNameFail`. `MobTypeEditorTest`: `create`(view 의 `stats`·`rankName`·`kindName`·`tierName`·물건 이름), `duplicateNameIgnoringCaseFail`(409 타입), `unknownItemFail`(`물건 종류를 찾을 수 없습니다. id: 9`), `requiredEnumNullFail`(요청 생성자 `kind` null → `IllegalArgumentException`), `viewOutsideTransaction`, `viewAllByName`, `delete`. `ItemTypeEditorTest` 추가: `deleteItemInFixedDropFail`·`deleteItemInTierDropFail`(3차 문구 `쓰이는 물건은 지울 수 없습니다: 토끼 가죽`), `changeCategoryOfDropItemFail`.
- [ ] **Step 4: 구현 → 통과.**
- [ ] **Step 5: 검증** — `./gradlew build`(ArchUnit: `mob → item` 만, `item → mob` 없음).
- [ ] **Step 6: 커밋** — `feat(world-server): 몹 유형 도메인·스탯 식·드롭 표·관리 서비스`.

**완료 조건:** 위 테스트 통과, 스탯이 룰북 첫 지역 몹 표와 같음, 문구가 admin-tool.md 와 글자 하나까지 같음.
**progress.md 한 줄:** `mob` 슬라이스(MobType·드롭 표·enum 셋·MobStats), 표 셋, `Realm.majorIndex/beastGrade`, 드롭 물건 쓰이는 검사.

---

### Task 2: 몹 배치와 몹 관리 REST

**권장 모델:** opus

**목적:** D81·D82 나머지. 게임 쪽은 Task 5.

**Files:**
- Create: `domain/mob/MobPlacement.java`, `application/mob/{MobPlacementQueryService,MobPlacementModifyService}.java`, `application/mob/provided/{MobPlacementFinder,MobPlacementEditor,MobPlacementSaveRequest,MobPlacementView}.java`, `application/mob/required/MobPlacementRepository.java`, `adapter/webapi/{MobApi,MobPlacementApi}.java`, `adapter/webapi/dto/{MobSummaryResponse,MobDetailResponse,MobPlacementResponse}.java`
- Modify: `orm.xml`, `application/mob/{MobTypeModifyService,MobTypeQueryService}.java`(배치 검사·`placements`), `adapter/webapi/{RoomApi,ApiControllerAdvice}.java`, `adapter/webapi/dto/RoomDetailResponse.java`
- Test: `domain/mob/MobPlacementTest`, `application/mob/required/MobPlacementRepositoryTest`, `application/mob/provided/{MobPlacementEditorTest,MobTypeEditorTest(추가)}`, `adapter/webapi/{MobApiWebMvcTest,MobPlacementApiWebMvcTest,RoomApiWebMvcTest(추가),AdminApiIntegrationTest(추가)}`, `adapter/security/SecurityIntegrationTest`(추가 한 줄)

**Interfaces:**
- Consumes: Task 1, 1차 `RoomFinder.find`, `RoomDeletedEvent`.
- Produces: `MobPlacement`, `MobPlacementFinder.findAll()`(id 순, 몹 유형·드롭 표 fetch — `@EntityGraph(attributePaths = {"mobType", "mobType.fixedDrops", "mobType.tierDrops"})` 가 bag 둘로 실패하면 드롭 표는 `MobTypeFinder.find` 로 따로 읽고 `findAll` 은 `mobType` 만), `findAllInRoom(Long)` → `List<MobPlacementView>`, `MobPlacementEditor`, `MobTypeView.placements`, REST 표 아홉 줄(admin-tool.md `## 5차 REST API`).

- [ ] **Step 1: 도메인·서비스 테스트** — `MobPlacementTest`: `countRangeFail`(0, 11 → `몹 수는 1~10으로 정하십시오.`). `MobPlacementEditorTest`: `createAndFindInRoom`, `unknownRoomFail`(`방을 찾을 수 없습니다. id: 9`), `unknownMobFail`, `updateChangesMobAndCount`, `unknownPlacementFail`(`몹 배치를 찾을 수 없습니다. id: 9`), `roomDeletedRemovesPlacements`. `MobTypeEditorTest` 추가: `deletePlacedFail`(409 `배치된 몹은 지울 수 없습니다: 청모랑`), `viewListsPlacements`(방 이름).
- [ ] **Step 2: 웹 테스트(`@WebMvcTest` + 보안, 관리자·`X-MUD-Client: 1`)** — `MobApiWebMvcTest`: `POST` 201 본문(`rankName:"1급"`, `stats:{qiBlood:49, body:8, power:8}`, `effectiveRespawnSeconds:90`, `fixedDrops[0].itemTypeName`), 빠진 `description`·목록 → 빈 값, `kind:null` → 400 `요청 본문을 읽을 수 없습니다`, 잘못된 `tier` 400, 목록 `GET` 이름순·`placementCount`, 이름 중복 409, 배치된 몹 `DELETE` 409, 관리자 아님 403, `X-MUD-Client` 없음 403. `MobPlacementApiWebMvcTest`: `POST /rooms/{id}/mobs` 201, `PUT`, `DELETE` 204, `count` 11 → 400. `RoomApiWebMvcTest`: 상세에 `mobs`.
- [ ] **Step 3: 통합 테스트** — `AdminApiIntegrationTest.manageMobs`: 재료 아이템 → 몹 만들기 → 방에 배치 → 방 상세 `mobs` → 몹 삭제 409 → 재료 삭제 409 → 배치 삭제 → 몹 삭제 204, 몹 상세가 트랜잭션 밖에서 물건·방 이름을 냄.
- [ ] **Step 4: 구현 → 통과.**
- [ ] **Step 5: 검증** — `./gradlew build`(ArchUnit: `mob → item, room`).
- [ ] **Step 6: 커밋** — `feat(world-server): 몹 배치와 몹 관리 API`.

**완료 조건:** 위 테스트 통과, 응답 필드가 admin-tool.md 표와 같음.
**progress.md 한 줄:** `mob_placement`, `/mobs` 5개·`/rooms/{id}/mobs`·`/mob-placements/{id}` 2개, 방 상세 `mobs`, 409 둘, 방 삭제 때 배치 지움.

---

### Task 3: 현재 기혈 저장 — 깎인 기혈 열, `CharacterVitalsEditor`, 상태의 현재 기혈

**권장 모델:** opus

**목적:** D83 전부(4차 L4의 5차 몫). 교전이 쓰는 포트를 먼저 만든다.

**Files:**
- Create: `application/character/provided/CharacterVitalsEditor.java`
- Create(추가): `domain/character/QiBlood.java`
- Modify: `domain/character/PlayerCharacter.java`, `orm.xml`(`qi_blood_lost` nullable·insertable/updatable false), `application/character/required/CharacterRepository.java`(네이티브 둘), `application/character/CharacterModifyService.java`, `domain/cultivation/StatSheet.java`, `application/cultivation/CultivationQueryService.java`, `application/game/required/GameMessage.java`(`StatusMessage.of`), `application/game/CultivationTexts.java`
- Test: `domain/character/QiBloodTest`, `domain/cultivation/StatSheetTest`(추가), `application/character/provided/{CharacterVitalsEditorTest,CharacterRealmEditorTest(추가)}`, `application/character/required/CharacterRepositoryTest`(추가), `application/game/provided/GameConnectorTest`(추가)

**Interfaces:**
- Consumes: 4차 `CharacterRealmEditor.designate`, `StatSheet.of`.
- Produces: `QiBlood`(D83 네 함수), `CharacterRepository.writeQiBloodLost/readQiBloodLost`, `CharacterVitalsEditor`(D83 네 메서드, 없는 캐릭터 4차 문구), `StatSheet.qiBloodCurrent()` 와 4인자 `StatSheet.of(…, double qiBloodLost)`(3인자는 지우고 부르는 곳을 고침), `domain/combat/Damage.gauge(double)`(이 태스크에서 `Damage` 를 만들고 `gauge` 만 — Task 4가 나머지).

- [ ] **Step 1: 도메인 테스트** — `QiBloodTest`: `lostAfterDamageCapsAtMax`(최대 60, 50 깎인 데 20 더 → 60, 현재 0), `lostForOne`(현재 1 → 59), `demotionReducesLostByMaxDrop`(QI_7 깎인 30 → QI_1 지정: 최대 120 → 60, 깎인 0 → 현재 60 = min(90, 60)), `demotionKeepsLowCurrent`(QI_7 깎인 100(현재 20) → QI_1: 깎인 40, 현재 20), `promotionRaisesCurrent`(QI_1 깎인 10 → QI_7: 깎인 그대로 10, 현재 110). `StatSheetTest`: `qiBloodCurrentFromLost`(연기 1층 깎인 12.5 → 47.5), `qiBloodCurrentNotBelowZero`. `DamageTest`: `gauge`(0.4 → 1, 19.8 → 19, 0 → 0).
- [ ] **Step 2: 실패 확인 → 구현.**
- [ ] **Step 3: 서비스·리포지토리 테스트** — `CharacterRepositoryTest`: `legacyNullIsFull`(native `update player_character set qi_blood_lost = null` 뒤 `readQiBloodLost == 0`), `qiBloodLostRoundTrip`, `entitySaveDoesNotWriteQiBloodLost`(엔티티를 읽고 `writeQiBloodLost(30)` 뒤 엔티티 `moveTo` 저장·flush·clear → 깎인 30 그대로). `CharacterVitalsEditorTest`: `takeDamagePersists`(flush·clear 뒤 값), `restoreReturnsFalseWhenFull`, `unknownCharacterFail`, `vitalsWriteDoesNotTouchRealm`(같은 트랜잭션에서 캐릭터를 읽어 둔 뒤 `CharacterRealmEditor.designate(QI_7)` → `takeDamage` → flush·clear → 경지 `QI_7`·깎인 값 둘 다 남음), `designateDoesNotOverwriteLost`(`takeDamage` 뒤 미리 읽어 둔 엔티티로 `designate`(올림) → 깎인 값 그대로). `CharacterRealmEditorTest` 추가: `designateDownReducesLost`. `GameConnectorTest` 추가: `statusShowsCurrentQiBlood`(깎인 12.5 → `기혈  47 / 60`, `status.stats.qiBlood == {47, 60}`).
- [ ] **Step 4: 구현 → 통과.** 4차 테스트 기대값은 그대로(깎이지 않은 캐릭터).
- [ ] **Step 5: 검증** — `./gradlew build`.
- [ ] **Step 6: 커밋** — `feat(world-server): 깎인 기혈 저장과 상태의 현재 기혈`.

**완료 조건:** 위 테스트 통과, 4차 테스트 그대로.
**progress.md 한 줄:** `player_character.qi_blood_lost`(nullable, 엔티티는 읽기만·네이티브 쿼리로 씀), `QiBlood`, `CharacterVitalsEditor`, 경지 내림 조정, `StatSheet.qiBloodCurrent`, 상태 기혈 현재 값.

---

### Task 4: 교전 도메인 — 메모리 개념·피해·드롭 굴림·난수

**권장 모델:** opus

**목적:** D84 전부와 D93의 난수 쪽. 게임 연결은 Task 5~7. 순수 도메인 테스트로 규칙을 고정한다.

**Files:**
- Create: `domain/combat/{MobProfile,MobInstance,MobHit,Engagement,PlayerCombat,Corpse,Condition,CombatRules}.java`, `domain/mob/{DropLine,DropRoll}.java`, `support/config/RandomConfig.java`, `test/java/mudmk3/ScriptedRandom.java`
- Modify: `domain/combat/Damage.java`(Task 3 에 이어), `test/java/mudmk3/TestFakesConfiguration.java`
- Test: `domain/combat/{MobInstanceTest,PlayerCombatTest,CorpseTest,ConditionTest,DamageTest(추가),CombatRulesTest}`, `domain/mob/DropRollTest`

**Interfaces:**
- Consumes: Task 1 `MobType`·`MobStats`, 3차 `Pricing.unitBuyPrice`, `ItemType`.
- Produces: D84의 클래스·메서드 시그니처 그대로(Task 5~7 이 씀), `ScriptedRandom`(D93), `RandomGenerator` 빈.

- [ ] **Step 1: 테스트** —
  - `DamageTest`: `afterBody(12, 6) == 9`, `afterBody(5, 18) == 0`, `display`(0 → `0`, 0.3 → `1 미만`, 0.5 → `1`, 8.6 → `9`), `playerPower(5) == 12`, `playerPower(0) == 3`.
  - `CombatRulesTest`: `next(t, t+0.2초) == t+3초`, `next(t, t+7초) == t+9초`(밀린 것 몰아치지 않음).
  - `ConditionTest`: 경계 75%·74.9%·40%·15%·14.9%.
  - `MobInstanceTest`(청모랑 프로필, 시각 `MutableClock.START` 기준): `firstEngageSchedulesAfterOnePointFive`, `firstOpponentThreatOne`, `receiveAppliesBody`(위력 12 → 피해 8, 기혈 41), `targetSwitchNeedsOnePointOneTimes`(A 위협 10, B 11 → A 유지, B 11.1 → B), `targetFallsBackWhenRemoved`, `killerTopDamageTieEarlier`, `diesAndSchedulesRespawn`(90초), `disengageLastSchedulesRefill`, `refillAfterTenSeconds`(9.9초 아님, 10초 다 참·위협 비움), `reengageCancelsRefill`, `respawnWithNewProfile`.
  - `PlayerCombatTest`: `currentTargetFallsBackToFirst`, `readyAfterInterval`, `endAllSchedulesRefill`, `refillDue`.
  - `CorpseTest`: `name`(`청모랑의 시체`), `killerOnlyFirstSixtySeconds`, `lockedForRemaining`(26초 지남 → 34초), `expiresAfterThreeMinutes`, `takeFrom`.
  - `DropRollTest`(`ScriptedRandom`): `fixedAlways`, `stonesWhenRollUnderThirtyPercent`(재료 기준가 합 1.5 → 매입가 0.6, u 0.5 → `floor(0.9) = 0` → 1개), `noStonesWithoutFixed`(난수 소비 없음), `tierChanceTimesReward`(두목 고급 1.6 → 확정 1 + 0.6 굴림), `emptyTierConsumesNoRandom`, `pickByNextInt`, `skipsUnknownItem`, `mergesSameItem`, 순서 고정 재료 → 영석 → 고급 → 희귀 → 진귀.
- [ ] **Step 2: 실패 확인 → 구현.**
- [ ] **Step 3: 통과 → 검증** — `./gradlew build`(ArchUnit: `combat → mob`, 순환 없음).
- [ ] **Step 4: 커밋** — `feat(world-server): 교전 도메인 — 몹 인스턴스·교전 상태·시체·피해·드롭 굴림`.

**완료 조건:** 위 테스트 통과, 수치가 룰북(피해 흐름·몹 스탯·드롭 표·시체)과 같음.
**progress.md 한 줄:** `domain/combat` 아홉 클래스, `DropRoll`(난수 소비 순서), `RandomConfig`·`ScriptedRandom`.

---

### Task 5: 서버 틱과 몹 나타나기 — 틱 포트·스케줄러·메모리 저장소·배치 맞추기·재등장·방 보기 몹

**권장 모델:** opus

**목적:** D85 전부, D89의 방 보기·`보기 <몹>`, D93의 테스트 도우미. 교전은 Task 6.

**Files:**
- Create: `application/game/provided/GameTicker.java`, `application/game/{CombatEngine,CombatTexts}.java`, `application/game/required/{MobInstanceRepository,PlayerCombatRepository,CorpseRepository}.java`, `adapter/persistence/memory/InMemory{MobInstance,PlayerCombat,Corpse}Repository.java`, `adapter/scheduling/GameTickScheduler.java`, `support/config/SchedulingConfig.java`, `test/java/mudmk3/CombatMemoryReset.java`, `test/java/mudmk3/domain/mob/MobFixture.java`(Task 1 것에 더함)
- Modify: `application/game/{GameService,RoomViewAssembler,ItemCommandHandler}.java`, `application/game/required/GameMessage.java`(`RoomMessage.mobs/corpses`, `MobView`, `CorpseView`, `SystemMessage.combat`, `NoticeMessage.combat`), `resources/application.yml`, `test/resources/application-test.yml`, `test/java/mudmk3/ApplicationServiceTest.java`, `application/game/provided/BaseGameTest.java`
- Test: `application/game/provided/{CombatSpawnTest,GameConnectorTest(추가)}`, `adapter/scheduling/GameTickSchedulerTest`, `adapter/persistence/memory/InMemoryMobInstanceRepositoryTest`, `adapter/websocket/GameWebSocketIntegrationTest`(추가 한 줄: `room` JSON 에 `mobs`·`corpses`)

**Interfaces:**
- Consumes: Task 2 `MobPlacementFinder.findAll`, Task 4 `MobInstance`·`MobProfile`·`CombatRules`, 4차 `CharacterFinder.find(...).getRealm()`.
- Produces: `GameTicker.tick()`, `CombatEngine.tick(Instant, BiConsumer<String, GameMessage>)`(이 태스크는 순서 1·5와 몹 다시 차기만; 2·4·플레이어 다시 차기는 Task 6·7 이 채움), `CombatEngine.mobsInRoom(Long)`·`corpsesInRoom(Long)`, 메모리 포트 셋(D85), `RoomMessage.MobView`·`CorpseView`, `SystemMessage.combat`·`NoticeMessage.combat`, `BaseGameTest.tick()/advance/advanceAndTick/placeMob`.

- [ ] **Step 1: 테스트** —
  - `CombatSpawnTest`(BaseGameTest): `firstTickSpawnsAll`(`MobPlacementEditor` 를 직접 불러 배치 수 2 → 틱 전 방 보기 `mobs` 빈 목록 → `tick()` 뒤 `[{청모랑, "1급", null, 2, false}]` — `placeMob` 을 쓰지 않음), `spawnNotifiesRoom`(방 사람 notice `청모랑이 나타난다.` event `respawn` tone `combat`), `placementChangeRespawnsFresh`(수 2 → 1, 다음 틱에 1마리), `placementDeletedRemovesMobs`, `mobTypeEditKeepsLiveMob`(기준 경지 고침 → 살아 있는 몹 rank 그대로; 지금은 재등장을 Task 7 처치로 볼 수 없어, 배치 수를 바꿔 새로 나타난 몹이 새 rank 인지로 확인), `roomViewOrder`(플레이어 → NPC → 몹 → 바닥 순서 필드), `strongBadgeAndGrouping`(강적 하나·일반 둘 → 두 줄, tier `강적`), `unknowableRank`(범인이 축기 몹 → `헤아릴 수 없음`, 연기 1층이 보면 `3급`), `lookMob`(`보기 청모랑` → `청모랑 (1급) — 푸른 털의 늑대. 멀쩡하다.`, 강적 `적안 독사 (1급) [강적] — …`, 묘사 없음 `청모랑 (1급) — 멀쩡하다.`, `보기 2.청모랑`), `lookOrderPrefersMob`(같은 이름 바닥 물건보다 몹).
  - `GameTickSchedulerTest`: 단위 — `tick()` 이 포트를 한 번 부르고, 포트가 예외를 던져도 예외가 밖으로 나오지 않음.
  - `InMemoryMobInstanceRepositoryTest`: `findAllInRoom` 이 살아 있는 것만 (placementId, slot) 순.
  - 스프링 컨텍스트: 테스트 프로필에서 `GameTickScheduler` 빈이 없음(`ApplicationContext.getBeansOfType` 빈 맵).
- [ ] **Step 2: 실패 확인 → 구현.** `CombatMemoryReset` 을 `@ApplicationServiceTest` 에 붙이고 `BaseGameTest` 도우미를 더한다. `RoomViewAssembler` 를 쓰는 1~4차 테스트 기대값에 빈 `mobs`·`corpses`(D94).
- [ ] **Step 3: 통과 → 검증** — `./gradlew build`.
- [ ] **Step 4: 커밋** — `feat(world-server): 서버 틱과 몹 나타나기·방 보기 몹`.

**완료 조건:** 위 테스트 통과, 1~4차 테스트 통과, 테스트 중 스케줄러가 돌지 않음.
**progress.md 한 줄:** `GameTicker`·`GameTickScheduler`(`mud.game.tick-enabled`), 메모리 저장소 셋, 배치 맞추기, 방 보기 `mobs`·`corpses`, `보기 <몹>`, 테스트 도우미.

---

### Task 6: 공격과 틱 교전 — `공격`, 행동 처리, 위협, 교전 메시지, 교전 중 막힘, 끊김

**권장 모델:** opus

**목적:** D86 전부와 `상태` 전투 줄(D89). 처치 뒤 시체·질 때·다시 차기는 Task 7(이 태스크에서 몹 기혈이 0이 되면 Task 7 자리 `onMobKilled`/`onPlayerDefeated` 를 부르되, 이 태스크의 구현은 몹을 쓰러뜨리고 교전을 끝내는 것까지 — 처치 줄 `청모랑을 쓰러뜨렸습니다.` 포함 — 와 질 때 기혈 1·교전 끝·outcome 에 넣기까지. `GameService.wakeAtStart` 와 `수습` 은 Task 7, 그 전까지 `Command.Collect` 는 D86 의 임시 자리).

**Files:**
- Create: `application/game/CombatCommandHandler.java`
- Modify: `domain/game/{Command,CommandParser}.java`, `domain/technique/ReservedWords.java`, `application/game/{CombatEngine,CombatTexts,GameService,ItemCommandHandler,ShopCommandHandler}.java`
- Test: `domain/game/CommandParserTest`(추가), `domain/technique/ReservedWordsTest`(추가), `application/game/provided/{CombatTest,GameConnectorTest(추가)}`, `application/game/CombatTextsTest`

**Interfaces:**
- Consumes: Task 3 `CharacterVitalsEditor.takeDamage`, Task 4·5 전부, 4차 `CultivationFinder.stats`.
- Produces: `Command.Attack`·`Command.Collect`(파싱만, 수습은 Task 7), `CombatEngine.attack(OnlinePlayer, @Nullable ItemQuery, Instant, send)`, `CombatOutcome tick(Instant, send)`(D86), `isFighting(Long)`, `leave(Long, Instant)`, `entered(Long, Instant)`(Task 7 이 다시 차기를 붙임), `CombatTexts.statusLine`.

- [ ] **Step 1: 테스트** — 준비는 `placeMob` + 플레이어 경지 `setRealm`(4차 도우미). 시각은 `advanceAndTick`.
  - `CommandParserTest`: `공격 청모랑`, `공격 2.청모랑`, `공격`(null), `수습 청모랑`. `ReservedWordsTest`: `공격` 예약어.
  - `CombatTest`: `attackArgumentMissing`(`누구를 공격하시겠습니까? (예: 공격 늑대)`), `attackNotHere`(`이곳에 '곰'은 없습니다.`), `attackNpcFail`(`왕씨는 공격할 수 있는 상대가 아닙니다.`), `attackPlayerFail`(`다른 사람과는 아직 싸울 수 없습니다.`), `attackStartsAndHitsImmediately`(연기 1층이 청모랑: `청모랑과 싸움이 벌어진다.` 다음 `청모랑에게 일격을 날린다. (피해 8)`, 같은 방 B notice `한백이 청모랑과 싸우기 시작한다.` event `engage`), `mobHitsAfterOnePointFive`(1초 틱 뒤 없음, 2초 틱에 `청모랑이 물어뜯기로 덮친다! (피해 5, 기혈 55/60)`), `playerAutoAttacksEveryThreeSeconds`, `attackAgainSameTarget`(`이미 청모랑과 싸우고 있습니다.`), `attackSecondMobSwitchesTargetWithoutExtraHit`, `attackOnCooldownWaits`(3초 안에 다른 몹 처치 뒤 새 `공격` → 즉시 치지 않고 다음 틱), `killEndsCombat`(`청모랑을 쓰러뜨렸습니다.`, 교전하지 않은 B `한백이 청모랑을 쓰러뜨렸다.` event `kill`, 그 뒤 이동 가능), `coFighterSeesFell`(함께 싸운 B `청모랑이 쓰러졌다.`), `mobSwitchesToRemainingPlayer`, `disconnectEndsEngagement`(끊긴 A 대신 B 를 침, 몹 상대 없으면 10초 뒤 다 참), `placementRemovedMidCombatEndsCombat`, `roomDeletedMidCombatRelocatesAndEnds`(`onRoomDeleted` 직접 호출, 4차 관례), `deadMobActionCancelled`(같은 틱에 A 의 일격이 먼저 처리되어 몹이 쓰러지면 몹 줄 없음), `defeatedPlayerActionCancelled`, `lateTickDoesNotBurst`(10초 밀린 한 틱에 공격 한 번씩), `statusCombatLine`(`[전투 중] 상대: 청모랑(다침), 청모랑 2`), `damageDisplayLessThanOne`(육신이 높아 피해 0.4 → `(피해 1 미만, …)`), `mortalPowerThree`.
  - `GameConnectorTest` 추가(막힘): 교전 중 `북` → `싸우는 중에는 움직일 수 없습니다.`, `종료` → `싸우는 중에는 떠날 수 없습니다.`(연결 그대로), `줍기`·`주기`·`대화`·`읽기`·`목록`·`사기`·`팔기`·`값` → `싸우는 중에는 할 수 없습니다.`(범인이어도 이 문구가 먼저), `소지품`·`상태`·`말하기`·`버리기` 는 됨.
  - `CombatTextsTest`: 조사(`들토끼3와`, `청모랑과`, `물어뜯기로`, `두 머리 물기로`), 피해·기혈 표시.
- [ ] **Step 2: 실패 확인 → 구현.**
- [ ] **Step 3: 통과 → 검증** — `./gradlew build`.
- [ ] **Step 4: 커밋** — `feat(world-server): 공격과 틱 교전·교전 중 막힘`.

**완료 조건:** 위 테스트 통과, 문구가 command.md 와 글자 그대로.
**progress.md 한 줄:** `Command.Attack/Collect`, `공격` 예약어, `CombatCommandHandler`, 행동 처리(정렬 키·취소·밀림), 위협·처치 줄, 막힘, 끊김, 상태 전투 줄.

---

### Task 7: 처치 보상·질 때·교전 뒤 기혈 — 시체·`수습`·재등장 확인·도움말

**권장 모델:** opus

**목적:** D87·D88·D96, 재등장 줄(Task 5 순서 5를 처치와 이어 확인), D92 도움말.

**Files:**
- Modify: `application/game/{CombatEngine,CombatCommandHandler,CombatTexts,GameService}.java`
- Test: `application/game/provided/{CombatRewardTest,CombatDefeatTest,GameConnectorTest(추가)}`

**Interfaces:**
- Consumes: Task 4 `DropRoll`·`Corpse`, Task 6 처리 자리, 3차 `InventoryEditor.addUpTo`, `ItemTypeFinder.spiritStone`, 1차 `CharacterMover.moveTo`, `WorldSettingsFinder.findStartRoom`.
- Produces: 처치 → 시체, `수습`(`Command.Collect` 임시 자리를 바꿈), 시체 사라짐(순서 4), 재등장 알림(순서 5), `GameService.wakeAtStart`(D88 — `GameService.tick()` 이 `CombatOutcome.defeatedCharacterIds()` 마다 부름), 플레이어 다시 차기(순서 3), `HELP` 5차.

- [ ] **Step 1: 테스트** —
  - `CombatRewardTest`: `corpseShownAfterKill`(방 보기 `corpses == [{청모랑의 시체, true}]`, B 에게는 `false`), `collectFixedDrops`(`청모랑의 시체를 수습했습니다: 청모랑 가죽 1, 늑대 이빨 1`, 시체 사라짐, B notice `한백이 청모랑의 시체를 수습한다.` event `collect` tone `item`), `collectWithStones`(`ScriptedRandom` 0.1·0.5 → `…, 하품 영석 1`), `collectTierDrop`(고정 재료가 있는 몹이라 영석 굴림이 먼저: `enqueueDoubles(0.9, 0.01)`(영석 안 됨 → 고급 0.01 < 0.08) · `enqueueInts(1)` → 고급 목록 둘째 물건), `killerIsTopDamageDealer`, `otherPlayerWaitsForCorpse`(`그 시체는 아직 다른 사람의 몫입니다. (34초 뒤 수습할 수 있습니다)`, 60초 뒤 됨), `collectPartialWhenBagFull`(거둔 줄 + `저물대에 자리가 없습니다. 남은 것: 늑대 이빨 1`, 칸을 비우고 다시 수습), `collectNothingFits`(둘째 줄만), `emptyCorpse`(`청모랑의 시체에는 거둘 것이 없습니다.`), `collectArgumentMissing`(`무엇을 수습하시겠습니까? (예: 수습 늑대)`), `collectNotHere`, `collectInCombatFail`, `corpseExpiresWithLeftovers`(3분 틱 뒤 방 보기에 없음, 알림 없음), `respawnAfterTierTime`(일반 89초 틱 없음, 90초 `청모랑이 나타난다.`, 기혈 가득), `respawnOverrideSeconds`(재등장 10초), `lookCorpseNotYet`(`보기 청모랑의 시체` → `아직 자세히 볼 수 없습니다.`), `collectSkipsDeletedItemType`(시체를 만든 뒤 드롭 표에서 빼고 그 물건 종류를 지움 → 수습 줄에 없고 오류 없음).
  - `CombatDefeatTest`(쌍두 흑망 vs 연기 1층): `defeatMovesToStartWithOne`(`기혈이 다했습니다. 눈앞이 캄캄해진다…` 다음 `{시작 방}에서 깨어났습니다. (기혈 1/60)`, 시작 방 `room`, 옛 방 B notice `한백이 쓰러진다.` event `defeated`, 시작 방 C `appear`, `상태` 기혈 `{1, 60}`), `defeatInStartRoomStays`, `defeatEndsAllEngagements`(몹 둘과 싸우다 짐 → 둘 다 상대 없음 → 10초 뒤 다 참), `refillTenSecondsAfterCombat`(9.5초 아님, 10초 `숨을 고르자 기운이 돌아온다. (기혈 60/60)`), `noRefillMessageWhenFull`, `reengageCancelsPlayerRefill`, `hurtPlayerRefillsAfterEnter`(깎인 채 끊고 다시 접속 → 10초 뒤 참).
  - `GameConnectorTest` 추가: `helpText` 를 D92 문구로.
- [ ] **Step 2: 실패 확인 → 구현.**
- [ ] **Step 3: 통과 → 검증** — `./gradlew build`.
- [ ] **Step 4: WebSocket 확인** — `GameWebSocketIntegrationTest.attackOverWebSocket`. 이 테스트는 실제 `Clock`·실제 난수·커밋되는 DB 다(`@ApplicationServiceTest` 가 아님). 그래서 시간·난수에 기대지 않는다: 몹 `들토끼`(기혈 42·육신 7)를 배치한 뒤 `GameTicker.tick()` 을 **한 번** 직접 불러 나타나게 하고(테스트 프로필이라 스케줄러 없음), 플레이어를 `CharacterRealmEditor.designate(FOUNDATION_EARLY)`(위력 64.2 → 한 번에 처치)로 정해 `공격 들토끼` 한 명령으로 `system` `tone:"combat"` 의 시작·일격·처치 JSON 을 받는다(몹 행동 시각 전). `수습 들토끼` 는 줄이 `들토끼의 시체를 수습했습니다: 토끼 가죽 1` 로 시작하는지만 본다(영석은 실제 난수). 정리: 기존 `cleanUp` 의 TRUNCATE 가 `mob_*` 표와 `qi_blood_lost` 를 함께 지우므로 DB 는 더할 것이 없고, `cleanUp` 끝에 메모리 저장소 셋 `deleteAll()`(`MobInstanceRepository`·`PlayerCombatRepository`·`CorpseRepository` 주입)을 더한다.
- [ ] **Step 5: 커밋** — `feat(world-server): 처치·시체·수습, 질 때 최소 처리, 교전 뒤 기혈`.

**완료 조건:** 위 테스트 통과, 문구가 command.md·룰북과 글자 그대로.
**progress.md 한 줄:** 시체·`수습`(부분·빈 시체·60초·3분), 재등장 알림, 질 때(시작 방·기혈 1), 다시 차기(10초, 접속 때), 도움말 5차.

---

### Task 8: admin-console — 몹 화면·드롭 표·방의 몹 배치

**권장 모델:** opus

**목적:** D91 전부.

**Files:**
- Create: `app/mobs/page.tsx`, `app/mob/page.tsx`, `src/components/{MobForm,DropTableEditor,MobPlacementsEditor}.tsx`, `src/lib/mobs.ts`
- Modify: `src/lib/api.ts`, `src/components/TopBar.tsx`, `app/room/page.tsx`
- Test: `src/lib/mobs.test.ts`, `src/components/{MobForm,DropTableEditor,MobPlacementsEditor}.test.tsx`

**Interfaces:**
- Consumes: Task 2 REST(admin-tool.md 모양), 4차 `GET /api/admin/realms`, 3차 아이템 목록.
- Produces: `/mobs/`·`/mob/`, 방 화면 `몹 배치`, 공통 바 `몹`.

- [ ] **Step 1: 테스트** — `mobs.test`: `formatRespawn`(30 → `30초`, 90 → `1분 30초`, 1,800 → `30분`), 코드·이름 표. `MobForm.test`: 체급이 두목일 때만 `공격 계수`(기본 0.6), 기준 경지 선택에 범인 없음, 빈 `재등장` 은 `null` 로 보냄, 저장 400 `detail` 을 보임, 고칠 때 스탯 줄 `기혈 49 · 육신 8 · 위력 8`·배치된 방 링크, 삭제 확인 문구 `'청모랑' 몹을 지울까요?`, 만들기 성공 뒤 `/mob/?id=` 로 감. `DropTableEditor.test`: 고정 재료 추가·삭제·수, 계층 셋 칸, 요청 모양 `fixedDrops`·`tierDrops`. `MobPlacementsEditor.test`: 추가(몹 이름순 선택, 수 기본 1), 수 고치기 `PUT`, 삭제, 서버 `detail`.
- [ ] **Step 2: 구현 → 통과.**
- [ ] **Step 3: 검증** — admin-console 검증 명령.
- [ ] **Step 4: 커밋** — `feat(admin-console): 몹 정의·드롭 표·방의 몹 배치`.

**완료 조건:** 테스트·타입 검사·정적 빌드 통과.
**progress.md 한 줄:** `/mobs/`·`/mob/`, `MobForm`·`DropTableEditor`·`MobPlacementsEditor`, 방 화면 `몹 배치`, 공통 바 `몹`.

---

### Task 9: play-client — 몹·시체 줄, `combat` 색, 자동완성

**권장 모델:** opus

**목적:** D90·D92(클라이언트 쪽).

**Files:**
- Modify: `src/lib/protocol.ts`(`Mob`, `Corpse`, `RoomMessage.mobs/corpses`, tone `combat`, notice event 넷·`collect`), `src/lib/render.ts`, `src/lib/complete.ts`, `src/components/Terminal.tsx`(방 이름 후보에 몹·시체), `app/globals.css`
- Test: `src/lib/{render,complete}.test.ts`, `src/components/Terminal.test.tsx`

**Interfaces:**
- Consumes: Task 5·6·7 메시지 모양(platform.md 5차).
- Produces: 스타일 `mob`·`combat`.

- [ ] **Step 1: 테스트** — `render.test`: 방 보기 줄 순서(사람 → NPC → 몹 → 시체 → 바닥), `청모랑 (1급) ×2`, `적안 독사 (1급) [강적]`, `청모랑 (1급, 전투 중)`, `흑망 (헤아릴 수 없음) [두목]`, `청모랑의 시체 (수습 가능)`, 시체 수습 불가면 이름만, 몹·시체 줄 style `mob`, `system`·`notice` tone `combat` → style `combat`. `complete.test`: `COMMANDS` 가 D92 순서, `공` + Tab → `공격`, `공격 청` + Tab → `청모랑`, `수습 청` → `청모랑의 시체`(후보 순서 사람·NPC → 몹 → 시체 → 바닥 → 소지품, 중복 제거). `Terminal.test`: 새 `room` 이 오면 몹 후보 갱신.
- [ ] **Step 2: 구현 → 통과.**
- [ ] **Step 3: 검증** — play-client 검증 명령.
- [ ] **Step 4: 커밋** — `feat(play-client): 몹·시체 줄과 교전 색, 5차 명령 자동완성`.

**완료 조건:** 테스트·타입 검사·정적 빌드 통과.
**progress.md 한 줄:** `mob`·`combat` 스타일(색 platform.md), `room.mobs/corpses` 렌더, COMMANDS 5차, 몹·시체 이름 후보.

---

### Task 10: 문서 — 스펙 `(제안)` 반영과 README

**권장 모델:** sonnet

**목적:** 구현된 결정과 실행 안내를 문서에 옮긴다. 코드를 바꾸지 않는다.

**Files:**
- Modify: `docs/spec/system/{index,command,admin-tool,platform,npc}.md`, `README.md`

**내용:**
- 스펙: `index.md` `### 5차 범위` 에 구현 날짜·진행 링크(`../../impl/iteration-5/progress.md`) 한 줄. 스펙에 없는 구현 세부를 해당 절 끝에 `(제안)` 으로: D84(`CombatRules` 값·`DropRoll` 난수 소비 순서 — platform.md 시계와 난수 줄), D85(`mud.game.tick-enabled`, 스케줄러 예외 처리), D86(행동 정렬 키, 막힘이 범인 막힘보다 먼저), D89(묶음 키), D93(테스트가 틱을 직접 부름 — platform.md 서버 틱 줄에 이미 있으면 그대로). progress.md 메모의 '계획과 다른 점' 을 코드 기준으로 적는다. 룰북에 없는 새 문구는 "룰북에 아직 없음" 표시 확인. 룰북 파일은 고치지 않는다.
- README: 모듈 소개에 5차 한 줄. `## 5차 시나리오 — 몹·공격·처치·재등장`(4차 시나리오 다음): 관리자 `아이템` 에서 `토끼 가죽`(재료, 기준가 1) → `몹` 에서 `들토끼`(요수·연기 1층·일반·`뒷발차기`·재등장 10초·고정 재료 토끼 가죽 1)와 `쌍두 흑망`(요수·축기 대원만·두목·계수 0.6·재등장 1,800초) → `방` 에서 광장에 들토끼 1, 다른 방에 쌍두 흑망 1 → play-client 방 보기·`보기 들토끼` → 관리자 `캐릭터` 에서 연기 7층 지정 → `공격 들토끼` → 교전 줄 → `수습 들토끼` → 10초 뒤 재등장 → 연기 1층 지정 → 쌍두 흑망 `공격` → 짐(시작 방·기혈 1) → 10초 뒤 기혈 참. `## 되는 명령` 에 `공격 <대상>`, `수습 <시체>`, `보기 <몹>` 줄과 `도움말`(D92). 수동 확인 체크리스트 5차(몹 줄·시체 줄 색, 교전 줄 색, `공격`·`수습` Tab, 관리 몹 화면·드롭 표·방 몹 배치, 질 때 화면). 스키마 한 줄(`ddl-auto=update` 가 `mob_type`·`mob_fixed_drop`·`mob_tier_drop`·`mob_placement` 표와 `player_character.qi_blood_lost` 열을 더함, 기존 캐릭터는 기혈 가득, 서버 틱 1초, 몹·시체는 메모리라 다시 시작하면 처음 상태). 스모크 절은 Task 11이 고친다.
- [ ] **Step 1: 수정.** 명령 이름·문구는 `GameService.HELP`·`CombatTexts`·`CommandParser`·admin 화면 문구와 대조한다.
- [ ] **Step 2: 커밋** — `docs(spec): 5차 제안 결정 반영` 과 `docs(infra): README에 5차 시나리오·명령 반영`(둘로 나눠도 됨).

**완료 조건:** 위 D 각각이 스펙 어딘가에 `(제안)` 으로 있거나 이미 있는 스펙 문장과 같음, README 명령·문구가 코드와 같음, 1~4차 내용 유지.
**progress.md 한 줄:** 스펙에 옮긴 D 목록, 코드와 대조한 결과, README 5차 절.

---

### Task 11: 통합 확인 — 스모크 5차 시나리오, compose 한 번

**권장 모델:** opus

**목적:** 실제 컨테이너(실제 1초 틱)에서 index.md 5차 완료 시나리오를 자동으로 확인한다. **이 이터레이션에서 Docker를 쓰는 유일한 태스크다.**

**Files:**
- Modify: `scripts/smoke-test.mjs`, `README.md`(스모크 절 5차 항목), `docs/impl/iteration-5/progress.md`

**1~4차 단계 고침 (D94):** 단계 24·35 `도움말` 기대를 D92 문구로. 그 밖은 그대로다(새 캐릭터 흐름·상태 기대값이 바뀌지 않음).

**5차 단계** (단계 35 뒤, 이름에 실행 번호 `<n>`; 몹 행동은 실제 시간이라 `waitFor(client, 조건, 제한 ms)` 도우미로 기다림 — 제한은 기대 시각 + 5초):
36. 관리자 — 아이템 `토끼 가죽<n>`(`MATERIAL`, 기준가 1). 몹 `들토끼<n>`(BEAST, `QI_1`, NORMAL, `뒷발차기`, 재등장 10, 고정 재료 `[{토끼 가죽<n>, 1}]`) → 201 `stats {42,7,7}`, `rankName "1급"`. 두목 계수 없이 → 400 `두목의 공격 계수는 0.4~0.8로 정하십시오.`. 몹 `흑망<n>`(BEAST, `FOUNDATION_PEAK`, BOSS, 0.6, 1,800, 드롭 없음) → 201 `stats {5400, 78, 57}`. 방 `사냥터<n>`(광장과 같은 지역, 광장 동 ↔ 서 출구) 만들기. 광장에 들토끼 1, 사냥터에 흑망 1 배치. `토끼 가죽<n>` 삭제 → 409, `들토끼<n>` 삭제 → 409.
37. E `보기` 를 `waitFor` 로 되풀이(≤ 5초, 배치는 다음 틱) → `mobs` 에 `{name:들토끼<n>, rank:"1급", tier:null, count:1, fighting:false}`. E `보기 들토끼<n>` → `들토끼<n> (1급) — 멀쩡하다.`. E `공격` → 인자 없음 문구. E `공격 곰` → `이곳에 '곰'은 없습니다.`. E `공격 <단계 12 NPC>` → `… 공격할 수 있는 상대가 아닙니다.`.
38. 관리자 — E `QI_7`(growth). E `공격 들토끼<n>` → combat `들토끼<n>와 싸움이 벌어진다.`·`들토끼<n>에게 일격을 날린다. (피해 19)`. E `동` → `싸우는 중에는 움직일 수 없습니다.`, E `종료` → `싸우는 중에는 떠날 수 없습니다.`(연결 그대로). E `상태` → text 에 `[전투 중] 상대: 들토끼<n>` 이 들어 있음(기혈 상태 괄호는 보지 않음). 기다림(≤ 12초): `들토끼<n>가 뒷발차기로 덮친다! (피해 1, 기혈 119/120)` 와 `들토끼<n>를 쓰러뜨렸습니다.`.
39. E `보기` → `corpses == [{name:들토끼<n>의 시체, collectible:true}]`, `mobs` 빔. 칸을 넉넉히: E `버리기 장춘공<n> 옥간`(단계 30에서 남은 1개). E `수습 들토끼<n>` → `들토끼<n>의 시체를 수습했습니다: 토끼 가죽<n> 1` 로 시작(영석은 무작위라 뒤에 붙을 수 있음). 칸이 모자라 둘째 줄 `저물대에 자리가 없습니다. 남은 것: …` 이 붙어도 통과로 본다(첫 줄에 토끼 가죽이 있으면 됨). E `소지품` items 에 `토끼 가죽<n>`.
40. 기다림(≤ 16초): E notice `들토끼<n>가 나타난다.`(event `respawn`)와 `숨을 고르자 기운이 돌아온다. (기혈 120/120)`. E `보기` → 들토끼 count 1.
41. 관리자 — E `QI_1`(growth). E `동` → 사냥터 `mobs` 에 `{흑망<n>, "3급", "두목", 1, false}`. E `공격 흑망<n>` → 기다림(≤ 10초): `기혈이 다했습니다. 눈앞이 캄캄해진다…`, `광장<n>에서 깨어났습니다. (기혈 1/60)`(시작 방 이름 변수), 이어 `room.room.name` 이 시작 방. E `상태` → `qiBlood {current:1, max:60}`. 기다림(≤ 15초): `숨을 고르자 기운이 돌아온다. (기혈 60/60)`, `상태` `{60,60}`.
42. E `도움말` 에 `공격 <대상> · 수습 <시체>`. 관리자 — 이 실행의 몹 배치 둘 삭제 → `waitFor` 폴링(≤ 5초)으로 광장 `mobs` 에 `들토끼<n>` 없음. 시작 소지품을 스모크 시작 때 읽어 둔 값으로 되돌린다(아래).

- [ ] **Step 1: 스크립트 수정** — 고침과 36~42를 더한다(Node 26 내장 `fetch`·`WebSocket` 만). 머리 주석 '1~4차' 를 '1~5차' 로. 고정 시간 대기는 쓰지 않고 모두 `waitFor`(메시지가 오거나 `보기` 를 되풀이해 조건이 맞을 때까지, 제한 시간) 로 한다. **시작 소지품 대비:** 기존 pgdata 의 시작 소지품이 비어 있지 않으면 E 의 칸이 모자랄 수 있으므로, 스모크 맨 앞(단계 1 뒤)에서 지금 시작 소지품을 읽어 두고 `[]` 로 정한 뒤 실행하고, 단계 42 끝에서 읽어 둔 값으로 되돌린다(그러면 단계 18 이 읽는 '처음 값' 도 `[]` 라 단계 25 는 `[]` 로 되돌리고, 4차 단계 34 주석의 '기본 2칸' 가정과도 맞음).
- [ ] **Step 2: 모듈 검증** — 세 모듈 검증 명령을 다시 돌려 통과 확인.
- [ ] **Step 3: compose 한 번** — 시작 전 `df -h .`·`docker system df` 를 메모에(10GB 미만이면 멈춤). Global Constraints 스모크 방법대로 `docker compose build` **한 번** → `up -d`(셸 값 덮어쓰기) → 기동 로그에 스키마 오류 없음, 틱 오류 로그 없음 확인(기존 `pgdata` 에 `qi_blood_lost` 열이 생기고 기존 캐릭터는 null) → `source "$NVM_DIR/nvm.sh" && nvm use 26.13.1 && node scripts/smoke-test.mjs` → `SMOKE OK`. 실패하면 원인을 고치고(해당 모듈 테스트 통과) 바뀐 서비스만 `docker compose up -d --build <서비스>`(전체 재빌드 반복 금지). PostgreSQL CHECK 로 몹 저장이 막히면 4차 규칙(admin-tool.md)대로 원인 제약을 기록하고 리드에게 보고.
- [ ] **Step 4: 되돌리기** — 관리 API로 시작 방을 id 1로, 시작 소지품을 `[]` 로 되돌리고 `GET /api/admin/world`·`/world/starting-items` 로 확인한다. 이 실행의 몹 배치가 남지 않았는지 확인한다.
- [ ] **Step 5: 정리** — `docker compose down`(볼륨 남김) → `docker image prune -f` → `docker builder prune -f` → `world-server/build`, `play-client/{out,.next}`, `admin-console/{out,.next}` 삭제(`git check-ignore` 로 먼저 확인). `mud-mk3-*` 이미지·`pgdata`·`node_modules` 는 지우지 않는다. 정리 뒤 `df -h .`·`docker system df` 를 메모에.
- [ ] **Step 6: README 스모크 절** — 5차 항목 두 줄(몹 정의·드롭 표·배치·공격·틱 교전·처치·수습 / 재등장·질 때·교전 뒤 기혈, 스모크는 실제 틱을 기다려 약 1분 더 걸림).
- [ ] **Step 7: progress.md** — 결과(통과 여부, 고친 것, 남은 수동 확인), 상태를 "5차 이터레이션 구현 완료" 로.
- [ ] **Step 8: 커밋** — `test(infra): 5차 스모크 시나리오(몹·공격·틱 교전·수습·재등장·질 때)`.

**완료 조건:** compose 환경에서 단계 1~42 `SMOKE OK`(기존 pgdata), 세 모듈 검증 통과, 시작 방 id 1·시작 소지품 `[]`, 정리 뒤 dangling 이미지 없음, pgdata 남음.
**progress.md 한 줄:** 스모크 고침·단계 36~42, compose 빌드 한 번, 디스크 전후 값, 되돌림 확인.

**완료 시나리오 대응** (index.md 5차 완료 시나리오 → 스모크 단계): 재료·몹 정의·드롭 표·배치 36 · 몹 보기 37 · 경지 지정 38 · 공격·틱 교전 메시지 38 · 처치·시체 38·39 · 수습 획득 39 · 재등장 40 · 강한 몹에게 짐 41 · 교전 뒤 기혈 40·41.

---

### Task 12: 최종 전체 리뷰

**권장 모델:** opus

**목적:** 5차 전체(main 의 5차 커밋 전부)를 스펙·룰북·이 계획과 대조한다. 코드를 고치면 따로 커밋한다.

- [ ] **Step 1: 리뷰** — `superpowers:requesting-code-review` 로 5차 첫 커밋 앞부터 HEAD 까지. 볼 것: Review Focus 다섯 줄이 테스트로 덮였는지, 문구가 룰북·스펙과 글자 그대로인지(`CombatTexts`, `GameService` 상수, admin 화면), 몹 스탯·드롭 수치, 게임 락 밖에서 메모리 상태를 건드리는 곳이 없는지(관리 REST·스케줄러), 메모리 객체가 엔티티를 들고 있지 않은지, ArchUnit 의존(D77), 틱의 예외가 다음 틱을 막지 않는지, PostgreSQL enum CHECK(새 enum 열·값), `(제안)` 반영 누락.
- [ ] **Step 2: Critical·Important 고침** — 해당 모듈 검증 통과 후 커밋 `fix(<module>): …`. Docker는 다시 빌드하지 않는다(다시 스모크가 필요하면 리드에게 묻는다).
- [ ] **Step 3: progress.md** — 리뷰 결과·고친 것·다음으로 미룬 Minor(`## 다음 할 일`), 창호 확인 항목(`(제안)` 결정 — 특히 임시 `공격`·교전 뒤 10초 회복·도주 없음·질 때 최소 처리, 브라우저 수동 확인).
- [ ] **Step 4: 커밋** — `docs(impl): 5차 이터레이션 완료 기록`.

**완료 조건:** Critical·Important 0, progress.md 완료 기록.
**progress.md 한 줄:** 최종 리뷰 결과와 고친 것.

---

## 태스크 의존 순서

```
1 → 2 ───────────────────────┬→ 8
3 (1과 무관, 1·2 뒤에 해도 됨) │
1,3 → 4 → 5 → 6 → 7 → 9 ─────┴→ 10 → 11 → 12
```

- 순서대로 하면 된다: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12. 나란히 하지 않는다(여러 태스크가 `orm.xml`·`GameService`·`CombatEngine`·`GameMessage` 를 고친다).
- 8은 2 뒤(REST 모양), 9는 7 뒤(실제 메시지와 맞춤). 10은 구현된 값을 옮기므로 9 뒤, 11은 10 뒤, 12는 마지막.
- 각 태스크는 앞 태스크가 커밋된 상태에서 혼자 검증 명령이 통과한다(Task 5는 교전 없이 나타나기만, Task 6은 처치 줄까지 하고 시체는 Task 7).

## 4차에서 미룬 Minor

**5차에서 함께 고치는 것** (5차 작업과 직접 부딪침):
- 경지 읽기·`braceletOpen` 중복(`ItemCommandHandler`·`ShopCommandHandler`·`BaseGameTest.give`) → Task 6이 두 핸들러 처음에 교전 막힘을 넣으며 '이 캐릭터의 경지·교전 여부' 를 읽는 package-private 도우미 하나로 모은다(동작은 그대로, 4차 테스트로 확인).
- 테스트 빈틈 '다시 연결하면 소지품 후보가 빔'(`Terminal.test.tsx`) → Task 9가 몹 후보 갱신 테스트를 쓰며 함께 더한다.

**그대로 둠** (5차 작업과 부딪치지 않음): `ScrollItem` 위치, `existsByCharacterIdAndTechniqueId`, 영근 확인 때 시작 방 없음 테스트, `dropsStaleBagGradeCheck` 보강, 영근·자질 버림 중복, `보기 <옥간>` 쿼리 수, `designateRealm` javadoc, `manageCharacters` 단언, index.md `**구현:**` 줄 위치, `CharacterRealmForm` 이중 상태, 이전 태스크 선택 항목(T1-M2, T2-M2, T2-M3, T4-M1, T9-M4, T10-M3) — `iteration-4/progress.md` `## 다음 할 일` 그대로.
