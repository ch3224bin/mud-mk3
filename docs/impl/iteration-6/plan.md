# 6차 이터레이션 구현 계획 — 공격 신통 쓰기 + 깨달음 쌓기

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 관리자는 admin-console에서 신통마다 이펙트 문장(1~5개)과 공격 신통의 단계별 값(기본 위력·신통 계수)을 정한다. 플레이어는 play-client에서 배운 공격 신통을 `<신통> [대상]` 으로 몹에게 써서 교전을 시작하거나 이어 가고, 이펙트 문장 하나와 피해를 보고, 법력을 쓰고(자연 회복), 공통·재사용 대기시간과 시전 시간을 따르며, 깨달음이 쌓이는 것을 `상태`·`공법` 으로 본다.

**Architecture:** 5차 구조를 넓힌다. `technique` 슬라이스의 `Ability` 에 값 컬렉션 둘(단계별 값·이펙트 문장)을 더하고, `character` 슬라이스에 깎인 법력(5차 깎인 기혈과 같은 전용 네이티브 쿼리), `cultivation` 슬라이스에 깨달음 표와 순수 계산을 더한다. 신통 쓰기는 `application/game` 의 package-private 컴포넌트로 나눈다: 5차 `CombatEngine` 에서 교전 시작·일격·처치 공통 경로를 `CombatActions` 로 떼어 내고(이슈 #15), 그 위에 `AbilityResolver`(이름 → 배운 신통)·`AbilityCaster`(발동 1~8)·`AbilityCommandHandler`(명령 검사·시전 시작·`중지`)를 둔다. 대기시간·시전은 메모리(`domain/combat/AbilityTimers` + `application/game/required` 포트 + `adapter/persistence/memory`), 법력 회복과 시전 발동은 5차 서버 틱 순서에 끼운다. 피해는 '위력 × 받는 쪽 배율(6차 늘 ×1) → 육신 층 → 기혈' 한 곳(`Damage.afterLayers`)을 5차 공격·몹 공격과 함께 지난다(상성이 붙을 자리).

**Tech Stack:** 1~5차와 같다 — Java 25 · Spring Boot 4.1.2 · Gradle 9.9.1 Kotlin DSL · PostgreSQL 18.6 / H2(테스트) · Next.js 16.4.6 · React 19.3.1 · TypeScript 7.0.2 · Vitest 5.1.0 · Docker Compose. **새 의존성 없음.**

**Spec:** 아래 `## Spec`. 계획은 스펙과 룰북을 근거로 하고, 스펙이 정하지 않은 구현 세부는 `## 제안 결정 목록`(D97~D119)에서 정했다.

---

## Spec

- 시스템 스펙(6차 결정의 원본): `docs/spec/system/index.md`(`### 6차 범위: 공격 신통 쓰기 + 깨달음 쌓기`, `### 6차 이터레이션 열린 질문`), `command.md`(`## 6차 명령어` 아래 전부: 신통 쓰기, 시전과 `중지`, 신통 교전 메시지, 법력 회복, 깨달음 쌓기, 6차 정보 명령, 6차 도움말), `admin-tool.md`(`## 6차 범위`·`## 6차 데이터`(공격 신통의 단계별 값, 이펙트 문장, 기존 신통 채우기)·`## 6차 화면`·`## 6차 REST API`, `### 관리 툴 변경의 반영` 6차 줄), `platform.md`(`### 6차 WebSocket 메시지 변경`, `### 6차 월드 상태와 저장`), `glossary.md`(이펙트 문장). 이펙트 문장 1~5개·무작위 하나는 창호의 결정, 나머지 6차 절은 `(제안)`.
- 룰북(규칙·수치·문구의 원본): `docs/spec/game-rule/technique.md`(깨달음 — 쌓이는 양·속성에 나뉘어 쌓이는 양, 신통이 가지는 것, 경지·종류별 기준값, 공법 예시: 장춘공), `technique-list.md`(오행 입문 공법, 강체결 — 테스트 값), `combat.md`(대상 지정, 처리 순서, 대기시간 중 입력, 시전 시간, 공격력·방어력, 피해 흐름), `stats.md`(법력 — 발휘 제한, 영력, 소수점), `recovery.md`(영기 농도, 회복과 휴식), `commands.md`(입력 문법, 공통 오류 메시지, 상태별로 쓸 수 없는 명령, 정보, 전투)
- 1~5차 계획·진행: `docs/impl/iteration-5/plan.md`(D77~D96, Global Constraints), `iteration-5/progress.md`(리드 판정 R5-1·R5-2, T4·T6 판정 — 피해 기록 유지, 끊김 때 공격 시각 유지), 이전 계획의 버전 표(`iteration-1/plan.md`)
- world-server 코딩 규칙: `.claude/rules/*.md`(특히 architecture.md `## 메모리 월드 상태`, persistence.md, testing.md). **world-server 태스크는 시작 전에 모두 읽는다.**
- 용어: `docs/spec/glossary.md`
- 6차 범위 밖(만들지 않는다, index.md `6차에 넣지 않는 것` 그대로): 상성 피해 증감(자리만), 공격 외 신통 쓰기, 무리한 사용, 공통 대기 끝 신통 예약, `대상` 명령, `참오`·수위·소경지 상승·돌파, 잔본·산공·서재, 법기, 단약·영석·영약, 명상, 방 영기 농도 데이터, 도주·막기·추격·정혈 태우기, 정식 사망, 기절, 선공·배회, 몹 신통, 지속 피해, 방어 층, 부적, 안전 장소·PvP, 파티, 퀘스트, LLM 챗.

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
- 조사는 `Josa`(끝의 닫는 괄호·문장 부호를 건너뛰고, 숫자·영문으로 끝나면 받침 없음, `으로/로` 는 받침 없음·ㄹ이면 `로`)로 고른다. 수는 `Numbers.count`/`Numbers.decimal`, 시간은 `Durations.korean`(남은 시간은 D112).
- 계획 문구의 `{이름}`·`{n}` 은 채워 넣는 자리, `{이름은}` 처럼 조사가 붙은 자리는 `Josa` 로 고른 조사를 붙인다는 뜻이다. **단, 이펙트 문장 안의 `{대상}`·`{시전자}` 는 실제 자리표시 글자다.**
- `(제안)` 관례: 이 계획의 D 가운데 스펙에 없는 구현 세부만 Task 10이 해당 절에 `(제안)` 으로 옮긴다. 룰북 파일(`docs/spec/game-rule/`)은 고치지 않는다.
- 툴 호출 파라미터(JSON)의 한글은 리터럴 UTF-8로 쓰고 `\uXXXX` 이스케이프를 쓰지 않는다.
- world-server 코딩 규칙(`.claude/rules/*.md`), 루트 패키지 `mudmk3`. 아래 world-server 경로는 `world-server/src/{main,test}/java/mudmk3/` 기준이다. open-in-view가 꺼져 있으므로 REST 응답·틱에 쓰는 지연 연관은 트랜잭션 안에서 값 record로 바꾼다. **메모리 상태 객체는 엔티티를 들고 있지 않는다** — 시전은 (공법 id, 신통 이름, 몹 id)만 들고 발동 때 DB에서 다시 읽는다.
- **새 enum 열은 orm.xml 에 `column-definition="varchar(N)"` 을 준다.** PostgreSQL 에서는 그래도 `<table>_<column>_check` 가 생긴다(4차 Task 14 교훈). **6차는 이미 있는 enum 열에 값을 더하지 않는다**(새 enum 열은 새 표 `character_insight.kind` 하나) — 그래서 `LegacySchemaCleaner` 는 고치지 않는다. 구현 중 기존 enum(`AbilityKind`·`CostStat`·`Realm`·`ItemCategory` 등)에 값을 더하게 되면 멈추고 리드에게 보고한다(그때는 그 열의 CHECK 를 이름으로 지우는 정리를 `LegacySchemaCleaner` 에 더하고 기존 pgdata 로 저장을 확인해야 한다). Task 11 이 기존 pgdata 로 깨달음 저장·`mana_lost` 쓰기·새 신통 저장을 확인한다.
- **기존 행이 있는 표에 더하는 열은 nullable** 로 매핑한다(4차 `realm`, 5차 `qi_blood_lost` 교훈): `player_character.mana_lost`. 새 값 컬렉션 표(`ability_stage_power`·`ability_effect_line`)는 새 표라 해당 없음 — 기존 신통은 Task 2 채우기가 행을 넣는다.
- **AFTER_COMMIT 과 테스트 트랜잭션(4차 교훈):** `@ApplicationServiceTest` 는 롤백되어 AFTER_COMMIT 리스너가 오지 않는다. 6차는 새 AFTER_COMMIT 리스너를 만들지 않는다(관리 변경은 신통을 쓸 때마다 DB에서 읽어 반영). 경지 지정 알림 테스트는 5차 `designateRealm` 도우미를 쓴다.
- **`ApplicationReadyEvent` 순서:** 경지 채우기 `@Order(0)` → 영석 심기 `@Order(10)` → 6차 기존 신통 채우기 `@Order(20)`(admin-tool.md `### 기존 신통 채우기`). H2·PostgreSQL 둘 다 동작해야 한다.
- **게임 락:** 명령·틱·접속·끊김은 모두 `GameService.serialized` 하나를 지난다. 신통 처리·시전 발동·법력 회복도 그 안에서만 메모리 상태를 건드린다. 관리 REST 는 메모리 대기시간을 읽지 않는다. 난수(`RandomGenerator`)는 게임 락 안에서만 쓴다.
- **메모리 상태와 테스트:** `CombatMemoryReset`(5차)에 6차 메모리 저장소(`AbilityTimersRepository`)의 `deleteAll()` 을 더한다.
- Next.js 앱 규칙은 1~5차와 같다(정적 export, `"use client"`, 상대 경로 import, 쿼리스트링 id). 저장 전 클라이언트 검사는 하지 않고 서버 `detail` 을 보인다. 숫자 칸은 5차 고침대로 브라우저 검사를 끈다(`noValidate`).
- 자동 테스트는 Google 자격 증명·네트워크 없이 돈다. 시간에 걸린 테스트는 `MutableClock` + 직접 `tick()`(`advance`·`advanceAndTick`), 난수에 걸린 테스트는 `ScriptedRandom`. **실제로 기다리는 테스트(`Thread.sleep`, Awaitility)를 쓰지 않는다.**

**저장소·커밋**
- **main 에서 일한다.** 태스크 하나 = 커밋 하나 이상. 구현자는 커밋까지 하고, 리뷰를 통과하면 리드가 태스크마다 `git push` 한다. 구현자는 서브에이전트를 띄우지 않는다.
- 커밋 메시지: `<type>(<module>): <한국어 요약>`(type: feat, test, build, docs, chore, fix, refactor / module: world-server, play-client, admin-console, infra, spec, impl). 끝에 빈 줄 다음 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- 태스크를 끝내면 `docs/impl/iteration-6/progress.md` 의 표에 한 줄(태스크 번호·제목, 커밋 해시는 리드가 채움, 메모)을 더하고 같은 커밋에 넣는다. 각 태스크 끝의 **progress.md 한 줄** 이 그 메모의 틀이다(실제로 바꾼 것·계획과 다른 점을 덧붙인다).

**검증 명령 (각 태스크 완료 조건에 포함, 고친 모듈만)**
- world-server: `cd world-server && ./gradlew build`(테스트 + SpotBugs + ArchUnit)
- admin-console / play-client: `cd <module> && source "$NVM_DIR/nvm.sh" && nvm use && npm ci && npm test && npx tsc --noEmit && npm run build`

**스모크 (Task 11만)** — README `### 스모크 테스트` 방법 그대로(5차 Global Constraints 와 같음): `.env` 는 고치지 않고 `docker compose build` 한 번 → `up` 때만 셸 값으로 `MUD_DEV_LOGIN_ENABLED=true`, `MUD_ADMIN_EMAILS=<.env 의 기존 값>,smoke-admin@example.com` 을 덮어써 `docker compose up -d` → `node scripts/smoke-test.mjs`. **기존 pgdata 로 올린다.** 끝나면 시작 방 id 1(태초 객잔)·시작 소지품 `[]`·이 실행의 몹 배치 0 을 확인한다. 실제 서버 틱(1초)이 돌므로 스모크는 실제 시간으로 기다린다(`waitFor`·되풀이, 고정 대기 없음).

## 제안 결정 목록

스펙이 정하지 않은 구현 세부. 번호는 5차(D77~D96)에 잇는다. 스펙이 이미 정한 것(문구·REST 표·화면 칸·식·틱 순서)은 다시 적지 않고 절을 가리킨다 — **그 절의 문장이 곧 요구 사항이다.**

**D97. 슬라이스·의존·배치** — 새 슬라이스는 없다. `domain/technique`(단계별 값·이펙트 문장 검사·렌더), `domain/character`(`Mana`), `domain/cultivation`(`InsightKind`·`CharacterInsight`·`Insight`·`InsightGain`), `domain/combat`(`AbilityTimers`·`AbilityKey`·`Casting`·`SpiritOutput`·`AbilityPower`·`AttackNature`, `Damage`·`MobInstance` 변경), `domain/shared/Realm.minorIndex()`. `application/game` 은 `cultivation`·`technique`·`character` 의 provided 포트만 쓴다. `domain/game` → `domain/technique`(`ReservedWords`) 의존이 새로 생긴다(technique 는 game 을 모르므로 순환 없음). ArchUnit 은 그대로 통과해야 한다.

**D98. 신통 데이터 모델** — admin-tool.md `### 공격 신통의 단계별 값`·`### 이펙트 문장`, platform.md `### 6차 월드 상태와 저장` 의 관리 데이터 줄 그대로.
- `domain/technique/AbilityStagePower(int stage, BigDecimal basePower, BigDecimal coefficient)` record(embeddable). 저장할 때 `basePower.setScale(2)`, `coefficient.setScale(3)`(검사 뒤라 자리 넘침 없음 — 응답은 저장한 모양 `2.00`·`1.200`).
- `AbilitySaveInfo` 끝에 컴포넌트 둘: `List<String> effectLines`(컴팩트 생성자가 줄마다 `strip()`, null 줄은 `NullPointerException` 대신 `IllegalArgumentException("effectLines 에 빈 값이 있습니다")`), `List<AbilityStagePower> stagePowers`. 다른 생성자는 두지 않는다(부르는 곳을 모두 고친다 — D117).
- `Ability` 필드 `List<String> effectLines`(element-collection 표 `ability_effect_line`: `ability_id`, `position`(order-column), `text` varchar(100) not null), `List<AbilityStagePower> stagePowers`(표 `ability_stage_power`: `ability_id`, `position`(order-column — bag 이 둘이 되지 않게, 스펙 열에 하나 더함), `stage` int, `base_power` numeric(7,2), `coefficient` numeric(6,3)). 둘 다 공법 저장 때 4차 신통처럼 통째로 바뀐다(신통 행이 새로 생김).
- `Ability` 메서드: `List<String> getEffectLines()`(바꿀 수 없음), `List<AbilityStagePower> getStagePowers()`, `Optional<AbilityStagePower> stagePower(int stage)`, `boolean usableInGame()` = `kind == ATTACK && target == SINGLE && (costStat == NONE || costStat == MANA) && !costPerTick`(소모 없음인데 틱마다 표시가 켜진 것도 쓸 수 없음으로 본다).

**D99. 검사 세부** — admin-tool.md `## 6차 REST API` 예외 표·검사 순서 그대로 `Ability.create` 안에서 4차 검사(재사용 대기시간) 뒤에 한다. 이 계획이 정하는 것:
- 단계별 값: 공격이 아님(목록이 비지 않음) → 행 맞음(크기 = 단계 수 − 배우는 단계 + 1, i번째 `stage` = 배우는 단계 + i) → **단계마다(낮은 단계부터) 소수 자리 → 기본 위력 범위 → 계수 범위**. 소수 자리는 `stripTrailingZeros().scale()` 이 기본 위력 > 2 또는 계수 > 3. 범위는 0 이상 10,000 이하 / 0 이상 100 이하.
- 이펙트 문장: 수(1~5) → **검사마다 모든 줄을 입력 순서로**: 길이(줄바꿈 `\n`·`\r` 이 있거나, `codePointCount` 가 1~100 밖 — 공백뿐은 strip 뒤 빈 줄이라 여기서 걸림) → 자리표시 → `{대상}`(대상이 `SINGLE` 이 아닌데 `{대상}` 이 있음) → 같은 문장(strip 뒤 글자 그대로 비교).
- 자리표시 훑기(`domain/technique/EffectLine.firstBadToken(String) → Optional<String>`): 앞에서부터 `{` 를 만나면 그 뒤 첫 `}` 까지(포함)를 토큰으로 보고 `{대상}`·`{시전자}` 가 아니면 그 토큰이 걸림, 뒤에 `}` 가 없으면 `{` 하나가 걸림; `{` 없이 나온 `}` 는 `}` 하나가 걸림. 처음 걸린 것 하나를 문구 괄호에 넣는다(`쓸 수 없는 자리표시가 있습니다: 화탄술 ({적})`).
- 4차 문구·순서는 그대로다.

**D100. 요청·응답** — admin-tool.md `## 6차 REST API` 모양 그대로.
- `AbilityRequest` 끝에 `@Nullable List<String> effectLines`, `@Nullable List<StagePowerRequest> stagePowers`(null → 빈 목록). `application/technique/provided/StagePowerRequest(Integer stage, BigDecimal basePower, BigDecimal coefficient)` — 셋 중 하나라도 null 이면 컴팩트 생성자 `IllegalArgumentException`(5차 관례, JSON 이면 공통 400 `요청 본문을 읽을 수 없습니다`), `toInfo()`.
- `TechniqueView.AbilityView` 끝에 `List<String> effectLines`, `List<StagePowerView> stagePowers`(`StagePowerView(int stage, BigDecimal basePower, BigDecimal coefficient)`), `boolean usableInGame`. `TechniqueDetailResponse.AbilityResponse` 도 같은 세 칸(`StagePowerResponse`).
- `CultivationProfile` 끝에 `Insight insight`, `CharacterDetailResponse` 에 `List<InsightResponse> insight`(`InsightResponse(String kind, String name, int amount)`, 여섯 개 늘 `METAL`·`WOOD`·`WATER`·`FIRE`·`EARTH`·`BODY` 순).

**D101. 이펙트 문장 렌더** — `domain/technique/EffectLine.render(String line, @Nullable String targetName, String casterName) → String`. `{대상}` → 대상 이름, `{시전자}` → 쓴 사람 이름. 바꿔 넣은 자리 바로 뒤가 조사이면 넣은 이름에 맞춰 고친다: 바로 뒤 글자가 `으로`(두 글자)·`로`·`이`·`가`·`을`·`를`·`은`·`는`·`과`·`와` 로 시작하면(`으로` 를 먼저 봄) 그 조사 쌍(`으로/로`, `이/가`, `을/를`, `은/는`, `과/와`)을 `Josa` 로 고른 것으로 바꾼다. 그 뒤에 다른 글자가 이어져도(`{대상}로부터`) 조사 부분만 바꾼다. `targetName` 이 null 이면 `{대상}` 을 그대로 둔다(6차에는 생기지 않음).

**D102. 기존 신통 채우기** — admin-tool.md `### 기존 신통 채우기` 그대로. `TechniqueModifyService.fillMissingAbilityData()`(`@EventListener(ApplicationReadyEvent.class) @Order(20)`, `@Transactional`) 가 모든 공법을 읽어 `Technique.fillMissingAbilityData() → int`(채운 신통 수)를 부르고, 하나라도 채웠으면 저장하고 `log.info("기존 신통 {}개의 이펙트 문장·단계별 값을 채웠습니다.", n)`. 도메인 `Ability.fillMissing(int stageCount) → boolean`: 이펙트 문장이 비면 `SINGLE` 은 `"{대상}에게 " + name + Josa.eulReul(name) + " 펼친다!"`, 아니면 `name + Josa.eulReul(name) + " 펼친다."`(스펙 예 `{대상}에게 화탄술을 펼친다!`·`화탄술을 펼친다.`) 한 줄; `ATTACK` 이고 단계별 값이 비면 배우는 단계 `L` 부터 `stageCount` 까지 `stage s` 마다 기본 위력 = b + step × (s − L), 계수 c(연체 신통(`bodyRefining`) b 5 · c 2.2 · step 0.5, 그 밖 b 2 · c 1.2 · step 0.2). 채우기는 검사를 거치지 않는다(이름에 `{` 가 든 옛 신통도 그대로 채움, 받아들임). 공격이 아닌 신통에 단계별 값은 넣지 않는다. 다시 떠도 이미 있는 값은 건드리지 않는다.

**D103. 깎인 법력** — platform.md `### 6차 월드 상태와 저장` 의 깎인 법력 줄 그대로, 5차 D83 과 같은 모양.
- 매핑 `PlayerCharacter.manaLost`(`@Nullable Double`) — orm.xml `<column name="mana_lost" nullable="true" insertable="false" updatable="false"/>`, getter 없음(SpotBugs `UWF_UNWRITTEN_FIELD` 은 5차처럼 그 필드 하나만 제외).
- `CharacterRepository`: `@Modifying(flushAutomatically = true) @Query(value = "update player_character set mana_lost = :lost where id = :id", nativeQuery = true) int writeManaLost(Long id, double lost)`, `@Query(value = "select coalesce(mana_lost, 0) from player_character where id = :id", nativeQuery = true) Optional<Double> readManaLost(Long id)`.
- 도메인 `domain/character/Mana`(static): `double lostAfterSpend(double lost, double amount, int max)`(min(max, lost + max(0, amount))), `double lostAfterRecover(double lost, double amount)`(max(0, lost − max(0, amount))), `double lostAfterDemotion(double lost, Realm from, Realm to)`(`to.spirit() == 0` 이면 0, `to.spirit() < from.spirit()` 이면 max(0, lost − (from.spirit() − to.spirit()) × 5), 아니면 그대로 — 최대치의 경지 몫만 보고 공법 몫은 보지 않음, 5차 기혈과 같은 판단), `double current(double lost, int max)`(max(0, max − lost)), 상수 `REGEN_RATE_PER_SECOND = 0.005`.
- `CharacterVitalsEditor` 에 더함: `double manaLost(Long characterId)`, `double spendMana(Long characterId, double amount, int maxMana)`(남은 현재 법력을 돌려줌), `boolean recoverMana(Long characterId, double amount)`(깎인 것이 0보다 클 때만 쓰고 true). `designate` 는 기혈 조정 뒤 `writeManaLost(id, Mana.lostAfterDemotion(...))`.
- `StatSheet` 에 `double manaCurrent`(`mana` 바로 뒤 칸): `StatSheet.of(Realm, @Nullable SpiritRoot, List<CharacterTechnique>, double qiBloodLost, double manaLost)`(5인자만 남기고 부르는 곳을 고침). `CultivationQueryService` 는 `CharacterVitalsEditor.manaLost` 로 읽어 넘긴다.
- 화면 수: `상태` 게이지·이펙트 줄 `법력 20/25`·거절 문구의 수는 모두 `Damage.gauge`(버림, 0 초과 1 미만은 1 — 스펙 '버린 정수(0보다 크고 1보다 작으면 1)').

**D104. 법력 회복** — command.md `### 법력 회복` 그대로. package-private `@Component ManaRecovery.recover(Instant now)` 를 `CombatEngine.tick` 의 배치 맞추기 다음·행동 앞에서 부른다. 접속해 세계에 있는 캐릭터(presence)마다: 회복 초 = 지난 회복 시각부터 흐른 초를 0~1초로 자른 값 — `PlayerCombat` 에 `@Nullable Instant manaRegenAt` 와 `double manaRegenSeconds(Instant now)`(자른 초를 돌려주고 시각을 now 로, 시각이 없으면 0초) 를 더하고, `CombatEngine.entered` 가 들어올 때마다 `PlayerCombat` 을 만들어(없으면) `manaRegenAt = now` 로 둔다(테스트의 0.5초 틱이 실제 1초 틱과 같은 양을 채우게 하려는 것, 실제 1초 틱에서는 늘 1초); `CharacterVitalsEditor.manaLost` 가 0보다 클 때만 `CultivationFinder.stats(id).mana()` 를 읽어 `recoverMana(id, max × 0.005 × 초)`. 최대치 0(범인)이면 아무것도 하지 않는다. 알림 없음.

**D105. 깨달음 저장** — platform.md 깨달음 줄 그대로.
- `domain/cultivation/InsightKind { METAL("금"), WOOD("목"), WATER("수"), FIRE("화"), EARTH("토"), BODY("연체") }` — `koreanName()`, `static InsightKind of(Element)`.
- 엔티티 `domain/cultivation/CharacterInsight`(`AbstractEntity`, `Long characterId`, `InsightKind kind`, `double amount`) — 표 `character_insight`(`character_id` not null, `kind` varchar(16) enum, `amount` double precision), 유니크 `UK_CHARACTER_INSIGHT_CHARACTER_ID_KIND`. FK 없음(5차 배치 관례). `static create(Long characterId, InsightKind kind, double amount)`.
- `application/cultivation/required/CharacterInsightRepository`: `save`, `List<CharacterInsight> findAllByCharacterId(Long)`, `@Modifying(flushAutomatically = true) @Query("update CharacterInsight i set i.amount = i.amount + :amount where i.characterId = :characterId and i.kind = :kind") int addAmount(Long characterId, InsightKind kind, double amount)`. 더하기는 `addAmount` 가 0 행이면 새 행 저장(게임 락 안에서만 써서 경합 없음).
- 값 record `domain/cultivation/Insight(Map<InsightKind, Double> amounts)` — 여섯 갈래 모두 채움(없으면 0), `double amount(InsightKind)`, `int floor(InsightKind)`, `static Insight empty()`.
- provided: `CultivationFinder.insight(Long characterId) → Insight`, 새 포트 `application/cultivation/provided/InsightEditor`(`/** 캐릭터의 깨달음을 쌓는다 */ void add(Long characterId, Map<InsightKind, Double> gains)` — 0 이하인 갈래는 건너뜀), 구현 `CultivationModifyService`.

**D106. 깨달음 계산** — command.md `### 깨달음 쌓기`, 룰북 그대로. `domain/cultivation/InsightGain`(static):
- `double baseAmount(Ability ability, Realm techniqueStartRealm)` — `MANA` 면 소모량, 아니면 기준 소모 = (기준 경지 `spirit() × 5`) ÷ 5 × (재사용 대기시간 ÷ 3). 기준 경지는 시작 경지, `MORTAL` 이면 `QI_1`.
- `double situationFactor(Realm caster, Realm target)` — `target.majorIndex() < caster.majorIndex()` 면 0.1, 같은 대경지에서 `caster.minorIndex() − target.minorIndex() >= 3` 이면 0.5, 아니면 1.0. `Realm.minorIndex()`: 범인 0, 연기 n층 n, 축기 초기 1·중기 2·후기 3·대원만 4.
- `Map<InsightKind, Double> split(double g, Ability ability, List<Element> techniqueElements)` — 연체 신통이면 BODY 에 g, 그 밖 신통 속성마다 g ÷ 속성 수, 공법 속성 가운데 신통 속성에 없는 것마다 g × 0.5 ÷ 그 수(연체 신통이면 공법 속성 전부가 '없는 것'). 0 인 갈래는 넣지 않는다.
- `Map<InsightKind, Double> capped(Map<InsightKind, Double> gains, double remaining)` — 합이 남은 상한보다 크면 갈래마다 같은 비율로 줄인다(남은 상한 0 이하면 빈 맵).

**D107. 메모리 신통 상태** — platform.md '메모리에만 두는 것' 그대로.
- `domain/combat/AbilityKey(Long techniqueId, String abilityName)` record, `domain/combat/Casting(Long techniqueId, String abilityName, long mobId, Instant firesAt, long seq)` record(seq 는 시전 시작 순번).
- `domain/combat/AbilityTimers`(final, lombok `@Getter`): `Long characterId`, `@Nullable Instant globalReadyAt`, `Map<AbilityKey, Instant> cooldowns`, `@Nullable Casting casting`. 메서드 `Optional<Duration> globalRemaining(Instant now)`, `Optional<Duration> cooldownRemaining(AbilityKey, Instant now)`, `void startGlobal(Instant now)`(now + 1.5초), `void startCooldown(AbilityKey, BigDecimal seconds, Instant now)`, `void startCasting(Casting)`, `Optional<Casting> casting()`, `void clearCasting()`. 상수 `domain/combat/AbilityRules.GLOBAL_COOLDOWN = Duration.ofMillis(1500)`.
- 포트 `application/game/required/AbilityTimersRepository`(`/** 신통 대기시간·시전을 메모리에 둔다 */` `save`, `Optional<AbilityTimers> find(Long characterId)`, `List<AbilityTimers> findAll()`, `void deleteAll()`), 구현 `adapter/persistence/memory/InMemoryAbilityTimersRepository`(`ConcurrentHashMap`). 대기시간은 세계에서 사라져도 남는다(룰북). 시전은 세계에서 사라질 때·질 때 지운다.
- 한 상대 상한은 `MobInstance` 에 둔다: `double grantInsight(Long characterId, double requested)`(남은 상한 = 처음 `profile.stats().qiBlood()`, 준 만큼 빼고 돌려줌 — D106 `capped` 와 같은 수), `boolean insightCapReachedFirstTime(Long characterId)`(남은 상한이 0 이 되었고 아직 알리지 않았으면 true 로 바꾸며 true). 그 플레이어가 `disengage` 될 때, 몹이 쓰러질 때·다시 찰 때·다시 나타날 때 지운다.

**D108. 위력·피해 자리** — command.md 성공 4·5, platform.md '위력·피해 계산의 자리' 그대로.
- `domain/combat/SpiritOutput.ratio(double manaCurrent, int manaMax) → double` — 최대 0 이면 1.0, 남은 비율 ≥ 0.2 면 1.0, 아니면 0.5 + 0.5 × (비율 ÷ 0.2).
- `domain/combat/AbilityPower`: `static double elemental(BigDecimal base, BigDecimal coefficient, int spirit, double spiritRatio, double aptitudeMultiplier)` = (base + spirit × ratio × coef) × mult, `static double bodyRefining(BigDecimal base, BigDecimal coefficient, int strength)` = base + strength × coef, `static double aptitudeMultiplier(List<Element> elements, StatSheet stats)` = 비면 1, 아니면 1 + (Σ `stats.aptitude(e)` ÷ 수) ÷ 100.
- `domain/combat/AttackNature(List<Element> elements, boolean bodyRefining)` record, `static AttackNature NONE`(5차 공격·몹 공격).
- `Damage.receiverMultiplier(AttackNature nature, @Nullable MobKind receiverKind) → double` — **6차에는 늘 1.0**(Javadoc: 상성(창호: 나중)과 룰북 귀물·음혼 속성 배율이 붙을 자리). `Damage.afterLayers(double power, double receiverMultiplier, int body)` = `afterBody(power × multiplier, body)`.
- `MobInstance.receive(Long characterId, double power, AttackNature nature, Instant now)` 로 바꾼다: 배율을 곱한 값(층을 거치기 전 피해)을 위협·피해 기록에 더하고 `afterLayers` 로 기혈을 깎는다. 몹 공격도 `Damage.afterLayers(power, Damage.receiverMultiplier(NONE, null), body)` 를 쓴다. 5차 기대값은 그대로다.

**D109. 명령 파싱** — command.md `### 신통 쓰기` 이름 해석 1 그대로. `Command.UseAbility(String name, @Nullable ItemQuery target)`(인자 전체가 대상 이름·번호, 비면 null), `Command.Stop()`(`중지`, 인자가 있으면 `Unknown("중지")`). `CommandParser` 의 default 가지: 방위 명령이면 5차 그대로(인자 있으면 `Unknown`), 아니고 첫 낱말이 `ReservedWords.contains(word)` 면 `Unknown(word)`(아직 없는 명령), 그 밖은 `UseAbility(word, wholeName(rest))`. 이미 있는 명령의 잘못된 인자(`상태 한립`)는 그대로 `Unknown`.

**D110. 처리기 나누기** — 모두 `application/game` package-private `@Component`. 순환 없게: `GameService` → `AbilityCommandHandler` → `AbilityCaster` → `CombatActions`; `CombatEngine` → `AbilityCaster`, `CombatActions`; `AbilityCaster`·`AbilityCommandHandler` → `AbilityResolver`.
- `CombatActions`(5차 `CombatEngine` 에서 떼어 냄, 이슈 #15): `void startEngagement(OnlinePlayer, PlayerCombat, MobInstance, Instant, send)`(교전 순번 + `engage` + `청모랑과 싸움이 벌어진다.` + 같은 방 `engage` notice), `MobHit strike(Long characterId, MobInstance, double power, AttackNature, Instant)`, `void onMobKilled(MobInstance, Set<Long> opponents, Long strikerId, Instant, send)`, `boolean inCombatTogether(OnlinePlayer, MobInstance)`, `void notifyOthers(OnlinePlayer, String text, String event, send)`, `TargetLookup findTarget(OnlinePlayer, ItemQuery)`(5차 `공격` 찾기 순서와 거절 문구 — sealed `TargetLookup.Found(MobInstance)`·`Rejected(String text)`). 교전 순번 증가 번호도 여기로 옮긴다. `CombatEngine.attack` 은 이것들을 쓰게 바꾸고 동작은 그대로(5차 테스트로 확인).
- `AbilityResolver`: `Optional<LearnedAbility> findLearned(Long characterId, String name)`(익힌 순서로 `abilitiesUpTo(stage)` 에서 이름 전체 대소문자 무시), `Optional<LearnedAbility> findLearned(Long characterId, AbilityKey key)`(발동 때 다시 읽기), `Optional<String> definedName(String name)`(`TechniqueFinder.findAbilityName`). `LearnedAbility(Technique technique, Ability ability, int stage)` record(트랜잭션 안에서만 씀) + `AbilityKey key()`.
- `TechniqueFinder.findAbilityName(String name) → Optional<String>`(모든 공법 신통에서 이름 대소문자 무시, 처음 것의 저장된 이름) — `TechniqueRepository` JPQL `select a.name from Ability a where lower(a.name) = lower(:name) order by a.id`.
- `AbilityCaster`: `void cast(OnlinePlayer caster, LearnedAbility, MobInstance target, boolean startGlobal, Instant now, send)`(command.md 성공 1~8), `void fire(Long characterId, Casting, Instant now, send)`(발동 다시 확인 → `cast(…, false, …)`).
- `AbilityCommandHandler`: `void use(OnlinePlayer, Command.UseAbility, Instant, send)`, `void stop(OnlinePlayer, send)`, `boolean isCasting(Long characterId)`. `신통` 목록의 남은 시간은 `GameService` 가 `AbilityTimersRepository.find` 로 읽어 `CultivationTexts.abilities` 에 넘긴다.
- 문구는 package-private `@Component AbilityTexts`(5차 `CombatTexts` 와 같은 방식).

**D111. 성공 처리 세부** — command.md 성공 1~8·5차 자동 공격과의 관계 그대로. 이 계획이 정하는 것:
- 1: 플레이어가 아무 몹과도 교전하지 않던 때 이 신통으로 교전을 시작하면 `PlayerCombat.delayAttackUntil(Instant at)`(다음 공격 시각 = max(기존, at))로 now + 3초. 이미 다른 몹과 교전 중이면 다음 공격 시각을 건드리지 않는다. 교전 여부와 상관없이 `target(mob)`.
- 3: 공통 대기는 `startGlobal` 이 true(시전 0) 일 때만 여기서 건다(시전이 있으면 시작 때 이미 걸림). 재사용 대기 키는 `LearnedAbility.key()`.
- 6: 문장 고르기 — 문장이 2개 이상이면 `random.nextInt(문장 수)` 하나(1개면 난수를 쓰지 않음). 줄 = `EffectLine.render(…, mob.getProfile().name(), caster.getName()) + " (피해 " + Damage.display(피해) + (MANA 면 ", 법력 " + gauge(현재) + "/" + 최대 : "") + ")"`. 현재는 소모 뒤 값, 최대는 `stats.mana()`.
- 7: G = `InsightGain.baseAmount × situationFactor(caster 경지, 몹 기준 경지)` → `split` → 합을 `mob.grantInsight` 로 상한에 맞춰 `capped` → `InsightEditor.add`. `insightCapReachedFirstTime` 이 true 면 `같은 상대에게서는 더 깨닫는 것이 없습니다.`(`info`).
- 순서 8 의 처치는 `CombatActions.onMobKilled`(5차 처치 줄·시체·수습·재등장).
- 거절 문구의 `필요` 와 `남은 것` 은 둘 다 `Damage.gauge` 정수다(스펙 '수는 버린 정수') — 소모량 2.5 면 `필요 2` 로 보인다(리드 판정 후보 L6-3).

**D112. 남은 시간 표시** — command.md '남은 시간은 4차 시간 문체이고, 60초 미만은 소수 첫째 자리로 올림' 그대로: `AbilityTexts.remaining(Duration d)` — 60초 미만은 나노초에서 0.1초 단위로 올림(`Math.ceilDiv`)한 `BigDecimal` 을 `Durations.korean`, 60초 이상은 1초 단위로 올림. 거절 문구와 `신통` 목록이 같은 함수를 쓴다.

**D113. 시전 세부** — command.md `### 시전과 `중지``, platform.md 서버 틱 3 그대로.
- 시작: 검사를 통과하면 `startGlobal` + `startCasting(Casting(…, now + 시전 시간, seq))`, 줄 `화탄술을 펼치기 시작한다… (2초)`(`combat`, 시간은 `Durations.korean(castSeconds)`).
- `CombatEngine.act` 의 행동 목록에 시전(발동 시각 ≤ now)을 넣는다. 정렬 키 = (시각, 무리, 순번): 교전 중인 행동(5차 플레이어·몹, 이미 교전 중인 캐릭터의 시전)은 무리 0·교전 시작 순번, 아직 교전하지 않은 캐릭터의 시전은 무리 1·시전 시작 순번. 발동 차례에 `AbilityCaster.fire`: 세계에 없음 → 조용히 지움, 신통이 없어지거나 `usableInGame()` 이 아니거나 단계별 값이 없음·몹이 없음·죽음·다른 방 → `펼치던 술법이 갈 곳을 잃고 흩어진다.`(`combat`), 법력 모자람 → 거절 문구(`error`), 모두 시전을 지우고 소모·재사용 대기 없음.
- 끊김: `CombatEngine.leave`(끊김·`종료`)와 질 때(`onPlayerDefeated`)가 `clearCasting`. 방 삭제로 옮겨질 때는 지우지 않는다(발동 때 다른 방이라 흩어짐).
- 막힘: `GameService.move` — 교전 중이면 5차 문구가 먼저, 아니고 시전 중이면 `술법을 펼치는 중입니다. (끊으려면 '중지')`.

**D114. 정보 명령 출력** — command.md `### 6차 정보 명령` 그대로.
- `CultivationTexts.status(PlayerCharacter, StatSheet, Insight)`: 법력 게이지 `gauge(manaCurrent)/mana`, 깨달음 줄 `깨달음  금 0 · 목 2 · 수 0 · 화 5 · 토 0 · 연체 0`(`Insight.floor`).
- `CultivationTexts.techniques(Realm, List<CharacterTechnique>, Insight)`: `(지금 n)` 의 n = 그 속성(연체술 공법은 BODY)의 `Insight.floor`.
- `CultivationTexts.abilities(List<CharacterTechnique>, @Nullable AbilityTimers, Instant now)`: 재사용 대기 남은 줄 끝에 `  (1.8초 남음)`(D112).
- `StatusMessage.of(PlayerCharacter, StatSheet, Insight, String text)`: `stats.mana` = `GaugeView(gauge(manaCurrent), mana)`, 끝에 `List<InsightView> insight`(`InsightView(String kind, String name, int amount)`, `cultivation` 다음·`text` 앞 — JSON 필드 순서).

**D115. 도움말·자동완성** — command.md `### 6차 도움말` 문구를 `GameService.HELP` 에 글자 그대로. play-client `COMMANDS` 는 그 절 순서(`공격` 다음 `중지`). README 명령 목록도 같은 문구.

**D116. admin-console** — admin-tool.md `## 6차 화면` 그대로. 파일·함수:
- `src/lib/techniques.ts`: `StagePowerDraft = { stage: number; basePower: string; coefficient: string }`, `FillDraft = { basePower: string; coefficient: string; baseStep: string; coefficientStep: string }`, `DEFAULT_FILL = { basePower: "2", coefficient: "1.2", baseStep: "0.2", coefficientStep: "0" }`, `MAX_EFFECT_LINES = 5`, `fillStagePowers(learnStage, stageCount, fill): StagePowerDraft[]`(배우는 단계 값 + (s − L) × 단계마다 +, 기본 위력은 소수 둘째·계수는 셋째 자리에서 반올림한 뒤 끝 0 을 지운 문자열 — `2.2`, `1.224`), `resizeStagePowers(drafts, learnStage, stageCount): StagePowerDraft[]`(남는 단계 값 그대로, 새 단계는 새 목록의 바로 앞 단계 값 복사, 앞이 없으면 `DEFAULT_FILL` 배우는 단계 값), `AbilityDraft` 에 `effectLines: string[]`·`stagePowers: StagePowerDraft[]`·`usableInGame?: boolean`, `newAbility()` 는 4차 기본값 + `effectLines: [""]` + 종류가 공격이면 `fillStagePowers(1, stageCount, DEFAULT_FILL)`(그래서 `newAbility(stageCount)`), `toAbility(draft)` 는 공격일 때만 `stagePowers` 를 숫자로 보내고 그 밖 `[]`.
- `src/components/AbilityDetailEditor.tsx`(새, 한 신통의 펼침 칸: 이펙트 문장 입력 줄·`삭제`·`문장 추가`·안내 문구, 공격일 때 `채우기` 칸 넷·`채우기` 단추·단계별 값 표 `단계`·`기본 위력`·`계수`), `AbilityTable.tsx`(행마다 `자세히`/`접기` 단추, 펼친 행 아래 `AbilityDetailEditor`, `usableInGame === false` 면 행 끝 `(게임에서 아직 쓸 수 없음)` — 저장된 응답 값, 새 행은 표시 없음), 배우는 단계·종류·단계 수가 바뀌면 `resizeStagePowers`(종류가 공격이 아니면 표를 숨기되 draft 의 값은 지우지 않음).
- `src/components/CharacterDetail.tsx`: 스탯 표 아래 `깨달음  금 0 · 목 112 · …`(응답 `insight` 의 `name amount` 를 ` · ` 로).
- `src/lib/api.ts`: 타입 `StagePower`, `AbilitySaveRequest.effectLines/stagePowers`, `AbilityResponse.usableInGame`, `CharacterDetailResponse.insight`.

**D117. 테스트 도우미·픽스처** — 
- `TechniqueFixture`: `ability(...)`·`fireBall()`·`ironFist()` 등 모든 신통에 기본 이펙트 문장과(공격이면) 단계별 값을 넣는다. 화탄술 이펙트 문장 `List.of("손끝에서 붉은 불덩이가 피어올라 {대상}을 덮친다!", "{시전자}의 손바닥에서 화탄이 튀어 나가 {대상}에게 터진다!", "작은 불덩이가 꼬리를 그으며 {대상}에게 날아든다!")`(admin-tool.md 예 셋 그대로), 단계별 값 1~9성 `2 + 0.2 × (s − 1)` · 1.2. 철권: 이펙트 `List.of("단단히 쥔 주먹이 {대상}의 몸에 꽂힌다!")`, 1~9성 `5 + 0.5 × (s − 1)` · 2.2. 청목전: `List.of("푸른 나뭇잎이 칼날처럼 {대상}을 벤다!")`, 1~8성 3·1.2(픽스처 값, 룰북 수치 아님). 그 밖 비공격 신통: `List.of(name + " 기운이 퍼진다.")`. 도우미 `stagePowers(int from, int to, String base, String step, String coefficient)`, `withEffectLines(AbilitySaveInfo, List<String>)`, `withStagePowers(AbilitySaveInfo, List<AbilityStagePower>)`, `withCast(AbilitySaveInfo, String castSeconds)`, `withCost(AbilitySaveInfo, CostStat, String amount, boolean perTick)`.
- `TechniqueRequestFixture` 가 새 칸을 옮긴다.
- `BaseGameTest`: `Long learn(Long accountId, TechniqueSaveInfo info)`(`TechniqueEditor.create` + `CharacterTechniqueRepository.save(CharacterTechnique.learn(…))` + flush, 공법 id), `void setManaLost(Long accountId, double lost)`(`CharacterRepository.writeManaLost` + flush), `double manaLost(Long accountId)`, `Insight insightOf(Long accountId)`.
- `CombatMemoryReset` 에 `AbilityTimersRepository.deleteAll()`.
- 게임 테스트의 기준 캐릭터: `enter` 로 만든 목·화(50·50) 캐릭터를 `setRealm(QI_1)`(영력 5·법력 25·근력 3·육신 6) 한 뒤 `learn(장춘공)`. 기준 몹: 5차 `MobFixture.wolf()` 청모랑(기혈 49·육신 8·위력 8). 화탄술 위력 = (2 + 5 × 1.2) × 1.5 = 12 → 피해 8.

**D118. 1~5차 테스트·스모크 고치기** — `AbilitySaveInfo`·`AbilityRequest`·`StatSheet.of`·`StatusMessage.of`·`CultivationTexts` 시그니처가 바뀐 곳을 모두 고친다. 4차 신통 요청 테스트(`TechniqueEditorTest`, `TechniqueApiWebMvcTest`, `AdminApiIntegrationTest`, `GameWebSocketIntegrationTest`)는 이펙트 문장 하나와 공격이면 단계별 값을 넣는다. `CommandParserTest` 의 `Unknown` 기대 가운데 명령이 아닌 첫 낱말(`대화왕씨`, `말 왕씨`, `말 안녕`, `보`)은 `UseAbility` 로 바꾸고, 게임 응답(`무슨 말인지 모르겠습니다. …`)은 그대로임을 `GameConnectorTest` 로 확인한다. `상태` 4차 기대 문구의 깨달음 줄은 모두 0 이라 그대로다. 스모크: 단계 24·35·42 의 `도움말` 기대를 D115 문구로, 단계 26 의 공법 요청 신통에 `effectLines`·`stagePowers` 를 더한다(화탄술은 D117 값).

**D119. 경합·순서** — 신통 정의는 명령·발동마다 DB에서 읽으므로 관리 저장은 다음 명령부터 보인다(admin-tool.md 6차 반영 줄). 깎인 법력은 전용 네이티브 쿼리로만 쓰므로 관리 경지 지정·이동의 엔티티 저장이 되돌리지 않는다(5차 D95 와 같음). 관리 경지 지정의 법력 조정과 같은 순간의 신통·회복 쓰기가 겹치면 나중 커밋이 이긴다(그 열 하나, 드묾, 받아들임). 깨달음은 게임 락 안에서만 쓰고 관리는 읽기만 한다.

### 6차 열린 질문 대응

index.md `### 6차 이터레이션 열린 질문` 은 모두 `→ (제안)` 답이 있다. 이 계획은 답을 바꾸지 않고 구현 세부만 더한다: 신통 데이터 D98·D99·D100, 기존 신통 D102, 이펙트 문장 D99·D101·D111, 명령 D109·D110, 검사 순서·문구 D111·D112, 시전 D113, 교전 시작·자동 공격 D111, 위력·발휘·상성 자리 D108, 법력 D103·D104, 깨달음 D105·D106·D107, 메시지·화면 D114·D116, 자동완성 D115.

## 리드 판정 후보

스펙을 읽다 걸린 것. 계획은 아래 '계획의 처리' 대로 가고, 리드가 다르게 판정하면 해당 D 만 바꾼다.

- **L6-1 `(1.8초)` 예가 나올 수 없음:** index.md 6차 완료 시나리오의 '곧바로 다시 써서 재사용 대기 거절(`화탄술은 아직 쓸 수 없습니다. (1.8초)`)' 은 1.2초 뒤인데, 검사 순서(공통 대기 → 재사용 대기)에서는 그때 공통 대기(1.5초)가 0.3초 남아 `아직 기운을 고르는 중입니다. (0.3초)` 가 먼저 나온다. 재사용 거절은 1.5~3초 사이에만 보인다. 계획의 처리: 순서는 스펙대로 두고, 테스트·스모크는 '곧바로 → 공통 대기 거절, 1.5초 뒤 → 재사용 거절' 둘 다 확인한다(예 수는 1.6초 뒤 `(1.4초)`).
- **L6-2 범인의 연체 신통:** command.md '범인은 법력 최대치가 0이라 7(또는 4)에서 걸린다' 는 법력·오행 신통만 맞다. 소모 없는 연체 신통(철권)은 4·7 어디에도 걸리지 않아 관리 툴로 범인까지 내린 캐릭터도 쓸 수 있다(룰북: 연체 신통은 영력 제한이 붙지 않음). 계획의 처리: 막지 않는다(테스트 `mortalCanUseBodyRefiningAbility`).
- **L6-3 소수 소모량의 `필요` 표시:** '수는 버린 정수' 를 `필요` 에도 적용하면 소모 2.5 신통이 `필요 2, 남은 것 2` 로 거절되어 어색하다. 계획의 처리: 스펙 글자대로 둘 다 버림(D111). 다르게 정하면 `필요` 만 `Numbers.decimal` 로 바꾼다.

**리드 판정 (확정, 2026-11-15)** — 아래가 위 후보와 D111 보다 앞선다.
- L6-1: 계획의 처리대로. 스펙 완료 시나리오의 `(1.8초)` 예는 Task 10 이 '곧바로 → 공통 대기 거절, 1.5초 뒤 → 재사용 대기 거절' 로 고친다.
- L6-2: 계획의 처리대로(막지 않음).
- L6-3: `필요` 는 소모량을 `Numbers.decimal` 로(2.5 → `필요 2.5`), `남은 것` 은 버린 정수 그대로. 근거: 버림이면 `필요 2, 남은 것 2` 로 거절되어 틀린 안내가 된다. Task 6 이 구현, Task 10 이 command.md 에 `(제안)` 으로 적는다.

**사전 점검 판정 (리드, 2026-11-15)** — 태스크 본문·D 결정보다 앞선다. 해당 태스크 구현자는 반드시 따른다.
- F1 (T5·T6): 몹이 쓰러질 때 한 상대 상한 지우기는 `receive` 가 아니라 `CombatActions.onMobKilled` 끝(깨달음을 쌓은 뒤)에서 한다.
- F2 (T4·T6): 깨달음 더하기는 JPQL 일괄 갱신을 쓰지 않는다. `findByCharacterIdAndKind` 로 찾아 엔티티 값을 고치고(dirty checking) 없으면 save.
- F3 (T1, D117): `changchunWithStages` 는 공법 단계 수에 맞춰 단계별 값을 만든다. 9단계가 아닌 공법은 `withStagePowers` 를 쓴다.
- F4 (T10, D111): D111 의 '`필요` 버림' 은 리드 판정 L6-3 으로 대체(`필요` = `Numbers.decimal`). Task 10 은 L6-1 의 index.md `(1.8초)` 예도 고친다.
- F5 (T6): 테스트 `insufficientManaShowsDecimalCost`(소모 2.5 → `필요 2.5, 남은 것 2`)를 더한다.
- F6 (T6): `capNotRepeated`·`insightCapResetsAfterReengage` 는 피해 0 신통(단계별 기본 위력 0·계수 0)으로 상황을 만든다.
- F7 (T11): 5차 단계 42 의 정리(몹 배치 삭제·시작 소지품 되돌림)를 단계 51 로 옮겨 한 번만 한다.
- F8 (T11): 단계 44 앞에 `버리기` 하나를 넣어 빈칸을 만들고 빈칸을 검사한다.
- F9 (T1): 'effectLines 빠짐·0개·6개 → 400' 문구는 `TechniqueEditorTest`(또는 관리 API 통합 테스트)에서 본다. WebMvc 테스트는 목록이 편집기로 넘어가는지만 본다.
- F10 (T5, D118): `귀속 비검`·`귀속`·`귀속 해제 비검` 파싱도 고칠 목록에 넣는다.
- F11 (T6): `TechniqueFinderTest` 는 없으므로 새 파일로 만든다.
- F12 (T6): 같은 이름 신통이 여럿일 수 있으므로 리포지토리는 `List` 로 받고 첫 값을 쓴다.
- F13 (T6): '1성 천안술' 은 배우는 단계가 1인 픽스처 변형을 쓴다.
- F14 (T6·T7): 소모 1 신통과 낙뢰술의 단계별 값은 화탄술과 같다(1성 기본 위력 2·계수 1.2 등).
- F15 (T2): 기존 신통 채우기 값은 `BigDecimal` 로 계산한다.
- F16 (T3, D104): 법력 회복 틱은 매 틱 모든 presence 의 회복 기준 시각을 먼저 갱신한다.
- F17 (T1, D99): 이펙트 문장 검증 순서는 계획대로 '검사 우선'(검사 종류마다 모든 줄을 본 뒤 다음 검사). Task 10 이 스펙 문장을 이에 맞춘다.
- F18 (D97·D106): 깨달음 계산은 계획대로 `domain/cultivation`, 새 의존 `combat → cultivation` 허용(순환 없음). Task 10 이 platform.md 를 고친다.
- F19 (T8): `toAbility` 는 `usableInGame` 을 빼고 보낸다. `abilityDraftOf` 도 고친다. `TechniqueForm` 의 단계 수 변경에서 `resizeStagePowers` 를 부른다.
- F20 (T5·T7): `Casting.seq` 카운터는 `CombatActions` 에 둔다. 행동 정렬 넷째 키는 '자동 공격 먼저'.
- F21 (T6): 검사 4(영력 0)의 '오행 신통' 은 `!bodyRefining` 이다(속성 없는 신통 포함).

## Review Focus

스펙이 직접 말하지 않지만 실제로 부딪칠 만한 것. 각 줄의 테스트는 담당 태스크에 들어 있다.

1. 교전 중 관리자가 공법을 저장(신통 행이 통째로 바뀜)·신통을 지움 → 재사용 대기는 남고(이름 키), 시전 중이던 신통은 발동 때 흩어지며(소모 없음), 다음 명령은 새 정의(단계별 값·이펙트 문장)를 쓴다. (Task 6 `cooldownSurvivesTechniqueSave`·`editedDefinitionUsedNextCast`, Task 7 `castingFizzlesWhenAbilityRemoved`)
2. 기존 pgdata 와 재시작 → 4차 신통은 채우기로 바로 쓸 수 있고(다시 떠도 덮어쓰지 않음), `mana_lost` 가 null 인 캐릭터는 법력 가득, 대기시간·시전은 다시 시작하면 사라진다. (Task 2 `fillIsIdempotent`·`fillKeepsExisting`, Task 3 `legacyNullIsFullMana`, Task 11 단계 확인)
3. 법력 경계 → 소모량과 같은 남은 법력이면 쓰고(20% 에서 발휘 100%), 1 모자라면 거절, 0~1 사이는 `1` 로 보이고, 회복은 최대치를 넘지 않으며 접속을 끊은 동안 차지 않는다. (Task 3 `manaRegenCapsAtMax`·`noRegenWhileOffline`, Task 6 `exactManaIsEnough`·`insufficientManaRejected`·`spiritOutputAtTwentyPercentIsFull`)
4. 신통으로 쓰러뜨림·질 때·끊김과 깨달음 상한·시전 → 처치·질 때·끊김이면 그 상대의 상한이 사라져 다시 싸우면 새로 세고, 시전은 질 때·끊김에 지워지며, 처치자·시체·수습은 5차 그대로. (Task 6 `abilityKillLeavesCorpse`·`insightCapResetsAfterReengage`, Task 7 `defeatClearsCasting`·`disconnectClearsCasting`)
5. 이름 해석 경계 → 명령어가 먼저(`보기` 라는 신통은 만들 수 없고 `상태 x` 는 모르는 명령), 대소문자 무시, 줄여 쓰기 없음, 두 공법의 같은 이름은 먼저 익힌 것, 배우지 않은 단계의 신통은 '깨치지 못한' 문구. (Task 6 `abilityNameIgnoresCase`·`abbreviationIsUnknown`·`firstLearnedWins`·`unlearnedStageRejected`, Task 5 `commandWordIsNotAbility`)

---

## 파일 구조 (요약, 새로 생기거나 바뀌는 것)

```
world-server/src/main/java/mudmk3/
  domain/shared/Realm.java                                                    (minorIndex)
  domain/technique/{AbilityStagePower,EffectLine}.java (새), Ability.java, AbilitySaveInfo.java, Technique.java
  domain/character/{Mana}.java (새), PlayerCharacter.java
  domain/cultivation/{InsightKind,CharacterInsight,Insight,InsightGain}.java (새), StatSheet.java
  domain/combat/{AbilityKey,Casting,AbilityTimers,AbilityRules,SpiritOutput,AbilityPower,AttackNature}.java (새), Damage.java, MobInstance.java, PlayerCombat.java
  domain/game/{Command,CommandParser}.java
  application/technique/{TechniqueModifyService,TechniqueQueryService}.java, provided/{AbilityRequest,StagePowerRequest(새),TechniqueView,TechniqueFinder}.java, required/TechniqueRepository.java
  application/character/{CharacterModifyService}.java, provided/CharacterVitalsEditor.java, required/CharacterRepository.java
  application/cultivation/{CultivationQueryService,CultivationModifyService}.java, provided/{CultivationFinder,CultivationProfile,InsightEditor(새)}.java, required/CharacterInsightRepository.java (새)
  application/game/{CombatActions,TargetLookup,AbilityResolver,LearnedAbility,AbilityCaster,AbilityCommandHandler,AbilityTexts,ManaRecovery}.java (새), {GameService,CombatEngine,CultivationTexts,CombatCommandHandler}.java, required/{GameMessage,AbilityTimersRepository(새)}.java
  adapter/persistence/memory/InMemoryAbilityTimersRepository.java (새), adapter/webapi/dto/{TechniqueDetailResponse,CharacterDetailResponse}.java
  resources/META-INF/orm.xml
world-server/src/test/java/mudmk3/CombatMemoryReset.java, domain/technique/TechniqueFixture.java, application/technique/provided/TechniqueRequestFixture.java, application/game/provided/BaseGameTest.java
admin-console/  src/lib/{api,techniques}.ts, src/components/{AbilityTable,AbilityDetailEditor(새),TechniqueForm,CharacterDetail}.tsx
play-client/    src/lib/{protocol,complete}.ts
docs/spec/system/*.md  README.md  scripts/smoke-test.mjs
```

---

### Task 1: 신통 정의 보강 — 단계별 값·이펙트 문장 도메인·영속·관리 서비스·REST

**권장 모델:** opus

**목적:** D98·D99·D100(신통 쪽). 기존 데이터 채우기는 Task 2, 렌더는 Task 5.

**Files:**
- Create: `domain/technique/{AbilityStagePower,EffectLine}.java`(`EffectLine` 은 이 태스크에서 `firstBadToken` 과 상수 `TARGET = "{대상}"`·`CASTER = "{시전자}"` 만), `application/technique/provided/StagePowerRequest.java`
- Modify: `domain/technique/{Ability,AbilitySaveInfo}.java`, `orm.xml`(Ability 값 컬렉션 둘, `AbilityStagePower` embeddable), `application/technique/provided/{AbilityRequest,TechniqueView}.java`, `adapter/webapi/dto/TechniqueDetailResponse.java`, 테스트 픽스처(D117)와 부르는 곳(D118)
- Test: `domain/technique/{AbilityTest(추가),EffectLineTest}`, `application/technique/required/TechniqueRepositoryTest`(추가), `application/technique/provided/TechniqueEditorTest`(추가), `adapter/webapi/TechniqueApiWebMvcTest`(추가)

**Interfaces:**
- Consumes: 4차 `Technique.create/update`, `Ability.create` 검사 순서.
- Produces: `AbilityStagePower`, `AbilitySaveInfo(…, List<String> effectLines, List<AbilityStagePower> stagePowers)`, `Ability.getEffectLines()/getStagePowers()/stagePower(int)/usableInGame()`, `EffectLine.firstBadToken(String)`, `AbilityView.effectLines/stagePowers/usableInGame`, `StagePowerRequest`, 응답 세 칸.

- [ ] **Step 1: 도메인 테스트** — `AbilityTest`(장춘공 9성, 화탄술 1성 기준, 실패는 `assertThatThrownBy(...).isInstanceOf(IllegalArgumentException.class).hasMessage(...)`):
  - `stagePowersOnNonAttackFail`(어풍결에 값 → `단계별 값은 공격 신통에만 둡니다: 어풍결`), `stagePowersMissingFail`(1~8성만), `stagePowersExtraFail`(1~10성), `stagePowersOrderFail`(2성 먼저) — 셋 다 `단계별 값은 배우는 단계부터 마지막 단계까지 하나씩 정하십시오: 화탄술`, `stagePowersFromLearnStage`(3성에 배우는 공격 신통은 3~9성 일곱 개로 저장됨), `basePowerScaleFail`(3성 `2.255` → `기본 위력은 소수 둘째 자리, 계수는 소수 셋째 자리까지 적으십시오: 화탄술 (3성)`), `coefficientScaleFail`(3성 `1.2245` → 같은 문구), `basePowerRangeFail`(3성 `10000.01`, `-1` → `기본 위력은 0~10,000으로 정하십시오: 화탄술 (3성)`), `coefficientRangeFail`(3성 `100.001` → `신통 계수는 0~100으로 정하십시오: 화탄술 (3성)`), `lowerStageReportedFirst`(2성 범위·3성 자리 → `(2성)` 범위 문구), `stagePowerScaleStored`(`2` → `2.00`, `1.2` → `1.200`).
  - `effectLinesZeroFail`·`effectLinesSixFail`(`이펙트 문장은 1~5개로 정하십시오: 화탄술`), `effectLineBlankFail`(`"   "`), `effectLineTooLongFail`(101자), `effectLineNewlineFail`(`"불\n꽃"`) — `이펙트 문장은 한 줄 1~100자로 적으십시오: 화탄술`, `effectLineHundredCharsOk`, `effectLineStripped`(`"  불꽃이 튄다!  "` → `불꽃이 튄다!`), `unknownPlaceholderFail`(`{적}에게` → `쓸 수 없는 자리표시가 있습니다: 화탄술 ({적})`), `unclosedBraceFail`(`{대상에게` → 뒤에 `}` 가 없어 `({)`), `strayCloseBraceFail`(`불꽃}` → `(})`), `targetPlaceholderOnSelfFail`(어풍결 `{대상}을 태운다` → `{대상}은 한 대상에게 쓰는 신통에만 쓸 수 있습니다: 어풍결`), `duplicateEffectLineFail`(`같은 이펙트 문장이 두 번 있습니다: 화탄술`), `noPlaceholderOk`, `casterPlaceholderOnSelfOk`, 순서 `stagePowerCheckedBeforeEffectLines`(둘 다 틀리면 단계별 값 문구), `lengthCheckedBeforePlaceholderAcrossLines`(1번 줄 `{적}`, 2번 줄 101자 → 길이 문구), `fourthCheckBeforeSixth`(재사용 대기시간 0 과 이펙트 0개 → 4차 재사용 대기 문구).
  - `usableInGame`: 화탄술 true, 철권 true, 어풍결 false, 화탄술 대상 `ALL_ENEMIES` false, 소모 `SENSE` false, 법력 틱마다 false.
  - `EffectLineTest.firstBadToken`: `{대상}·{시전자}` 없음 → 빈 값, `{적}` → `{적}`, `{{대상}` → `{{대상}`, `a}` → `}`, `{` → `{`.
- [ ] **Step 2: 실패 확인 → 구현.**
- [ ] **Step 3: 리포지토리·서비스·웹 테스트** — `TechniqueRepositoryTest`: `saveEffectLinesAndStagePowersInOrder`(flush·clear 뒤 순서·scale), `replaceAbilitiesReplacesCollections`(update 뒤 flush 오류 없음, 옛 줄 남지 않음). `TechniqueEditorTest`: `viewHasEffectLinesStagePowersAndUsable`(트랜잭션 밖 view), `updateReplacesEffectLines`, `stagePowerRequestNullFail`(`new StagePowerRequest(null, …)` → `IllegalArgumentException`). `TechniqueApiWebMvcTest`: `POST` 201 본문 `abilities[0].effectLines`·`stagePowers[0] == {stage:1, basePower:2.00, coefficient:1.200}`·`usableInGame:true`, 빠진 `effectLines` → 400 `이펙트 문장은 1~5개로 정하십시오: 화탄술`, `stagePowers:[{stage:null,…}]` → 400 `요청 본문을 읽을 수 없습니다`.
- [ ] **Step 4: 구현 → 통과.** 1~5차 테스트를 D117·D118 대로 고친다.
- [ ] **Step 5: 검증** — `./gradlew build`.
- [ ] **Step 6: 커밋** — `feat(world-server): 신통 단계별 값과 이펙트 문장 정의`.

**완료 조건:** 위 테스트 통과, 예외 문구가 admin-tool.md 6차 표와 글자 하나까지 같음, 1~5차 테스트 통과.
**progress.md 한 줄:** `AbilityStagePower`·이펙트 문장 값 컬렉션(표 둘), 6차 검사(D99), `usableInGame`, 요청·응답 세 칸, 픽스처 기본값.

---

### Task 2: 기존 신통 채우기 — 서버가 뜰 때

**권장 모델:** opus

**목적:** D102. 4차 pgdata 의 신통을 6차에 바로 쓸 수 있게 한다.

**Files:**
- Modify: `domain/technique/{Technique,Ability}.java`(`fillMissingAbilityData`·`fillMissing`), `application/technique/TechniqueModifyService.java`, `application/technique/required/TechniqueRepository.java`(필요하면 `findAll`)
- Test: `domain/technique/TechniqueTest`(추가), `application/technique/provided/TechniqueFillTest`(새, `@ApplicationServiceTest`)

**Interfaces:**
- Consumes: Task 1 칸.
- Produces: `Technique.fillMissingAbilityData() → int`, `TechniqueModifyService.fillMissingAbilityData()`(`@EventListener(ApplicationReadyEvent.class) @Order(20)`).

- [ ] **Step 1: 테스트** — 옛 상태는 네이티브 SQL 로 만든다(`delete from ability_effect_line`·`delete from ability_stage_power` 뒤 `entityManager.clear()`). `TechniqueFillTest`: `fillsSingleTargetLine`(화탄술 → `["{대상}에게 화탄술을 펼친다!"]`), `fillsSelfLine`(어풍결 → `["어풍결을 펼친다."]`, 천안술 → `["천안술을 펼친다."]`), `fillsElementalStagePowers`(화탄술 1~9성 `2.00·1.200` … `3.60·1.200`), `fillsFromLearnStage`(3성에 배우는 공격 신통 → 3성 2.00 … 9성 3.20), `fillsBodyRefiningStagePowers`(철권 1성 5.00·2.200 … 9성 9.00·2.200), `noStagePowersForNonAttack`, `fillKeepsExisting`(이펙트만 지운 신통 → 단계별 값은 원래 값 그대로), `fillIsIdempotent`(두 번 불러도 두 번째는 0개, 값 같음), `filledAbilityPassesValidation`(채운 공법을 그대로 `TechniqueEditor.update` 해도 400 없음). `TechniqueTest`: `fillMissingAbilityDataCount`.
- [ ] **Step 2: 실패 확인 → 구현.** 리스너 순서 `@Order(20)` 을 `CharacterModifyService`(0)·`ItemTypeModifyService`(10)와 같은 방식으로.
- [ ] **Step 3: 통과 → 검증** — `./gradlew build`.
- [ ] **Step 4: 커밋** — `feat(world-server): 서버 시작 때 기존 신통의 이펙트 문장·단계별 값 채우기`.

**완료 조건:** 위 테스트 통과, 채운 값이 admin-tool.md `### 기존 신통 채우기` 와 같음.
**progress.md 한 줄:** `@Order(20)` 채우기(이펙트 기본 문장·입문 공격 값), 멱등, 검사 우회 범위.

---

### Task 3: 현재 법력 — 깎인 법력 저장·경지 내림·상태·법력 회복 틱

**권장 모델:** opus

**목적:** D103·D104. 신통이 쓰는 법력 포트를 먼저 만든다.

**Files:**
- Create: `domain/character/Mana.java`, `application/game/ManaRecovery.java`
- Modify: `domain/character/PlayerCharacter.java`, `orm.xml`(`mana_lost`), `application/character/required/CharacterRepository.java`, `application/character/provided/CharacterVitalsEditor.java`, `application/character/CharacterModifyService.java`, `domain/cultivation/StatSheet.java`, `application/cultivation/CultivationQueryService.java`, `domain/combat/PlayerCombat.java`(`manaRegenAt`), `application/game/{CombatEngine,CultivationTexts}.java`, `application/game/required/GameMessage.java`(법력 게이지), `spotbugs-exclude-filter.xml`
- Test: `domain/character/ManaTest`, `domain/cultivation/StatSheetTest`(추가), `domain/combat/PlayerCombatTest`(추가), `application/character/required/CharacterRepositoryTest`(추가), `application/character/provided/{CharacterVitalsEditorTest,CharacterRealmEditorTest}`(추가), `application/game/provided/ManaRecoveryTest`(새, BaseGameTest), `test/java/mudmk3/application/game/provided/BaseGameTest.java`(`setManaLost`·`manaLost`)

**Interfaces:**
- Consumes: 5차 `CharacterVitalsEditor`·`writeQiBloodLost` 방식, `CombatEngine.tick`, `PresenceRepository.findAll`.
- Produces: `Mana`(D103), `CharacterVitalsEditor.manaLost/spendMana/recoverMana`, `StatSheet.manaCurrent()`·5인자 `of`, `ManaRecovery.recover(Instant)`, `PlayerCombat.manaRegenSeconds(Instant)`.

- [ ] **Step 1: 도메인 테스트** — `ManaTest`: `spendCapsAtMax`(최대 25, 깎인 22 + 5 → 25), `recoverNotBelowZero`, `demotionReducesLost`(QI_7 깎인 40 → QI_1: 깎인 10, 현재 15), `demotionToMortalClears`, `promotionKeepsLost`, `current`. `StatSheetTest`: `manaCurrentFromLost`(연기 1층 깎인 5.5 → 19.5), `manaCurrentNotBelowZero`. `PlayerCombatTest`: `manaRegenSecondsNullIsZero`, `manaRegenSecondsClampedToOne`(5초 뒤 1), `manaRegenSecondsHalf`(0.5초 뒤 0.5).
- [ ] **Step 2: 실패 확인 → 구현.**
- [ ] **Step 3: 서비스·리포지토리·게임 테스트** — `CharacterRepositoryTest`: `legacyNullIsFullMana`(native `mana_lost = null` → `readManaLost == 0`), `entitySaveDoesNotWriteManaLost`. `CharacterVitalsEditorTest`: `spendManaPersists`, `recoverManaFalseWhenFull`, `manaWriteDoesNotTouchQiBlood`. `CharacterRealmEditorTest`: `designateDownReducesManaLost`. `ManaRecoveryTest`(연기 1층, 법력 25): `statusShowsCurrentMana`(깎인 5.5 → `법력  19 / 25`, `status.stats.mana == {19, 25}`), `manaRegenPerSecond`(깎인 10 → `advanceAndTick(8)` → 깎인 9.0, `법력  16 / 25`), `manaRegenCapsAtMax`(깎인 0.1 → 1초 뒤 0), `manaRegenDuringCombat`(청모랑과 교전 중에도 참), `noRegenWhileOffline`(끊고 10초 틱 → 다시 접속 직후 깎인 값 그대로), `mortalNoRegen`(범인, 오류 없음), `noRegenWithoutLoss`(깎인 0 이면 쓰지 않음 — native `select mana_lost` 가 null 그대로), `enterStartsRegenClock`(접속 직후 0.5초 틱 하나 → 0.0625 만 참).
- [ ] **Step 4: 구현 → 통과.** 1~5차 기대값은 그대로(법력이 깎이지 않은 캐릭터).
- [ ] **Step 5: 검증** — `./gradlew build`.
- [ ] **Step 6: 커밋** — `feat(world-server): 깎인 법력 저장과 법력 자연 회복`.

**완료 조건:** 위 테스트 통과, 1~5차 테스트 그대로.
**progress.md 한 줄:** `player_character.mana_lost`(nullable, 네이티브 쿼리로만), `Mana`, `CharacterVitalsEditor` 법력 셋, 경지 내림 조정, `StatSheet.manaCurrent`, 상태 법력 현재 값, `ManaRecovery`(틱 2, 0~1초 비례).

---

### Task 4: 깨달음 — 저장·계산·보기(상태·공법·관리 REST)

**권장 모델:** opus

**목적:** D105·D106·D114 의 상태·공법 쪽과 D100 의 캐릭터 응답. 쌓는 곳(신통 발동)은 Task 6.

**Files:**
- Create: `domain/cultivation/{InsightKind,CharacterInsight,Insight,InsightGain}.java`, `application/cultivation/provided/InsightEditor.java`, `application/cultivation/required/CharacterInsightRepository.java`
- Modify: `domain/shared/Realm.java`(`minorIndex`), `orm.xml`, `application/cultivation/{CultivationQueryService,CultivationModifyService}.java`, `application/cultivation/provided/{CultivationFinder,CultivationProfile}.java`, `application/game/{GameService,CultivationTexts}.java`, `application/game/required/GameMessage.java`(`StatusMessage.insight`), `adapter/webapi/dto/CharacterDetailResponse.java`
- Test: `domain/shared/RealmTest`(추가), `domain/cultivation/{InsightGainTest,InsightTest}`, `application/cultivation/required/CharacterInsightRepositoryTest`, `application/cultivation/provided/{InsightEditorTest,CultivationFinderTest(추가)}`, `application/game/provided/GameConnectorTest`(추가), `application/game/CultivationTextsTest`(있으면 추가), `adapter/webapi/CharacterApiWebMvcTest`(추가)

**Interfaces:**
- Consumes: 4차 `Ability`·`Technique`, Task 3 `StatusMessage` 법력.
- Produces: `InsightKind`, `Insight`, `InsightGain.baseAmount/situationFactor/split/capped`, `Realm.minorIndex()`, `CultivationFinder.insight`, `InsightEditor.add`, `CultivationTexts.status(…, Insight)`·`techniques(…, Insight)`, `StatusMessage.insight`.

- [ ] **Step 1: 도메인 테스트** — `RealmTest`: `minorIndex`(MORTAL 0, QI_7 7, FOUNDATION_EARLY 1, FOUNDATION_PEAK 4). `InsightGainTest`: `baseAmountIsManaCost`(화탄술 5), `baseAmountForNoCost`(철권, 강체결 범인 → 연기 1층: 25 ÷ 5 × 4/3 = 6.666…, `isCloseTo(6.6667, within(1e-4))`), `situationFactor`(QI_1→QI_2 1.0, QI_7→QI_4 0.5, QI_7→QI_5 1.0, FOUNDATION_EARLY→QI_13 0.1, QI_1→FOUNDATION_EARLY 1.0, FOUNDATION_PEAK→FOUNDATION_EARLY 0.5), `splitFireballInChangchun`(화 5, 목 2.5), `splitWoodAbilityInWood`(목 3 — 천안술), `splitMultiElement`(화·토 신통 G 6 → 화 3·토 3; 화·토 공법의 화 신통 G 4 → 화 4·토 2), `splitBodyRefining`(철권 → BODY 6.67), `splitNoElementInElementalTechnique`(속성 없는 신통, 목 공법, G 4 → 목 2 만), `cappedScales`(화 5·목 2.5, 남은 4 → 화 2.667·목 1.333), `cappedZero`. `InsightTest`: `floor`, 빈 갈래 0.
- [ ] **Step 2: 실패 확인 → 구현.**
- [ ] **Step 3: 서비스·게임·웹 테스트** — `CharacterInsightRepositoryTest`: `addAmountUpdatesExisting`, `uniqueCharacterKind`. `InsightEditorTest`: `addCreatesThenAccumulates`(화 5 + 화 5 → 10, 소수 그대로 2.5 + 2.5), `skipsZero`. `CultivationFinderTest`: `insightEmptyIsZero`, `profileHasInsight`(트랜잭션 밖). `GameConnectorTest`: `statusInsightLine`(화 230.9·목 112.2 → `깨달음  금 0 · 목 112 · 수 0 · 화 230 · 토 0 · 연체 0`, `status.insight` 여섯 개 순서·`{FIRE, 화, 230}`), `techniquesShowCurrentInsight`(목 2.5 → `장춘공  1/9성 · 목 · 오행  다음 2성: 목 깨달음 120 (지금 2)`), `bodyRefiningTechniqueShowsBodyInsight`(연체 6.67 → `강체결  1/9성 · 연체  다음 2성: 연체 깨달음 530 (지금 6)`). `CharacterApiWebMvcTest`: 상세 `insight` 여섯 개(`{kind:"WOOD", name:"목", amount:112}`).
- [ ] **Step 4: 구현 → 통과.** 4차 `상태`·`공법` 기대(모두 0)는 그대로.
- [ ] **Step 5: 검증** — `./gradlew build`.
- [ ] **Step 6: 커밋** — `feat(world-server): 깨달음 저장·계산과 상태·공법 표시`.

**완료 조건:** 위 테스트 통과, 수치가 룰북 깨달음 절 예와 같음.
**progress.md 한 줄:** `character_insight`(+ `InsightKind` CHECK), `Insight`·`InsightGain`, `Realm.minorIndex`, `InsightEditor`, 상태 깨달음 줄·`status.insight`, `공법 (지금 n)`, 관리 캐릭터 상세 `insight`.

---

### Task 5: 신통 전투 도메인 — 위력·발휘·받는 쪽 배율 자리·대기시간·시전·이펙트 문장 렌더·한 상대 상한, 명령 파싱

**권장 모델:** opus

**목적:** D101·D107·D108·D109 와 D110 의 `CombatActions` 떼어 내기(동작 그대로). 게임 연결은 Task 6·7.

**Files:**
- Create: `domain/combat/{AbilityKey,Casting,AbilityTimers,AbilityRules,SpiritOutput,AbilityPower,AttackNature}.java`, `application/game/required/AbilityTimersRepository.java`, `adapter/persistence/memory/InMemoryAbilityTimersRepository.java`, `application/game/{CombatActions,TargetLookup}.java`
- Modify: `domain/technique/EffectLine.java`(`render`), `domain/combat/{Damage,MobInstance,PlayerCombat}.java`(`afterLayers`·`receiverMultiplier`, `receive` 시그니처, 상한, `delayAttackUntil`), `domain/game/{Command,CommandParser}.java`, `application/game/{CombatEngine,CombatCommandHandler,GameService}.java`(떼어 낸 것 쓰기, `UseAbility`·`Stop` 은 이 태스크에서 4차 `UNKNOWN` 문구 — Task 6·7 이 바꿈), `test/java/mudmk3/CombatMemoryReset.java`
- Test: `domain/combat/{AbilityTimersTest,SpiritOutputTest,AbilityPowerTest,DamageTest(추가),MobInstanceTest(추가),PlayerCombatTest(추가)}`, `domain/technique/EffectLineTest`(추가), `domain/game/CommandParserTest`(추가·고침), `adapter/persistence/memory/InMemoryAbilityTimersRepositoryTest`

**Interfaces:**
- Consumes: 5차 `MobInstance`·`PlayerCombat`·`CombatEngine`, Task 1 `EffectLine`.
- Produces: D107 클래스·포트, D108 함수, `EffectLine.render`, `Command.UseAbility`·`Command.Stop`, `CombatActions`·`TargetLookup`(D110 시그니처), `PlayerCombat.delayAttackUntil(Instant)`, `MobInstance.grantInsight/insightCapReachedFirstTime`.

- [ ] **Step 1: 테스트** —
  - `SpiritOutputTest`: `ratio(25,25)=1`, `ratio(5,25)=1`(20%), `ratio(2.5,25)=0.75`, `ratio(1.25,25)=0.625`, `ratio(0,25)=0.5`, `ratio(0,0)=1`.
  - `AbilityPowerTest`: `elemental(2, 1.2, 5, 1.0, 1.5) == 12`(룰북 예), `elemental(2, 1.2, 34, 1.0, 1.5) ≈ 64.2`, `elemental(2, 1.2, 5, 0.9, 1.5) ≈ 11.1`, `bodyRefining(5, 2.2, 3) ≈ 11.6`, `aptitudeMultiplier`(화 50 → 1.5, 화·토 신통에 화 50·토 없음 → 1.25, 속성 없음 → 1, 화 없는 영근 → 1).
  - `DamageTest`: `receiverMultiplierIsOne`(아무 `AttackNature`·`MobKind.GHOST` 도 1.0), `afterLayers(12, 1.0, 8) == 8`.
  - `MobInstanceTest`: 5차 테스트를 새 `receive` 로 고쳐 같은 값, `grantInsightCapsAtMaxQiBlood`(청모랑 49: 45 받고 7.5 요청 → 4), `capNotifiedOnce`, `capClearedOnDisengage`, `capClearedOnRefillAndRespawn`, `capPerCharacter`.
  - `PlayerCombatTest`: `delayAttackUntilKeepsLater`.
  - `AbilityTimersTest`: `globalRemaining`(시작 1.2초 뒤 0.3초, 1.5초 뒤 빈 값), `cooldownRemaining`(키별), `castingClear`.
  - `EffectLineTest.render`: `{대상}을` + `청모랑` → `청모랑을`, + `들토끼3` → `들토끼3를`, `{대상}를` + `청모랑` → `청모랑을`, `{대상}로` + `청모랑` → `청모랑으로`, + `늑대굴` → `늑대굴로`, `{대상}으로` + `들토끼` → `들토끼로`, `{대상}로부터` + `청모랑` → `청모랑으로부터`, `{시전자}는` + `한백` → `한백은`, `{대상}와` → `청모랑과`, `{대상}가` → `청모랑이`, `{대상}에게` 그대로, `{시전자}의 손바닥에서 화탄이 튀어 나가 {대상}에게 터진다!` + `한백`·`청모랑` → `한백의 손바닥에서 화탄이 튀어 나가 청모랑에게 터진다!`.
  - `CommandParserTest`: `화탄술 청모랑` → `UseAbility("화탄술", 청모랑)`, `화탄술 2.청모랑`, `화탄술` → `UseAbility("화탄술", null)`, `Bolt wolf`, `commandWordIsNotAbility`(`탐지` → `Unknown("탐지")`, `상태 한립` → `Unknown("상태")`, `북 가자` → `Unknown("북")`), `중지` → `Stop`, `중지 지금` → `Unknown("중지")`. D118 대로 기존 `Unknown` 기대 고침.
  - `InMemoryAbilityTimersRepositoryTest`: 저장·찾기·`deleteAll`.
- [ ] **Step 2: 실패 확인 → 구현.** `CombatActions` 로 떼어 낸 뒤 5차 `CombatTest`·`CombatRewardTest`·`CombatDefeatTest`·`CombatSpawnTest` 가 고치지 않고 통과해야 한다(동작 그대로).
- [ ] **Step 3: 통과 → 검증** — `./gradlew build`(ArchUnit: `domain/game → domain/technique` 순환 없음).
- [ ] **Step 4: 커밋** — 둘로 나눈다: `refactor(world-server): 교전 공통 경로를 CombatActions 로 떼어 내고 받는 쪽 배율 자리를 둠`, `feat(world-server): 신통 전투 도메인 — 대기시간·시전·위력·이펙트 렌더·한 상대 상한·명령 파싱`.

**완료 조건:** 위 테스트 통과, 5차 테스트 고치지 않고 통과(파서 기대 고침 제외), 수치가 룰북(공격력·법력 발휘)과 같음.
**progress.md 한 줄:** `CombatActions`·`TargetLookup`(이슈 #15 일부), `Damage.afterLayers/receiverMultiplier`(늘 1), `AbilityTimers`·포트·메모리 구현, `SpiritOutput`·`AbilityPower`, `EffectLine.render`, 몹 깨달음 상한, `UseAbility`·`Stop` 파싱.

---

### Task 6: 신통 쓰기 — 이름 해석·검사·즉시 발동·교전 합치기·이펙트 줄·깨달음 쌓기

**권장 모델:** opus

**목적:** command.md `### 신통 쓰기`(이름 해석·검사 1·3~7·성공 1~8)·`### 신통 교전 메시지`·`### 깨달음 쌓기`, D110·D111·D112. 시전(시전 시간 > 0)·`중지`·`신통` 목록은 Task 7 — 이 태스크에서 시전 시간이 있는 신통은 검사 통과 뒤 시전 시간 0 처럼 바로 쏘지 **않고** 임시로 `아직 쓸 수 없는 신통입니다` 문구를 쓴다(Task 7 이 바꿈, 테스트에서 다루지 않음).

**Files:**
- Create: `application/game/{AbilityResolver,LearnedAbility,AbilityCaster,AbilityCommandHandler,AbilityTexts}.java`
- Modify: `application/technique/provided/TechniqueFinder.java`·`TechniqueQueryService.java`·`required/TechniqueRepository.java`(`findAbilityName`), `application/game/{GameService,CombatEngine}.java`, `test/.../BaseGameTest.java`(`learn`·`insightOf`)
- Test: `application/game/provided/AbilityUseTest`(새), `application/game/provided/AbilityInsightTest`(새), `application/technique/provided/TechniqueFinderTest`(있는 곳에 추가: `findAbilityName`), `application/game/AbilityTextsTest`

**Interfaces:**
- Consumes: Task 1~5 전부, 5차 `CombatActions.onMobKilled`, `RandomGenerator`.
- Produces: `AbilityResolver`·`LearnedAbility`, `AbilityCaster.cast/fire`(fire 는 Task 7 이 부름), `AbilityCommandHandler.use`, `AbilityTexts`(문구 상수·`remaining(Duration)`), `TechniqueFinder.findAbilityName`.

- [ ] **Step 1: 테스트** — 준비는 D117(목·화 50, 연기 1층, 장춘공, 청모랑). 문구는 command.md 글자 그대로.
  - `AbilityUseTest`:
    - 성공: `fireballStartsCombatAndHits`(`화탄술 청모랑` → `청모랑과 싸움이 벌어진다.` 다음 `ScriptedRandom.enqueueInts(0)` 으로 고른 `손끝에서 붉은 불덩이가 피어올라 청모랑을 덮친다! (피해 8, 법력 20/25)`, 둘 다 `combat`; 같은 방 B notice `한백이 청모랑과 싸우기 시작한다.` event `engage`; B 는 이펙트 줄을 받지 않음), `effectLinePickedByRandom`(`enqueueInts(1)` → `한백의 손바닥에서 화탄이 튀어 나가 청모랑에게 터진다! (피해 8, 법력 20/25)`), `singleEffectLineUsesNoRandom`(`ScriptedRandom.intCalls() == 0`), `noManaCostShowsDamageOnly`(강체결 철권 → `단단히 쥔 주먹이 청모랑의 몸에 꽂힌다! (피해 8)`, 상태 법력 그대로), `targetOmittedInCombat`(교전 중 `화탄술` → 노리는 상대에게), `targetByOrdinal`(`화탄술 2.청모랑`), `castSwitchesTarget`(두 몹과 교전 중 다른 몹에 쓰면 노리는 상대가 바뀜 — 다음 자동 공격이 그 몹), `autoAttackStartsThreeSecondsAfterAbility`(`advanceAndTick(2.5)` 에 `일격` 줄 없음, 3초에 `청모랑에게 일격을 날린다. (피해 8)`), `castInCombatKeepsAutoAttackTime`, `abilityKillLeavesCorpse`(축기 초기 화탄술 위력 (2 + 34 × 1.2) × 1.5 = 64.2 → 한 번에 → 이펙트 줄 다음 `청모랑을 쓰러뜨렸습니다.`, 방 보기 시체 `청모랑의 시체`), `abilityDamageCountsForKiller`, `spiritOutputAtTwentyPercentIsFull`(깎인 20 → 법력 5/25 에서 화탄술 피해 8, 법력 0/25), `spiritOutputBelowTwentyPercent`(소모 1 신통, 깎인 21 → 위력 11.1 → `(피해 7, 법력 3/25)`), `exactManaIsEnough`, `mortalCanUseBodyRefiningAbility`(L6-2: 범인 철권 → `(피해 3)`), `editedDefinitionUsedNextCast`(관리로 1성 기본 위력 10 으로 저장 → 다음 화탄술 위력 (10 + 6) × 1.5 = 24 → 피해 20).
    - 이름: `abilityNameIgnoresCase`(`Bolt` 신통을 `bolt 청모랑`), `abbreviationIsUnknown`(`화탄 청모랑` → `무슨 말인지 모르겠습니다. ('도움말' 로 명령을 볼 수 있습니다)`), `unknownWordIsUnknown`(`뇌격 청모랑`), `unlearnedStageRejected`(`어풍결` → `어풍결은 아직 깨치지 못한 신통입니다.`), `notLearnedTechniqueRejected`(정의된 다른 공법의 `철권` → `철권은 아직 깨치지 못한 신통입니다.`), `firstLearnedWins`(두 공법에 같은 이름 `화탄술`, 먼저 익힌 공법 값으로 피해), `commandWinsOverAbility`(`공격 청모랑` 은 5차 공격).
    - 거절(모두 `error`, 소모·대기 없음 — 거절 뒤 바로 다시 써도 공통 대기 거절이 나오지 않음): `unusableFormRejected`(1성 천안술 → `천안술은 아직 쓸 수 없는 신통입니다.`, 법력 틱마다 신통도 같음), `targetMissingOutOfCombat`(`누구에게 쓰시겠습니까? (예: 화탄술 늑대)`), `targetNotHere`(`이곳에 '곰'은 없습니다.`), `targetNpc`(`왕씨는 공격할 수 있는 상대가 아닙니다.`), `targetPlayer`(`다른 사람과는 아직 싸울 수 없습니다.`), `noSpiritRejected`(장춘공을 익힌 뒤 범인으로 → `영력이 없어 술법을 펼칠 수 없습니다.`), `globalCooldownRejected`(1.2초 뒤 → `아직 기운을 고르는 중입니다. (0.3초)`), `cooldownRejected`(1.6초 뒤 → `화탄술은 아직 쓸 수 없습니다. (1.4초)`; 1.65초 뒤 → `(1.4초)` 올림), `cooldownOverAfterThreeSeconds`, `insufficientManaRejected`(깎인 22 → `법력이 모자랍니다. (필요 5, 남은 것 3)`), `manaBelowOneShowsOne`(깎인 24.5 → `남은 것 1`), `checkOrder`(영력 0 캐릭터가 교전 밖에서 대상 없이 `화탄술` → 순서 3 이 4 보다 먼저라 `누구에게 쓰시겠습니까? (예: 화탄술 늑대)`), `cooldownSurvivesTechniqueSave`(쓴 뒤 관리 저장 → 1.6초 뒤 재사용 거절 그대로), `castAllowedInCombatButMoveBlocked`(교전 중 화탄술 됨, `북` 은 5차 막힘).
  - `AbilityInsightTest`: `fireballInsight`(한 번 → 화 5·목 2.5, `상태` `깨달음  금 0 · 목 2 · 수 0 · 화 5 · 토 0 · 연체 0`), `ironFistInsight`(연체 6.67 → `연체 6`), `lowerMinorRealmHalves`(연기 7층이 청모랑(연기 2층) → 화 2.5·목 1.25), `lowerMajorRealmTenth`(축기 초기 → 화 0.5·목 0.25), `insightCapPerOpponent`(시계만 움직이고(`advance(3)`, 틱 없음 → 자동 공격 없음) 법력을 채워 가며 화탄술 7번: 6번째까지 화 30·목 15, 7번째 줄 순서 이펙트 → `같은 상대에게서는 더 깨닫는 것이 없습니다.`(`info`) → `청모랑을 쓰러뜨렸습니다.`, 합 49, `insightOf` 화 32.667·목 16.333), `capNotRepeated`, `insightCapResetsAfterReengage`(상한 뒤 교전이 끝나고(몹 다시 참) 다시 싸우면 또 쌓임), `rejectedCastAddsNoInsight`, `insightSurvivesDefeat`.
  - `AbilityTextsTest`: `remaining`(0.3 → `0.3초`, 1.35 → `1.4초`, 0.001 → `0.1초`, 59.95 → `1분`, 61.2 → `1분 2초`), `effectSuffix`(`(피해 1 미만, 법력 20/25)`).
- [ ] **Step 2: 실패 확인 → 구현.** `GameService.handleCommand` 에 `Command.UseAbility` → `abilityCommandHandler.use`.
- [ ] **Step 3: 통과 → 검증** — `./gradlew build`.
- [ ] **Step 4: 커밋** — `feat(world-server): 공격 신통 쓰기·이펙트 문장·깨달음 쌓기`.

**완료 조건:** 위 테스트 통과, 문구가 command.md·룰북과 글자 그대로, 5차 테스트 통과.
**progress.md 한 줄:** `AbilityResolver`·`AbilityCaster`·`AbilityCommandHandler`·`AbilityTexts`, 이름 해석·검사 순서·거절 문구, 즉시 발동 1~8, 교전 시작·자동 공격 시각, 이펙트 줄·난수, 깨달음 쌓기·상한, `findAbilityName`.

---

### Task 7: 시전과 `중지`·`신통` 목록 남은 시간·도움말

**권장 모델:** opus

**목적:** command.md `### 시전과 `중지``·`### 6차 정보 명령` 의 `신통` 줄·`### 6차 도움말`, D113·D114(`abilities`)·D115.

**Files:**
- Modify: `application/game/{AbilityCommandHandler,AbilityCaster,CombatEngine,GameService,CultivationTexts,AbilityTexts}.java`
- Test: `application/game/provided/{AbilityCastingTest(새),GameConnectorTest(추가)}`

**Interfaces:**
- Consumes: Task 6 전부, Task 5 `AbilityTimers.casting`.
- Produces: 시전 시작·발동·흩어짐·`중지`, 행동 정렬 키(D113), `CultivationTexts.abilities(…, @Nullable AbilityTimers, Instant)`, `GameService.HELP` 6차.

- [ ] **Step 1: 테스트** — 시전 신통 픽스처 `낙뢰술`(장춘공 1성, 공격·화·한 대상·법력 5·시전 2초·대기 3초, 이펙트 `하늘에서 내리친 뇌기가 {대상}을 꿰뚫는다!`).
  - `AbilityCastingTest`: `castStartLine`(`낙뢰술을 펼치기 시작한다… (2초)` `combat`, 상태 법력 25/25 그대로, 교전 시작 없음), `castFiresAfterTwoSeconds`(`advanceAndTick(2)` → `청모랑과 싸움이 벌어진다.`·`하늘에서 내리친 뇌기가 청모랑을 꿰뚫는다! (피해 8, 법력 20/25)`), `globalCooldownStartsAtCastStart`(시작 1초 뒤 `중지` 하고 화탄술 → `아직 기운을 고르는 중입니다. (0.5초)`), `otherAbilityBlockedWhileCasting`(`술법을 펼치는 중입니다. (끊으려면 '중지')`), `moveBlockedWhileCasting`(교전 밖 `북` → 같은 문구, 교전 중이면 `싸우는 중에는 움직일 수 없습니다.`), `otherCommandsAllowedWhileCasting`(`상태`·`말하기`·`공격 청모랑`), `stopCasting`(`중지` → `펼치던 술법을 거두었습니다.` combat, 2초 뒤 발동 없음, 소모 없음), `stopWithoutCasting`(`펼치는 술법이 없습니다.` error), `castingFizzlesWhenTargetDies`(다른 사람이 쓰러뜨림 → `펼치던 술법이 갈 곳을 잃고 흩어진다.`, 법력 그대로, 재사용 대기 없음), `castingFizzlesWhenAbilityRemoved`(관리로 낙뢰술을 지운 공법 저장), `castingFizzlesWhenTargetLeaves`(배치 지움), `castingManaShortAtFire`(시작 뒤 깎인 22 로 → `법력이 모자랍니다. (필요 5, 남은 것 3)` error), `castingNotInterruptedByHit`(교전 중 시전, 몹이 쳐도 발동), `autoAttackContinuesWhileCasting`, `defeatClearsCasting`, `disconnectClearsCasting`(다시 접속 뒤 `중지` → `펼치는 술법이 없습니다.`), `castingOrderAfterEngagedActions`(같은 틱에 교전 중 자동 공격과 교전하지 않던 사람의 시전 → 자동 공격 줄이 먼저), `castOnSpiritZeroAtFire` 는 두지 않음(D113 범위 밖).
  - `GameConnectorTest`: `abilitiesShowCooldownRemaining`(화탄술 1.2초 뒤 `신통` → `  화탄술  공격·화  법력 5  대기 3초  (1.8초 남음)`, 3초 뒤 꼬리 없음), `helpText`(command.md `### 6차 도움말` 문구 글자 그대로), `unknownWordStillUnknown`(`대화왕씨` → `무슨 말인지 모르겠습니다. ('도움말' 로 명령을 볼 수 있습니다)`).
- [ ] **Step 2: 실패 확인 → 구현.** Task 6 의 시전 임시 자리를 바꾼다.
- [ ] **Step 3: 통과 → 검증** — `./gradlew build`.
- [ ] **Step 4: WebSocket 확인** — `GameWebSocketIntegrationTest` 에 `abilityOverWebSocket` 하나(커밋되는 DB): 준비는 `TechniqueEditor.create(장춘공 요청)` + `CharacterTechniqueRepository.save(CharacterTechnique.learn(…))` + `CharacterRealmEditor.designate(QI_1)` 를 한 연기 1층이 `GameTicker.tick()` 한 번으로 나타난 들토끼에 `화탄술 들토끼` → `system` `tone:"combat"` 줄이 화탄술 이펙트 문장 셋 가운데 하나로 시작하고 `(피해 9, 법력 20/25)` 로 끝남(들토끼 육신 7: 12 − 3.5 = 8.5 → 9). 실제 난수라 문장은 셋 중 하나로만 단언. 정리는 5차 `cleanUp` 에 `AbilityTimersRepository.deleteAll()` 을 더한다.
- [ ] **Step 5: 커밋** — `feat(world-server): 신통 시전·중지와 신통 목록 남은 시간·6차 도움말`.

**완료 조건:** 위 테스트 통과, 문구가 command.md 와 글자 그대로.
**progress.md 한 줄:** 시전 시작·발동(틱 행동 정렬)·흩어짐·`중지`, 시전 중 막힘, 끊김·질 때 시전 지움, `신통` 남은 시간, `HELP` 6차.

---

### Task 8: admin-console — 신통 이펙트 문장·단계별 값 화면, 캐릭터 깨달음

**권장 모델:** opus

**목적:** D116 전부(admin-tool.md `## 6차 화면`).

**Files:**
- Create: `src/components/AbilityDetailEditor.tsx`
- Modify: `src/lib/{api,techniques}.ts`, `src/components/{AbilityTable,TechniqueForm,CharacterDetail}.tsx`
- Test: `src/lib/techniques.test.ts`(추가), `src/components/{AbilityDetailEditor,AbilityTable,TechniqueForm}.test.tsx`(추가·새), `src/components/CharacterDetail.test.tsx`(새)

**Interfaces:**
- Consumes: Task 1 REST(`effectLines`·`stagePowers`·`usableInGame`), Task 4 `insight`.
- Produces: 펼침 칸, 채우기, 행 맞추기, 깨달음 줄.

- [ ] **Step 1: 테스트** — `techniques.test`: `fillStagePowers(1, 9, DEFAULT_FILL)` → 1성 `2`·`1.2` … 9성 `3.6`·`1.2`, `fillStagePowers(1, 3, {basePower:"2", coefficient:"1.2", baseStep:"0.2", coefficientStep:"0.024"})` → 3성 계수 `1.248`, 반올림(`0.333` 계수 단계 → 셋째 자리), `resizeStagePowers`(1~9 → 배우는 단계 3: 3~9 값 그대로; 단계 수 9 → 11: 10·11성 = 9성 값 복사; 배우는 단계 3 → 1: 1성 기본값 `2`·`1.2`, 2성 = 1성 복사), `toAbility`(공격 아니면 `stagePowers: []`, 공격이면 숫자, `effectLines` 그대로), `newAbility(9)`(`effectLines: [""]`, 공격 기본이면 9줄). `AbilityDetailEditor.test`: 문장 `삭제` 가 1개일 때 비활성, `문장 추가` 가 5개에서 비활성, 안내 문구 `쓸 때 하나가 무작위로 보입니다. {대상} · {시전자} 를 쓸 수 있고, 바로 뒤 조사는 받침에 맞춰 바뀝니다. (예: {대상}을 → 청모랑을)` 글자 그대로, 공격일 때만 `단계별 값`·`채우기`, `채우기` 단추가 표를 채움, 표 열 `단계`·`기본 위력`·`계수`, 단계 이름 `1성`. `AbilityTable.test`: `자세히` 로 펼침·`접기`, 응답 `usableInGame:false` 행 끝 `(게임에서 아직 쓸 수 없음)`, 종류를 공격 → 둔술 → 공격으로 바꾸면 값이 돌아옴. `TechniqueForm.test`: 저장 요청 본문에 `effectLines`·`stagePowers`, 서버 400 `detail`(`이펙트 문장은 1~5개로 정하십시오: 화탄술`)을 보임. `CharacterDetail.test`: `깨달음  금 0 · 목 112 · 수 0 · 화 230 · 토 0 · 연체 0`.
- [ ] **Step 2: 구현 → 통과.** 4차 테스트의 응답 픽스처에 새 칸을 더한다.
- [ ] **Step 3: 검증** — admin-console 검증 명령.
- [ ] **Step 4: 커밋** — `feat(admin-console): 신통 이펙트 문장·단계별 값과 캐릭터 깨달음`.

**완료 조건:** 테스트·타입 검사·정적 빌드 통과, 화면 문구가 admin-tool.md 6차 화면과 글자 그대로.
**progress.md 한 줄:** `AbilityDetailEditor`(이펙트 1~5·채우기·단계별 값 표), 행 맞추기·숨긴 값 유지, `(게임에서 아직 쓸 수 없음)`, 캐릭터 깨달음 줄.

---

### Task 9: play-client — `중지` 자동완성, `status.insight`, 신통 줄

**권장 모델:** opus

**목적:** D115(클라이언트 쪽), platform.md `### 6차 WebSocket 메시지 변경`. 새 tone·새 type 없음 — 신통 줄은 5차 `combat` 색 그대로.

**Files:**
- Modify: `src/lib/protocol.ts`(`StatusMessage.insight: InsightEntry[]`, `InsightEntry = { kind: "METAL" | "WOOD" | "WATER" | "FIRE" | "EARTH" | "BODY"; name: string; amount: number }`), `src/lib/complete.ts`(`COMMANDS`)
- Test: `src/lib/{complete,render}.test.ts`, `src/components/Terminal.test.tsx`(필요하면)

**Interfaces:**
- Consumes: Task 4·6·7 메시지 모양.
- Produces: `COMMANDS` 6차 순서.

- [ ] **Step 1: 테스트** — `complete.test`: `COMMANDS` 가 command.md `### 6차 도움말` 순서(`보기 말하기 대화 공격 중지 수습 상태 공법 신통 읽기 소지품 줍기 버리기 주기 목록 사기 팔기 값 확인 취소 누구 도움말 종료` + 방위), `중` + Tab → `중지`, 신통 이름은 후보에 없음. `render.test`: 이펙트 줄(`system` `combat`)이 `combat` 스타일, 거절 줄(`error`), 상한 줄(`info`), `status` 에 `insight` 가 있어도 `text` 를 그대로 그림(여러 줄).
- [ ] **Step 2: 구현 → 통과.**
- [ ] **Step 3: 검증** — play-client 검증 명령.
- [ ] **Step 4: 커밋** — `feat(play-client): 6차 명령 자동완성과 상태 깨달음 타입`.

**완료 조건:** 테스트·타입 검사·정적 빌드 통과.
**progress.md 한 줄:** `COMMANDS` 6차(`중지`), `status.insight` 타입, 신통 줄 색 확인.

---

### Task 10: 문서 — 스펙 `(제안)` 반영과 README

**권장 모델:** sonnet

**목적:** 구현된 결정과 실행 안내를 문서에 옮긴다. 코드를 바꾸지 않는다.

**Files:**
- Modify: `docs/spec/system/{index,command,admin-tool,platform}.md`, `README.md`

**내용:**
- 스펙: `index.md` `### 6차 범위` 의 구현 계획 줄의 `(아직 없음)` 을 지우고, 5차와 같은 `**구현:**` 줄(날짜·계획·진행 링크 `../../impl/iteration-6/progress.md`)을 더한다. 스펙에 없는 구현 세부를 해당 절 끝에 `(제안)` 으로: D98(값 컬렉션 표와 `position` 열, scale) → platform.md 관리 데이터 줄, D99(단계마다 검사·이펙트 검사 단위·자리표시 훑기·글자 수 셈) → admin-tool.md 6차 REST API, D101(조사 고치기 범위·`으로` 먼저·뒤 글자) → admin-tool.md 이펙트 문장, D102(로그·검사 우회) → 기존 신통 채우기, D104(0~1초 비례·`PlayerCombat` 시각) → command.md 법력 회복, D107(상한을 몹에 둠) → platform.md 메모리 줄, D108(`afterLayers`·위협은 배율 곱한 값) → platform.md 위력·피해 자리, D109(예약어는 모르는 명령) → command.md 이름 해석, D111(자동 공격 시각·문장 고르는 난수·`필요` 버림) → command.md 성공·교전 메시지, D112(60초 이상 1초 올림) → command.md 남은 시간 줄, D113(정렬 무리·방 삭제 때 시전 남김) → command.md 시전·platform.md 서버 틱. progress.md 메모의 '계획과 다른 점' 을 코드 기준으로 적는다. 룰북에 없는 새 문구는 `룰북에 아직 없음` 표시 확인. 룰북 파일은 고치지 않는다.
- README: 모듈 소개에 6차 한 줄. `## 6차 시나리오 — 공격 신통·이펙트 문장·깨달음`(5차 시나리오 다음): 관리자 `공법` 에서 공법(목)과 공격 신통(화·한 대상·법력 10·대기 3초, `자세히` 로 이펙트 문장 셋, `채우기` 로 단계별 값) → 이펙트 문장을 비우고 저장해 400 확인 → 옥간·몹(귀물·음혼 정예 연기 1층) 배치 → play-client 에서 `읽기`·`신통` → `<신통> <몹>` → 이펙트 줄과 `(피해 …, 법력 …)` → 곧바로 다시(공통 대기)·1.5초 뒤(재사용 대기) 거절 → 법력 부족 거절 → `상태`·`공법` 깨달음 → 자동 공격으로 처치 → 법력이 차는 것. `## 되는 명령` 에 `<신통> [대상]`·`중지` 줄과 `도움말`(D115). 수동 확인 체크리스트 6차(admin 신통 펼침 칸·채우기·행 맞추기·`(게임에서 아직 쓸 수 없음)`, 캐릭터 깨달음 줄, play-client 이펙트 줄 색·`중지` Tab·시전 줄). 스키마 한 줄(`ddl-auto=update` 가 `ability_stage_power`·`ability_effect_line`·`character_insight` 표와 `player_character.mana_lost` 열을 더함, 서버가 뜰 때 기존 신통을 채움, 대기시간·시전은 메모리라 다시 시작하면 사라짐). 스모크 절은 Task 11 이 고친다.
- [ ] **Step 1: 수정.** 명령 이름·문구는 `GameService.HELP`·`AbilityTexts`·`CommandParser`·admin 화면 문구와 grep 으로 대조한다.
- [ ] **Step 2: 커밋** — `docs(spec): 6차 제안 결정 반영` 과 `docs(infra): README에 6차 시나리오·명령 반영`(둘로 나눠도 됨).

**완료 조건:** 위 D 각각이 스펙 어딘가에 `(제안)` 으로 있거나 이미 있는 스펙 문장과 같음, README 명령·문구가 코드와 같음, 1~5차 내용 유지.
**progress.md 한 줄:** 스펙에 옮긴 D 목록, 코드와 대조한 결과, README 6차 절.

---

### Task 11: 통합 확인 — 세 모듈 빌드·테스트, compose 한 번, 스모크 6차 시나리오

**권장 모델:** opus

**목적:** 실제 컨테이너(실제 1초 틱)와 **기존 pgdata** 에서 index.md 6차 완료 시나리오를 자동으로 확인한다. **이 이터레이션에서 Docker를 쓰는 유일한 태스크다.**

**Files:**
- Modify: `scripts/smoke-test.mjs`, `README.md`(스모크 절 6차 항목), `docs/impl/iteration-6/progress.md`

**1~5차 단계 고침 (D118):** 단계 24·35·42 `도움말` 기대를 D115 문구(`HELP_6`)로 같게 비교. 단계 26 의 공법 요청: 화탄술 `effectLines` 셋·`stagePowers` 1~9성(D117 값), 다른 신통은 이펙트 문장 하나(`{name} 기운이 퍼진다.`), 청목전은 이펙트 하나·1~8성 3·1.2. 그 밖은 그대로.

**6차 단계** (단계 42 뒤, 이름에 실행 번호 `<n>`. 새 이름: 공법 `열화공<n>`(9자), 신통 `염탄<n>`(8자), 옥간 `열화공<n> 옥간`, 몹 `음혼<n>` — 숫자로 끝나 받침 없음: `음혼<n>를`·`음혼<n>와`, `염탄<n>는`):
43. 관리자 — **기존 데이터 채우기 확인:** `GET /api/admin/techniques` 의 모든 공법 상세를 읽어 모든 신통의 `effectLines` 가 1개 이상, `kind == "ATTACK"` 인 신통의 `stagePowers` 수 = 단계 수 − 배우는 단계 + 1 인지 본다(기존 pgdata 의 4차 행 포함). **이펙트 문장 거절:** 공법 `열화공<n>`(목, 범인부터, 2성 — 1성 필요 깨달음 0, 2성 200) + 신통 `염탄<n>`(공격·화·한 대상·법력 10·시전 0·대기 3·전투 중만, `stagePowers` `[{1, 2, 1.2}, {2, 2.2, 1.2}]`)을 `effectLines: []` 로 → 400 `이펙트 문장은 1~5개로 정하십시오: 염탄<n>`, 여섯 줄로 → 같은 400. 이펙트 문장 셋(`붉은 불씨가 {대상}을 감싸 터진다!`, `{시전자}의 손끝에서 튄 불꽃이 {대상}에게 꽂힌다!`, `작은 화염이 꼬리를 끌며 {대상}로 날아든다!`)으로 → 201, 상세 `effectLines` 셋·`stagePowers[1].basePower == 2.2`·`usableInGame == true`. 옥간 `열화공<n> 옥간`(SCROLL, `techniqueId`) 광장 바닥에 1개. 몹 `음혼<n>`(GHOST, `QI_1`, ELITE, `서늘한 손길`, 재등장 30) → 201 `stats {120, 3, 7}`, 광장에 1 배치.
44. E `줍기 열화공<n> 옥간` → `읽기 열화공<n> 옥간` → 첫 줄 `옥간을 이마에 대자 구결이 머릿속으로 흘러든다. 열화공<n> 1성에 이르렀습니다!`·`새 신통을 깨쳤습니다: 염탄<n>`. E `신통` 에 `  염탄<n>  공격·화  법력 10  대기 3초`. E `상태` → `법력  25 / 25`(5차 끝 E 는 연기 1층).
45. E `보기` 되풀이(≤ 5초) → `mobs` 에 `음혼<n>`. E `염탄<n> 음혼<n>` → combat `음혼<n>와 싸움이 벌어진다.` 다음 줄이 세 렌더 문장(`붉은 불씨가 음혼<n>를 감싸 터진다!`, `<E 이름>의 손끝에서 튄 불꽃이 음혼<n>에게 꽂힌다!`, `작은 화염이 꼬리를 끌며 음혼<n>로 날아든다!`) 가운데 하나 + ` (피해 11, 법력 15/25)`(위력 (2 + 5 × 1.2) × 1.5 = 12, 육신 3 → 10.5 → 11).
46. E 곧바로 `염탄<n> 음혼<n>` → error `아직 기운을 고르는 중입니다. (` 로 시작. 300ms 간격으로 다시 보내(≤ 3초) error `염탄<n>는 아직 쓸 수 없습니다. (` 로 시작하는 줄을 받을 때까지(L6-1), 그다음 성공(≤ 4초)할 때까지 되풀이 → 이펙트 줄이 ` (피해 11, 법력 5/25)` 로 끝남(회복이 1 미만이라 5 — `법력 [5-9]/25` 로 비교). E `상태` → `status.stats.mana.current < 10`.
47. 재사용 대기가 끝날 때까지 되풀이(≤ 4초) → error `법력이 모자랍니다. (필요 10, 남은 것 ` 로 시작하고 남은 것이 10 미만.
48. E `상태` text 에 `깨달음  금 0 · 목 10 · 수 0 · 화 20 · 토 0 · 연체 0`, `status.insight` 의 FIRE 20·WOOD 10. E `공법` 의 `열화공<n>` 줄에 `목 깨달음 200 (지금 10)`. 관리자 `GET /api/admin/characters/<E id>` 의 `insight` 에 `{FIRE, 화, 20}`. 
49. 기다림(≤ 45초): `음혼<n>를 쓰러뜨렸습니다.`(자동 공격으로). E 가 지지 않음(`기혈이 다했습니다.` 가 오면 실패).
50. 법력 회복: E `상태` 를 1초 간격으로 되풀이(≤ 15초) → `mana.current` 가 단계 46 의 값보다 커짐.
51. E `도움말` 이 `HELP_6` 과 같음. 관리자 — 이 실행의 몹 배치 삭제 → 광장 `mobs` 에 `음혼<n>` 없음(≤ 5초 폴링). 시작 소지품 되돌림(5차 단계 42 방식).

- [ ] **Step 1: 스크립트 수정** — 고침과 43~51을 더한다(Node 26 내장 `fetch`·`WebSocket` 만). 머리 주석 '1~5차' 를 '1~6차' 로. 고정 시간 대기 없이 `waitFor`·되풀이로만. 실패해도 이 실행의 몹 배치를 `finally` 에서 지운다(5차 방식).
- [ ] **Step 2: 모듈 검증** — 세 모듈 검증 명령을 다시 돌려 통과 확인.
- [ ] **Step 3: compose 한 번** — 시작 전 `df -h .`·`docker system df` 를 메모에(10GB 미만이면 멈춤). Global Constraints 스모크 방법대로 `docker compose build` **한 번** → `up -d`(셸 값 덮어쓰기, **기존 pgdata**) → 기동 로그에 스키마 오류 없음, `기존 신통 …개의 이펙트 문장·단계별 값을 채웠습니다.` 로그(기존 4차 신통이 있으면), 틱 오류 로그 없음 확인 → psql 로 `mana_lost` 열이 생기고 기존 캐릭터는 null, `character_insight_kind_check` 가 여섯 값을 모두 포함, `ability_effect_line` 에 기존 신통 행이 있음 확인 → `source "$NVM_DIR/nvm.sh" && nvm use 26.13.1 && node scripts/smoke-test.mjs` → `SMOKE OK`. 실패하면 원인을 고치고(해당 모듈 테스트 통과) 바뀐 서비스만 `docker compose up -d --build <서비스>`(전체 재빌드 반복 금지). PostgreSQL CHECK 로 저장이 막히면 Global Constraints 의 enum 규칙대로 원인 제약을 기록하고 리드에게 보고.
- [ ] **Step 4: 되돌리기** — 관리 API로 시작 방 id 1·시작 소지품 `[]` 확인(`GET /api/admin/world`·`/world/starting-items`), psql 로 `mob_placement` 에 이 실행의 배치가 없음 확인.
- [ ] **Step 5: 정리** — `docker compose down`(볼륨 남김) → `docker image prune -f` → `docker builder prune -f` → `world-server/build`, `play-client/{out,.next}`, `admin-console/{out,.next}` 삭제(`git check-ignore` 로 먼저 확인). `mud-mk3-*` 이미지·`pgdata`·`node_modules` 는 지우지 않는다. 정리 뒤 `df -h .`·`docker system df` 를 메모에.
- [ ] **Step 6: README 스모크 절** — 6차 항목 두 줄(신통 이펙트 문장·단계별 값 정의와 0·6개 거절·기존 신통 채우기 / 신통 사용·이펙트 줄·공통·재사용 대기·법력 부족·깨달음·처치·법력 회복, 스모크는 실제 틱을 기다려 약 1분 더 걸림).
- [ ] **Step 7: progress.md** — 결과(통과 여부, 고친 것, 디스크 전후 값, 남은 수동 확인), 상태를 "6차 이터레이션 구현 완료" 로.
- [ ] **Step 8: 커밋** — `test(infra): 6차 스모크 시나리오(신통 정의·이펙트 문장·사용·대기·법력·깨달음)`.

**완료 조건:** compose 환경에서 단계 1~51 `SMOKE OK`(기존 pgdata), 세 모듈 검증 통과, 시작 방 id 1·시작 소지품 `[]`·이 실행 몹 배치 0, 정리 뒤 dangling 이미지 없음, pgdata 남음.
**progress.md 한 줄:** 스모크 고침·단계 43~51, compose 빌드 한 번, 기존 pgdata 채우기·CHECK 확인, 디스크 전후 값, 되돌림 확인.

**완료 시나리오 대응** (index.md 6차 완료 시나리오 → 스모크 단계): 공법·공격 신통(이펙트 여럿·단계별 값) 정의 43 · 0개·6개 거절 43 · 옥간 43 · 읽기로 익힘 44 · `신통` 확인 44 · 몹에게 신통 45 · 교전 시작·이펙트·피해 45 · 법력 감소 46 · 재사용 대기 거절 46 · 법력 부족 거절 47 · 깨달음 48 · 처치 49 · 법력 참 50.

---

### Task 12: 최종 전체 리뷰

**권장 모델:** opus

**목적:** 6차 전체(main 의 6차 커밋 전부)를 스펙·룰북·이 계획과 대조한다. 코드를 고치면 따로 커밋한다.

- [ ] **Step 1: 리뷰** — `superpowers:requesting-code-review` 로 6차 첫 커밋 앞부터 HEAD 까지. 볼 것: Review Focus 다섯 줄이 테스트로 덮였는지, 문구가 룰북·스펙과 글자 그대로인지(`AbilityTexts`, `CultivationTexts`, `GameService.HELP`, admin 화면), 위력·깨달음 수치(룰북 예), 검사 순서(command.md 1~7·admin-tool.md 6차), 게임 락 밖에서 메모리 상태(`AbilityTimers`·몹 상한)를 건드리는 곳이 없는지, 메모리 객체가 엔티티를 들고 있지 않은지, 깎인 법력이 엔티티 저장으로 되돌려지지 않는지, 받는 쪽 배율 자리(5차 공격·몹 공격도 지나는지), ArchUnit 의존(D97·D110 순환 없음), 기존 pgdata 채우기의 멱등성, PostgreSQL enum CHECK(새 enum 열·값), `(제안)` 반영 누락, 리드 판정 후보 L6-1~3 처리.
- [ ] **Step 2: Critical·Important 고침** — 해당 모듈 검증 통과 후 커밋 `fix(<module>): …`. Docker는 다시 빌드하지 않는다(다시 스모크가 필요하면 리드에게 묻는다).
- [ ] **Step 3: progress.md** — 리뷰 결과·고친 것·다음으로 미룬 Minor(`## 다음 할 일`), 창호 확인 항목(`(제안)` 결정 — 특히 법력 부족 거절(룰북 무리한 사용과 다름), 임시 `공격`·자동 공격 유지, 이펙트 문장 자리표시·조사, 기존 신통 기본 문장, L6-1~3, 브라우저 수동 확인).
- [ ] **Step 4: 커밋** — `docs(impl): 6차 이터레이션 완료 기록`.

**완료 조건:** Critical·Important 0, progress.md 완료 기록.
**progress.md 한 줄:** 최종 리뷰 결과와 고친 것.

---

## 태스크 의존 순서

```
1 → 2 ─────────────────────────────┬→ 8
3 ──┐                              │
4 ──┼→ (1·3·4) → 5 → 6 → 7 → 9 ────┴→ 10 → 11 → 12
```

- 순서대로 하면 된다: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12. 나란히 하지 않는다(여러 태스크가 `orm.xml`·`GameService`·`CombatEngine`·`GameMessage`·`CultivationTexts`·`StatSheet` 를 고친다).
- 8은 1·4 뒤(REST 모양), 9는 7 뒤(실제 메시지와 맞춤). 10은 구현된 값을 옮기므로 9 뒤, 11은 10 뒤, 12는 마지막.
- 각 태스크는 앞 태스크가 커밋된 상태에서 혼자 검증 명령이 통과한다(Task 5 는 `UseAbility`·`Stop` 을 모르는 명령으로, Task 6 은 시전 신통을 임시 거절로 둔다).

## 5차에서 미룬 Minor

5차 남은 문제는 GitHub 이슈 #1~#18 에 있다(`iteration-5/progress.md` `## 상태`).

**6차에서 함께 고치는 것** (6차 작업과 직접 부딪침):
- #15 `CombatEngine` 나누기와 `notifyOthers`·묶음 키 중복 → Task 5 가 교전 공통 경로를 `CombatActions` 로 떼어 낸다(`notifyOthers` 는 거기 하나만). 묶음 키 중복은 그대로 두고 이슈에 남긴다. 이슈에 6차 진행 댓글.
- #9 world-server 테스트 위생 가운데 `CombatMemoryReset` 에 저장소를 더하는 것 → Task 5 가 6차 저장소를 더하며 함께.

**그대로 둠** (6차 작업과 부딪치지 않음): #11 드롭 표 스냅샷, #12 틱·명령 실패 때 메모리 불일치(6차 대기시간·시전도 같은 한계 — 이슈에 한 줄 댓글), #13 `room` 재전송, #14 전투 메모리 정리, #16·#17·#18, 1~4차 이슈 #1~#8·#10.
