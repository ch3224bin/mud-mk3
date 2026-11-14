# 4차 이터레이션 구현 계획 — 경지·스탯 + 임의 경지 상승 + 공법·신통 배우기

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 캐릭터가 경지(범인·연기 1~13층·축기 초기~대원만)와 영근을 가지고, 스탯을 룰북 표대로 계산해 `상태` 로 본다. 관리자는 admin-console에서 공법·신통을 정의하고 옥간·서책에 담고, 캐릭터 경지를 바로 지정한다. 플레이어는 생성 때(기존 캐릭터는 다음 접속 때) 영근을 고르고, `읽기` 로 옥간을 읽어 공법 1성과 신통을 익히며(범인이 입문 공법을 읽으면 연기 1층), `공법`·`신통` 으로 본다. 저물탁(축기 초기부터 칸이 열림)과 범인의 상점 막힘을 더한다.

**Architecture:** 3차 구조를 그대로 넓힌다. `world-server`(헥사고날)에 슬라이스 `technique`(공법·신통 정의)와 `cultivation`(익힌 공법·스탯 계산·읽기)을 더하고, 경지·영근은 `character` 에, 경지·오행 enum은 `domain/shared` 에 둔다(`technique` 이 `character` 를 쓰지 않게). 저장하는 것은 경지·영근·익힌 공법뿐이고 스탯·배운 신통은 명령마다 계산한다(현재 수치 = 최대치, 리드 판정 L4). 관리 툴 경지 지정은 커밋 뒤(AFTER_COMMIT) 접속 중인 그 캐릭터에게만 알린다. 서버 틱은 여전히 없다. play-client에는 새 메시지 `status`, `tone: "status" | "growth"` 가 생긴다.

**Tech Stack:** 1~3차와 같다 — Java 25 · Spring Boot 4.1.2 · Gradle 9.9.1 Kotlin DSL · PostgreSQL 18.6 / H2(테스트) · Next.js 16.4.6 · React 19.3.1 · TypeScript 7.0.2 · Vitest 5.1.0 · Docker Compose. **새 의존성 없음.**

**Spec:** 아래 `## Spec`. 계획은 스펙과 룰북을 근거로 하고, 스펙이 정하지 않은 구현 세부는 `## 제안 결정 목록`(D57~D76)에서 정했다.

---

## Spec

- 시스템 스펙(4차 결정의 원본, 모두 `(제안)`): `docs/spec/system/index.md`(`### 4차 범위`, `### 4차 이터레이션 열린 질문`), `command.md`(`## 4차 명령어` 아래 전부), `admin-tool.md`(`## 4차 범위`·`## 4차 데이터`·`## 4차 화면`·`## 4차 REST API`, `### 관리 툴 변경의 반영`), `platform.md`(`### 4차 WebSocket 메시지 변경`, `### 4차 월드 상태와 저장`, `#### 경지 몫 스탯 표`, `## 저장소 구성` 4차 줄, `## 플레이 화면` 4차 줄)
- 리드 판정 L1~L5: `docs/impl/iteration-4/progress.md` `## 리드 판정`
- 룰북(규칙·수치·문구의 원본): `docs/spec/game-rule/realm.md`(경지, 소경지 필요 수위, 경지별 스탯 상승), `stats.md`(스탯 분류, 소수점, 최대치가 바뀔 때, 기혈·법력·의식·정혈·육신·근력·영력, 영근 · 영근 자질), `character.md`(생성 절차, 입문, 튜토리얼 출력 예, 범인 상태), `technique.md`(공법, 공법이 가지는 것, 신통이 가지는 것, 공법 예시: 장춘공, 잔본 규칙 문구), `technique-list.md`(오행 입문 공법, 강체결, 오행 연기 후반 공법 — 스모크·README 콘텐츠 값), `item.md`(저물대 · 저물탁), `commands.md`(정보 출력 형식, 성장, 상태별로 쓸 수 없는 명령, 출력 문체, 이름 겹침 정리)
- 1~3차 계획·진행: `docs/impl/iteration-1/plan.md`(D1~D18, 버전 표), `iteration-2/plan.md`(D19~D31), `iteration-3/plan.md`(D32~D56), `iteration-3/progress.md`(리드 판정, `## 후속 수정: 귀속·거래 가능 삭제`, 미룬 Minor)
- world-server 코딩 규칙: `.claude/rules/*.md`. **world-server 태스크는 시작 전에 모두 읽는다.**
- 용어: `docs/spec/glossary.md`
- 4차 범위 밖(만들지 않는다, index.md `4차에 넣지 않는 것` 그대로): 신통 쓰기·전투·몹, 깨달음 쌓기·`참오`·잔본·산공·서재(`서재`·`서재 익히기`·`읽기 확인`), 수위 쌓기·소경지 상승·돌파·성공률, 경지 안정 기간, 단약 효과, 본명법보, 귀속, 퀘스트, LLM 챗, 튜토리얼(`고르기`·`튜토리얼 건너뛰기`), 성별·외모·출신, 결단 이후 경지, 현재 수치 저장(L4), 청우촌 밖 이동 막힘, 영수대, 아이템 필요 경지 칸, 품질, 경지 조건 낱말, 권장 경지 표시.

## Global Constraints

모든 태스크의 요구 사항에 이 절과 `## 제안 결정 목록` 이 포함된다. 구현자는 이 두 절과 `progress.md` 의 `## 리드 판정` 을 먼저 읽는다.

**버전·의존성 (추측 금지)** — 1차 계획 `## Global Constraints` 버전 표를 그대로 쓴다. **새 라이브러리·플러그인·이미지를 넣지 않는다.** `build.gradle.kts`, `package.json`, `package-lock.json`, Dockerfile, `compose.yaml` 의 버전 줄을 바꾸지 않는다. 꼭 필요해 보이면 멈추고 리드에게 보고한다(리드가 공식 출처에서 최신 안정판을 확인하고 이 계획에 근거를 적은 뒤에만 넣는다).

**로컬 환경**
- macOS arm64, 기본 JDK Temurin 25. world-server는 항상 `./gradlew`.
- 프론트 태스크는 Node 26.13.1: 명령 앞에 `source "$NVM_DIR/nvm.sh" && nvm use`(모듈의 `.nvmrc`).
- **디스크:** 태스크 시작 전 `df -h .` 를 본다. 남은 공간이 **10GB 미만이면 아무것도 지우지 말고 멈춰** 리드에게 보고한다. `ENOSPC` 도 같다.
- **Docker 빌드·실행은 Task 14에서 한 번만** 한다. Task 1~13은 `docker build`, `docker compose build/up` 을 쓰지 않는다. `pgdata` 볼륨은 어느 태스크에서도 지우지 않는다.

**문구·규칙**
- 화면 문구·서버 메시지·예외 메시지는 한국어. **룰북 수치·문장은 글자 그대로 쓰고, 룰북에 없는 문구는 시스템 스펙(이 계획이 인용한 문장)을 글자 그대로 쓴다.** 스펙에도 없는 문구는 이 계획에 적힌 문장을 글자 그대로 쓴다.
- 조사는 `Josa`(3차 규칙: 끝의 닫는 괄호·문장 부호를 건너뛰고, 숫자·영문으로 끝나면 받침 없음)로 고른다. 4차에 `으로/로` 를 더한다(D57). 수는 1,000 이상이면 쉼표(`Numbers.count`), 소수는 `Numbers.decimal`.
- `(제안)` 관례: 스펙·룰북에 없는 결정은 이 계획의 D번호로 정하고, Task 13이 시스템 스펙의 해당 절에 `(제안)` 을 붙여 옮긴다. 룰북에 없는 문구는 스펙에 "룰북에 아직 없음" 으로 표시한다. 룰북 파일(`docs/spec/game-rule/`)은 고치지 않는다.
- 툴 호출 파라미터(JSON)의 한글은 리터럴 UTF-8로 쓰고 `\uXXXX` 이스케이프를 쓰지 않는다.
- 계획 문구의 `{이름}`·`{n}` 은 채워 넣는 자리, `{이름은}` 처럼 조사가 붙은 자리는 `Josa` 로 고른 조사를 붙인다는 뜻이다.
- world-server 코딩 규칙(`.claude/rules/*.md`), 루트 패키지 `mudmk3`. 아래 world-server 경로는 `world-server/src/{main,test}/java/mudmk3/` 기준이다. open-in-view가 꺼져 있으므로 REST 응답에 쓰는 지연 연관은 서비스가 트랜잭션 안에서 값 record로 바꿔 돌려주거나 `@EntityGraph` 로 읽는다(3차 관례).
- **새 enum 열은 모두 orm.xml 에 `column-definition="varchar(N)"` 을 준다**(3차 `item_type.category`·`bag_grade` 와 같음). 이렇게 하면 Hibernate 가 enum CHECK 제약을 만들지 않는다(3차 H2 DDL `bag_grade varchar(16)` 에 CHECK 없음을 확인함). 그래서 기존 `pgdata` 의 `bag_grade` 에 `BRACELET_*` 를 넣을 수 있고, 4차 표(`realm`, `element`, `kind`, `target`, `cost_stat`, `usable_when`, `start_realm`)도 나중에 값을 더할 때 막히지 않는다. Task 14 Step 3 에서 pgdata 로 저물탁 저장(단계 34)이 되는지가 이것의 확인이다.
- Next.js 앱 규칙은 1~3차와 같다(정적 export, `"use client"`, 상대 경로 import, 쿼리스트링 id). 저장 전 클라이언트 검사는 하지 않고 서버 `detail` 을 보인다.
- 자동 테스트는 Google 자격 증명·네트워크 없이 돈다. 시간에 걸린 테스트는 `MutableClock` 을 쓴다.

**저장소·커밋**
- **main 에서 일한다.** 태스크 하나 = 커밋 하나 이상. 구현자는 커밋까지 하고, 리뷰를 통과하면 리드가 태스크마다 `git push` 한다. 구현자는 서브에이전트를 띄우지 않는다.
- 커밋 메시지: `<type>(<module>): <한국어 요약>`(type: feat, test, build, docs, chore, fix, refactor / module: world-server, play-client, admin-console, infra, spec, impl). 끝에 빈 줄 다음 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- 태스크를 끝내면 `docs/impl/iteration-4/progress.md` 의 표에 한 줄(태스크 번호·제목, 커밋 해시는 리드가 채움, 메모)을 더하고 같은 커밋에 넣는다. 각 태스크 끝의 **progress.md 한 줄** 이 그 메모의 틀이다(실제로 바꾼 것·계획과 다른 점을 덧붙인다).

**검증 명령 (각 태스크 완료 조건에 포함, 고친 모듈만)**
- world-server: `cd world-server && ./gradlew build`(테스트 + SpotBugs + ArchUnit)
- admin-console / play-client: `cd <module> && source "$NVM_DIR/nvm.sh" && nvm use && npm ci && npm test && npx tsc --noEmit && npm run build`

**스모크 (Task 14만)** — README `### 스모크 테스트` 방법 그대로: `.env` 는 고치지 않고, `docker compose build` 한 번 → `up` 때만 셸 값으로 `MUD_DEV_LOGIN_ENABLED=true`, `MUD_ADMIN_EMAILS=<.env 의 기존 값>,smoke-admin@example.com` 을 덮어써 `docker compose up -d` → `node scripts/smoke-test.mjs`. 끝나면 **시작 방을 id 1(태초 객잔)로, 시작 소지품을 `[]` 로 되돌리고** 확인한다.

## 제안 결정 목록

스펙이 정하지 않은 구현 세부. 번호는 3차(D32~D56)에 잇는다. 스펙이 이미 정한 것(문구·REST 표·화면 칸·스탯 표)은 다시 적지 않고 절을 가리킨다 — **그 절의 문장이 곧 요구 사항이다.** Task 13이 D를 스펙에 `(제안)` 으로 옮긴다.

**D57. 경지 `Realm`** — `domain/shared/Realm`(enum, `technique` 과 `character` 가 함께 씀). 선언 순서 = 경지 순서: `MORTAL, QI_1 … QI_13, FOUNDATION_EARLY, FOUNDATION_MIDDLE, FOUNDATION_LATE, FOUNDATION_PEAK`. 값은 platform.md `#### 경지 몫 스탯 표` 를 **상수 표로** 옮긴다(식으로 계산하지 않음). 메서드: `String koreanName()`(`범인`·`연기 1층`·`축기 대원만`), `int body()`, `int strength()`, `int spirit()`, `int senseEssence()`(의식·정혈 같은 값), `OptionalInt requiredCultivation()`(범인은 비어 있음), `boolean isBelow(Realm other)`(ordinal 비교), `boolean opensBracelet()`(`FOUNDATION_EARLY` 이상). `Josa` 에 `euroRo(String)`·`withEuroRo(String)`: 받침이 없거나 ㄹ이면 `로`, 그 밖은 `으로`(`연기 7층으로`, `범인으로`, `축기 초기로`). 근거: platform.md 표·command.md 경지가 바뀔 때.

**D58. 오행 `Element` 와 영근 `SpiritRoot`** — `domain/shared/Element { METAL("금"), WOOD("목"), WATER("수"), FIRE("화"), EARTH("토") }`(선언 순서 = 오행 순서), `koreanName()`, `static Optional<Element> fromKorean(String)`.
- `domain/character/SpiritRootEntry(Element element, double aptitude)` embeddable, `domain/character/SpiritRoot`(값 객체, 엔트리를 오행 순으로 가짐): `static Optional<SpiritRoot> parse(String input)` — 공백으로 나눈 낱말이 모두 오행 이름, 1~5개, 겹침 없음일 때만 값, 처음 자질 = `100.0 / 개수`(double 그대로). `String name()`(`목·화 진영근`, 개수 1 천영근·2~3 진영근·4~5 위영근), `String summary()`(`목 50, 화 50`, 자질은 `(int) Math.floor`), `List<SpiritRootEntry> entries()`, `boolean has(Element)`, `double aptitude(Element)`.
- 저장: `PlayerCharacter.spiritRoots` `<element-collection>` → 표 `character_spirit_root`(`character_id` FK → `player_character.id`, `element` varchar(16) STRING, `aptitude` double not null). 비어 있으면 '아직 정하지 않음'. 근거: platform.md 4차 월드 상태(영근), stats.md 영근(소수 그대로, 화면은 버림).

**D59. 캐릭터 경지 열과 시작 채우기** — `PlayerCharacter.realm`(`player_character.realm` varchar(32) STRING, **orm.xml `nullable="true"`**). `PlayerCharacter.create` 는 `MORTAL`. `CharacterRepository` 에 `@Modifying @Query("update PlayerCharacter c set c.realm = :realm where c.realm is null") int fillMissingRealms(Realm realm)`. `CharacterModifyService.fillMissingRealms()` 를 `@EventListener(ApplicationReadyEvent.class) @Order(0) @Transactional` 로 두고 `QI_1` 로 채운다. 3차 `ItemTypeModifyService.seedSpiritStones` 에 `@Order(10)` 을 붙여 순서를 고정한다(`LegacySchemaCleaner` 는 `SmartInitializingSingleton` 이라 늘 그보다 먼저). 도메인 getter `getRealm()` 은 null이면 `IllegalStateException`. 근거: platform.md 4차 월드 상태, 리드 판정 L5.

**D60. 스탯 계산 `StatSheet`** — `domain/cultivation/StatSheet`(record: `int qiBlood, int mana, int sense, int essence, int spirit, int body, int strength, Map<Element, Double> aptitudes`), `static StatSheet of(Realm realm, SpiritRoot root /* 없으면 빈 영근 */, List<CharacterTechnique> learned)`. 식은 platform.md `### 4차 월드 상태와 저장` 의 '저장하지 않고 계산하는 것' 그대로: 공법 X = 익힌 공법마다 1성~지금 단계 증가의 합, 기혈 = 육신 × 10, 법력 = 영력이 0이면 0, 아니면 영력 × 5 + 공법 법력, 영근 자질 = 처음 자질 + (공법 속성에 그 오행이 든 익힌 공법의) 영근 자질 증가 합. 현재 수치 = 최대치(L4). 반올림은 표 상수라 계산 중에 생기지 않는다.

**D61. 공법 `Technique`** — `domain/technique/Technique`(`@Entity`), 값 record `TechniqueStage(int insightRequired, int aptitudeRequired, int manaBonus, int senseBonus, int spiritBonus, int essenceBonus, int bodyBonus, int strengthBonus, int aptitudeGain)` embeddable, 입력 record `TechniqueSaveInfo(String name, @Nullable String description, List<Element> elements, Realm startRealm, List<TechniqueStage> stages, List<AbilitySaveInfo> abilities)`.
- 칸·범위·문구는 admin-tool.md `### 공법 Technique` 와 `## 4차 REST API` 예외 표 그대로. 도메인 `Technique.create(info)`·`update(info)` 가 검사하고 `IllegalArgumentException(문구)` 를 던진다(→ 400). **검사 순서**(처음 걸린 것만): 이름(앞뒤 공백 자른 뒤 공백 없음·1~12자) → 묘사 500자 → 같은 오행 두 번 → 단계 수 1~12 → 단계마다 1성부터(1성 필요 깨달음 ≠ 0 → 2성 이후 범위 → 나눔 → 연체술의 자질 조건·증가 → 오행공법의 육신·근력 → 그 밖의 범위, 범위 칸 이름 순서 `자질 조건`·`법력`·`의식`·`영력`·`정혈`·`육신`·`근력`·`영근 자질`) → 신통 수 1~30 → 신통마다 정의 순서로(D62) → 1성 신통 있음.
- `update` 는 단계·신통을 통째로 바꾼다(신통은 지우고 새로 넣음). 이름 유일(대소문자 무시)은 서비스가 `existsByNameIgnoreCase(AndIdNot)` 로 보고 `DuplicateTechniqueNameException("이미 있는 공법 이름입니다: " + name)`(409).
- 메서드: `getName()`, `getDescription()`, `List<Element> getElements()`(오행 순), `boolean isBodyRefining()`(속성 없음), `String lineName()`(`오행`/`연체술`), `getStartRealm()`, `int stageCount()`, `TechniqueStage stage(int n)`(1부터), `List<Ability> getAbilities()`(정의 순), `List<Ability> abilitiesUpTo(int stage)`.
- 표: `technique`(`name` varchar(12) not null `UK_TECHNIQUE_NAME`, `description` varchar(500) not null, `start_realm` varchar(32) STRING not null), `technique_element`(`technique_id`, `element` varchar(16) STRING), `technique_stage`(`technique_id`, `<order-column name="position"/>`, 아홉 int 열 not null). 근거: admin-tool.md 4차 데이터.

**D62. 신통 `Ability`** — `domain/technique/Ability`(`@Entity`, `Technique.abilities` one-to-many cascade all + orphan-removal, `<order-column name="position"/>`), enum `AbilityKind`(코드·이름은 admin-tool.md `### 신통 Ability` 종류 순서), `AbilityTarget { SELF("자신"), SINGLE("한 대상"), ALL_ENEMIES("방의 모든 적") }`, `CostStat { NONE("없음"), MANA("법력"), SENSE("의식"), ESSENCE("정혈") }`, `UsableWhen { COMBAT_ONLY("전투 중만"), ANYTIME("전투 밖에도") }`. 입력 `AbilitySaveInfo(String name, int learnStage, AbilityKind kind, List<Element> elements, boolean bodyRefining, AbilityTarget target, CostStat costStat, BigDecimal costAmount, boolean costPerTick, BigDecimal castSeconds, BigDecimal cooldownSeconds, UsableWhen usableWhen)`.
- **신통마다 검사 순서**: 이름 규칙 → 한 공법 안 중복 → 예약어 → 배우는 단계 → 같은 오행 두 번 → 연체 신통에 오행 → 연체술 공법의 법력 소모 → 소모 양·시간 소수 둘째 자리(`stripTrailingZeros().scale() > 1`) → `NONE` 인데 양 ≠ 0 → 소모가 있는데 양 ≤ 0 → 시전 0~60 → 대기 0.1~3,600. 문구는 admin-tool.md 예외 표 그대로.
- **예약어** `domain/technique/ReservedWords.ALL`(룰북 commands.md 의 명령·줄임말·방위 첫 낱말, 4차에 없는 것 포함, 아래 그대로): `보기 ㅂ 동 서 남 북 북동 북서 남동 남서 위 아래 e w s n ne nw se sw u d 들어가기 살피기 만지기 탐지 은신 전송 상태 ㅅ 소지품 ㅈ 신통 공법 서재 제작법 시간 파티 누구 도움말 대상 중지 도주 막기 추격 정혈 명상 ㅁ 일어서기 영석 복용 수련 본명패 읽기 참오 산공 돌파 수습 줍기 버리기 부적 진법 꺼내기 거두기 수리 본명 온양 제작 채집 채굴 대화 목록 사기 팔기 값 확인 취소 주기 교환 거래소 경매 찾기 가입 탈퇴 공적 동부 맡기기 보관함 비경 주입 나가기 퀘스트 영수 계약 먹이기 되살리기 말하기 ' 귓속말 외치기 종료 뒤로 고르기 튜토리얼`. 비교는 영문 대소문자 무시.
- 표: `ability`(`technique_id` FK not null, `position`, `name` varchar(12), `learn_stage` int, `kind` varchar(32), `body_refining` boolean, `target` varchar(16), `cost_stat` varchar(16), `cost_amount` numeric(8,1), `cost_per_tick` boolean, `cast_seconds` numeric(4,1), `cooldown_seconds` numeric(6,1), `usable_when` varchar(16) — enum은 STRING, 모두 not null), `ability_element`(`ability_id`, `element`). 신통 이름 DB 유니크 제약은 두지 않는다(통째로 바꿀 때 flush 순서 충돌을 피함 — 3차 `npc_topic` 과 같은 까닭).

**D63. 슬라이스·의존** — platform.md `## 저장소 구성` 4차 줄 그대로: `technique`(domain·application, 다른 슬라이스를 쓰지 않음), `cultivation`(domain·application). 의존 `item → technique`, `cultivation → character, technique, item, inventory`, `game → cultivation, technique`(+3차). 이 계획이 더하는 것: `Realm`·`Element` 는 `domain/shared`(슬라이스가 아님). game 슬라이스에 provided 포트 `PresenceFinder`(관리 REST가 접속 중인지 봄). 공법의 익힌 캐릭터 수·담긴 아이템·익힌 최고 단계는 `TechniqueRepository` JPQL로 센다(엔티티 이름만 문자열로 씀, 2차 `FactionRepository.countZones` 관례). 순환 없음, ArchUnit 통과.

**D64. 익힌 공법 `CharacterTechnique`** — `domain/cultivation/CharacterTechnique`(`@Entity`): `<many-to-one name="character">`(`character_id` FK → `player_character.id`), `<many-to-one name="technique">`(`technique_id` FK), `stage` int not null(4차에는 1), UK `UK_CHARACTER_TECHNIQUE_CHARACTER_ID_TECHNIQUE_ID`. 익힌 순서 = id 순. `static CharacterTechnique learn(PlayerCharacter, Technique)`(stage 1), getter `getTechnique()`·`getStage()`, `StageBonusSum bonusSum()`(1성~지금 단계 합).

**D65. 읽기 `TechniqueReader`** — 이름 찾기는 game이 소지품 표시 순서로 한다(3차 `ItemNames.select`, 인자 전체가 이름, 번호 허용). 고른 물건 종류 id로 `TechniqueReader.read(Long characterId, Long itemTypeId)` → `sealed interface ReadResult`: `NotScroll`, `NoTechnique`, `AlreadyLearned(String techniqueName, int stage, int stageCount)`, `RealmTooLow(Realm required)`, `MissingRoot(Element element)`, `LowAptitude(Element element, int required, int current /* 버림 */)`, `Learned(String techniqueName, TechniqueStage firstStage, List<String> newAbilityNames, boolean initiated)`. 검사 순서 command.md `### 읽기` 3~8(1·2는 game), 자질은 double 그대로 `aptitude < required` 면 미달, 연체술은 7·8 건너뜀. 성공은 한 트랜잭션: `InventoryEditor.remove(characterId, itemTypeId, 1, braceletOpen)` → `CharacterTechnique.learn` 저장 → 범인이고 공법 시작 경지가 범인이면 `CharacterRealmEditor.initiate(characterId)`(범인 → 연기 1층, **이벤트를 내지 않음** — 경지 알림은 관리 지정만, command.md 경지가 바뀔 때). 문구 조립은 game(D67).

**D66. 생성·영근 흐름 (`GameSession`)** — 단계 `NAMING → ROOTING → CONFIRMING → PLAYING`(새 캐릭터), `ROOTING_EXISTING → CONFIRMING_EXISTING → PLAYING`(영근 없는 기존 캐릭터, `characterId` 를 미리 가짐). 문구·`뒤로`·그 밖 입력 처리는 command.md `### 캐릭터 생성 · 영근 (4차)` 그대로. 3차까지의 이름 확인 단계(`이름 {이름}\n이대로…`)는 없어지고 이름이 맞으면 바로 영근 prompt다. 영근 prompt `영근을 고르십시오. 오행(금 목 수 화 토) 가운데 1~5개를 띄어 쓰십시오.`, 확인 prompt 하나는 `\n` 으로 이은 네 줄(`{name} — {summary}` / `────────` / `이름 {이름} · 영근 {name} ({summary})` / 마지막 줄). `확인` 때 이름이 그 사이 쓰였으면 3차처럼 `이미 있는 이름입니다. 다른 이름을 정하십시오.` 와 이름 prompt. 기존 캐릭터 연결 끊김·재접속은 세션을 버리고 다음 접속에 처음부터(스펙 그대로). `doDisconnect` 는 PLAYING 이 아닌 세션을 presence 없이 지운다(3차와 같음).

**D67. 정보 출력 조립** — `application/game/CultivationTexts`(package-private `@Component`): `String status(PlayerCharacter, StatSheet)`, `String techniques(Realm current, List<CharacterTechnique>)`, `String abilities(List<CharacterTechnique>)`, `String readSuccess(ReadResult.Learned)`, `String readFailure(ReadResult)`, `String realmChanged(Realm from, Realm to)`. 모양은 command.md `### 상태`·`### 공법 · 신통`·`### 읽기`·`### 경지가 바뀔 때` 그대로. 이 계획이 정하는 세부:
- 신통 속성은 오행이 여럿이면 `·` 로 이음(`공격·화·토`), 연체 신통은 `연체`. 소모 양은 `Numbers.decimal`(`법력 5`, `의식 1.5 (틱마다)`).
- 시간 `domain/shared/Durations.korean(BigDecimal seconds)`: 60 미만 `3초`·`1.5초`; 60 이상 `{분}분` + 나머지가 있으면 ` {초}초`(`1분`, `7분 12초`, `1분 1.5초`); 3,600 이상 `{시}시간` + 나머지 분이 있으면 ` {분}분`(`1시간`, `1시간 5분`, 초는 버림).
- 읽기 스탯 줄의 영근 자질은 공법 속성마다 오행 순으로 `목영근 자질 +1`. 0이 아닌 칸만, 순서 `법력 · 의식 · 영력 · 정혈 · 육신 · 근력 · 영근 자질`, ` · ` 로 이음.
- 경지 알림 괄호 안 차이가 모두 0이면 괄호를 뺀다(경지 표상 생기지 않지만 안전망).

**D68. 범인의 상점 막힘** — `ShopCommandHandler` 의 `목록`·`사기`·`팔기`·결제 `확인`(사기를 다시 계산할 때) 처음에 경지가 `MORTAL` 이면 `SystemMessage.error("아직 범인의 몸이라 할 수 없습니다.")` 하고 끝낸다. 인자 검사·상점 찾기보다 먼저다(상점이 없는 방에서도 이 문구). 확인 대기는 그대로 둔다(10초 규칙이 지움). `값`·`취소`, 대기 없는 `확인`(`확인할 것이 없습니다.`)은 막지 않는다. 근거: command.md 범인의 상점 막힘, L1.

**D69. 경지 지정과 알림** — `application/character/provided/CharacterRealmEditor`: `void designate(Long characterId, Realm realm)`(관리 툴; 바뀌면 `RealmChangedEvent(Long characterId, Realm from, Realm to)` 를 낸다, 같으면 아무것도 안 함), `void initiate(Long characterId)`(읽기 입문, 범인일 때만 `QI_1`, 이벤트 없음). 없는 캐릭터 `IllegalArgumentException("캐릭터를 찾을 수 없습니다. id: " + id)`. `GameService.onRealmChanged` 는 `@TransactionalEventListener(AFTER_COMMIT)` + `@Transactional(NOT_SUPPORTED)` 로 게임 락을 잡고(3차 `onRoomDeleted` 와 같음) presence 에 있으면 `SystemMessage.growth(CultivationTexts.realmChanged(from, to))` 를 보낸다. 같은 방 알림 없음. 반영 전에 온 명령도 DB에서 새 경지를 읽는다(경지를 메모리에 두지 않음).

**D70. 저물탁** — 스펙(admin-tool.md `### 저물탁`, command.md `### 저물탁`)에 이 계획이 더하는 구현 세부:
- `BagGrade` 에 `BRACELET_LOW(100)`·`BRACELET_MIDDLE(200)`·`BRACELET_HIGH(400)` 를 더하고 `boolean isBracelet()`, `String displayName()`(`하급 저물대`·`하급 저물탁`) — `koreanName()`(`하급`)은 그대로. `ItemCategory.BAG` 이름 `저물대·저물탁`. `ItemType.isBracelet()`. `Slots.MAX_BRACELETS = 1`, 저물대 3개 한도는 저물탁을 세지 않음.
- **`inventory` 는 경지를 모르고 `boolean braceletOpen` 을 인자로 받는다**: `Inventory.capacity(boolean braceletOpen)`, 그리고 칸을 보는 도메인 메서드와 `InventoryEditor`·`InventoryFinder` 의 칸 관련 메서드 모두에 마지막 인자로 더한다. game은 `CharacterFinder.find(characterId).getRealm().opensBracelet()` 로 구한다. `createWithStartingItems`·시작 소지품 저장 검사는 `false`(새 캐릭터는 범인).
- **칸을 넘은 소지품 규칙**(관리 툴로 경지를 내려 생김, 스펙 "늘어나는 것은 안 되고 줄이는 것은 된다"를 이렇게 판정): 늘리는 변경(줍기·사기·받기·시작 소지품)은 3차처럼 `변경 뒤 쓰는 칸 ≤ 변경 뒤 칸` 일 때만(그래서 넘은 소지품에서는 `roomFor` 0). 줄이는 변경(버리기·팔기·주는 쪽)은 그 조건, **또는** `쓰는 칸이 늘지 않고 칸도 줄지 않으면` 된다. 팔기 실패 원인은 칸이 줄면 `BAG_IN_USE`/`BRACELET_IN_USE`, 쓰는 칸이 늘면 `NO_ROOM`.
- 결과 값 추가: `AddCheck.BRACELET_LIMIT`, `RemoveCheck.BRACELET_IN_USE`, `PurchaseResult.BraceletLimit()`(BagLimit 다음), `SaleResult.BRACELET_IN_USE`, `GiveResult.RECEIVER_BRACELET_LIMIT`(RECEIVER_BAG_LIMIT 다음)·`GIVER_BRACELET_IN_USE`. 칸이 줄어 실패할 때 빼는 물건이 저물탁이면 `…_BRACELET_…`, 저물대면 3차 값.
- 문구: `저물탁은 하나까지만 찰 수 있습니다.`, `{이름은} 저물탁을 더 찰 수 없습니다.`, `저물탁에 든 물건을 먼저 비우십시오.`, 소지품 줄 ` (축기 초기 필요)` — 모두 스펙 그대로. 저물탁은 소지품 줄에 3차 저물대처럼 이름으로 보인다.

**D71. WebSocket 메시지** — platform.md `### 4차 WebSocket 메시지 변경` 그대로. record: `StatusMessage(String characterName, RealmView realm, SpiritRootView spiritRoot, StatsView stats, @Nullable CultivationView cultivation, String text, String tone)`(`type()` = `"status"`, JSON 필드 순서 = 이 순서, tone `"status"`), `RealmView(String code, String name)`, `SpiritRootView(String name, List<RootView> roots)`, `RootView(String element /* "WOOD" */, String elementName, int aptitude)`, `StatsView(GaugeView qiBlood, GaugeView mana, GaugeView sense, GaugeView essence, GaugeView spirit, int body, int strength)`, `GaugeView(int current, int max)`, `CultivationView(int current, int required)`. `SystemMessage.growth(String)`(tone `"growth"`). `공법`·`신통` 은 `SystemMessage.info`, 읽기 거절은 `error`.

**D72. 관리 REST** — admin-tool.md `## 4차 REST API` 표·요청·응답·예외 표 그대로. 이 계획이 정하는 것: 어댑터 `RealmApi`(`GET /api/admin/realms`), `TechniqueApi`, `CharacterApi`; 409 예외 `DuplicateTechniqueNameException`·`TechniqueInUseException`(단계 수 줄이기·삭제 두 경우)을 `ApiControllerAdvice.handleConflict` 에 더함. 숫자 칸이 빠지면 0, `elements` 가 빠지면 빈 목록 — 요청 record 컴팩트 생성자에서 채운다(`Integer`/`BigDecimal` null → 0, 신통 `learnStage` 도 `Integer`). 필수 enum(`startRealm`·`kind`·`target`·`costStat`·`usableWhen`, 경지 지정의 `realm`)이 null 이면 스펙대로 `요청 본문을 읽을 수 없습니다`(Task 2 `TechniqueSaveRequest` 설명의 컴팩트 생성자 방식, `@NotNull` 쓰지 않음). 공법 상세·요약은 서비스가 트랜잭션 안에서 만든 값 record(`TechniqueView`)로 돌려준다. 캐릭터 목록은 `CultivationFinder.summaries()`(이름순, 트랜잭션 안에서 영근 이름·공법 수까지) + `PresenceFinder.onlineCharacterIds()`.

**D73. admin-console** — admin-tool.md `## 4차 화면` 그대로. 파일: `app/{techniques,technique,characters,character}/page.tsx`, `src/components/{TechniqueForm,StageTable,AbilityTable,CharacterRealmForm}.tsx`, `src/lib/{techniques.ts,api.ts,items.ts}`, `src/components/{ItemForm,TopBar}.tsx`, `app/items/page.tsx`. 경지 선택 항목은 `GET /api/admin/realms` 로 읽는다. 종류·대상·소모·쓸 수 있는 때·오행 선택지는 `src/lib/techniques.ts` 상수(코드·이름은 D62·D58).

**D74. play-client** — platform.md 플레이 화면 4차 줄과 command.md `### 4차 도움말` 그대로. 스타일 `status`(색 `--status: #c8d3f5`, `.line-status`), `growth`(색 `--growth: #f0c674`, `.line-growth`) — 색은 이 계획이 정함. 이름 후보 순서: occupants → 바닥 물건 → 마지막 `inventory.items[].name`(중복 제거, 앞에 나온 것 유지). 기존 캐릭터의 `session(creating, 이름)` 은 1차 creating 과 같은 화면(입력창 그대로).

**D75. 1~3차 테스트·스모크 고치기 (L1)** — 새 캐릭터가 범인이고 영근 단계가 생겨서:
- world-server 테스트: `CharacterCreator.create(accountId, name)`(영근 없음)는 Task 7에서 지운다. `BaseGameTest.enter/enterAt` 은 `CharacterFixture.spiritRoot()`(= `SpiritRoot.parse("목 화")`)로 만들고, 상점을 쓰는 테스트는 준비에서 `setRealm(accountId, Realm.QI_1)`(Task 4의 BaseGameTest 도우미, 리포지토리 직접 — 이벤트·`growth` 알림이 없어 기존 메시지 기대값이 그대로)을 부른다. `designateRealm`(`CharacterRealmEditor.designate`, 알림 있음)은 알림을 보는 테스트에만 쓴다. WebSocket 생성 테스트는 이름 다음에 `목 화` → `확인`.
- 스모크: `createCharacter(client, name, roots = "목 화")` 가 영근 prompt·확인 prompt(`목·화 진영근 — 목 50, 화 50` 으로 시작)를 지난다. 3차 단계 22 앞에서 관리자가 C를 `PUT /api/admin/characters/{id}/realm {realm:"QI_1"}` 로 지정하고 C가 `growth` 알림을 받는다.

**D76. 도움말** — command.md `### 4차 도움말` 문구를 `GameService.HELP` 에 글자 그대로. play-client `COMMANDS` 순서도 그 절 그대로.

### 4차 열린 질문 대응

index.md `### 4차 이터레이션 열린 질문` 은 이미 모두 `→ (제안)` 답이 있다. 이 계획은 답을 바꾸지 않고 구현 세부만 더한다: 경지 데이터 D57·D59, 스탯 D60, 영근 D58·D66, 공법·신통 D61·D62, 옥간 담긴 공법 Task 3, 지우기·고치기 Task 2·5, 읽기 D65·D67, 정보 출력 D67, 범인 막힘 D68, 경지 지정·알림 D69, 상태 창 D71·D74, 저물탁 D70.

## Review Focus

스펙이 직접 말하지 않지만 실제로 부딪칠 만한 것. 각 줄의 테스트는 담당 태스크에 들어 있다.

1. 이미 캐릭터가 있는 `pgdata` 로 올림 → 서버가 뜨고(`realm` 열 nullable) 기존 행이 `QI_1`, 영근 없는 캐릭터는 접속 때 영근 prompt, 고르는 중 재접속하면 처음부터. (Task 1 `CharacterRepositoryTest.fillMissingRealms`, Task 7 `existingCharacterChoosesRoot`·`reconnectWhileRootingRestarts`)
2. 관리자가 축기 → 연기로 내려 저물탁이 닫힘 → `[소지품 130/22칸]`, 줍기·사기·받기 거절, 버리기 됨, 저물대 버리기는 거절. (Task 4 `InventoryTest.overCapacity*`, `GameConnectorTest.braceletClosesAfterDemotion`)
3. 익힌 공법의 단계 수를 줄이기·지우기 → 409, 신통을 지우거나 스탯 증가를 바꾸면 다음 `신통`·`상태` 에 바로 반영. (Task 5 `TechniqueEditorTest.shrinkBelowLearnedStageFail`·`deleteLearnedFail`, Task 8 `abilitiesFollowDefinition`)
4. 확인 대기 중 관리자가 범인으로 내림 → `확인` 은 `아직 범인의 몸이라 할 수 없습니다.` 이고 영석·물건 그대로. (Task 9 `confirmAfterDemotedToMortalFail`)
5. 영근 3개(자질 33.33…) 캐릭터가 자질 조건 33인 공법은 익히고 34는 `(화영근 자질 34 필요, 지금 33)` 로 거절; 오프라인 캐릭터·같은 경지로 지정하면 알림 없음·오류 없음. (Task 5 `aptitudeComparesFraction`, Task 9 `designateOfflineOrSameRealmSendsNothing`)

---

## 파일 구조 (요약, 새로 생기거나 바뀌는 것)

```
world-server/src/main/java/mudmk3/
  domain/shared/{Realm,Element,Durations,Josa}.java
  domain/character/{PlayerCharacter,CharacterCreateInfo,SpiritRoot,SpiritRootEntry}.java
  domain/technique/{Technique,TechniqueStage,TechniqueSaveInfo,Ability,AbilitySaveInfo,AbilityKind,AbilityTarget,CostStat,UsableWhen,ReservedWords,DuplicateTechniqueNameException}.java   (새)
  domain/cultivation/{CharacterTechnique,StatSheet}.java                                                     (새)
  domain/item/{BagGrade,ItemCategory,ItemType,ItemTypeSaveInfo,Slots}.java, domain/inventory/*.java           (저물탁·담긴 공법)
  domain/game/{Command,CommandParser}.java
  application/technique/{TechniqueQueryService,TechniqueModifyService}.java, provided/{TechniqueFinder,TechniqueEditor,TechniqueSaveRequest,StageRequest,AbilityRequest,TechniqueView,TechniqueInUseException}.java, required/{TechniqueRepository,ScrollItem}.java   (새)
  application/cultivation/{CultivationQueryService,CultivationModifyService}.java, provided/{CultivationFinder,TechniqueReader,ReadResult,CultivationProfile,CharacterSummary,LearnedTechniqueView}.java, required/CharacterTechniqueRepository.java   (새)
  application/character/... (+CharacterRealmEditor, RealmChangedEvent), application/item/..., application/inventory/..., application/world/...
  application/game/{GameService,GameSession,ItemCommandHandler,ShopCommandHandler,CultivationTexts,PresenceQueryService}.java, provided/PresenceFinder.java, required/GameMessage.java
  adapter/webapi/{RealmApi,TechniqueApi,CharacterApi,ItemTypeApi,ApiControllerAdvice}.java, dto/...
  resources/META-INF/orm.xml
admin-console/  app/{techniques,technique,characters,character}/page.tsx, app/items/page.tsx, src/lib/{api,items,techniques}.ts, src/components/{TopBar,ItemForm,TechniqueForm,StageTable,AbilityTable,CharacterRealmForm}.tsx
play-client/    src/lib/{protocol,render,complete}.ts, src/components/Terminal.tsx, app/globals.css
docs/spec/system/*.md  README.md  scripts/smoke-test.mjs
```

---

### Task 1: 경지·오행·영근 도메인과 캐릭터 저장(경지 열·시작 채우기·영근 표)

**권장 모델:** opus

**목적:** D57·D58·D59. 게임 흐름은 Task 7. 이 태스크에서 기존 `CharacterCreator.create(accountId, name)` 은 남겨 둔다(영근 없는 캐릭터를 만듦 — 1~3차 테스트가 그대로 돌게). Task 7이 지운다.

**Files:**
- Create: `domain/shared/{Realm,Element}.java`, `domain/character/{SpiritRoot,SpiritRootEntry}.java`
- Modify: `domain/shared/Josa.java`, `domain/character/{PlayerCharacter,CharacterCreateInfo}.java`, `src/main/resources/META-INF/orm.xml`
- Modify: `application/character/provided/{CharacterCreator,CharacterFinder}.java`, `application/character/required/CharacterRepository.java`, `application/character/{CharacterModifyService,CharacterQueryService}.java`, `application/item/ItemTypeModifyService.java`(`@Order(10)`)
- Test: `domain/shared/{RealmTest,ElementTest,JosaTest(추가)}`, `domain/character/{SpiritRootTest,PlayerCharacterTest(추가),CharacterFixture(추가)}`, `application/character/required/CharacterRepositoryTest`(추가), `application/character/provided/CharacterCreatorTest`(추가)

**Interfaces:**
- Produces:
  - `Realm`·`Element`·`Josa.euroRo/withEuroRo` (D57·D58).
  - `SpiritRoot`·`SpiritRootEntry` (D58).
  - `PlayerCharacter`: `Realm getRealm()`, `Optional<SpiritRoot> findSpiritRoot()`, `void chooseSpiritRoot(SpiritRoot)`(이미 있으면 `IllegalStateException("영근은 이미 정했습니다")`), `void changeRealm(Realm)`. `CharacterCreateInfo(accountId, name, roomId, @Nullable SpiritRoot spiritRoot)`.
  - `CharacterCreator`: 기존 `create(Long, String)` + `PlayerCharacter create(Long accountId, String name, SpiritRoot spiritRoot)`(검사 순서는 기존과 같음), `void chooseSpiritRoot(Long characterId, SpiritRoot)`.
  - `CharacterFinder` + `PlayerCharacter find(Long characterId)`(없으면 `IllegalArgumentException("캐릭터를 찾을 수 없습니다. id: " + id)`).
  - `CharacterRepository.fillMissingRealms(Realm)`, `CharacterModifyService.fillMissingRealms()` (D59).
  - orm.xml: `realm` `<enumerated>STRING</enumerated>` `column-definition="varchar(32)"` nullable, `spiritRoots` element-collection → `character_spirit_root`(D58 열).

- [ ] **Step 1: 도메인 테스트** — `RealmTest`: `MORTAL`(1,1,0,10, 필요 수위 없음), `QI_1`(6,3,5,20,190), `QI_2`(7,4,6,22,290), `QI_13`(18,9,17,44,11,000), `FOUNDATION_EARLY`(36,18,34,88,47,000), `FOUNDATION_MIDDLE`(42,21,40,103,67,000), `FOUNDATION_LATE`(48,24,45,117,90,000), `FOUNDATION_PEAK`(54,27,51,132,120,000), 18개 전부 표와 같은지 한 표 테스트, `koreanName` 셋, `opensBracelet`(QI_13 false, FOUNDATION_EARLY true), `isBelow`. `ElementTest`: `fromKorean("목") == WOOD`, `"불"` 비어 있음. `SpiritRootTest`: `parse("화 목")` → 이름 `목·화 진영근`, 요약 `목 50, 화 50`; `parse("화")` → `화 천영근`, `화 100`; `parse("금 목 수")` → `금·목·수 진영근`, `금 33, 목 33, 수 33`, `aptitude(METAL) == 100.0/3`; `parse("금 목 수 화")` → `위영근`; 실패 `""`·`"금 금"`·`"불"`·`"금,목"`. `PlayerCharacterTest`: 새 캐릭터 `MORTAL`·영근 없음, `chooseSpiritRootTwiceFail`. `JosaTest`: `연기 7층으로`, `축기 초기로`, `범인으로`, `연기 1층으로`, `축기 대원만으로`.
- [ ] **Step 2: 실패 확인 → 구현.**
- [ ] **Step 3: 리포지토리·서비스 테스트** — `CharacterRepositoryTest`: `saveWithSpiritRoot`(flush·clear 뒤 오행 순·자질), `fillMissingRealms`(native `update player_character set realm = null where id = ?` 뒤 `fillMissingRealms(QI_1)` 가 1을 돌려주고 다시 읽으면 `QI_1`, 이미 있는 값은 그대로). `CharacterModifyServiceTest`(또는 같은 `@SpringBootTest`): `fillMissingRealmsOnStartupListener`(null 행을 만든 뒤 빈의 `fillMissingRealms()` 를 부르면 `QI_1`, `@Order(0)`·영석 심기 `@Order(10)` 애노테이션이 붙어 있음). `CharacterCreatorTest`: `createWithSpiritRoot`(`MORTAL`, `목·화 진영근`), `chooseSpiritRootForLegacy`, `chooseSpiritRootTwiceFail`, `findUnknownFail`(문구 그대로).
- [ ] **Step 4: 구현 → 통과.**
- [ ] **Step 5: 검증** — `./gradlew build` 통과(1~3차 테스트 그대로).
- [ ] **Step 6: 커밋** — `feat(world-server): 경지·오행·영근 도메인과 캐릭터 경지 저장·시작 채우기`.

**완료 조건:** 위 테스트 통과, 표 값이 platform.md 와 같음.
**progress.md 한 줄:** `Realm`·`Element`(shared), `SpiritRoot`, `player_character.realm`(nullable)·`character_spirit_root`, 시작 채우기(`@Order(0)`, 영석 심기 `@Order(10)`), `Josa` 으로/로, `CharacterFinder.find`.

---

### Task 2: 공법·신통 도메인·영속·관리 서비스

**권장 모델:** opus

**목적:** D61·D62와 `technique` 슬라이스 포트. REST는 Task 3. 익힌 캐릭터 검사는 Task 5가 채운다.

**Files:**
- Create: `domain/technique/*`(D61·D62 클래스), `application/technique/{TechniqueQueryService,TechniqueModifyService}.java`, `application/technique/provided/{TechniqueFinder,TechniqueEditor,TechniqueSaveRequest,StageRequest,AbilityRequest,TechniqueView,TechniqueInUseException}.java`, `application/technique/required/TechniqueRepository.java`
- Modify: `orm.xml`
- Test: `domain/technique/{TechniqueTest,AbilityTest,ReservedWordsTest,TechniqueFixture}`, `application/technique/required/TechniqueRepositoryTest`, `application/technique/provided/TechniqueEditorTest`

**Interfaces:**
- Consumes: `Realm`, `Element`(Task 1).
- Produces:
  - 도메인 D61·D62. `TechniqueFixture.changchun()`: 장춘공(목, 범인부터, 9성, 단계 표 technique.md 장춘공 표 — 필요 깨달음 0·120·190·280·400·560·800·1,100·1,540, 법력 2성부터 +2, 의식 2·4·6·8성 +1, 영력 5·9성 +1, 목영근 자질 2성부터 +1), 신통 화탄술(1성, 공격, 화, 한 대상, 법력 5, 시전 0, 대기 3, 전투 중만)·천안술(2성, 탐지, 목, 자신, 법력 3, 대기 10, 전투 밖에도)·어풍결(3성, 둔술, 목, 자신, 법력 10, 대기 10, 전투 밖에도). `TechniqueFixture.ironBody()`: 강체결(연체술, 범인부터, 9성, technique-list.md 강체결 표, 철권 1성 공격·연체·한 대상·소모 없음·대기 4). `TechniqueFixture.greenWood()`: 청목결(목, 연기 7층부터, 8성, 오행 연기 후반 표 — 자질 조건 1~4성 15·5~8성 20, 청목전 1성 공격·목·법력 12·대기 3, 목령갑 4성 방어·목·법력 18·대기 15).
  - `record StageRequest(@Nullable Integer insightRequired, … 아홉 칸)`, `record AbilityRequest(String name, @Nullable Integer learnStage, AbilityKind kind, @Nullable List<Element> elements, boolean bodyRefining, AbilityTarget target, CostStat costStat, @Nullable BigDecimal costAmount, boolean costPerTick, @Nullable BigDecimal castSeconds, @Nullable BigDecimal cooldownSeconds, UsableWhen usableWhen)`, `record TechniqueSaveRequest(String name, @Nullable String description, @Nullable List<Element> elements, Realm startRealm, @NotNull List<@NotNull StageRequest> stages, @NotNull List<@NotNull AbilityRequest> abilities)` + `toInfo()`. 필수 enum(`startRealm`, 신통의 `kind`·`target`·`costStat`·`usableWhen`)이 null 이면 admin-tool.md `## 4차 REST API` 끝 문장대로 공통 400 `요청 본문을 읽을 수 없습니다` 여야 한다 — `@NotNull`(Bean Validation 은 `입력 값이 올바르지 않습니다: …` 가 되어 스펙과 다름)을 쓰지 말고 요청 record 컴팩트 생성자에서 `IllegalArgumentException("<필드> 는 꼭 보내야 합니다")` 를 던진다(Jackson 역직렬화 중 생성자 예외 → `HttpMessageNotReadableException` → 공통 문구). Task 3 웹 테스트가 `detail` 을 확인하고, 이 경로로 공통 문구가 나오지 않으면 멈추고 리드에게 보고한다. 빠진 숫자 → 0, 빠진 목록 → 빈 목록(D72).
  - `TechniqueView`(값 record, 트랜잭션 안에서 만듦): `id, name, description, elements, elementNames(@Nullable "목"), lineName, startRealm, startRealmName, stages, abilities(+kindName, targetName), learnerCount, scrollItems: List<ScrollItem>`. Task 2에서 `learnerCount` 0·`scrollItems` 빈 목록(Task 3·5가 채움).
  - `TechniqueFinder`(`/** 공법·신통 정의를 찾는다 */`): `Technique find(Long)`(없으면 `IllegalArgumentException("공법을 찾을 수 없습니다. id: " + id)`), `TechniqueView view(Long)`, `List<TechniqueView> viewAll()`(이름순).
  - `TechniqueEditor`(`/** 공법·신통을 만들고 고치고 지운다 */`): `TechniqueView create(@Valid TechniqueSaveRequest)`, `TechniqueView update(Long, @Valid TechniqueSaveRequest)`, `void delete(Long)`. `TechniqueModifyService` 에 `private void checkDeletable(Technique)`·`private void checkStageCount(Technique, int newCount)` 자리(Task 3·5가 채움).
  - `TechniqueRepository extends Repository<Technique, Long>`: `save`, `findById`(`@EntityGraph(attributePaths = {"elements","stages","abilities","abilities.elements"})`), `findAllByOrderByNameAsc`(같은 그래프), `existsByNameIgnoreCase`, `existsByNameIgnoreCaseAndIdNot`, `delete`.
  - `TechniqueInUseException`(409용, Task 3·5가 던짐).

- [ ] **Step 1: 도메인 테스트** — `TechniqueTest`: 픽스처 셋 생성, `lineName`(장춘공 `오행`, 강체결 `연체술`), `abilitiesUpTo(1)` 은 화탄술만. 실패(문구 admin-tool.md 그대로): `nameWithSpaceFail`(`장 춘공`), `nameTooLongFail`(13자), `descriptionTooLongFail`, `duplicateElementFail`(`같은 속성이 두 번 있습니다: 목`), `stageCountFail`(0, 13), `firstStageInsightFail`, `insightRangeFail`(`(3성)`), `insightNotDivisibleFail`(화·토 공법 3성 201 → `필요 깨달음은 속성 수(2)로 나누어떨어져야 합니다. (3성)`), `bodyRefiningAptitudeFail`, `fiveElementBodyFail`, `stageValueRangeFail`(`(3성 법력)` — 법력 10,001), `abilityCountFail`(0, 31), `noFirstStageAbilityFail`. `AbilityTest`: `reservedNameFail`(`상태` → `명령어와 같은 이름은 쓸 수 없습니다: 상태`, `E` 도 예약어), `duplicateNameFail`, `learnStageFail`(`배우는 단계는 1성~9성 가운데로 정하십시오: 어풍결`), `bodyRefiningWithElementFail`, `manaOnBodyRefiningFail`, `costScaleFail`(`5.25`), `noneWithAmountFail`, `costAmountFail`, `castRangeFail`(61), `cooldownRangeFail`(0, 3,600.1). `ReservedWordsTest`: D62 목록의 몇 낱말(`상태`·`ㅅ`·`참오`·`e`·`'`)과 `화탄술` 아님.
- [ ] **Step 2: 실패 확인 → 구현.**
- [ ] **Step 3: 리포지토리 테스트** — `TechniqueRepositoryTest`: `saveWithStagesAndAbilities`(flush·clear 뒤 단계 순서·신통 순서·신통 속성), `replaceAbilities`(update 뒤 flush 오류 없음, 같은 이름 신통을 지우고 다시 넣기), `duplicateNameFail`.
- [ ] **Step 4: 서비스 테스트** — `TechniqueEditorTest`: `create`, `duplicateNameIgnoringCaseFail`(409 예외 타입), `updateKeepsOwnName`, `viewAllByName`, `viewOutsideTransaction`(트랜잭션 밖에서 `TechniqueView` 필드 읽기), `delete`, `unknownIdFail`.
- [ ] **Step 5: 구현 → 통과.**
- [ ] **Step 6: 검증** — `./gradlew build`(ArchUnit: `technique` 은 다른 슬라이스를 쓰지 않음).
- [ ] **Step 7: 커밋** — `feat(world-server): 공법·신통 정의 도메인·영속·관리 서비스`.

**완료 조건:** 위 테스트 통과, 문구가 admin-tool.md 예외 표와 글자 하나까지 같음.
**progress.md 한 줄:** `technique` 슬라이스(Technique·TechniqueStage·Ability·enum 넷·ReservedWords), 표 다섯, `TechniqueFinder`·`TechniqueEditor`·`TechniqueView`, 검사 순서 D61·D62.

---

### Task 3: 공법·경지 관리 REST와 옥간·서책의 담긴 공법

**권장 모델:** opus

**목적:** D72의 `realms`·`techniques` API, admin-tool.md `### 옥간·서책의 담긴 공법`(item → technique), 공법 삭제의 담긴 물건 409.

**Files:**
- Create: `adapter/webapi/{RealmApi,TechniqueApi}.java`, `adapter/webapi/dto/{RealmResponse,TechniqueSummaryResponse,TechniqueDetailResponse}.java`, `application/technique/required/ScrollItem.java`
- Modify: `domain/item/{ItemType,ItemTypeSaveInfo}.java`, `orm.xml`(`item_type.technique_id` many-to-one FK, 기본 EAGER), `application/item/provided/ItemTypeSaveRequest.java`(+`@Nullable Long techniqueId`), `application/item/ItemTypeModifyService.java`, `application/technique/required/TechniqueRepository.java`(+`countScrollItems`, `findScrollItems`), `application/technique/TechniqueModifyService.java`·`TechniqueQueryService.java`, `adapter/webapi/dto/ItemTypeResponse.java`(+`techniqueId`, `techniqueName`, `bagGradeName` 은 Task 4가 바꿈), `adapter/webapi/ApiControllerAdvice.java`
- Test: `domain/item/ItemTypeTest`(추가), `application/item/provided/ItemTypeEditorTest`(추가), `application/technique/provided/TechniqueEditorTest`(추가), `adapter/webapi/{RealmApiWebMvcTest,TechniqueApiWebMvcTest,ItemTypeApiWebMvcTest(추가),AdminApiIntegrationTest(추가)}`, `adapter/security/SecurityIntegrationTest`(추가 한 줄)

**Interfaces:**
- Consumes: Task 2 포트, 3차 `ItemTypeEditor`.
- Produces:
  - `ItemType`: `Optional<Technique> findTechnique()`. `ItemTypeSaveInfo` + `@Nullable Technique technique`; 종류가 `SCROLL` 이 아닌데 공법이 있으면 `IllegalArgumentException("공법은 옥간·서책에만 담을 수 있습니다.")`(만들기·고치기 모두 — 담긴 공법이 있는 채로 종류를 바꾸는 요청도 이 문구). `ItemTypeModifyService` 가 `techniqueId` 를 `TechniqueFinder.find` 로 찾는다(없으면 `공법을 찾을 수 없습니다. id: 9` 400).
  - `record ScrollItem(Long id, String name)`; `TechniqueRepository` + `@Query("select count(i) from ItemType i where i.technique.id = :techniqueId") long countScrollItems(Long)`, `@Query("select new mudmk3.application.technique.required.ScrollItem(i.id, i.name) from ItemType i where i.technique.id = :techniqueId order by i.name") List<ScrollItem> findScrollItems(Long)`. `TechniqueView.scrollItems` 를 채우고, 삭제 때 담긴 물건이 있으면 `TechniqueInUseException("이 공법이 담긴 물건이 있어 지울 수 없습니다: " + 첫 물건 이름)`.
  - REST: admin-tool.md 4차 REST 표의 `realms`·`techniques` 여섯 줄과 `item-types` 줄. 응답 DTO 필드는 표 그대로(`TechniqueDetailResponse.of(TechniqueView)` — 저장 칸 전부 + `id, lineName, startRealmName, learnerCount, scrollItems`, 신통에 `kindName`·`targetName`). `ItemTypeResponse` + `techniqueId`·`techniqueName`(없으면 null).

- [ ] **Step 1: 도메인·서비스 테스트** — `ItemTypeTest`: `scrollWithTechnique`, `techniqueOnNonScrollFail`. `ItemTypeEditorTest`: `createScrollWithTechnique`, `unknownTechniqueFail`, `changeCategoryWithTechniqueFail`, `removeTechniqueThenChangeCategory`. `TechniqueEditorTest`: `deleteWithScrollItemFail`(409 문구 `…: 장춘공 옥간`), `viewListsScrollItems`.
- [ ] **Step 2: 웹 테스트(`@WebMvcTest` + 보안, 관리자·`X-MUD-Client: 1`)** — `RealmApiWebMvcTest`: 18개, `[0] == {code:"MORTAL", name:"범인"}`, `[14].name == "축기 초기"`. `TechniqueApiWebMvcTest`: `POST` 201 본문(`lineName:"오행"`, `elementNames:"목"`, `startRealmName:"범인"`, `abilities[0].kindName:"공격"`, `targetName:"한 대상"`), 빠진 숫자 칸·`elements` 를 0·빈 목록으로, 잘못된 `kind` 400, `startRealm:null`·신통 `kind:null` 400 — 셋 다 `detail` `요청 본문을 읽을 수 없습니다`, 목록 `GET` 이 이름순·요약 필드(`stageCount`·`abilityCount`·`learnerCount`·`elementNames` 연체술 null), 이름 중복 409, 쓰이는 공법 `DELETE` 409, 관리자 아님 403, `X-MUD-Client` 없음 403. `ItemTypeApiWebMvcTest`: `techniqueId` 왕복, `techniqueId` 빠지면 null.
- [ ] **Step 3: 통합 테스트** — `AdminApiIntegrationTest.manageTechniques`: 경지 목록 → 공법 만들기 → 옥간 종류에 담기 → 공법 상세 `scrollItems` 가 트랜잭션 밖에서 이름을 냄 → 공법 삭제 409 → 담긴 공법 빼기 → 삭제 204.
- [ ] **Step 4: 구현 → 통과.**
- [ ] **Step 5: 검증** — `./gradlew build`(ArchUnit: `item → technique`, 역방향 없음).
- [ ] **Step 6: 커밋** — `feat(world-server): 공법·경지 관리 API와 옥간·서책의 담긴 공법`.

**완료 조건:** 위 테스트 통과, 응답 필드가 admin-tool.md 표와 같음.
**progress.md 한 줄:** `GET /realms`, `/techniques` 5개, `item_type.technique_id`, 아이템 요청·응답 `techniqueId`/`techniqueName`, 담긴 물건 있는 공법 삭제 409.

---

### Task 4: 저물탁 — 등급·한도·경지에 따른 칸, 게임 연결

**권장 모델:** opus

**목적:** D70 전부. 경지는 Task 1의 `CharacterFinder.find(...).getRealm()` 로 game이 구한다.

**Files:**
- Modify: `domain/item/{BagGrade,ItemCategory,ItemType,Slots}.java`, `domain/inventory/{Inventory,AddCheck,RemoveCheck,PurchaseResult,SaleResult,GiveResult}.java`
- Modify: `application/inventory/provided/{InventoryFinder,InventoryEditor}.java`, `application/inventory/{InventoryQueryService,InventoryModifyService}.java`, `application/world/WorldSettingsModifyService.java`(저물탁 2개 이상 400), `application/game/{ItemCommandHandler,ShopCommandHandler}.java`, `adapter/webapi/dto/ItemTypeResponse.java`(`bagGradeName` = `displayName()`)
- Test: `domain/item/{BagGradeTest,ItemCategoryTest(추가),SlotsTest(추가)}`, `domain/inventory/{InventoryTest,InventoryFixture}`(추가), `application/inventory/provided/InventoryEditorTest`(추가), `application/world/provided/WorldSettingsEditorTest`(추가), `application/game/provided/GameConnectorTest`(추가), 영향받는 3차 테스트 기대값(`bagGradeName`, `categoryName` `저물대·저물탁`)

**Interfaces:**
- Consumes: Task 1 `Realm.opensBracelet()`, `CharacterFinder.find`.
- Produces: D70의 enum·결과 값, `Inventory.capacity(boolean braceletOpen)`, 칸 관련 도메인·포트 메서드의 마지막 인자 `boolean braceletOpen`(예: `InventoryEditor.addUpTo(Long characterId, Long itemTypeId, int count, boolean braceletOpen)`, `GiveResult give(Long from, Long to, Long itemTypeId, int count, boolean giverBraceletOpen, boolean receiverBraceletOpen)`). `ItemCommandHandler.inventoryText(Inventory, boolean braceletOpen)` — 닫힌 저물탁 줄에 ` (축기 초기 필요)`. `InventoryMessage.totalSlots` = `capacity(braceletOpen)`.

- [ ] **Step 1: 도메인 테스트** — `BagGradeTest`: 여섯 등급 칸(20·40·80·100·200·400), `displayName`(`하급 저물대`·`상급 저물탁`), `isBracelet`. `InventoryTest`: `braceletOpenAddsSlots`(하급 저물탁, 열림 → 102, 닫힘 → 2), `braceletTakesNoSlot`, `secondBraceletFail`(BRACELET_LIMIT), `bagLimitIgnoresBracelet`(저물대 3 + 저물탁 1 됨), `overCapacityAddFail`(저물탁 닫힘·130칸 사용 → `roomFor` 0·`checkAdd` NO_ROOM), `overCapacityRemoveItemOk`, `overCapacityRemoveBagFail`(BAG_IN_USE), `removeOpenBraceletInUseFail`(BRACELET_IN_USE), `removeClosedBraceletWhileOverOk`(칸이 줄지 않음), `overCapacitySaleNoNewSlotOk` / `overCapacitySaleNewSlotFail`(NO_ROOM), `purchaseBraceletLimit`, `giveReceiverBraceletLimit`.
- [ ] **Step 2: 서비스 테스트** — `InventoryEditorTest`: `addUpToRespectsBraceletOpen`, `giveBraceletToReceiverWithBraceletFail`. `WorldSettingsEditorTest`: `twoBraceletsFail`(`저물탁은 하나까지만 찰 수 있습니다.`), `braceletClosedInStartingCapacity`(하급 저물탁 1·하품 영석 30·회춘단 3·비검 1 → 저물탁 칸은 0이라 400 `시작 소지품이 칸을 넘습니다. (필요 3칸, 칸 2)`).
- [ ] **Step 3: 게임 테스트** — `GameConnectorTest`: `inventoryShowsClosedBracelet`(범인, `하급 저물탁 (축기 초기 필요)` 줄, `[소지품 0/2칸]`), `braceletOpensAtFoundation`(Task 1의 `PlayerCharacter.changeRealm` 을 리포지토리로 저장해 `FOUNDATION_EARLY` → `[소지품 0/102칸]`, `(축기 초기 필요)` 없음), `braceletClosesAfterDemotion`(축기에서 120칸 채운 뒤 범인으로 → `[소지품 120/2칸]`, `줍기` → `저물대에 자리가 없습니다.`, `버리기` 성공), `pickSecondBraceletFail`(`저물탁은 하나까지만 찰 수 있습니다.`), `giveBraceletToHolderFail`(`{이름은} 저물탁을 더 찰 수 없습니다.`), `dropBraceletInUseFail`(`저물탁에 든 물건을 먼저 비우십시오.`). 경지를 바꾸는 테스트 도우미는 `BaseGameTest.setRealm(accountId, Realm)`(리포지토리 직접, Task 9에서 `designateRealm` 이 생김).
- [ ] **Step 4: 구현 → 통과.** 3차 테스트의 바뀐 기대값(저물대 종류 이름·등급 이름)만 고친다.
- [ ] **Step 5: 검증** — `./gradlew build`.
- [ ] **Step 6: 커밋** — `feat(world-server): 저물탁(축기 초기부터 칸이 열림)과 칸을 넘은 소지품 규칙`.

**완료 조건:** 위 테스트 통과, 3차 저물대 동작 그대로.
**progress.md 한 줄:** `BagGrade` 저물탁 셋·`displayName`, `braceletOpen` 인자, 저물탁 한도·문구, 넘은 소지품 규칙(D70), 소지품 `(축기 초기 필요)`.

---

### Task 5: 수련 슬라이스 — 익힌 공법·스탯 계산·읽기 서비스·경지 지정

**권장 모델:** opus

**목적:** D60·D64·D65·D69(포트·이벤트). 게임 명령은 Task 8·9, REST는 Task 6.

**Files:**
- Create: `domain/cultivation/{CharacterTechnique,StatSheet}.java`, `application/cultivation/{CultivationQueryService,CultivationModifyService}.java`, `application/cultivation/provided/{CultivationFinder,TechniqueReader,ReadResult,CultivationProfile,CharacterSummary,LearnedTechniqueView}.java`, `application/cultivation/required/CharacterTechniqueRepository.java`, `application/character/provided/{CharacterRealmEditor,RealmChangedEvent}.java`
- Modify: `orm.xml`, `application/character/CharacterModifyService.java`(`CharacterRealmEditor` 구현), `application/technique/required/TechniqueRepository.java`(+`countLearners`, `maxLearnedStage`), `application/technique/{TechniqueModifyService,TechniqueQueryService}.java`
- Test: `domain/cultivation/{StatSheetTest,CharacterTechniqueFixture}`, `application/cultivation/required/CharacterTechniqueRepositoryTest`, `application/cultivation/provided/{TechniqueReaderTest,CultivationFinderTest}`, `application/character/provided/CharacterRealmEditorTest`, `application/technique/provided/TechniqueEditorTest`(추가)

**Interfaces:**
- Consumes: Task 1·2·3·4(`InventoryEditor.remove(…, braceletOpen)`, `ItemType.findTechnique`).
- Produces:
  - `StatSheet.of(Realm, SpiritRoot, List<CharacterTechnique>)` (D60), `CharacterTechnique` (D64).
  - `CultivationFinder`(`/** 캐릭터의 수련 상태(경지·영근·익힌 공법·스탯)를 본다 */`): `StatSheet stats(Long characterId)`, `List<CharacterTechnique> findLearned(Long characterId)`(익힌 순서, 게임 트랜잭션 안에서 씀), `CultivationProfile profile(Long characterId)`(값 record: `id, name, realm, @Nullable SpiritRoot spiritRoot, StatSheet stats, List<LearnedTechniqueView> techniques` — `LearnedTechniqueView(Long techniqueId, String name, int stage, int stageCount)`), `List<CharacterSummary> summaries()`(`CharacterSummary(Long id, String name, Realm realm, @Nullable String spiritRootName, int techniqueCount)`, 이름순). 없는 캐릭터 id 는 `stats`·`profile` 모두 `IllegalArgumentException("캐릭터를 찾을 수 없습니다. id: " + id)`(`CharacterFinder.find`).
  - `TechniqueReader`(`/** 옥간·서책을 읽어 공법을 익힌다 */`): `ReadResult read(Long characterId, Long itemTypeId)` (D65). 물건이 소지품에 있는지는 부르는 쪽이 확인한다.
  - `CharacterRealmEditor`·`RealmChangedEvent` (D69).
  - `TechniqueRepository` + `@Query("select count(c) from CharacterTechnique c where c.technique.id = :id") long countLearners(Long id)`, `@Query("select max(c.stage) from CharacterTechnique c where c.technique.id = :id") Optional<Integer> maxLearnedStage(Long id)`. `TechniqueView.learnerCount` 를 채우고, 삭제 `TechniqueInUseException("익힌 캐릭터가 있는 공법은 지울 수 없습니다: " + name)`(담긴 물건 검사보다 먼저), 단계 수 줄이기 `TechniqueInUseException("이 공법을 더 높은 단계까지 익힌 캐릭터가 있어 단계 수를 줄일 수 없습니다.")`.
  - 매핑 D64. `CharacterTechniqueRepository extends Repository<CharacterTechnique, Long>`: `save`, `List<CharacterTechnique> findAllByCharacterIdOrderByIdAsc(Long)`, `boolean existsByCharacterIdAndTechniqueId(Long, Long)`, `@Query` 캐릭터별 개수.

- [ ] **Step 1: 도메인 테스트** — `StatSheetTest`: 범인·화 천영근 → (10, 0, 10, 10, 0, 1, 1), 자질 `{FIRE:100}`; 연기 1층·장춘공 1성 → 기혈 60·법력 25·의식 20·정혈 20·영력 5·육신 6·근력 3; 장춘공 3성으로 만든 픽스처(stage 3) → 법력 25+4=29, 의식 21, 목 자질 50+2; 연기 7층 → 기혈 120·법력 55·의식 32; 강체결 5성 → 육신 +2·근력 +2(기혈도 +20); `manaZeroWhenSpiritZero`(범인 + 법력 증가 있는 공법 → 0); `aptitudeOnlyForTechniqueElements`(목 공법 증가가 화 자질에 안 더해짐); 축기 대원만 기혈 540·법력 255.
- [ ] **Step 2: 실패 확인 → 구현.**
- [ ] **Step 3: 리포지토리 테스트** — `CharacterTechniqueRepositoryTest`: `learnAndFindInOrder`, `duplicateFail`(flush 때 `DataIntegrityViolationException`).
- [ ] **Step 4: 서비스 테스트** — `TechniqueReaderTest`(실제 빈, 3차 픽스처로 옥간 종류·소지품 준비): `notScroll`, `noTechnique`, `alreadyLearned`(`AlreadyLearned("장춘공", 1, 9)`, 옥간 그대로), `realmTooLow`(청목결, 범인 → `RealmTooLow(QI_7)`, 옥간 그대로), `missingRoot`(화 천영근이 장춘공 → `MissingRoot(WOOD)`), `aptitudeComparesFraction`(금·목·화 진영근, 목 자질 조건 33 공법 → 성공, 34 → `LowAptitude(WOOD, 34, 33)`), `bodyRefiningSkipsRootChecks`(화 천영근이 강체결 성공), `learnConsumesOneScroll`(옥간 2 → 1), `mortalInitiates`(범인이 장춘공 → `initiated == true`, 경지 `QI_1`, 이벤트 없음 — `@RecordApplicationEvents`), `nonMortalDoesNotInitiate`(연기 3층이 장춘공 → 경지 그대로). `CharacterRealmEditorTest`: `designatePublishesEvent`(QI_1 → QI_7, `RealmChangedEvent(id, QI_1, QI_7)`), `designateSameRealmNoEvent`, `designateUnknownFail`. `CultivationFinderTest`: `profileOutsideTransaction`, `summariesByName`(영근 없음 null·공법 수), `profileUnknownFail`(문구 그대로). `TechniqueEditorTest` 추가: `shrinkBelowLearnedStageFail`, `shrinkAboveLearnedStageOk`, `deleteLearnedFail`, `learnerCountInView`.
- [ ] **Step 5: 구현 → 통과.**
- [ ] **Step 6: 검증** — `./gradlew build`(ArchUnit: `cultivation → character, technique, item, inventory`, 순환 없음).
- [ ] **Step 7: 커밋** — `feat(world-server): 익힌 공법·스탯 계산·읽기와 경지 지정`.

**완료 조건:** 위 테스트 통과, 스탯이 platform.md 표·식과 같음.
**progress.md 한 줄:** `cultivation` 슬라이스(CharacterTechnique·StatSheet·TechniqueReader·CultivationFinder), `CharacterRealmEditor`·`RealmChangedEvent`, 공법 익힌 캐릭터 검사 두 가지.

---

### Task 6: 캐릭터 관리 REST — 목록·상세·경지 지정

**권장 모델:** opus

**목적:** D72의 `characters` 세 줄과 접속 표시.

**Files:**
- Create: `adapter/webapi/CharacterApi.java`, `adapter/webapi/dto/{CharacterSummaryResponse,CharacterDetailResponse,RealmDesignateRequest}.java`, `application/game/provided/PresenceFinder.java`, `application/game/PresenceQueryService.java`
- Test: `adapter/webapi/CharacterApiWebMvcTest`, `AdminApiIntegrationTest`(추가), `application/game/provided/GameConnectorTest`(추가 한 줄: `PresenceFinder`)

**Interfaces:**
- Consumes: Task 5 `CultivationFinder`·`CharacterRealmEditor`.
- Produces:
  - `PresenceFinder`(`/** 지금 세계에 접속해 있는 캐릭터를 본다 */`): `Set<Long> onlineCharacterIds()` — `PresenceRepository` 를 읽는다(영근을 고르는 중인 캐릭터는 세계에 없으므로 아님).
  - `record RealmDesignateRequest(Realm realm)` — null 이면 컴팩트 생성자에서 던져 `요청 본문을 읽을 수 없습니다`(D72).
  - `GET /api/admin/characters` → `CharacterSummaryResponse {id, name, realm, realmName, spiritRootName, techniqueCount, online}`, `GET /{id}` → `CharacterDetailResponse`(admin-tool.md 모양 그대로: `spiritRoot.roots[].aptitude` 는 소수, `stats` 일곱 최대치, `techniques` 익힌 순서), `PUT /{id}/realm` → 200 상세(지정 뒤 다시 읽음).

- [ ] **Step 1: 웹 테스트** — 목록 본문(`spiritRootName:"목·화 진영근"`, 없으면 null, `online:true`), 상세(`realmName:"연기 1층"`, `stats.qiBlood:60`, `roots[0].aptitude:50.0`), `PUT` 200, `realm:null` 400·잘못된 값 400(둘 다 `detail` `요청 본문을 읽을 수 없습니다`), 없는 id 400 `캐릭터를 찾을 수 없습니다. id: 9`, 관리자 아님 403, `X-MUD-Client` 없음 403.
- [ ] **Step 2: 통합 테스트** — `manageCharacters`: 캐릭터 둘(하나는 영근 없음) → 목록 이름순 → `PUT realm FOUNDATION_EARLY` → 상세 `stats.qiBlood:360` 이 트랜잭션 밖에서 직렬화.
- [ ] **Step 3: 구현 → 통과.**
- [ ] **Step 4: 검증** — `./gradlew build`.
- [ ] **Step 5: 커밋** — `feat(world-server): 캐릭터 목록·상세·경지 지정 관리 API`.

**완료 조건:** 위 테스트 통과.
**progress.md 한 줄:** `/api/admin/characters` 3개, `PresenceFinder`.

---

### Task 7: 게임 — 생성 영근 단계와 영근 없는 기존 캐릭터

**권장 모델:** opus

**목적:** D66, D75의 world-server 생성 부분. 2인자 `CharacterCreator.create` 를 지운다.

**Files:**
- Modify: `application/game/{GameService,GameSession}.java`, `application/character/provided/CharacterCreator.java`·`CharacterModifyService.java`(2인자 `create` 제거)
- Modify(호출부): `grep -rn "characterCreator.create\|CharacterCreator" world-server/src` 로 찾은 테스트 전부 — `BaseGameTest`, `GameConnectorTest`, `GameConnectorConcurrencyTest`, `GameWebSocketIntegrationTest`, `CharacterCreatorTest`, `InventoryEditorTest` 등. `domain/character/CharacterFixture`(+`spiritRoot()`).
- Test: `application/game/provided/GameConnectorTest`(추가), `adapter/websocket/GameWebSocketIntegrationTest`(고침·추가)

**Interfaces:**
- Consumes: Task 1 `SpiritRoot.parse`, `CharacterCreator.create(…, SpiritRoot)`·`chooseSpiritRoot`, `CharacterFinder`.
- Produces: `GameSession` 단계 D66(`rooting(…)`, `rootingExisting(accountId, characterId, name)`, `pendingRoot`). 문자열 상수: `ROOT_PROMPT`, `INVALID_ROOT`, `ROOT_MISSING = "영근을 아직 정하지 않았습니다. 영근을 고르면 세계에 들어갑니다."`, `ROOT_CHOSEN = "영근을 정했습니다."`, 확인 마지막 줄 둘(command.md 그대로).

- [ ] **Step 1: 테스트** — `GameConnectorTest`: `createAsksRootAfterName`(이름 → 영근 prompt), `invalidRootAsksAgain`(`목 목` → error 문구 + 같은 prompt), `rootBackToName`, `confirmPromptShape`(`화 목` → prompt 네 줄 그대로, `목·화 진영근 — 목 50, 화 50` … `이대로 만들려면 '확인', 고치려면 '뒤로'.`), `confirmBackToRoot`, `otherInputRepeatsConfirm`, `createMortalWithRoot`(확인 → `캐릭터를 만들었습니다.` → session(playing) → room → appear, DB `MORTAL`·영근), `nameTakenAtConfirm`, `threeRootsSummaryFloors`(`금 33, 목 33, 수 33`), `existingCharacterChoosesRoot`(영근 없는 캐릭터 — 테스트는 `CharacterRepository` 로 영근 없이 저장 — 접속 → session(creating, 이름) → info `ROOT_MISSING` → 영근 prompt → `목 화` → 확인 prompt 마지막 줄 `이대로 정하려면 …` → `뒤로` 는 같은 영근 prompt → 다시 `목 화` → `확인` → info `영근을 정했습니다.` → session(playing) → 저장된 방 room → 같은 방 다른 사람에게 appear), `reconnectWhileRootingRestarts`(고르는 중 같은 계정 재접속 → 앞 연결 4409, 새 연결은 안내·영근 prompt부터), `disconnectWhileRootingAsksNextTime`.
- [ ] **Step 2: 실패 확인 → 구현.** 1~3차 테스트의 생성 단계를 `이름` → `목 화` → `확인` 으로 고친다(기대 문구 중 3차 이름 확인 prompt 는 영근 확인 prompt 로). `BaseGameTest.enter/enterAt` 은 `CharacterFixture.spiritRoot()` 로 만든다.
- [ ] **Step 3: WebSocket 통합** — `authAndCreateCharacter` 를 영근 단계로 고치고 `legacyCharacterChoosesRootOverWebSocket` 한 개를 더한다(JSON `{"type":"session","state":"creating","characterName":"한백"}`).
- [ ] **Step 4: 검증** — `./gradlew build`(1~3차 게임 테스트 전부 통과).
- [ ] **Step 5: 커밋** — `feat(world-server): 캐릭터 생성 영근 단계와 영근 없는 기존 캐릭터의 영근 고르기`.

**완료 조건:** 위 테스트 통과, 문구가 command.md 와 글자 하나까지 같음, `CharacterCreator` 에 2인자 `create` 가 없음.
**progress.md 한 줄:** 생성 단계 이름 → 영근 → 확인, 기존 캐릭터 영근 고르기, 2인자 생성 제거와 고친 테스트 목록.

---

### Task 8: 게임 — `상태`·`공법`·`신통`과 `status` 메시지

**권장 모델:** opus

**목적:** D67(상태·공법·신통)·D71.

**Files:**
- Create: `application/game/CultivationTexts.java`, `domain/shared/Durations.java`
- Modify: `domain/game/{Command,CommandParser}.java`(`Status`·`Techniques`·`Abilities`), `application/game/GameService.java`, `application/game/required/GameMessage.java`(D71 record, `SystemMessage.growth`)
- Test: `domain/shared/DurationsTest`, `domain/game/CommandParserTest`(추가), `application/game/CultivationTextsTest`(package 같은 테스트, 도메인 픽스처로), `application/game/provided/GameConnectorTest`(추가), `adapter/websocket/WebSocketGameMessageSenderTest`(추가)

**Interfaces:**
- Consumes: Task 5 `CultivationFinder.stats`·`findLearned`, Task 2 `TechniqueFixture`.
- Produces: `Command.Status()`(`상태`·`ㅅ`, 인자 있으면 Unknown), `Command.Techniques()`(`공법`), `Command.Abilities()`(`신통`). `CultivationTexts` 메서드 D67(읽기·경지 알림 메서드는 Task 9가 채움 — 이 태스크는 `status`·`techniques`·`abilities`). `StatusMessage.of(PlayerCharacter, StatSheet, String text)`.

- [ ] **Step 1: 테스트** — `DurationsTest`: 3 → `3초`, 1.5 → `1.5초`, 60 → `1분`, 432 → `7분 12초`, 61.5 → `1분 1.5초`, 3,600 → `1시간`, 3,900 → `1시간 5분`. `CultivationTextsTest`: `statusQi1`(command.md `### 상태` 예 여섯 줄 그대로), `statusMortal`(범인 예 — 수위 줄 없음), `statusCommaNumbers`(연기 13층 `수위  0 / 11,000`), `techniquesLine`(`장춘공  1/9성 · 목 · 오행  다음 2성: 목 깨달음 120 (지금 0)`), `bodyRefiningLine`(`강체결  1/9성 · 연체  다음 2성: 연체 깨달음 530 (지금 0)`), `multiElementSplitsInsight`(화·토 공법 2성 400 → `다음 2성: 화 깨달음 200 (지금 0) · 토 깨달음 200 (지금 0)`), `lastStageLine`(1성짜리 공법 → `X  1/1성 · 끝에 이름 · 목 · 오행`), `realmTooLowSuffix`(연기 3층에서 청목결 → 끝에 `  (연기 7층 필요)`), `noTechniques`(`익힌 공법이 없습니다.`), `abilitiesBlock`(command.md 예 그대로 — `[장춘공 1성]` / `  화탄술  공격·화  법력 5  대기 3초` / `[강체결 1성]` / `  철권  공격·연체  법력 없음  대기 4초`), `castTimeShown`(시전 2 → `법력 5  시전 2초  대기 3초`), `perTickCost`(`의식 1 (틱마다)`), `noAbilities`(`배운 신통이 없습니다.`). `CommandParserTest`: `상태`·`ㅅ`·`공법`·`신통`, `상태 한립` → Unknown.
- [ ] **Step 2: 게임 테스트** — `GameConnectorTest`: `statusMessage`(범인 `status` 메시지 필드: `realm {MORTAL, 범인}`, `stats.qiBlood {10,10}`, `cultivation null`, `tone "status"`), `statusAfterRealmChange`(연기 1층 → `cultivation {0,190}`), `techniquesInfo`, `abilitiesFollowDefinition`(익히기는 `읽기` 명령(Task 9)을 기다리지 않고 Task 5 `TechniqueReader.read` 를 직접 불러 준비; 익힌 뒤 관리자가 화탄술 대기 3 → 5로 고치면 다음 `신통` 이 `대기 5초`, 2성 신통을 1성으로 옮기면 바로 보임), 정보 명령은 같은 방 알림 없음.
- [ ] **Step 3: 직렬화** — `WebSocketGameMessageSenderTest.status`: JSON 필드 순서 D71, `spiritRoot.roots[0] == {"element":"WOOD","elementName":"목","aptitude":50}`.
- [ ] **Step 4: 구현 → 통과.**
- [ ] **Step 5: 검증** — `./gradlew build`.
- [ ] **Step 6: 커밋** — `feat(world-server): 상태·공법·신통 명령과 status 메시지`.

**완료 조건:** 위 테스트 통과, 출력이 command.md 예와 글자 하나까지 같음.
**progress.md 한 줄:** `Command.Status/Techniques/Abilities`, `CultivationTexts`, `Durations`, `StatusMessage`(tone `status`), `SystemMessage.growth`.

---

### Task 9: 게임 — `읽기`·옥간 보기·범인 상점 막힘·경지 알림·도움말 (+3차 상점 테스트 고침)

**권장 모델:** opus

**목적:** command.md `### 읽기`·`### 범인의 상점 막힘`·`### 경지가 바뀔 때`·`### 4차 도움말`, D65(게임 쪽)·D67(읽기·알림)·D68·D69(리스너)·D75·D76.

**Files:**
- Modify: `domain/game/{Command,CommandParser}.java`(`Read(@Nullable ItemQuery)`), `application/game/{GameService,ItemCommandHandler,ShopCommandHandler,CultivationTexts}.java`
- Test: `domain/game/CommandParserTest`(추가), `application/game/CultivationTextsTest`(추가), `application/game/provided/{BaseGameTest,GameConnectorTest}`(추가·고침), `adapter/websocket/GameWebSocketIntegrationTest`(추가)

**Interfaces:**
- Consumes: Task 5 `TechniqueReader`·`CharacterRealmEditor`·`RealmChangedEvent`, Task 4 `braceletOpen`.
- Produces: `Command.Read`(인자 전체가 이름 — `ItemQuery.parse`, 수량 해석 없음; 인자 없으면 `item == null`). `GameService.onRealmChanged(RealmChangedEvent)`(D69). `BaseGameTest.designateRealm(Long accountId, Realm)`(`CharacterRealmEditor.designate`, 접속 중이면 `growth` 알림이 감). Task 4의 `setRealm`(알림 없음)은 남겨 준비용으로 쓴다. `HELP` = D76.

- [ ] **Step 1: 읽기 테스트** — `CultivationTextsTest`: `readSuccessInitiates`(`옥간을 이마에 대자 구결이 머릿속으로 흘러든다. 장춘공 1성에 이르렀습니다!` / `단전에 따뜻한 기운이 맺힌다. 연기 1층이 되었습니다. 새 신통: 화탄술`), `readSuccessNonMortal`(둘째 줄 `새 신통을 깨쳤습니다: 청목전`), `readWithStageBonus`(1성 법력 2·목 자질 1 → 둘째 줄 `법력 +2 · 목영근 자질 +1`, 셋째 줄 신통), `readFailures`(command.md 3~8 문구 — `읽을 수 있는 물건이 아닙니다.`, `아무것도 적혀 있지 않습니다.`, `이미 깨친 내용입니다. (장춘공 1/9성)`, `지금은 이 공법을 익힐 수 없습니다. (연기 7층 필요)`, `… (화영근 필요)`, `… (화영근 자질 40 필요, 지금 33)`). `GameConnectorTest`: `readNoArgFail`(`무엇을 읽으시겠습니까? (예: 읽기 장춘공 옥간)`), `readNotHeldFail`(`그런 물건은 가지고 있지 않습니다.`), `readInitiates`(growth 메시지, `상태` 가 연기 1층, 옥간 사라짐, 같은 방 알림 없음), `readKeepsScrollOnFail`, `readOrdinal`(`읽기 2.옥간`), `readConfirmIsItemName`(`읽기 확인` → `그런 물건은 가지고 있지 않습니다.`), `lookScrollShowsTechnique`(`보기 장춘공 옥간` → 둘째 줄 `담긴 공법: 장춘공 (목 · 오행 · 9성 · 범인부터)`, 강체결은 `(연체술 · 9성 · 범인부터)`).
- [ ] **Step 2: 막힘·알림 테스트** — `mortalShopBlocked`(범인 `목록`·`사기 회춘단`·`팔기 회춘단` → `아직 범인의 몸이라 할 수 없습니다.`, 상점 없는 방에서도), `mortalAppraiseAllowed`(`값` 은 됨), `confirmAfterDemotedToMortalFail`(연기 1층에서 확인 대기 → `designateRealm(MORTAL)` → `확인` → 막힘 문구, 영석·물건 그대로), `realmChangeNotifies`(접속 중 QI_1 → QI_7 → growth `경지가 연기 7층으로 바뀌었습니다. (육신 +6, 근력 +3, 영력 +6, 의식 +12, 정혈 +12)`, QI_7 → FOUNDATION_EARLY → `경지가 축기 초기로 바뀌었습니다. (육신 +24, 근력 +12, 영력 +23, 의식 +56, 정혈 +56)`, 내리면 `-`, 같은 방 다른 사람은 받지 않음), `designateOfflineOrSameRealmSendsNothing`, `helpText`(D76 문구 그대로).
- [ ] **Step 3: 3차 상점 테스트 고침(L1)** — 상점을 쓰는 1~3차 게임·WebSocket 테스트 준비에 `setRealm(…, QI_1)` 을 더한다(알림이 없어 기대 메시지 순서가 그대로 — `designateRealm` 을 쓰면 `growth` 메시지가 끼어 깨진다. 기대값은 바꾸지 않음). 바꾼 테스트 이름을 progress.md 메모에 적는다.
- [ ] **Step 4: 구현 → 통과.**
- [ ] **Step 5: 검증** — `./gradlew build`.
- [ ] **Step 6: 커밋** — `feat(world-server): 읽기·옥간 보기·범인 상점 막힘·경지 알림·4차 도움말`.

**완료 조건:** 위 테스트 통과, 문구가 command.md 와 글자 하나까지 같음, 3차 상점 테스트는 준비만 바뀜.
**progress.md 한 줄:** `Command.Read`, 읽기 성공·거절 문구, `보기` 옥간 둘째 줄, 범인 상점 막힘(D68), AFTER_COMMIT 경지 알림, 도움말 D76, L1로 고친 테스트.

---

### Task 10: admin-console — 공법 화면, 아이템의 담긴 공법·저물탁 등급

**권장 모델:** opus

**목적:** admin-tool.md `## 4차 화면` 의 공통 바 `공법`, `/techniques/`, `/technique/`, `/item/`·`/items/` 변경. D73.

**Files:**
- Create: `app/techniques/page.tsx`, `app/technique/page.tsx`, `src/lib/techniques.ts`, `src/components/{TechniqueForm,StageTable,AbilityTable}.tsx` (+ `.test.tsx` 셋, `src/lib/techniques.test.ts`)
- Modify: `src/lib/api.ts`(타입·함수: `RealmResponse`, `Technique*`, `listRealms`, `listTechniques`, `getTechnique`, `createTechnique`, `updateTechnique`, `deleteTechnique`, `ItemTypeResponse/SaveRequest.techniqueId`·`techniqueName`, `BagGradeCode` 여섯), `src/lib/items.ts`(`BAG_GRADES` 여섯 + `bagGradeLabel` = `하급 저물대 (20칸)`, `BAG` 이름 `저물대·저물탁`), `src/components/{ItemForm,TopBar}.tsx`, `app/items/page.tsx`, `src/lib/api.test.ts`, `src/components/ItemForm.test.tsx`

**Interfaces:**
- Consumes: Task 3 REST.
- Produces: `TechniqueForm({initial?: TechniqueDetailResponse, realms: RealmResponse[], onSaved})`, `StageTable({stages, onChange})`(단계 수를 바꾸면 행이 늘거나 줄고 새 행은 0, 1성 필요 깨달음 칸은 입력 없이 `읽으면`), `AbilityTable({abilities, stageCount, onChange})`(새 행 기본값 admin-tool.md 그대로). 상수 `ELEMENTS`, `ABILITY_KINDS`, `ABILITY_TARGETS`, `COST_STATS`, `USABLE_WHENS`(코드·이름 D58·D62).

- [ ] **Step 1: 테스트** — `techniques.test.ts`: 상수 코드·이름. `StageTable.test`: 단계 수 9 → 12 행 추가 0, 12 → 3 행 줄임, 1성 칸 `읽으면`. `AbilityTable.test`: `신통 추가` 기본값(1성·공격·속성 없음·한 대상·소모 없음·시전 0·대기 3·전투 중만), `삭제`. `TechniqueForm.test`: 만들기 요청 본문(`elements` 오행 순, `stages[0].insightRequired: 0`), 저장 실패 시 서버 `detail` 표시, 고칠 때 `이 공법이 담긴 물건` 링크·`익힌 캐릭터 2명`, `삭제` 확인 `'장춘공' 공법을 지울까요?`, 만들기 성공 뒤 `/technique/?id=<새 id>` 로 이동, 안내 `속성을 고르지 않으면 연체술 공법입니다.`. `ItemForm.test`: 종류 `옥간·서책` 일 때만 `담긴 공법`(첫 항목 `없음` + 이름순), 등급 선택 여섯 항목 문구, `techniqueId` 왕복. `api.test.ts`: 새 함수 경로·메서드·`X-MUD-Client`.
- [ ] **Step 2: 실패 확인 → 구현 → 통과.** 목록 열은 admin-tool.md 그대로(`이름`·`계열`·`속성`(없으면 `-`)·`시작 경지`·`단계 수`(`9성`)·`신통 수`·`익힌 캐릭터 수`), `/items/` 에 `담긴 공법` 열(`-`).
- [ ] **Step 3: 검증** — `npm ci && npm test && npx tsc --noEmit && npm run build`.
- [ ] **Step 4: 커밋** — `feat(admin-console): 공법·신통 화면과 아이템 담긴 공법·저물탁 등급`.

**완료 조건:** 테스트·타입 검사·정적 빌드 통과, 1~3차 화면 테스트는 바뀐 이름(`저물대·저물탁`, 등급 문구) 기대값만 고침.
**progress.md 한 줄:** `/techniques/`·`/technique/`, `TechniqueForm`·`StageTable`·`AbilityTable`, 아이템 담긴 공법·저물탁 등급, 공통 바 `공법`.

---

### Task 11: admin-console — 캐릭터 목록·상세·경지 지정

**권장 모델:** opus

**목적:** admin-tool.md `## 4차 화면` 의 공통 바 `캐릭터`, `/characters/`, `/character/`.

**Files:**
- Create: `app/characters/page.tsx`, `app/character/page.tsx`, `src/components/CharacterRealmForm.tsx`(+ `.test.tsx`)
- Modify: `src/lib/api.ts`(`CharacterSummaryResponse`, `CharacterDetailResponse`, `listCharacters`, `getCharacter`, `designateRealm`), `src/components/TopBar.tsx`, `src/lib/api.test.ts`

**Interfaces:**
- Consumes: Task 6 REST, Task 10 `listRealms`.
- Produces: `CharacterRealmForm({character, realms, onSaved})` — 경지 선택 + `경지 지정`(확인 없이), 성공 `경지를 지정했습니다.`, 안내 `수위·돌파 없이 경지를 바로 바꿉니다. 스탯은 새 경지 값이 되고 바로 다 찹니다.`(admin-tool.md 그대로), 저장 뒤 받은 상세로 스탯 표를 다시 그림.

- [ ] **Step 1: 테스트** — `CharacterRealmForm.test`: `PUT` 본문 `{realm:"QI_7"}`, 성공 문구, 실패 `detail`, 스탯 표 갱신. 목록 렌더(영근 없음 `미정`, 접속 `접속 중`/`-`), 상세 영근 없음 `아직 정하지 않음`, 익힌 공법 없음 `없음`, 공법 링크 `/technique/?id=`.
- [ ] **Step 2: 구현 → 통과.**
- [ ] **Step 3: 검증** — `npm ci && npm test && npx tsc --noEmit && npm run build`.
- [ ] **Step 4: 커밋** — `feat(admin-console): 캐릭터 목록·상세와 경지 지정`.

**완료 조건:** 테스트·타입 검사·정적 빌드 통과.
**progress.md 한 줄:** `/characters/`·`/character/`, `CharacterRealmForm`, 공통 바 `캐릭터`.

---

### Task 12: play-client — `status`·`growth`, 자동완성

**권장 모델:** opus

**목적:** D74·D76(클라이언트 쪽).

**Files:**
- Modify: `src/lib/protocol.ts`(`StatusMessage`, `SystemMessage.tone` 에 `"growth"`), `src/lib/render.ts`, `src/lib/complete.ts`(`COMMANDS`, 이름 후보에 소지품), `src/components/Terminal.tsx`(마지막 `inventory` 기억), `app/globals.css`
- Test: `src/lib/{render,complete}.test.ts`, `src/components/Terminal.test.tsx`

**Interfaces:**
- Consumes: Task 8·9 메시지 모양(D71).
- Produces: `StatusMessage` 타입(D71 필드), 스타일 `status`·`growth`.

- [ ] **Step 1: 테스트** — `render.test`: `status.text` 를 줄마다 style `status`, `system` tone `growth` 여러 줄 → style `growth`, 영근 확인 prompt 네 줄. `complete.test`: `COMMANDS` 가 D76 순서 그대로, `읽` + Tab → `읽기`, `읽기 장` + Tab → 마지막 `inventory` 의 `장춘공 옥간`, 후보 순서 occupants → 바닥 → 소지품(중복 제거). `Terminal.test`: `inventory` 를 받은 뒤 후보 갱신.
- [ ] **Step 2: 구현 → 통과.**
- [ ] **Step 3: 검증** — `npm ci && npm test && npx tsc --noEmit && npm run build`.
- [ ] **Step 4: 커밋** — `feat(play-client): 상태·성장 메시지 표시와 4차 명령 자동완성`.

**완료 조건:** 테스트·타입 검사·정적 빌드 통과.
**progress.md 한 줄:** `status`·`growth` 스타일(색 D74), COMMANDS 4차, 소지품 이름 후보.

---

### Task 13: 문서 — 스펙 `(제안)` 반영과 README

**권장 모델:** sonnet

**목적:** 구현된 결정과 실행 안내를 문서에 옮긴다. 코드를 바꾸지 않는다.

**Files:**
- Modify: `docs/spec/system/{index,command,admin-tool,platform}.md`, `README.md`

**내용:**
- 스펙: `index.md` `### 4차 범위` 에 계획 링크(`../../impl/iteration-4/plan.md`)·구현 날짜. D57~D76 가운데 스펙에 없는 세부를 해당 절 끝에 `(제안)` 으로: D57(enum 코드·`으로/로`)·D59(`@Order`)·D61/D62(검사 순서·예약어 목록)·D63(`Realm`·`Element` 는 shared, `PresenceFinder`)·D65(입문은 알림 없음)·D66(3차 이름 확인 단계 없어짐)·D67(신통 여러 속성·시간 문체 경계)·D68(상점 없는 방에서도 막힘 문구가 먼저)·D70(넘은 소지품 판정·결과 이름)·D74(색). 구현 중 계획과 달라진 것(progress.md 메모)을 코드 기준으로 적는다. 룰북에 없는 새 문구는 "룰북에 아직 없음" 표시. 룰북 파일은 고치지 않는다.
- README: 모듈 소개에 4차 한 줄. `## 4차 시나리오 — 경지·영근·공법 배우기·저물탁`(3차 시나리오 다음): 관리자 `공법` 에서 `장춘공`(목·범인부터·9성, 화탄술 1성)·`청목결`(목·연기 7층부터·8성, 자질 조건 15, 청목전 1성) 만들기(값은 technique.md 장춘공 표·technique-list.md 오행 연기 후반 표) → `아이템` 에서 `장춘공 옥간`·`청목결 옥간`(옥간·서책, 담긴 공법) → 바닥에 둠 → play-client 새 캐릭터 영근 `목 화` → `상태`(범인) → `목록`(범인 막힘) → `줍기`·`보기 장춘공 옥간`·`읽기 장춘공 옥간`(연기 1층) → `읽기 청목결 옥간`(거절) → 관리자 `캐릭터` 에서 연기 7층 지정(알림) → `상태` → `읽기 청목결 옥간` → `공법`·`신통` → `하급 저물탁`(저물대·저물탁, 하급 저물탁, 기준가 5,000) 바닥 → `줍기` → `소지품` `(축기 초기 필요)` → 축기 초기 지정 → 칸 열림. 3차 시나리오 8단계 앞에 "상점은 범인이 쓸 수 없다 — 관리 툴 `캐릭터` 에서 연기 1층으로 지정하거나 입문 옥간을 읽는다(L1)" 한 줄. `## 되는 명령` 에 `상태`·`ㅅ`, `공법`, `신통`, `읽기 <물건>` 줄과 `도움말` 줄(D76 그대로), 생성 때 영근 입력 한 줄. 수동 확인 체크리스트 4차(영근 단계 `뒤로`, 상태 줄 색, 성장 줄 색, 경지 지정 알림이 접속 중 창에만, 공법 화면 단계 표·신통 표, `읽` + Tab, 저물탁 칸). 스키마 한 줄(`ddl-auto=update` 가 `technique`·`technique_element`·`technique_stage`·`ability`·`ability_element`·`character_spirit_root`·`character_technique` 표와 `player_character.realm`·`item_type.technique_id` 열을 더하고, 기존 캐릭터는 연기 1층·다음 접속 때 영근을 고른다). 스모크 절은 Task 14가 고친다.
- [ ] **Step 1: 수정.** 명령 이름·문구는 `GameService.HELP`·`CultivationTexts`·`CommandParser`·admin 화면 문구와 대조한다.
- [ ] **Step 2: 커밋** — `docs(spec): 4차 제안 결정 반영` 과 `docs(infra): README에 4차 시나리오·명령 반영`(둘로 나눠도 됨).

**완료 조건:** D57~D76 각각이 스펙 어딘가에 `(제안)` 으로 있거나 이미 있는 스펙 문장과 같음, README 명령·문구가 코드와 같음, 1~3차 내용 유지.
**progress.md 한 줄:** 스펙에 옮긴 D 목록, 코드와 대조한 결과, README 4차 절.

---

### Task 14: 통합 확인 — 스모크 1~3차 고침·4차 시나리오, compose 한 번

**권장 모델:** opus

**목적:** 실제 컨테이너에서 index.md 4차 완료 시나리오를 자동으로 확인한다. **이 이터레이션에서 Docker를 쓰는 유일한 태스크다.**

**Files:**
- Modify: `scripts/smoke-test.mjs`, `README.md`(스모크 절 4차 항목), `docs/impl/iteration-4/progress.md`

**1~3차 단계 고침 (D75):** `createCharacter(client, name, roots = "목 화")` 가 이름 → 영근 prompt(`영근을 고르십시오.` 로 시작) → `목 화` → 확인 prompt(`목·화 진영근 — 목 50, 화 50` 로 시작) → `확인`. 단계 22 앞에서 관리자 `GET /api/admin/characters` 로 C의 id를 찾아 `PUT …/realm {realm:"QI_1"}` → C가 `system` tone `growth` `경지가 연기 1층으로 바뀌었습니다. (육신 +5, 근력 +2, 영력 +5, 의식 +10, 정혈 +10)`. 단계 16 저물대 응답 검사(`categoryName !== "저물대" || bagGradeName !== "하급"`)를 `저물대·저물탁`·`하급 저물대` 로(D70·admin-tool.md `ItemTypeResponse`). 단계 24 `도움말` 기대를 D76 문구로.

**4차 단계** (단계 25 뒤, 이름에 3차처럼 실행 번호 `<n>` — 숫자로 끝나 조사는 받침 없음; 공법·신통 이름은 띄어쓰기 없이 12자까지):
26. 관리자 — 공법 `장춘공<n>`(목, `MORTAL`, 9성, 단계 값 technique.md 장춘공 표, 신통 화탄술 1성 공격·화·한 대상·법력 5·대기 3·전투 중만, 어풍결 3성 둔술·목·자신·법력 10·대기 10·전투 밖에도) → 201. `청목결<n>`(목, `QI_7`, 8성, 오행 연기 후반 표 값·자질 조건, 신통 청목전 1성 공격·목·한 대상·법력 12·대기 3, 목령갑 4성 방어·목·자신·법력 18·대기 15) → 201. 신통 이름 `상태` 인 요청 → 400 `명령어와 같은 이름은 쓸 수 없습니다: 상태`. `GET /api/admin/realms` 길이 18.
27. 관리자 — 아이템 `장춘공<n> 옥간`·`청목결<n> 옥간`(옥간·서책, 기준가 100·500, `techniqueId`) → 응답 `techniqueName`. 단약 종류에 `techniqueId` → 400 `공법은 옥간·서책에만 담을 수 있습니다.`. `장춘공<n>` 삭제 → 409. 광장 바닥에 `장춘공<n> 옥간` 2개·`청목결<n> 옥간` 1개(재등장 없음).
28. 새 플레이어 E(새 계정) 접속 → 이름 → 영근 `목 목` → error `오행(금 목 수 화 토) 가운데 1~5개를 겹치지 않게 띄어 쓰십시오.` → `목 화` → 확인 prompt 네 줄 → `확인` → 광장.
29. E `상태` → `status.realm.code == "MORTAL"`, `stats.qiBlood == {current:10, max:10}`, `cultivation == null`, text 첫 줄 `<E> · 범인 · 목·화 진영근 (목 50, 화 50)`. E `목록` → `아직 범인의 몸이라 할 수 없습니다.`.
30. E `줍기 장춘공<n> 옥간 2`, `줍기 청목결<n> 옥간`. E `보기 장춘공<n> 옥간` → 둘째 줄 `담긴 공법: 장춘공<n> (목 · 오행 · 9성 · 범인부터)`. E `읽기 장춘공<n> 옥간` → growth `옥간을 이마에 대자 구결이 머릿속으로 흘러든다. 장춘공<n> 1성에 이르렀습니다!\n단전에 따뜻한 기운이 맺힌다. 연기 1층이 되었습니다. 새 신통: 화탄술`. E `상태` → `qiBlood {60,60}`, `mana {25,25}`, `cultivation {0,190}`. E `읽기 장춘공<n> 옥간` → `이미 깨친 내용입니다. (장춘공<n> 1/9성)`.
31. E `읽기 청목결<n> 옥간` → `지금은 이 공법을 익힐 수 없습니다. (연기 7층 필요)` → `소지품` items 에 `청목결<n> 옥간`.
32. 관리자 — `GET /api/admin/characters` 에 E(`realm:"QI_1"`, `spiritRootName:"목·화 진영근"`, `techniqueCount:1`, `online:true`) → `PUT …/realm {realm:"QI_7"}` → 200 `stats.qiBlood:120`. E가 growth `경지가 연기 7층으로 바뀌었습니다. (육신 +6, 근력 +3, 영력 +6, 의식 +12, 정혈 +12)`. E `상태` → `qiBlood {120,120}`, `mana {55,55}`, `cultivation {0,1700}`.
33. E `읽기 청목결<n> 옥간` → growth 둘째 줄 `새 신통을 깨쳤습니다: 청목전`. E `공법` → `장춘공<n>  1/9성 · 목 · 오행  다음 2성: 목 깨달음 120 (지금 0)` 과 `청목결<n>  1/8성 · 목 · 오행  다음 2성: 목 깨달음 1,800 (지금 0)`. E `신통` → `[장춘공<n> 1성]`, `  화탄술  공격·화  법력 5  대기 3초`, `[청목결<n> 1성]`, `  청목전  공격·목  법력 12  대기 3초`.
34. 관리자 — 아이템 `하급 저물탁<n>`(`BAG`, `BRACELET_LOW`, 5,000) → 광장 바닥 1개. E `줍기 하급 저물탁<n>` → `소지품` text 에 `하급 저물탁<n> (축기 초기 필요)`, `totalSlots == 2`.
35. 관리자 — E `FOUNDATION_EARLY` → E growth `경지가 축기 초기로 바뀌었습니다. (육신 +24, 근력 +12, 영력 +23, 의식 +56, 정혈 +56)`. E `소지품` → `totalSlots == 102`, `(축기 초기 필요)` 없음. E `도움말` 에 `상태(ㅅ) · 공법 · 신통 · 읽기 <물건>`.

- [ ] **Step 1: 스크립트 수정** — 1~3차 고침과 26~35를 더한다(Node 26 내장 `fetch`·`WebSocket` 만). 머리 주석의 '1차·2차·3차' 를 '1~4차' 로.
- [ ] **Step 2: 모듈 검증** — 세 모듈 검증 명령을 다시 돌려 통과 확인.
- [ ] **Step 3: compose 한 번** — 시작 전 `df -h .`·`docker system df` 를 메모에 적는다(10GB 미만이면 멈춤). Global Constraints 스모크 방법대로 `docker compose build` **한 번** → `up -d`(셸 값 덮어쓰기) → 기동 로그에 스키마 오류 없음 확인(기존 `pgdata` 의 캐릭터가 `QI_1` 로 채워짐 — `GET /api/admin/characters` 로 확인) → `source "$NVM_DIR/nvm.sh" && nvm use 26.13.1 && node scripts/smoke-test.mjs` → `SMOKE OK`. 실패하면 원인을 고치고(해당 모듈 테스트 통과) 바뀐 서비스만 `docker compose up -d --build <서비스>`(전체 재빌드 반복 금지).
- [ ] **Step 4: 되돌리기** — 관리 API로 시작 방을 id 1로, 시작 소지품을 `[]` 로 되돌리고 `GET /api/admin/world`·`/world/starting-items` 로 확인한다.
- [ ] **Step 5: 정리** — `docker compose down`(볼륨 남김) → `docker image prune -f` → `docker builder prune -f` → `world-server/build`, `play-client/{out,.next}`, `admin-console/{out,.next}` 삭제(`git check-ignore` 로 먼저 확인). `mud-mk3-*` 이미지·`pgdata`·`node_modules` 는 지우지 않는다. 정리 뒤 `df -h .`·`docker system df` 를 메모에.
- [ ] **Step 6: README 스모크 절** — 4차 항목 두 줄(공법·옥간·영근 생성·범인 막힘·읽기·경지 지정 알림 / 공법·신통 출력·저물탁)과 '3차 상점 단계 전에 C를 연기 1층으로 지정' 한 줄.
- [ ] **Step 7: progress.md** — 결과(통과 여부, 고친 것, 남은 수동 확인), 상태를 "4차 이터레이션 구현 완료" 로.
- [ ] **Step 8: 커밋** — `test(infra): 4차 스모크 시나리오(공법·영근·읽기·경지 지정·저물탁)`.

**완료 조건:** compose 환경에서 단계 1~35 `SMOKE OK`, 세 모듈 검증 통과, 시작 방 id 1·시작 소지품 `[]`, 정리 뒤 dangling 이미지 없음.
**progress.md 한 줄:** 스모크 1~3차 고침·단계 26~35, compose 빌드 한 번, 디스크 전후 값, 되돌림 확인.

**완료 시나리오 대응** (index.md 4차 완료 시나리오 → 스모크 단계): 공법·신통 정의 26 · 옥간 종류 27 · 바닥에 둠 27 · 영근 고르기 28 · 범인 스탯 29 · 입문 읽기·연기 1층 30 · 연기 7층 공법 거절 31 · 경지 지정·알림 32 · 스탯 변화 32 · 다시 읽기 33 · `공법`·`신통` 33 · 저물탁 정의·바닥·줍기·`(축기 초기 필요)` 34 · 축기 초기 지정·칸 열림 35.

---

### Task 15: 최종 전체 리뷰

**권장 모델:** opus

**목적:** 4차 전체(main 의 4차 커밋 전부)를 스펙·룰북·이 계획과 대조한다. 코드를 고치면 따로 커밋한다.

- [ ] **Step 1: 리뷰** — `superpowers:requesting-code-review` 로 4차 첫 커밋 앞부터 HEAD 까지. 볼 것: Review Focus 다섯 줄이 테스트로 덮였는지, 문구가 룰북·스펙과 글자 그대로인지(`CultivationTexts`, `GameService` 상수, admin 화면), 스탯 표 값, ArchUnit 의존(D63), open-in-view 꺼진 상태의 지연 연관, 동시성(관리 지정 vs 게임 읽기 — 나중 커밋이 이김), `(제안)` 반영 누락.
- [ ] **Step 2: Critical·Important 고침** — 해당 모듈 검증 통과 후 커밋 `fix(<module>): …`. Docker는 다시 빌드하지 않는다(모듈 코드가 바뀌어 스모크를 다시 해야 하면 리드에게 묻는다).
- [ ] **Step 3: progress.md** — 리뷰 결과·고친 것·다음으로 미룬 Minor(`## 다음 할 일`), 창호 확인 항목(`(제안)` 결정, 리드 판정 L1~L5, 브라우저 수동 확인).
- [ ] **Step 4: 커밋** — `docs(impl): 4차 이터레이션 완료 기록`.

**완료 조건:** Critical·Important 0, progress.md 완료 기록.
**progress.md 한 줄:** 최종 리뷰 결과와 고친 것.

---

## 태스크 의존 순서

```
1 → 2 → 3 ─────────────┬→ 10 → 11
1 → 4 (3 뒤에 해도 됨) │
2,3,4 → 5 → 6 ─────────┘
5 → 7 → 8 → 9 → 12 → 13 → 14 → 15
```

- 순서대로 하면 된다: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15. 나란히 하지 않는다(여러 태스크가 `orm.xml`·`GameService`·`ItemTypeModifyService` 를 고친다).
- 10은 3 뒤, 11은 6·10 뒤, 12는 9 뒤(실제 메시지와 맞춤). 13은 구현된 값을 옮기므로 12 뒤, 14는 13 뒤, 15는 마지막.
- 각 태스크는 앞 태스크가 커밋된 상태에서 혼자 검증 명령이 통과한다(Task 1은 2인자 생성을 남기고, Task 4는 경지를 리포지토리로 바꾸는 테스트 도우미를 쓰는 식으로 뒤 태스크를 기다리지 않는다).

## 3차에서 미룬 Minor

**4차에서 함께 고치는 것** (4차 작업과 직접 부딪침):
- 저물대 3개일 때 사기·팔기 `BAG_IN_USE`/`NO_ROOM` 테스트 없음 → Task 4가 저물탁 결과 값을 더하며 저물대 경우 테스트도 하나씩 더한다.
- play-client 이름 중복 후보 → Task 12가 후보에 소지품을 더하며 중복 제거를 함께 한다.

**그대로 둠** (4차 작업과 부딪치지 않음): `iteration-3/progress.md` `## 다음 할 일` 의 나머지 Minor 전부, 1·2차 이월 항목, 귀속 삭제 후속의 선택 테스트 둘.
