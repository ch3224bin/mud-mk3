# 3차 이터레이션 구현 계획 — 아이템 + 영석 + 상점 + 저물대·귀속 + 주기

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 관리자가 admin-console에서 아이템 종류(이름·묘사·종류·기준가·거래 가능·저물대 등급)를 정의하고, 방 바닥에 물건을 두고, 새 캐릭터의 시작 소지품을 정하고, NPC에 상점 역할(상점 종류·파는 물건·값)을 준다. 플레이어는 play-client에서 `소지품`·`보기 <물건>`·`줍기`·`버리기`·`목록`·`사기`·`팔기`·`값`·`주기`·`귀속`·`귀속 해제`·`확인`·`취소` 로 물건과 영석(4등급)을 다룬다.

**Architecture:** 2차 구조를 그대로 넓힌다. `world-server`(헥사고날)에 슬라이스 `item`(아이템 종류, 영석 4종을 엔진이 심음)·`inventory`(캐릭터 소지품)·`floor`(방 바닥: 관리자 배치 + 버린 물건)를 더하고, `npc` 에 상점 역할, `world` 에 시작 소지품을 붙인다. 소지품·바닥 물건은 PostgreSQL에 두고, 게임은 1·2차처럼 명령마다 DB에서 읽고 쓰며 명령 하나가 트랜잭션 하나다(서버 전체 게임 락 안). 그래서 두 쪽이 함께 바뀌는 사기·주기도 다 되거나 다 안 된다. 서버 틱은 여전히 없다: 버린 물건 5분과 배치 물건 재등장은 읽을 때 시각(`Clock`)으로 거른다. play-client에는 `room.items`, 새 메시지 `inventory`, `tone: "item"` 이 생긴다.

**Tech Stack:** 1·2차와 같다 — Java 25 · Spring Boot 4.1.2 · Gradle 9.9.1 Kotlin DSL · PostgreSQL 18.6 / H2(테스트) · Next.js 16.4.6 · React 19.3.1 · TypeScript 7.0.2 · Vitest 5.1.0 · Docker Compose. **새 의존성 없음** (시계는 JDK `java.time.Clock`).

**Spec:** 아래 `## Spec` 참고. 계획은 스펙과 룰북을 근거로 하고, 정하지 않은 것(3차 열린 질문 전부)은 `## 제안 결정 목록` 에서 정했다.

---

## Spec

- 창호 결정 원문: `.superpowers/sdd/iter3/decisions.md` (범위와 완료 시나리오. 바꾸지 않는다)
- 시스템 스펙: `docs/spec/system/index.md`(`### 3차 범위`, `### 3차 이터레이션 열린 질문`), `admin-tool.md`(`## 3차 범위`, 1·2차 화면·REST API), `command.md`(`## 3차 명령어`), `world-structure.md`(`## 방 바닥 물건`), `npc.md`(역할 — 3차 상점), `platform.md`(`### 3차 WebSocket 메시지 변경`, `### 3차 월드 상태와 저장`, `## 실시간 통신과 월드 상태`)
- 룰북(게임 규칙 — 규칙·수치·문구의 원본): `docs/spec/game-rule/item.md`(공통 속성, 귀속, 줍기와 버리기, 저물대 · 저물탁, 시작 소지품, 품질, 영석, 결제, 상점에서 사고팔기, 직접 거래), `commands.md`(입력 문법, 대상 지정, 공통 오류 메시지, 정보 — 소지품 형식, NPC · 상점 · 거래, 확인이 필요한 명령), `npc.md`(역할별 이용 — 목록 형식·실패 문구), `game-structure.md`(`### 상점` 표), `world.md`(방 보기 — 떨어진 물건은 대상 목록 맨 끝)
- 1·2차 계획·진행: `docs/impl/iteration-1/plan.md`(D1~D18, 버전 표), `docs/impl/iteration-2/plan.md`(D19~D31), `docs/impl/iteration-2/progress.md`(리드 판정·미룬 Minor)
- 용어: `docs/spec/glossary.md`
- world-server 코딩 규칙: `.claude/rules/*.md`. **world-server 태스크는 시작 전에 모두 읽는다.**
- 3차 범위 밖(만들지 않는다, decisions.md '3차가 아닌 것' 그대로 + 이 계획이 뺀 것): 아이템을 써서 효과 내기(`복용`·`부적`·`진법`·`꺼내기`·`거두기`·`영석` 법력 회복), 교환 창·거래소·경매장·`찾기`, 제작, 몹 드롭·`수습`·`줍기 유품`, 공법·경지, 전투, LLM 챗. 이 계획이 더 뺀 것: 품질(D34), 필요 경지(D34), 저물탁(D33), 영수대(D38), 처음부터 귀속된 물건(D43), `도움말 <명령>`, `!`, 전투·기절 등 상태별 막힘(상태가 없음).

## Global Constraints

모든 태스크의 요구 사항에 이 절과 `## 제안 결정 목록` 이 포함된다. 태스크를 받은 구현자는 이 두 절을 먼저 읽는다.

**버전 (추측 금지)** — 1차 계획 `docs/impl/iteration-1/plan.md` 의 `## Global Constraints` 버전 표를 그대로 쓴다. 이 이터레이션은 **새 라이브러리·플러그인·이미지를 넣지 않는다.** `build.gradle.kts`, `package.json`, `package-lock.json`, Dockerfile, `compose.yaml` 의 버전 줄을 바꾸지 않는다. 꼭 필요해 보이면 멈추고 리드에게 보고한다(리드가 공식 출처에서 최신 안정판을 확인하고 이 계획에 근거를 적은 뒤에만 넣는다).

**로컬 환경 (1·2차와 같음)**

- macOS arm64, 기본 JDK Temurin 25. world-server는 항상 `./gradlew`.
- 프론트 태스크는 Node 26.13.1: 명령 앞에 `source "$NVM_DIR/nvm.sh" && nvm use` (모듈의 `.nvmrc`).
- 디스크: 지금 남은 공간 약 55GB. 그래도 Docker 빌드·실행은 **Task 14에서 한 번만** 한다. Task 1~13은 Docker 명령(`docker build`, `docker compose build/up`)을 쓰지 않는다. `ENOSPC`(No space left on device)가 나면 아무것도 지우지 말고 멈춰 리드에게 보고한다.

**저장소·규칙**

- 모듈·루트 파일 구성은 1·2차와 같다(`world-server/`, `play-client/`, `admin-console/`, 루트 `compose.yaml`·`.env.example`·`README.md`·`scripts/`).
- world-server 코딩 규칙은 `.claude/rules/*.md` 를 따른다. 루트 패키지는 `mudmk3`. 아래 경로는 `world-server/src/{main,test}/java/mudmk3/` 기준이다.
- open-in-view가 꺼져 있다. REST 어댑터는 트랜잭션 밖에서 DTO를 만들므로, 응답에 쓰는 지연 연관(`FloorPlacement.itemType`, `InventoryEntry.itemType` 등)은 리포지토리 `@EntityGraph` 로 미리 읽거나, 요소 컬렉션의 id(`ShopItem.itemTypeId`, `StartingItem.itemTypeId`)는 어댑터가 `ItemTypeFinder` 로 이름을 찾는다(2차 `NpcApi` 관례).
- Next.js 앱 규칙은 1·2차와 같다(정적 export, `"use client"`, 상대 경로 import, 쿼리스트링 id).
- 화면 문구·서버 메시지·예외 메시지는 한국어. **룰북 문장을 그대로 쓰고, 룰북에 없는 문구는 이 계획에 적힌 문장을 글자 그대로 쓴다.** 조사(은/는, 을/를, 이/가, 과/와)는 `Josa` 로 앞 낱말 받침에 맞춘다. 끝의 닫는 괄호·문장 부호는 건너뛰고 본 끝 글자가 한글이면 그 받침으로, 숫자·영문이면 받침 없음으로 본다(`회춘단(연기 초)는`, `비검과`, `회춘단123456는`). 수는 1,000 이상이면 쉼표를 찍는다(`Numbers.count`, 예 `9,880`).
- 계획 문구 안의 `{이름}`·`{n}` 같은 중괄호는 채워 넣는 자리이고, `{이름은}` 처럼 조사가 붙은 자리는 `Josa` 로 고른 조사를 붙인다는 뜻이다.
- 자동 테스트는 Google 자격 증명·네트워크 없이 돈다. 시간에 걸린 테스트는 실제로 기다리지 않고 테스트 시계(`MutableClock`, Task 1)를 움직인다.

**커밋**

- 태스크 하나 = 커밋 하나 이상. 구현자는 커밋만 하고 **push 하지 않는다**(리드가 한다). 구현자는 서브에이전트를 띄우지 않는다.
- 커밋 메시지: `<type>(<module>): <한국어 요약>` (type: feat, test, build, docs, chore, fix / module: world-server, play-client, admin-console, infra, spec).
- 커밋 메시지 끝에 빈 줄 다음 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` 를 붙인다.
- 태스크를 끝내면 `docs/impl/iteration-3/progress.md` 의 표에 한 줄(태스크 번호·제목, 커밋 해시는 리드가 채움, 메모)을 더하고 같은 커밋에 넣는다. 각 태스크 끝의 **progress.md 한 줄** 이 그 메모의 틀이다(실제로 바꾼 것·계획과 다른 점을 덧붙인다).

**검증 명령 (각 태스크 완료 조건에 포함)**

- world-server: `cd world-server && ./gradlew build` (테스트 + SpotBugs + ArchUnit 통과)
- play-client / admin-console: `cd <module> && source "$NVM_DIR/nvm.sh" && nvm use && npm ci && npm test && npm run build`

## 제안 결정 목록

스펙·룰북이 정하지 않아 이 계획에서 정한 것. 번호는 2차(D19~D31)에 이어 붙인다. 각 결정 끝 `근거:` 에 룰북 절이나 이유를 적는다. Task 12가 `docs/spec/system/` 해당 문서에 `(제안)` 으로 옮긴다. 열린 질문과의 대응은 D32~D56 아래 [열린 질문 대응표](#3차-열린-질문-대응표)에 있다.

**D32. 아이템 종류 모델** — 도메인 `ItemType`(슬라이스 `item`). 항목:
- 이름: 앞뒤 공백을 자르고 여러 공백은 하나로 본 뒤 1~20자, 띄어쓰기 허용(`늑대 가죽`), **유일(영문 대소문자 무시)**. 금지: 영석 등급 낱말 그대로(`하품` `중품` `상품` `극품`), 마지막 낱말이 수(`^\d+$`)이거나 `전부`, `^\d+\.` 으로 시작. 근거: 명령 문법(commands.md 입력 문법 — 마지막 낱말이 수면 수량, `전부`, 번호 `2.가죽`, `버리기 하품 10`)이 이름을 헷갈리지 않게.
- 묘사 0~500자(null → `""`), 종류(D33), 기준가 0~100,000,000(하품 기준, item.md 공통 속성), 거래 가능(기본 켬), 저물대 등급(종류가 저물대일 때만 필수: 하급 20칸·중급 40칸·상급 80칸, item.md 저물대 표).
- 겹침은 입력받지 않고 종류로 정한다(D33). 품질·필요 경지는 3차에 없다(D34).
- 근거: item.md `## 아이템 공통 속성` 표에서 3차에 쓰이는 속성만 둔다.

**D33. 종류(카테고리) 표** — enum `ItemCategory`. 3차에 종류가 가르는 것은 겹침·매입 비율·직접 귀속 가능·칸 차지뿐이다(쓰는 효과가 없음). 선언 순서 = 목록 순서 = item.md 종류 순서.

| 코드 | 이름 | 겹침 | 직접 귀속 | 매입 비율 | 관리자가 만듦 |
|---|---|---|---|---|---|
| `ARTIFACT` | 법기 | 1 | ○ | 25% | ○ |
| `PILL` | 단약 | 99 | | 25% | ○ |
| `ELIXIR` | 영약 | 99 | | 25% | ○ |
| `TALISMAN` | 부적 | 99 | | 25% | ○ |
| `FORMATION_DISK` | 진법원반 | 1 | ○ | 25% | ○ |
| `SPIRIT_STONE` | 영석 | 10,000 | | 사지 않음 | ✕ (엔진, D35) |
| `MATERIAL` | 재료 | 99 | | 40% | ○ |
| `SCROLL` | 옥간·서책 | 99 | | 25% | ○ |
| `TOOL` | 제작 도구 | 1 | ○ | 25% | ○ |
| `BAG` | 저물대 | 칸 차지 안 함 | ○ | 25% | ○ |
| `BEAST_EGG` | 영수 알·새끼 | 1 | | 사지 않음 | ○ |
| `BEAST_FOOD` | 영수 먹이 | 99 | | 25% | ○ |
| `PUPPET` | 괴뢰 | 1 | | 25% | ○ |
| `PASS` | 출입권 | 99 | | 25% | ○ |
| `QUEST` | 퀘스트 물건 | 1 | | 사지 않음 | ○ |

- 부적은 3차에 품질·횟수가 없어 모두 '횟수 1회인 부적'(겹침 99)으로 본다. 영수 알·새끼는 매입 비율 표에 없고 영수점이 '받지 않음' 이라 사지 않는다.
- **저물탁은 3차에 없다.** 필요 경지(축기 초기, 의식으로 여닫음)를 지킬 경지·의식이 없기 때문이다. 경지가 생기는 4차에 더한다.
- 근거: item.md 겹침 규칙 표, 매입 비율 표, 귀속(직접 귀속 종류), 저물대 · 저물탁 표, `## 그 밖의 물건`(영수 먹이·출입권 겹침 99).

**D34. 품질·필요 경지 없음** — 3차 물건은 모두 하계로 본다(이름 앞에 품질을 붙이지 않음, 판매가 배율 ×1, 매입 배율 ×1.0). 필요 경지 칸도 두지 않는다. 근거: 품질이 있는 물건을 얻는 길(제작·드롭·비경·거래)이 3차에 없고 상점은 하계를 판다(item.md 품질 — "상점이 파는 물건은 하계", "품질을 따로 적지 않은 물건은 하계"). 필요 경지는 경지가 모자라도 가지고 다니고 거래할 수 있어(item.md 공통 속성) 3차 동작에 영향이 없다. 둘 다 경지·제작이 생길 때 더한다.

**D35. 영석 — 엔진이 심는 아이템 종류 4개** — 영석은 별도 잔액이 아니라 **소지품 칸을 차지하는 일반 물건**이다(item.md 영석: "영석은 일반 소지품이다. 저물대·저물탁 칸을 차지하고", 같은 등급끼리 10,000개). 그래서 바닥·소지품·주기·버리기를 다른 물건과 같은 코드로 다룬다.
- 서버가 뜰 때(`ApplicationReadyEvent`) 없으면 만든다: `하품 영석`(기준가 1), `중품 영석`(100), `상품 영석`(10,000), `극품 영석`(1,000,000). 종류 `SPIRIT_STONE`, 거래 가능, 열 `spirit_stone_grade`(유일)로 등급을 안다. 묘사(글자 그대로): 하품 `영기가 옅게 서린 돌. 수선자들이 값을 치르는 데 쓴다.` / 중품 `영기가 또렷하게 서린 돌. 하품 영석 100개의 값이다.` / 상품 `영기가 짙게 서린 돌. 하품 영석 10,000개의 값이다.` / 극품 `영기가 넘실대는 돌. 하품 영석 1,000,000개의 값이다.`
- 관리 툴로 만들거나 고치거나 지울 수 없다(409 `영석은 엔진이 정한 물건이라 고치거나 지울 수 없습니다.`, 종류 `SPIRIT_STONE` 으로 만들기 400 `영석 종류는 만들 수 없습니다.`). 바닥 배치·시작 소지품에는 쓸 수 있다. 상점은 팔지도 사지도 않는다.
- 4등급을 모두 둔다(item.md 영석 등급, 결제). 등급끼리 저절로 바뀌지 않는다.
- 명령에서 `하품`·`중품`·`상품`·`극품` 낱말 하나는 그 영석의 전체 이름으로 본다(`버리기 하품 10`, `주기 한립 하품 100` — item.md 줍기와 버리기·직접 거래).

**D36. 결제** (item.md `## 결제` 그대로) — 순수 함수 `SpiritStonePayment.plan(held, price)`:
1. 하품 → 중품 → 상품 → 극품 순으로, 그 등급으로 남은 값을 다 낼 수 있으면 필요한 만큼만(올림) 내고 끝낸다. 2. 다 내도 모자라면 그 등급을 모두 내고 다음 등급으로. 3. 낸 값이 가격보다 많으면 남은 값을 하품으로 거슬러 받는다. 등급 가치는 하품 1·중품 100·상품 10,000·극품 1,000,000. 하품 기준 합이 가격보다 적으면 결제하지 않는다.
- **상위 등급 깨기 확인을 한다**: 결제에 상품·극품이 들어가면 사기 전에 묻고 `확인` 을 10초 안에 입력해야 산다(D42). 중품은 묻지 않는다.
- **거스름 넘침을 검사한다**: 거스름 하품이 칸에 다 들어가지 않으면 결제하지 않는다.
- 3차에 결제를 쓰는 곳은 `사기` 뿐이다.

**D37. 소지품** — 도메인 `Inventory`(슬라이스 `inventory`, 캐릭터마다 하나, `character_id` 유일).
- 칸 = 기본 2칸 + 가진 저물대 칸의 합. 저물대는 3개까지 가질 수 있고 칸을 차지하지 않는다. 그 밖의 물건은 `(종류, 귀속)` 묶음마다 `올림(수 ÷ 겹침)` 칸. 근거: item.md 저물대 · 저물탁("기본 2칸", "모두 차고 있는 것으로 본다", "3개까지", "덜 찬 칸부터 채운다" — 묶음 수로 세면 덜 찬 칸부터 채운 것과 같다).
- 칸을 바꾸는 경우(item.md 칸이 모자랄 때): 줍기는 들어가는 만큼만, 사기·받기는 다 들어가지 않으면 하지 않는다. 저물대를 버리거나 팔거나 줘서 칸 합이 쓰는 칸보다 작아지면 하지 않는다. 저물대가 이미 3개면 더 얻지 못한다.
- 개체마다 다른 값은 3차에 **귀속 여부뿐**이다. 같은 종류라도 귀속된 것과 안 된 것은 다른 묶음(다른 줄)이다. 법기 내구도·숙련도는 법기 사용이 생길 때 더한다.
- **저장**: 소지품이 바뀌는 명령의 트랜잭션에서 바로 DB에 쓴다(1차 '캐릭터 위치는 이동할 때마다' 와 같은 방식). 메모리에 두지 않는다. 근거: platform.md 월드 상태는 '중요한 시점에 DB에 저장' 하고, 물건이 오가는 것은 두 쪽이 함께 바뀌어야 하는 중요한 시점이다(D53).
- 2차 이전에 만든 캐릭터는 소지품이 없으므로 처음 볼 때 빈 소지품(기본 2칸)으로 시작한다(시작 소지품은 주지 않는다).
- 표시 순서(소지품 목록·이름 찾기): 영석(하품 → 극품) 다음 나머지를 얻은 순서(묶음이 처음 생긴 순서)로.

**D38. 시작 소지품** — `WorldSettings` 에 `startingItems: [{itemTypeId, count}]`(입력 순서, 30줄까지). 관리 툴에서 통째로 바꾼다. 새 캐릭터를 만드는 트랜잭션에서 그 순서대로 소지품에 넣는다.
- 저장할 때 검사: 같은 물건 두 번 400 `같은 물건이 두 번 있습니다: 회춘단`, 수 1~999,999, 저물대 4개 이상 400 `저물대는 세 개까지만 찰 수 있습니다.`, 칸 넘침 400 `시작 소지품이 칸을 넘습니다. (필요 25칸, 칸 22)`, 없는 물건 400 `물건 종류를 찾을 수 없습니다. id: 9`.
- 룰북의 시작 소지품(하급 저물대 1, 영수대 1, 하품 영석 30, 회춘단(연기 초) 3, 회기단(연기 초) 3)은 관리자가 넣는 콘텐츠다. **영수대는 3차에 넣지 않는다**(영수 시스템이 없고 종류 표에도 없다 — 영수가 생길 때 더한다). README 시나리오는 나머지 넷을 넣는다.
- 바꾼 시작 소지품은 그 뒤에 만드는 캐릭터부터 받는다. 근거: item.md 시작 소지품, admin-tool.md 3차 범위.

**D39. 바닥 물건** — 슬라이스 `floor`. 둘 다 PostgreSQL에 둔다(서버를 다시 시작해도 남음).
- **관리자 배치** `FloorPlacement`: 방(`room_id`, FK 없음 — NPC와 같은 까닭), 물건 종류, 수(1~9,999), 재등장(분, 1~1,440 또는 없음), 남은 수, 비워진 시각. 모두가 함께 쓰는 하나다(캐릭터마다가 아님). 줍는 만큼 남은 수가 줄고, 0이 된 시각부터 재등장 분이 지나면 처음 수로 다시 놓인다. 재등장이 없으면 한 번뿐이다(관리자가 다시 고치면 처음 수로 돌아간다). 근거: 룰북은 버린 물건만 정했다. 모두 한 번인 MUD 관례에 재등장을 고를 수 있게 했고, 유니크 NPC 재등장 기본 10분(npc.md)을 관리 화면 기본값으로 쓴다.
- **버린 물건** `DroppedItem`: 방, 물건 종류, 수, 사라지는 시각(버린 시각 + 5분). 그동안 누구든 줍는다(item.md 줍기와 버리기). 같은 물건을 또 버리면 따로 5분을 잰다.
- **시간 재기(서버 틱 없음)**: 바닥을 읽을 때(방 보기·줍기·보기) 그 방의 사라질 시각이 지난 버린 물건을 지우고, 재등장할 때가 된 배치를 채운다. 그래서 예약 작업·스케줄러를 두지 않는다. 아무도 다시 보지 않는 방의 지난 행은 남지만 보이지 않는다. 시각은 주입한 `Clock`(D55)으로 잰다. 근거: platform.md 3차 월드 상태 열린 질문, 2차 '서버 틱 없음' 을 지킨다.
- 방을 지우면 그 방의 배치·버린 물건도 같은 트랜잭션에서 지운다(2차 `RoomDeletedEvent` 동기 리스너 관례).
- 귀속 물건·퀘스트 물건은 버릴 수 없으므로 버린 물건은 늘 귀속 아님이다. 관리자는 퀘스트 물건·영석도 바닥에 둘 수 있다.

**D40. 방 보기의 바닥 물건** — `room` 메시지에 `items: [{name, count}]` 를 더한다. 대상 목록 맨 끝(룰북 world.md 방 보기 — 떨어진 물건). 같은 종류는 배치·버린 것을 합쳐 한 줄로 묶고, 순서는 그 종류가 처음 나온 자리(배치 id 순 → 버린 것 id 순). 그리는 모양은 수가 2 이상이면 `하품 영석 3개`, 1이면 `연기기 비검`(룰북 예 그대로). 바닥이 바뀌어도 같은 방 사람의 방 보기를 다시 보내지 않는다(D47 알림만).

**D41. 상점 역할 데이터** — `Npc` 에 상점 역할(없거나 하나)을 붙인다: 상점 종류 + 파는 물건 목록 `[{itemTypeId, price}]`(입력 순서, 50개까지).
- **상점 종류는 엔진 enum** `ShopKind`(룰북 game-structure.md 상점 표를 그대로 옮김). 사 주는 것은 NPC마다 고르지 않고 종류가 정한다. 파는 물건도 그 종류의 '파는 것' 종류만 둘 수 있다(400 `{상점 종류는} {종류를} 팔 수 없습니다.`, 예 `단약방은 법기를 팔 수 없습니다.`).

| 코드 | 이름 | 파는 것(종류) | 사 주는 것(종류) |
|---|---|---|---|
| `PILL_SHOP` | 단약방 | 단약 | 단약·영약 |
| `ARTIFACT_SHOP` | 법기점 | 법기·괴뢰·제작 도구 | 법기·괴뢰·제작 도구 |
| `BOOK_SHOP` | 서방 | 옥간·서책 | 옥간·서책 |
| `TALISMAN_SHOP` | 부적방 | 부적 | 부적·진법원반 |
| `MATERIAL_SHOP` | 재료상 | 재료 | 재료 |
| `GENERAL_STORE` | 잡화점 | 저물대·영수 먹이·출입권 | 저물대·영수 먹이·출입권 |
| `BEAST_SHOP` | 영수점 | 영수 알·새끼·단약 | 없음 |

- **값**: 파는 물건마다 `price`(1~100,000,000) 또는 `null`. `null` 이면 룰북 계산값 판매가 = 기준가 × 하계 판매가 배율 1 을 쓰고 기준가가 바뀌면 따라간다. 숫자를 넣으면 그 값으로 고정한다. 관리 화면은 빈칸(= 계산값)을 기본으로 보이고 자리 표시로 `기준가 3` 을 보인다.
- **매입가는 입력받지 않는다**: 룰북 식 하계 기준가 × 매입 비율(D33) × 1.0.
- **불변식 검사**(item.md 불변식 1, 매입가 < 판매가 × 0.5): 파는 물건을 저장할 때, 그리고 그 물건의 기준가를 바꿀 때 검사한다. 정수로 `2 × 기준가 × 매입 비율(%) < 100 × 판매가`. 어기면 400 `판매가는 매입가의 2배보다 커야 합니다: 회춘단 (매입가 0.75, 판매가 1)`(상점 저장) / 409 `상점 값이 매입가 규칙에 맞지 않게 됩니다: 회춘단 (왕씨 1)`(기준가 변경). 기준가 0 인 물건을 `null` 값으로 두면 400 `기준가가 0인 물건은 값을 정해야 합니다: 회춘단` / 409 `기준가가 0이면 상점이 값을 정해야 합니다: 회춘단 (왕씨)`. 불변식 2(제작물)는 제작이 없어 검사하지 않는다.
- 영석은 둘 수 없다(400 `영석은 상점에서 팔 수 없습니다.`), 같은 물건 두 번 400 `같은 물건이 두 번 있습니다: 회춘단`, 51개 이상 400 `상점에 둘 수 있는 물건은 50개까지입니다.`.
- 재고는 무한, 가격은 바뀌지 않는다, 판 물건은 사라진다, 매입 수량 한도 없음(룰북 그대로). 상점 이름은 `{NPC 이름} {상점 종류}`(룰북 목록 머리 `[왕씨 단약방]`).
- 상점 역할은 배치와 상관없이 정할 수 있고, 게임에서는 그 NPC가 있는 방에서만 쓴다.

**D42. 상점 명령** (룰북 commands.md NPC · 상점 · 거래, npc.md 역할별 이용 — 문구 그대로)
- **상점 고르기**: 같은 방의 상점 NPC(배치 순 = id 순). 없으면 `이곳에서는 할 수 없습니다.`(공통 오류). 하나면 NPC 이름을 빼고, 인자 첫 낱말이 그 NPC 이름 전체와 같으면 떼어 낸다. 둘 이상이면 첫 낱말이 상점 NPC 이름(전체 또는 앞부분, 2차 `대화` 규칙)이어야 하고, 아니면 `상인이 여럿입니다. 상인 이름을 붙이십시오: 왕씨 · 이씨`.
- `목록 [NPC]`:
  ```
  [왕씨 단약방]  파는 것  값(하품)
    1. 회춘단(연기 초)  3
    2. 회기단(연기 초)  4
  사 주는 것: 단약·영약
  ```
  파는 물건이 없으면 둘째 줄 `  파는 물건이 없습니다.`, 사 주는 것이 없으면 `사 주는 것: 없음`. 룰북 예의 `(기혈 회복)` 효과 설명과 칸 맞춤 공백은 효과가 없는 3차에 빼고 두 칸 띄운다.
- `사기 [NPC] <물건> [수량]`(수량 빼면 1, `전부` 는 수량 오류): 상점의 파는 물건에서 이름으로 찾는다. 성공 `회춘단(연기 초) 5개를 샀습니다. (하품 영석 -15)`. 실패 순서: 인자 없음 `무엇을 사시겠습니까? (예: 사기 회춘단 5)` → 수량 오류 → 없는 물건 `왕씨: "그런 물건은 팔지 않소."` → `영석이 모자랍니다. (필요 15, 가진 것 9)`(가진 것 = 하품 기준 합) → `저물대는 세 개까지만 찰 수 있습니다.` → `저물대에 자리가 없습니다.` → `거스름을 받을 자리가 없습니다. (하품 9,880개, 1칸 필요)` → 상위 등급 확인 `값을 치르려면 상품 영석 1개를 깨야 합니다. 거스름 하품 9,880개를 받습니다. 계속하려면 '확인'.`(깨는 영석이 둘 이상 등급이면 `상품 영석 1개, 극품 영석 1개` 처럼 `, ` 로 잇고, 거스름이 0이면 가운데 문장을 뺀다).
- `확인`: 10초 안이면 같은 상점·같은 물건·같은 수로 지금 값을 다시 계산해 확인 없이 산다(그 사이 바뀐 것은 다시 검사해 해당 실패 문구). 확인 대기는 하나만, `확인`·`취소` 가 아닌 명령이 오면 조용히 취소된다. 10초가 지났거나 대기가 없으면 `확인할 것이 없습니다.`. `취소` 는 `그만두었습니다.`(룰북), 대기가 없으면 `그만둘 것이 없습니다.`. 근거: commands.md 확인이 필요한 명령(10초, 다른 명령이면 취소, `취소`, `그만두었습니다.`).
- `팔기 [NPC] <물건> [수량]`(빼면 1, `전부` 는 가진 것 모두): 내 소지품에서 찾는다. 성공 `늑대 가죽 10개를 팔았습니다. (하품 영석 +8)`. 실패 순서: 인자 없음 `무엇을 파시겠습니까? (예: 팔기 늑대 가죽 3)` → 수량 오류 → `그런 물건은 가지고 있지 않습니다.` → 영석 `왕씨: "그런 물건은 받지 않소."` → 퀘스트 물건 `왕씨: "그건 내가 살 물건이 아니오."` → 귀속 `왕씨: "귀속된 물건은 사 줄 수 없소."` → 종류가 사 주는 것에 없음 `왕씨: "그런 물건은 받지 않소."` 와, 그 종류를 사 주는 상점 종류가 있으면(표 순서 첫째) 뒤에 ` (재료는 재료상에 팔 수 있습니다)` → `늑대 가죽은 2개뿐입니다.` → 매입가 합계(내림) 1 미만 `왕씨: "값이 너무 적어 사 줄 수가 없소. 더 모아 오시오."` → 저물대를 팔아 칸이 모자라게 됨 `저물대에 든 물건을 먼저 비우십시오.` → 받을 하품이 들어갈 칸 없음 `저물대에 자리가 없습니다.`. 상점이 주는 영석은 하품뿐(item.md 얻는 영석의 등급).
- `값 [NPC] <물건> [수량]`: `늑대 가죽 1개에 하품 영석 0.8 — 10개면 8`. 개당 값은 소수 끝 0을 지운 값(`0.75`, `10`), 뒤의 수는 적은 수량, 없으면 가진 수. 실패는 `팔기` 와 같은 순서·문구(인자 없음은 `무엇의 값을 보시겠습니까? (예: 값 늑대 가죽)`, 수 넘침 검사와 1 미만 검사는 하지 않는다). `값` 을 3차에 넣는다 — 룰북 역할별 이용에 있고 새 규칙이 없다.
- 명령은 묻는 사람에게만 답한다(같은 방 알림 없음).
- `{NPC}:` 자리는 그 상점 NPC 이름이다(룰북 `상인:`·`왕씨:` 자리).

**D43. 귀속** — `귀속 <물건>` / `귀속 해제 <물건>`.
- **3차는 비용 없이 귀속한다.** 룰북은 정혈 최대치의 10%를 쓰지만 3차에는 정혈 스탯이 없다. 정혈 소모(와 `정혈이 모자랍니다.` 거절)는 스탯이 생기는 이터레이션에서 붙인다. 리드 방향과 같고 근거(item.md 귀속 — 정혈은 stats.md 스탯)도 맞다.
- 대상: 직접 귀속 종류(법기·진법원반·저물대·제작 도구, D33)의 귀속 안 된 것 하나. 귀속된 것은 주기·팔기·버리기 금지(item.md 귀속 그대로).
- 3차에 귀속되는 경우는 `귀속` 명령뿐이다. 처음부터 귀속된 물건(퀘스트 물건·퀘스트 보상·본명법보)은 퀘스트·법보가 없어 만들지 않는다. 퀘스트 물건 종류는 귀속처럼 막히되 문구가 다르다(item.md).
- 문구: 성공 `비검에 정혈을 한 방울 떨어뜨린다. 비검이 당신에게 귀속되었습니다.`(룰북 문장에서 정혈 수치 `(정혈 -2)` 만 뺌) / 해제 `비검과 이어진 정혈의 끈을 끊었습니다. 귀속이 풀렸습니다.`(룰북) / 퀘스트 물건 해제 `퀘스트 물건과 본명법보는 이렇게 풀 수 없습니다.`(룰북) / 인자 없음 `무엇을 귀속하시겠습니까? (예: 귀속 비검)`, `무엇의 귀속을 푸시겠습니까? (예: 귀속 해제 비검)` / 없음 `그런 물건은 가지고 있지 않습니다.` / 귀속할 수 없는 종류 `회춘단은 귀속할 수 없습니다.` / 귀속 안 된 것이 없음 `이미 귀속된 물건입니다.` / 귀속된 것이 없음 `귀속된 물건이 아닙니다.`

**D44. 주기** — `주기 <대상> <물건> [수량]`(영석은 `주기 한립 하품 100`, item.md 직접 거래).
- 대상: 같은 방에 접속해 있는 다른 플레이어(들어온 순서). 이름 전체가 같으면 그 사람, 없으면 이름이 입력으로 시작하는 첫 사람. 없으면(접속 안 함·다른 방·NPC·생성 중) `여기에 한립은 없습니다.`(룰북, 입력 그대로 + `은/는`). 내 이름이면 `자기 자신에게는 줄 수 없습니다.`.
- 받는 쪽 칸이 모자라면 넘어가지 않는다(item.md). 수수료 없음, 성 밖에서도 된다.
- 문구: 주는 쪽 `한립에게 회춘단 2개를 주었습니다.` / 받는 쪽 `여소이가 회춘단 2개를 건넸습니다.` / `한립의 저물대에 자리가 없습니다.` / `여기에 한립은 없습니다.` / `귀속된 물건은 남에게 줄 수 없습니다.` / `퀘스트 물건은 남에게 줄 수 없습니다.`(이상 룰북). 새 문구: 인자 없음 `누구에게 무엇을 주시겠습니까? (예: 주기 한립 회춘단 2)`, 거래 불가 `회춘단은 남에게 줄 수 없는 물건입니다.`, 받는 쪽 저물대 3개 `한립은 저물대를 더 찰 수 없습니다.`, 주는 쪽이 저물대를 줘 칸이 모자라게 됨 `저물대에 든 물건을 먼저 비우십시오.`.

**D45. 줍기·버리기·소지품·물건 보기**
- `줍기 <물건> [수량]`(빼면 1, `전부` 는 바닥의 그 물건 모두): 바닥(D40 묶음)에서 찾는다. 성공 `늑대 가죽 3개를 주웠습니다.` / 일부 `저물대에 자리가 없어 늑대 가죽 3개 중 1개만 주웠습니다.`(룰북) / 하나도 못 넣음 `저물대에 자리가 없습니다.`(저물대 3개 때문이면 `저물대는 세 개까지만 찰 수 있습니다.`) / 인자 없음 `무엇을 주우시겠습니까? (예: 줍기 늑대 가죽)`(룰북) / 없음 `이곳에 '가죽'은 없습니다.`(룰북 공통 오류 '방에 대상 없음', 입력 그대로) / 수 넘침 `늑대 가죽은 2개뿐입니다.`. 바닥에서는 버린 것(오래된 것부터) 다음 배치(id 순)에서 덜어 낸다.
- `버리기 <물건> [수량]`(빼면 1, `전부` 는 가진 것 모두): 성공 `늑대 이빨 4개를 버렸습니다.` / `그런 물건은 가지고 있지 않습니다.` / `늑대 이빨은 2개뿐입니다.` / `퀘스트 물건은 버릴 수 없습니다.` / `귀속된 물건은 버릴 수 없습니다.`(이상 룰북) / 인자 없음 `무엇을 버리시겠습니까? (예: 버리기 늑대 가죽)` / 저물대를 버려 칸이 모자라게 됨 `저물대에 든 물건을 먼저 비우십시오.`.
- 수량 오류(0, 10자리 이상): `수량은 1 이상의 수로 적으십시오.`(룰북 공통 오류).
- `소지품`(`ㅈ`): 룰북 형식.
  ```
  [소지품 3/22칸]  하품 영석 30
  하급 저물대   회춘단(연기 초) x3   회기단(연기 초) x3
  ```
  첫 줄 `[소지품 {쓴 칸}/{전체 칸}칸]`, 영석이 있으면 두 칸 띄우고 가진 등급만 `하품 영석 132 · 중품 영석 1`. 둘째 줄부터 영석 밖 물건을 표시 순서(D37)로 한 줄에 셋씩, 사이 공백 셋. 2개 이상이면 ` x{수}`, 귀속이면 ` (귀속)`, 퀘스트 물건이면 ` (퀘스트)`. 영석도 물건도 없으면 둘째 줄 `가진 물건이 없습니다.`. 룰북 예의 `(꺼냄)`·내구도는 3차에 없다.
- `보기 <물건>`: 바닥 → 내 소지품 순서로 찾는다(룰북 대상 지정 — 대상은 방에서, 물건은 소지품에서). 출력 `늑대 가죽 — 거칠고 질긴 늑대의 가죽이다.`(묘사가 비면 이름만). 물건이 없고 같은 방 플레이어·NPC 이름에 맞으면 2차 그대로 `아직 자세히 볼 수 없습니다.`(사람·NPC 자세히 보기는 3차 범위가 아님), 아무것도 아니면 `그런 것은 보이지 않습니다.`(룰북).

**D46. 물건 이름 찾기와 수량 문법** (commands.md 입력 문법·대상 지정)
- 인자 해석 `ItemArguments.parse`: 마지막 낱말이 `전부` 면 모두, 수(`^\d+$`)면 수량(1~999,999,999, 0이나 10자리 이상은 수량 오류), 나머지가 이름. 이름이 비면 인자 없음.
- 이름이 `N.이름`(N 1~999)이면 N번째 맞는 것. 이름이 `하품`·`중품`·`상품`·`극품` 하나면 그 영석의 전체 이름(D35).
- 맞는 때: 이름 전체, 이름의 앞부분, 이름을 이루는 낱말 하나의 앞부분(영문 대소문자 무시). 맞는 것이 여럿이면 번호가 없을 때 이름 전체가 같은 것을 먼저, 없으면 보이는 순서(바닥은 D40, 소지품은 D37, 상점은 목록 순서) 첫째. 번호는 전체 일치 우선 없이 보이는 순서로 센다. 근거: 룰북 그대로이고, 전체 일치 우선은 2차 `대화` 대상 찾기와 같게 했다.
- 버리기·주기·팔기·값에서 고른 묶음이 귀속이고 같은 종류의 귀속 안 된 묶음이 있으면 귀속 안 된 묶음을 쓴다(번호를 붙였으면 그대로). 귀속은 귀속 안 된 묶음을, 귀속 해제는 귀속된 묶음을 같은 방식으로 고른다.

**D47. 같은 방 알림** — 줍기·버리기·주기는 같은 방 다른 사람에게 알린다(새 `notice` tone `item`). 문구: `한립이 늑대 가죽 2개를 줍는다.` / `한립이 늑대 가죽 1개를 버린다.` / `여소이가 한립에게 회춘단 2개를 건넨다.`(주는 사람·받는 사람 빼고). 사기·팔기·값·목록·귀속·소지품·보기는 알리지 않는다. 근거: 바닥이 바뀐 것을 같은 방 사람이 알 수 있게(방 보기를 다시 보내지 않으므로, D40). 상점 거래는 룰북에서 상인과 나 사이의 일이다.

**D48. WebSocket 메시지 변경** (1·2차에 더함, 다른 메시지는 그대로)

| type | 필드 | 뜻 |
|---|---|---|
| `room` | 2차 필드 + `items`: `[{name, count}]`(D40 순서, 없으면 `[]`) — JSON 필드 순서 `zone, zoneEntered, room, owner, exits, occupants, items` | 방 보기 |
| `inventory` | `usedSlots`, `totalSlots`, `stones`: `[{name, count}]`(가진 등급만, 하품부터), `items`: `[{name, count, bound, quest}]`(영석 밖, 표시 순서), `text`(D45 그린 줄들, `\n`), `tone`: `"item"` | 소지품(묻는 사람에게만) |
| `system` | `tone` 에 `"item"` 을 더함 | 물건 명령의 결과(줍기·버리기·물건 보기·목록·사기·팔기·값·주기·받기·귀속·확인 질문). 거절은 그대로 `error` |
| `notice` | `tone`: `"movement"` \| `"item"`, `event` 에 `"pick"` \| `"drop"` \| `"give"` 를 더함 | D47 |

- 새 `tone` 값 `item`(platform.md 실시간 통신 — 강조는 의미 값, 예 `item`). 클라이언트 스타일 이름도 `item`(바닥 물건 줄 포함).

**D49. 관리 REST API 추가** (1차 D12 규칙 그대로: `/api/admin/**`, ADMIN, 변경에 `X-MUD-Client: 1`, `ProblemDetail`, 검증 실패·잘못된 값·없는 id 400, 이름 중복·규칙 충돌 409)

| 메서드 · 경로 | 요청 | 응답 |
|---|---|---|
| `GET /api/admin/item-types` | — | 200 `ItemTypeResponse[]`(영석 하품→극품, 다음 이름순) |
| `POST /api/admin/item-types` | `ItemTypeSaveRequest` | 201 `ItemTypeResponse` |
| `GET /api/admin/item-types/{itemTypeId}` | — | 200 `ItemTypeResponse` |
| `PUT /api/admin/item-types/{itemTypeId}` | `ItemTypeSaveRequest` | 200. 영석 409, 쓰이는 물건의 종류·저물대 등급 바꾸기 409, 상점 불변식 409 |
| `DELETE /api/admin/item-types/{itemTypeId}` | — | 204. 쓰이는 물건 409, 영석 409 |
| `POST /api/admin/rooms/{roomId}/floor-items` | `FloorPlacementSaveRequest` | 201 `FloorPlacementResponse` |
| `PUT /api/admin/floor-items/{floorItemId}` | `FloorPlacementSaveRequest` | 200 `FloorPlacementResponse`(남은 수를 처음 수로) |
| `DELETE /api/admin/floor-items/{floorItemId}` | — | 204 |
| `GET /api/admin/rooms/{roomId}` | — | 2차 + `floorItems: FloorPlacementResponse[]`(id 순) |
| `GET /api/admin/world/starting-items` | — | 200 `StartingItemResponse[]`(입력 순서) |
| `PUT /api/admin/world/starting-items` | `{items: [{itemTypeId, count}]}` | 200 `StartingItemResponse[]` |
| `PUT /api/admin/npcs/{npcId}/shop` | `ShopSaveRequest` | 200 `NpcDetailResponse` |
| `DELETE /api/admin/npcs/{npcId}/shop` | — | 204(상점이 없어도 204) |

- `ItemTypeSaveRequest {name, description, category, baseValue, tradable, bagGrade}`(`category`·`bagGrade` 는 enum 이름, `bagGrade` 는 저물대만, 나머지 null).
- `ItemTypeResponse {id, name, description, category, categoryName, stackSize, baseValue, tradable, bagGrade, bagGradeName, bagSlots, spiritStone, buyRatePercent}`(`stackSize` 는 저물대 0, `bagSlots` 는 저물대 밖 0, `buyRatePercent` 는 사지 않으면 0).
- `FloorPlacementSaveRequest {itemTypeId, count, respawnMinutes}`(`respawnMinutes` null = 한 번뿐). `FloorPlacementResponse {id, roomId, itemTypeId, itemName, count, respawnMinutes, remaining}`(`remaining` 은 지금 시각으로 계산한 남은 수, 저장하지 않음).
- `StartingItemResponse {itemTypeId, itemName, count}`.
- `ShopSaveRequest {kind, items: [{itemTypeId, price}]}`(`price` null = 계산값). `NpcDetailResponse` 에 `shop: {kind, kindName, items: [{itemTypeId, itemName, price, effectivePrice}]} | null`, `NpcSummaryResponse` 에 `shopKindName`(없으면 null).
- 예외 문구(위 D에 없는 것): `이미 있는 물건 이름입니다: 회춘단`(409), `물건 종류를 찾을 수 없습니다. id: 9`(400), `쓰이는 물건은 지울 수 없습니다: 회춘단`(409), `쓰이는 물건은 종류·저물대 등급을 바꿀 수 없습니다: 회춘단`(409), `바닥 물건을 찾을 수 없습니다. id: 9`(400), 잘못된 enum 값은 2차 공통 400. DB 제약 위반(드문 동시 변경)은 409 `다른 곳에서 함께 바꾸고 있어 저장하지 못했습니다. 다시 시도하십시오.`.

**D50. admin-console 화면 추가** (1·2차 화면에 더함)
- 위쪽 공통 바 링크: `NPC` 옆에 `아이템`, `시작 소지품`.
- `/items/` 아이템 목록(이름·종류·기준가·겹침(저물대는 `{n}칸`)·거래(`가능`/`불가`)) + `아이템 만들기` 링크(`/item/`).
- `/item/` (id 없음 = 새로, `/item/?id=7` = 고치기): `이름`, `종류`(영석 빼고), `저물대 등급`(종류가 저물대일 때만: `하급 (20칸)`·`중급 (40칸)`·`상급 (80칸)`), `묘사`(textarea), `기준가 (하품)`(기본 0), `거래 가능`(기본 켬), `저장`, 고칠 때 `삭제`(확인 `'회춘단' 아이템을 지울까요?`). 영석이면 모든 칸이 읽기 전용이고 `영석은 엔진이 정한 물건이라 고칠 수 없습니다.`. 만들기 성공 시 `/item/?id=<새 id>`.
- `/start-items/` 시작 소지품: 행마다 `물건`(선택)·`수`·`삭제`, `물건 추가`, `저장`, 안내 `새로 만드는 캐릭터부터 받습니다.`, 저장 성공 `저장했습니다.`, 빈 목록 `시작 소지품이 없습니다.`.
- `/room/?id=` 방 화면에 `바닥 물건` 절: 표(`물건`·`수`·`남은 수`·`재등장`(`10분` 또는 `한 번뿐`)·`수정`·`삭제`), 두기 폼(`물건`·`수`(기본 1)·`재등장(분)`(기본 10, 비우면 한 번뿐)·`바닥에 두기`), 없으면 `없음`.
- `/npc/?id=` 에 `상점` 절(고칠 때만): `상점 종류`(`상점 아님` + 7종), 파는 물건 표(`물건` — 그 종류가 파는 종류의 물건만, `값 (하품)` — 빈칸 자리 표시 `기준가 {n}`, `삭제`), `물건 추가`, `상점 저장`(`상점 아님` 이면 상점을 없앰), 안내 `값을 비워 두면 기준가로 팝니다. 사 주는 값은 룰북 매입 규칙으로 정해집니다.`. `/npcs/` 목록에 `상점` 열(종류 이름 또는 `-`).
- 저장 전 클라이언트 검사는 하지 않고 서버 `detail` 을 보인다(1·2차 관례).

**D51. play-client** (1차 D8·D18, 2차 D29에 더함)
- 방 그림: NPC 줄 다음에 바닥 물건 줄(D40 모양, style `item`). `inventory` 는 `text` 를 줄로 나눠 style `item`. `system`·`notice` 의 `tone: "item"` 도 style `item`. CSS `.line-item` 색 `#9fd4a3`.
- 자동완성 `COMMANDS` 순서: `보기 말하기 대화 소지품 줍기 버리기 주기 목록 사기 팔기 값 귀속 확인 취소 누구 도움말 종료` + 방위. 이름 후보는 마지막 `room` 의 occupants 이름 + 바닥 물건 이름. `notice` 의 `item` 이벤트(`pick`·`drop`·`give`)는 이름 후보를 바꾸지 않는다.

**D52. 슬라이스·의존·스키마** — 새 슬라이스(domain·application): `item`, `inventory`, `floor`. 의존: `inventory → item, world`, `floor → item, room`, `npc → item, room`, `world → item, room`, `character → inventory`(+2차), `game → inventory, floor, item, npc`(+2차). `item` 은 아무 슬라이스도 쓰지 않는다 — 쓰이는지 검사는 `ItemTypeRepository` 의 JPQL로 센다(2차 `FactionRepository.countZones` 관례). 순환 없음.
- 테이블(`ddl-auto=update` 로 생김, 마이그레이션 도구 없음): `item_type`(UK `UK_ITEM_TYPE_NAME`, `UK_ITEM_TYPE_SPIRIT_STONE_GRADE`), `inventory`(UK `UK_INVENTORY_CHARACTER_ID`), `inventory_entry`(FK `inventory_id`·`item_type_id`), `floor_placement`(FK `item_type_id`, `room_id` FK 없음), `dropped_item`(같음), `npc.shop_kind` 열, `npc_shop_item`(`position` 순서 열, `item_type_id` FK 없음), `world_starting_item`(`position` 순서 열, `item_type_id` FK 없음).
- `inventory_entry` 에 `(inventory_id, item_type_id, bound)` DB 유니크 제약은 두지 않는다. 묶음 유일은 도메인이 지킨다(결제에서 하품을 다 내고 거스름 하품을 받는 것처럼 한 트랜잭션에서 같은 묶음을 비웠다 채울 때 생기는 일시 충돌을 피함 — 2차 `npc_topic` 과 같은 까닭).

**D53. 동시성·트랜잭션** — 게임 명령은 1차대로 서버 전체 게임 락 하나로 직렬화되고, 락 안에서 명령마다 트랜잭션 하나를 열고 닫는다. 그래서:
- 같은 바닥 물건을 둘이 동시에 주워도 한 사람씩 처리되고, 뒤 사람은 앞 사람이 커밋한 남은 수를 읽는다(겹쳐 줍기 없음).
- 주기는 받는 사람이 그 순간 같은 방에 접속해 있는지(메모리 presence)와 두 소지품 변경을 한 명령 안에서 처리한다. 상대가 나가거나 끊는 처리도 같은 락을 지나므로 '주는 중에 나감' 이 끼어들 수 없다. 끊긴 뒤라면 `여기에 한립은 없습니다.` 이고 물건은 그대로다.
- 사기·팔기·주기·줍기·버리기는 검사를 다 한 뒤에 바꾼다(도메인 메서드가 실패하면 아무것도 바꾸지 않는다). 커밋이 실패하면 메시지를 보내지 않는다(1차 outbox).
- 관리 요청은 게임 락 밖이다. 관리자 배치 수정과 게임 줍기가 겹치면 나중 커밋이 이긴다(관리 수정은 남은 수를 처음 수로 되돌리는 동작이라 받아들임 — 2차 동시 편집 경합과 같은 판정). 쓰이는 아이템 종류 지우기 경합은 FK가 막고 409(D49).

**D54. 관리 변경 반영과 아이템 종류 바꾸기·지우기**
- 아이템 종류·바닥 배치·시작 소지품·상점 역할의 변경은 실행 중인 게임에 바로 반영된다(게임이 명령마다 DB에서 읽음). 바닥은 다음 방 보기부터, 시작 소지품은 그 뒤 만드는 캐릭터부터, 상점은 다음 상점 명령부터. 같은 방 사람에게 따로 알리지 않는다.
- 쓰이는 물건(소지품·바닥 배치·사라지지 않은 버린 물건·상점·시작 소지품 어디든)은 지울 수 없고(409), 종류·저물대 등급을 바꿀 수 없다(409 — 칸 계산이 바뀌어 소지품이 칸을 넘는 일을 막음). 이름·묘사·기준가·거래 가능은 바꿀 수 있다(기준가는 D41 불변식 검사). 지울 때 사라질 시각이 지난 버린 물건 행은 먼저 지운다.

**D55. 시계** — `support/config/ClockConfig` 의 `Clock` 빈(`Clock.systemUTC()`)을 시간이 필요한 서비스(바닥, 확인 대기, 아이템 종류 지우기)가 주입받는다. 테스트는 `@Primary` `MutableClock`(고정 시각에서 `advance(Duration)`)으로 바꾼다. 새 의존성 없음.

**D56. 도움말 문구**
`명령: 보기(ㅂ) · 동 서 남 북 북동 북서 남동 남서 위 아래 (e w s n ne nw se sw u d) · 말하기(') · 대화 <NPC> [낱말] · 소지품(ㅈ) · 보기 <물건> · 줍기 <물건> [수량] · 버리기 <물건> [수량] · 주기 <대상> <물건> [수량] · 목록 · 사기 <물건> [수량] · 팔기 <물건> [수량] · 값 <물건> · 귀속 <물건> · 귀속 해제 <물건> · 확인 · 취소 · 누구 · 도움말 · 종료`

**새 문구 모음** — 위 결정에서 룰북에 없는 문구(Task 12가 '룰북에 아직 없음' 으로 표시): `무엇을 버리시겠습니까? (예: 버리기 늑대 가죽)`, `무엇을 사시겠습니까? (예: 사기 회춘단 5)`, `무엇을 파시겠습니까? (예: 팔기 늑대 가죽 3)`, `무엇의 값을 보시겠습니까? (예: 값 늑대 가죽)`, `누구에게 무엇을 주시겠습니까? (예: 주기 한립 회춘단 2)`, `무엇을 귀속하시겠습니까? (예: 귀속 비검)`, `무엇의 귀속을 푸시겠습니까? (예: 귀속 해제 비검)`, `늑대 가죽 3개를 주웠습니다.`, `저물대는 세 개까지만 찰 수 있습니다.`, `저물대에 든 물건을 먼저 비우십시오.`, `가진 물건이 없습니다.`, `상인이 여럿입니다. 상인 이름을 붙이십시오: 왕씨 · 이씨`, `  파는 물건이 없습니다.`, `사 주는 것: 없음`, `확인할 것이 없습니다.`, `그만둘 것이 없습니다.`, `자기 자신에게는 줄 수 없습니다.`, `회춘단은 남에게 줄 수 없는 물건입니다.`, `한립은 저물대를 더 찰 수 없습니다.`, `회춘단은 귀속할 수 없습니다.`, `이미 귀속된 물건입니다.`, `귀속된 물건이 아닙니다.`, 알림 세 줄(D47), 영석 묘사 넷(D35).

### 3차 열린 질문 대응표

`docs/spec/system/index.md` `### 3차 이터레이션 열린 질문` 의 질문 순서대로. Task 12가 질문마다 `→ (제안)` 답으로 옮긴다.

| 질문 | 답 |
|---|---|
| 아이템 종류 항목·이름 유일 | D32 |
| 종류 목록·종류가 가르는 것 | D33 |
| 품질 | D34 (없음) |
| 필요 경지 | D34 (없음) |
| 쓰이는 아이템 종류 지우기 | D54 |
| 개체마다 다른 값 | D37 (귀속뿐) |
| 관리 변경 반영 | D54 |
| 영석 4등급 | D35 (모두) |
| 영석은 데이터인가 고정인가 | D35 (엔진이 심는 아이템 종류) |
| 깨기 확인·거스름 넘침 | D36·D42 (둘 다 함) |
| 상점 데이터 | D41 |
| 재고 | D41 (무한, 룰북) |
| 상점 종류 데이터 vs NPC마다 | D41 (엔진 enum) |
| `값 <물건>` | D42 (넣음) |
| 불변식 검사 | D41 (함) |
| 소지품 한도 | D37 |
| 저물대 크기·저물탁 | D32·D33 (등급 속성, 저물탁 없음) |
| 귀속 시점·경우 | D43 (`귀속` 명령뿐) |
| 귀속 소모 | D43 (비용 없음) |
| 영수대 | D38 (넣지 않음) |
| 배치 물건 재등장 | D39 |
| 5분을 무엇으로 | D39 (읽을 때 시각으로) |
| 다시 시작하면 남는가 | D39 (남음, DB) |
| 방 보기 표시·묶기 | D40 |
| `보기 <물건>` 찾는 순서 | D45 |
| 이름 찾기 범위 | D46 (앞부분·낱말 앞부분·번호·`전부` 모두) |
| 주기 받는 쪽 없음 | D44 |
| 오가면 알리는가 | D47 |
| (platform) 소지품 저장 시점 | D37 |
| (platform) 메시지 이름·tone | D48 |
| (platform) 두 쪽이 함께 바뀌는 명령 | D53 |

## Review Focus

스펙이 직접 말하지 않지만 실제로 부딪칠 만한 것. 각 줄의 테스트는 담당 태스크에 들어 있다.

1. 두 플레이어가 같은 바닥 물건을 동시에 `줍기 가죽 전부` → 둘이 주운 합이 바닥 수와 같고 바닥은 0, 음수·중복 없음. (Task 7 `GameConnectorConcurrencyTest.concurrentPickNeverDuplicates`)
2. 주기 직전에 받는 사람이 나가거나 끊음 / 다른 방 / 생성 중 / 내 이름 → 거절이고 주는 쪽 소지품 그대로. (Task 8 `giveToAbsentPlayerKeepsItem`, `giveToSelfFail`)
3. 하품을 모두 내고 거스름 하품을 받는 결제가 한 트랜잭션에서 flush 오류 없이 수가 맞음, 영석 0이 된 묶음은 사라짐. (Task 3 `InventoryRepositoryTest.payAllLowStonesThenReceiveChange`)
4. 저물대를 버리기·팔기·주기 해 칸이 쓰는 칸보다 작아짐 → 거절이고 아무것도 바뀌지 않음; 저물대 4번째 줍기·사기·받기 → 거절. (Task 3 `removeBagInUseFail`·`fourthBagFail`·`giveBagToFullReceiverFail`, Task 7 `dropBagInUseFail`·`pickFails`, Task 8 `giveFails`)
5. 쓰이는 아이템 종류 지우기·종류 바꾸기, 바닥 물건이 있는 방 지우기 → 409 또는 같이 정리되고 500이 나지 않음. (Task 3·4·5·6 `deleteItemTypeIn...Fail`, Task 5 `deleteRoomRemovesFloorItems`)
6. 버린 물건 5분·배치 재등장이 시계로만 바뀌고, 그 사이 같은 종류가 한 줄로 묶여 보임. (Task 5 `RoomFloorTest`, Task 7 `droppedItemDisappearsAfterFiveMinutes`)

---

## 파일 구조 (요약, 새로 생기거나 바뀌는 것)

```
world-server/src/main/java/mudmk3/
  domain/item/{ItemType,ItemCategory,BagGrade,SpiritStoneGrade,ItemTypeSaveInfo,DuplicateItemTypeNameException,ItemQuery,ItemNames,Slots,Pricing}.java   (새)
  domain/inventory/{Inventory,InventoryEntry,SpiritStonePayment,AddCheck,RemoveCheck,PurchaseResult,SaleResult,GiveResult}.java                         (새)
  domain/floor/{FloorPlacement,FloorPlacementSaveInfo,DroppedItem,FloorStack,FloorStacks}.java                                                          (새)
  domain/npc/{Npc,ShopKind,ShopItem,ShopOffer}.java                                                                                                     (상점)
  domain/world/{WorldSettings,StartingItem}.java                                                                                                        (시작 소지품)
  domain/game/{Command,CommandParser,Amount,ItemArguments}.java                                                                                         (물건 명령)
  domain/shared/{Josa,Numbers}.java
  application/item/..., application/inventory/..., application/floor/...                                                                               (새)
  application/npc/..., application/world/..., application/character/CharacterModifyService.java
  application/game/{GameService,GameSession,RoomViewAssembler,ItemCommandHandler,ShopCommandHandler}.java, required/GameMessage.java
  adapter/webapi/{ItemTypeApi,FloorItemApi,WorldApi,NpcApi,RoomApi,ApiControllerAdvice}.java, dto/...
  support/config/ClockConfig.java
  resources/META-INF/orm.xml
admin-console/  app/{items,item,start-items}/page.tsx, app/{room,npc,npcs}/page.tsx, src/lib/{api,items}.ts, src/components/{TopBar,ItemForm,StartItemsEditor,FloorItemsEditor,ShopEditor}.tsx
play-client/    src/lib/{protocol,render,complete}.ts, src/components/Terminal.tsx, app/globals.css
docs/spec/system/*.md  README.md  scripts/smoke-test.mjs
```

---

### Task 1: 아이템 종류 도메인·영속·관리 서비스, 영석 심기, 시계

**권장 모델:** opus

**목적:** D32·D33·D35·D46(이름 맞추기)·D55의 바탕을 만든다. REST는 Task 2.

**Files:**
- Create: `domain/item/{ItemType,ItemCategory,BagGrade,SpiritStoneGrade,ItemTypeSaveInfo,DuplicateItemTypeNameException,ItemQuery,ItemNames,Slots,Pricing}.java`, `domain/shared/Numbers.java`
- Modify: `domain/shared/Josa.java`, `src/main/resources/META-INF/orm.xml`
- Create: `application/item/required/ItemTypeRepository.java`, `application/item/provided/{ItemTypeFinder,ItemTypeEditor,ItemTypeSaveRequest,ItemTypeInUseException,SpiritStoneChangeException}.java`, `application/item/{ItemTypeQueryService,ItemTypeModifyService}.java`, `support/config/ClockConfig.java`
- Test: `domain/item/{ItemTypeTest,ItemTypeFixture,ItemCategoryTest,ItemQueryTest,ItemNamesTest,SlotsTest,PricingTest}`, `domain/shared/{JosaTest(추가),NumbersTest}`, `application/item/required/ItemTypeRepositoryTest`, `application/item/provided/ItemTypeEditorTest`, `MutableClock`(test `mudmk3` 패키지), `TestFakesConfiguration`(추가)

**Interfaces:**
- Produces:
  - `enum ItemCategory` — D33 표 순서·코드. `String koreanName()`, `int stackSize()`(BAG은 0), `boolean bindable()`, `int buyRatePercent()`(사지 않으면 0), `boolean creatableByAdmin()`(SPIRIT_STONE만 false).
  - `enum BagGrade { LOW("하급",20), MIDDLE("중급",40), HIGH("상급",80) }` — `koreanName()`, `slots()`.
  - `enum SpiritStoneGrade { LOW("하품",1), MIDDLE("중품",100), HIGH("상품",10_000), TOP("극품",1_000_000) }` — `koreanName()`, `long value()`, `String itemName()`(`"하품 영석"`), `static Optional<SpiritStoneGrade> fromWord(String)`(`하품` 등 낱말 하나).
  - `record ItemTypeSaveInfo(String name, @Nullable String description, ItemCategory category, long baseValue, boolean tradable, @Nullable BagGrade bagGrade)` — 검증 애노테이션 없음(도메인이 검사).
  - `ItemType`(`@Entity`, `AbstractEntity`): `static ItemType create(ItemTypeSaveInfo)`, `static ItemType spiritStone(SpiritStoneGrade)`, `void update(ItemTypeSaveInfo)`, getter `getName()`·`getDescription()`·`getCategory()`·`getBaseValue()`·`isTradable()`, `Optional<BagGrade> findBagGrade()`, `Optional<SpiritStoneGrade> findSpiritStoneGrade()`, `int stackSize()`, `int bagSlots()`, `boolean isBag()`, `boolean isSpiritStone()`, `boolean isQuest()`, `boolean isBindable()`, `boolean isCalled(String)`(대소문자 무시). 도메인 검사(`IllegalArgumentException`, 문구 글자 그대로): `물건 이름은 1~20자로 정하십시오: {이름}`, `영석 등급 이름은 물건 이름으로 쓸 수 없습니다: 하품`, `물건 이름의 마지막 낱말은 수나 '전부'일 수 없습니다: {이름}`, `물건 이름은 '숫자.' 로 시작할 수 없습니다: {이름}`, `묘사는 500자까지 쓸 수 있습니다.`, `영석 종류는 만들 수 없습니다.`, `기준가는 0 이상 100,000,000 이하로 정하십시오.`, `저물대 등급을 고르십시오.`, `저물대 등급은 저물대에만 정합니다.`. 영석에 `update` → `IllegalStateException`(서비스가 먼저 막음).
  - `record ItemQuery(String name, int ordinal)` — `static ItemQuery parse(String text)`: `^(\d{1,3})\.(.+)$` 이고 N≥1이면 `(나머지, N)`, 아니면 `(text, 0)`; 이름이 영석 등급 낱말 하나면 `SpiritStoneGrade.itemName()` 으로 바꾼다.
  - `final class ItemNames` — `static boolean matches(String itemName, String query)`(D46 세 경우, 대소문자 무시), `static <T> Optional<T> select(List<T> candidates, Function<T, String> nameOf, ItemQuery query)`(D46 고르기 규칙).
  - `final class Slots` — `BASE = 2`, `MAX_BAGS = 3`, `static int slotsFor(ItemType, long count)`(저물대 0, 나머지 올림), `static int capacity(int bagSlotSum)`.
  - `final class Pricing` — `static long defaultSellPrice(ItemType)`(= 기준가), `static BigDecimal unitBuyPrice(ItemType)`(기준가 × 비율 / 100), `static long buyTotal(ItemType, long count)`(내림), `static boolean isFairSellPrice(ItemType, long price)`(`2 × 기준가 × 비율 < 100 × price`, 비율 0이면 true).
  - `Josa`: `withEunNeun`, `withEulReul`, `withGwaWa`(+ 조사만 돌려주는 `eunNeun`·`eulReul`·`gwaWa`). 받침 판단은 Global Constraints 규칙(끝의 `)` 등 문장 부호를 건너뜀) — 기존 `iGa`·`withIGa` 도 같은 규칙으로 바꾼다(2차 테스트 그대로 통과해야 함). `Numbers`: `static String count(long)`(`%,d`, `Locale.ROOT`), `static String decimal(BigDecimal)`(끝 0 지움, 지수 표기 없음).
  - 매핑: 테이블 `item_type`(`name` varchar(20) not null `UK_ITEM_TYPE_NAME`, `description` varchar(500) not null, `category` varchar(32) STRING not null, `base_value` bigint not null, `tradable` boolean not null, `bag_grade` varchar(16) STRING null, `spirit_stone_grade` varchar(16) STRING null `UK_ITEM_TYPE_SPIRIT_STONE_GRADE`).
  - `ItemTypeRepository extends Repository<ItemType, Long>`: `save`, `findById`, `List<ItemType> findAll()`, `List<ItemType> findAllByIdIn(Collection<Long>)`, `Optional<ItemType> findBySpiritStoneGrade(SpiritStoneGrade)`, `boolean existsByNameIgnoreCase(String)`, `boolean existsByNameIgnoreCaseAndIdNot(String, Long)`, `void delete(ItemType)`. 쓰이는지 세는 JPQL은 Task 3~6이 하나씩 더한다.
  - `record ItemTypeSaveRequest(@NotBlank @Size(max=20) String name, @Nullable @Size(max=500) String description, @NotNull ItemCategory category, @Min(0) @Max(100_000_000) long baseValue, boolean tradable, @Nullable BagGrade bagGrade)` + `toInfo()`(이름 앞뒤 공백 자름·여러 공백 하나로).
  - `ItemTypeFinder`(`/** 아이템 종류를 찾는다 */`): `ItemType find(Long)`(없으면 `IllegalArgumentException("물건 종류를 찾을 수 없습니다. id: " + id)`), `List<ItemType> findAll()`(영석 등급 순 → 이름순), `Map<Long, ItemType> findAllById(Collection<Long>)`, `ItemType spiritStone(SpiritStoneGrade)`(없으면 `IllegalStateException`).
  - `ItemTypeEditor`(`/** 아이템 종류를 만들고 고치고 지운다 */`): `ItemType create(@Valid ItemTypeSaveRequest)`, `ItemType update(Long, @Valid ItemTypeSaveRequest)`, `void delete(Long)`. 이름 중복 → `DuplicateItemTypeNameException("이미 있는 물건 이름입니다: " + name)`, 영석 고치기·지우기 → `SpiritStoneChangeException("영석은 엔진이 정한 물건이라 고치거나 지울 수 없습니다.")`, 쓰이는 물건 → `ItemTypeInUseException("쓰이는 물건은 지울 수 없습니다: " + name)` / `("쓰이는 물건은 종류·저물대 등급을 바꿀 수 없습니다: " + name)`. 쓰이는지는 `ItemTypeModifyService` 의 `private boolean isInUse(Long)` 이 리포지토리 count 들의 합으로 본다(Task 1에서는 0을 돌려주는 자리, 뒤 태스크가 count를 더한다).
  - `ItemTypeModifyService` 의 `@EventListener(ApplicationReadyEvent.class) @Transactional public void seedSpiritStones()` — 등급마다 없으면 `ItemType.spiritStone(grade)` 저장(D35 이름·기준가·묘사).
  - `ClockConfig`: `@Configuration` + `@Bean Clock clock()`. 테스트 `MutableClock extends Clock`(`2026-11-14T00:00:00Z` 시작, `advance(Duration)`, `reset()`), `TestFakesConfiguration` 에 `@Bean @Primary MutableClock`.

- [ ] **Step 1: 도메인 테스트** — `ItemTypeTest`: `create`(공백 정리 `"  늑대   가죽 "` → `늑대 가죽`, 묘사 null → `""`), `bagNeedsGrade`·`gradeOnlyForBag`, `reservedGradeNameFail`(`하품`), `lastWordNumberFail`(`회춘단 2`, `가죽 전부`), `ordinalPrefixFail`(`2.가죽`), `spiritStoneCategoryFail`, `baseValueRangeFail`(-1, 100,000,001), `spiritStone`(`하품 영석` 기준가 1, 겹침 10,000). `ItemCategoryTest`: D33 표 값 전부. `ItemQueryTest`: `2.가죽` → `(가죽,2)`, `0.가죽` → `(0.가죽,0)`, `하품` → `(하품 영석,0)`. `ItemNamesTest`: `회춘단` 이 `회춘단(연기 초)` 에 맞음, `가죽` 이 `늑대 가죽` 에 맞음, `죽` 은 안 맞음, `select` 가 전체 일치 우선·번호는 순서대로. `SlotsTest`: 99개 1칸, 100개 2칸, 영석 10,001개 2칸, 저물대 0칸. `PricingTest`: 회춘단 기준가 3 → 개당 0.75, 3개 2, 늑대 가죽 2(재료) → 0.8, 10개 8, `isFairSellPrice(회춘단, 1) == false`, `(회춘단, 2) == true`. `NumbersTest`: 9880 → `9,880`, 0.75·10·0.8. `JosaTest` 추가: `비검과`·`회춘단은`·`법기를`·`회춘단123456는`·`회춘단(연기 초)는`·`늑대 가죽은`.
- [ ] **Step 2: 실패 확인 → 도메인·orm.xml·리포지토리 구현.**
- [ ] **Step 3: 리포지토리 테스트(`@DataJpaTest`)** — `ItemTypeRepositoryTest`: `saveAndFind`, `duplicateNameFail`(flush 때 `DataIntegrityViolationException`), `findBySpiritStoneGrade`.
- [ ] **Step 4: 서비스 테스트(`@ApplicationServiceTest`)** — `ItemTypeEditorTest`: `spiritStonesSeeded`(네 등급이 있고 이름·기준가가 D35), `create`, `duplicateNameIgnoringCaseFail`, `updateKeepsOwnName`, `updateSpiritStoneFail`, `deleteSpiritStoneFail`, `delete`, `unknownIdFail`(메시지 `물건 종류를 찾을 수 없습니다. id: 999`), `findAllOrder`(영석 넷 다음 이름순).
- [ ] **Step 5: 구현 → 통과.**
- [ ] **Step 6: 검증** — `cd world-server && ./gradlew build` 통과(ArchUnit: `item` 은 다른 슬라이스를 쓰지 않음).
- [ ] **Step 7: 커밋** — `feat(world-server): 아이템 종류 도메인·영속·관리 서비스와 영석 4종`.

**완료 조건:** 위 테스트 통과, orm.xml 이름이 위 값대로, 1·2차 테스트 전부 통과.
**progress.md 한 줄:** `item` 슬라이스(ItemType·ItemCategory·BagGrade·SpiritStoneGrade·ItemQuery·ItemNames·Slots·Pricing), 영석 4종 시작 시 심기, `Josa` 은/는·을/를·과/와, `Numbers`, `Clock` 빈과 `MutableClock`.

---

### Task 2: 아이템 종류 관리 REST API

**권장 모델:** sonnet

**목적:** D49의 `/api/admin/item-types` 다섯 개.

**Files:**
- Create: `adapter/webapi/ItemTypeApi.java`, `adapter/webapi/dto/ItemTypeResponse.java`
- Modify: `adapter/webapi/ApiControllerAdvice.java`(`DuplicateItemTypeNameException`·`ItemTypeInUseException`·`SpiritStoneChangeException` → 409, `DataIntegrityViolationException` → 409 D49 문구)
- Test: `adapter/webapi/ItemTypeApiWebMvcTest`, `adapter/webapi/AdminApiIntegrationTest`(추가), `adapter/security/SecurityIntegrationTest`(추가 한 줄)

**Interfaces:**
- Consumes: Task 1 포트.
- Produces: D49 `ItemTypeResponse.of(ItemType)`(필드 D49 순서). `bagGradeName` 은 `하급` 등, 없으면 null.

- [ ] **Step 1: 웹 테스트(`@WebMvcTest` + 보안, 관리자·`X-MUD-Client: 1`)** — `POST` 201 본문(`categoryName:"단약"`, `stackSize:99`, `buyRatePercent:25`, `spiritStone:false`), 저물대 `bagSlots:20`, 빈 이름 400, 잘못된 `category` 400, 중복 409 `detail` `이미 있는 물건 이름입니다: 회춘단`, 영석 `PUT` 409 `detail` D35 문구, 쓰이는 물건 `DELETE` 409, 관리자 아님 403, `X-MUD-Client` 없음 403.
- [ ] **Step 2: 실패 확인 → 구현 → 통과.**
- [ ] **Step 3: 통합 테스트** — `AdminApiIntegrationTest.manageItemTypes`: 목록 앞 넷이 영석 → 만들기 → 단건 → 고치기 → 지우기 204 → 단건 400.
- [ ] **Step 4: 검증** — `./gradlew build` 통과.
- [ ] **Step 5: 커밋** — `feat(world-server): 아이템 종류 관리 API`.

**완료 조건:** 위 테스트 통과.
**progress.md 한 줄:** `/api/admin/item-types` 5개, 409 처리 4개(DB 제약 위반 포함).

---

### Task 3: 소지품 도메인·결제·영속·소지품 서비스

**권장 모델:** opus

**목적:** D36·D37의 `inventory` 슬라이스. 게임 명령(Task 7·8)이 쓰는 포트까지. 시작 소지품은 Task 4.

**Files:**
- Create: `domain/inventory/{Inventory,InventoryEntry,SpiritStonePayment,AddCheck,RemoveCheck,PurchaseResult,SaleResult,GiveResult}.java`
- Modify: `orm.xml`, `application/item/required/ItemTypeRepository.java`(+`countInInventories`), `application/item/ItemTypeModifyService.java`(`isInUse` 에 더함)
- Create: `application/inventory/required/InventoryRepository.java`, `application/inventory/provided/{InventoryFinder,InventoryEditor}.java`, `application/inventory/{InventoryQueryService,InventoryModifyService}.java`
- Test: `domain/inventory/{InventoryTest,SpiritStonePaymentTest,InventoryFixture}`, `application/inventory/required/InventoryRepositoryTest`, `application/inventory/provided/InventoryEditorTest`, `application/item/provided/ItemTypeEditorTest`(추가)

**Interfaces:**
- Consumes: Task 1 `ItemType`·`Slots`·`SpiritStoneGrade`·`ItemTypeFinder`.
- Produces:
  - `record SpiritStonePayment(Map<SpiritStoneGrade, Long> paid, long change)` — `static Optional<SpiritStonePayment> plan(Map<SpiritStoneGrade, Long> held, long price)`(D36, price ≥ 1), `boolean breaksHighStones()`(상품·극품을 냄), `List<SpiritStoneGrade> brokenHighGrades()`.
  - `enum AddCheck { OK, NO_ROOM, BAG_LIMIT }`, `enum RemoveCheck { OK, BAG_IN_USE }`.
  - `sealed interface PurchaseResult`: `Bought(SpiritStonePayment)`, `NeedsConfirm(SpiritStonePayment)`, `NotEnough(long have)`, `BagLimit()`, `NoRoom()`, `NoRoomForChange(long change, int slotsNeeded)`. 검사 순서: NotEnough → BagLimit → NoRoom(결제한 영석을 뺀 뒤 산 물건이 안 들어감) → NoRoomForChange(거스름까지 넣으면 안 들어감, `slotsNeeded` = 거스름을 넣었을 때 늘어나는 칸) → NeedsConfirm(확인 안 했고 상품·극품을 깸) → Bought.
  - `enum SaleResult { SOLD, BAG_IN_USE, NO_ROOM }`, `enum GiveResult { GIVEN, RECEIVER_NO_ROOM, RECEIVER_BAG_LIMIT, GIVER_BAG_IN_USE }`.
  - `InventoryEntry`(`@Entity`): `getItemType()`, `isBound()`, `getCount()`.
  - `Inventory`(`@Entity`): `static Inventory empty(Long characterId)`, `getCharacterId()`, `List<InventoryEntry> getEntries()`(D37 표시 순서, 바꿀 수 없음), `int usedSlots()`, `int capacity()`, `int bagCount()`, `Map<SpiritStoneGrade, Long> spiritStones()`, `long spiritStoneValue()`, `int roomFor(ItemType)`(더 넣을 수 있는 최대 수, 저물대는 3개 한도), `AddCheck checkAdd(ItemType, long count)`, `void add(ItemType, int count, boolean bound)`, `RemoveCheck checkRemove(ItemType, boolean bound, int count)`, `void remove(ItemType, boolean bound, int count)`, `PurchaseResult checkPurchase(ItemType, int count, long price, ItemType lowStone)`, `void purchase(ItemType, int count, SpiritStonePayment, Map<SpiritStoneGrade, ItemType> stones)`, `SaleResult checkSale(ItemType, boolean bound, int count, long payout, ItemType lowStone)`, `void sell(ItemType, boolean bound, int count, long payout, ItemType lowStone)`, `void bind(ItemType)`(귀속 안 된 묶음에서 1개를 귀속 묶음으로), `void unbind(ItemType)`. 규칙: 같은 `(종류, 귀속)` 묶음은 하나이고 이미 있으면(수 0이라도) 그 묶음을 쓴다, 바꾸는 메서드는 검사를 통과하지 못하면 `IllegalStateException` 이고 아무것도 바꾸지 않는다, 바꾼 뒤 수가 0인 묶음을 마지막에 뺀다.
  - 매핑: `inventory`(`character_id` bigint not null `UK_INVENTORY_CHARACTER_ID`, FK 없음), `inventory_entry`(`inventory_id` FK not null, `item_type_id` FK not null, `bound` boolean not null, `count` int not null). `Inventory.entries` one-to-many cascade all + orphan-removal, `<order-by>id</order-by>`. DB 유니크 제약 없음(D52).
  - `InventoryRepository extends Repository<Inventory, Long>`: `save`, `@EntityGraph(attributePaths = {"entries", "entries.itemType"}) Optional<Inventory> findByCharacterId(Long)`.
  - `ItemTypeRepository` + `@Query("select count(e) from InventoryEntry e where e.itemType.id = :itemTypeId") long countInInventories(Long itemTypeId)`.
  - `InventoryFinder`(`/** 캐릭터 소지품을 찾는다 */`): `Inventory find(Long characterId)` — 없으면 저장하지 않은 빈 소지품.
  - `InventoryEditor`(`/** 게임 명령으로 캐릭터 소지품을 바꾼다. 검사가 실패하면 아무것도 바꾸지 않는다 */`): `int addUpTo(Long characterId, Long itemTypeId, int count)`(넣은 수), `RemoveCheck remove(Long characterId, Long itemTypeId, boolean bound, int count)`, `PurchaseResult purchase(Long characterId, Long itemTypeId, int count, long totalPrice, boolean confirmed)`, `SaleResult sell(Long characterId, Long itemTypeId, boolean bound, int count, long payout)`, `GiveResult give(Long fromCharacterId, Long toCharacterId, Long itemTypeId, boolean bound, int count)`, `void bind(Long characterId, Long itemTypeId)`, `void unbind(Long characterId, Long itemTypeId)`. 소지품이 없으면 만들어 저장하고 쓴다.

- [ ] **Step 1: 도메인 테스트** — `SpiritStonePaymentTest`: 룰북 예(하품 50·중품 2, 120 → `{LOW:50, MIDDLE:1}`, 거스름 30), `exactLow`(하품 15, 15 → 거스름 0), `middleOnly`(중품 1, 3 → `{MIDDLE:1}`, 97), `notEnough`(하품 9, 15 → empty), `breakHigh`(상품 1, 120 → `{HIGH:1}`, 9,880, `breaksHighStones`). `InventoryTest`: `emptyHasTwoSlots`, `bagAddsSlots`(하급 저물대 → 22), `stackFillsFirst`(회춘단 98 + 3 → 2칸), `stonesTakeSlots`(하품 10,001 → 2칸), `roomForPartial`(2칸, 하품 1칸 사용 → 비검 1), `fourthBagFail`(BAG_LIMIT), `removeBagInUseFail`(하급 저물대 하나에 칸 21개 사용 → BAG_IN_USE, 그대로), `boundIsSeparateEntry`(비검 2 → bind → `비검`·`비검(귀속)` 두 묶음), `unbindMerges`, `purchaseOrder`(PurchaseResult 각 경우), `purchaseWithChange`(하품 50·중품 2로 120 → 하품 30·중품 1, 산 물건), `saleNoRoomFail`(꽉 찬 소지품에서 하품이 처음 생기는 판매), `entriesDisplayOrder`(영석 등급 순 → 얻은 순서), `zeroEntriesRemoved`.
- [ ] **Step 2: 실패 확인 → 구현.**
- [ ] **Step 3: 리포지토리 테스트** — `InventoryRepositoryTest`: `saveWithEntries`(flush·clear 뒤 순서·귀속), `payAllLowStonesThenReceiveChange`(하품 50·중품 2 소지품에 `purchase(…, 120)` → flush 예외 없음, clear 뒤 하품 30·중품 1).
- [ ] **Step 4: 서비스 테스트** — `InventoryEditorTest`: `addUpToCreatesInventory`, `giveMovesBothSides`, `giveReceiverNoRoom`(양쪽 그대로), `giveBagToFullReceiverFail`(받는 쪽 저물대 3개 → RECEIVER_BAG_LIMIT), `purchaseNeedsConfirmThenBought`(confirmed false → NeedsConfirm, true → Bought), `sellPaysLowStones`. `ItemTypeEditorTest` 추가: `deleteItemTypeInInventoryFail`, `changeCategoryInInventoryFail`, `renameInInventory`.
- [ ] **Step 5: 구현 → 통과.**
- [ ] **Step 6: 검증** — `./gradlew build` 통과(ArchUnit: `inventory → item` 만).
- [ ] **Step 7: 커밋** — `feat(world-server): 소지품·저물대 칸·영석 결제 도메인과 소지품 서비스`.

**완료 조건:** 위 테스트 통과, 결제 값이 item.md 예와 같음.
**progress.md 한 줄:** `inventory` 슬라이스(Inventory·InventoryEntry·SpiritStonePayment·결과 타입), 묶음 유일은 도메인(DB 제약 없음), `InventoryFinder`·`InventoryEditor`, 아이템 종류 사용 검사에 소지품.

---

### Task 4: 시작 소지품 — 관리 서비스·REST, 캐릭터 생성 때 지급

**권장 모델:** sonnet

**목적:** D38.

**Files:**
- Create: `domain/world/StartingItem.java`
- Modify: `domain/world/WorldSettings.java`, `orm.xml`, `application/world/provided/{WorldSettingsFinder,WorldSettingsEditor}.java`, `application/world/{WorldSettingsQueryService,WorldSettingsModifyService}.java`, Create `application/world/provided/StartingItemRequest.java`
- Modify: `application/inventory/provided/InventoryEditor.java`·`InventoryModifyService.java`(`createWithStartingItems`), `application/character/CharacterModifyService.java`, `application/item/required/ItemTypeRepository.java`(+`countInStartingItems`), `ItemTypeModifyService.java`
- Modify: `adapter/webapi/WorldApi.java`, Create `adapter/webapi/dto/{StartingItemResponse,StartingItemsSaveRequest}.java`
- Test: `domain/world/WorldSettingsTest`(추가), `application/world/provided/WorldSettingsEditorTest`(추가), `application/character/provided/CharacterCreatorTest`(추가), `adapter/webapi/WorldApiWebMvcTest`(추가), `AdminApiIntegrationTest`(추가), `ItemTypeEditorTest`(추가)

**Interfaces:**
- Consumes: Task 1·3.
- Produces:
  - `record StartingItem(Long itemTypeId, int count)` embeddable. `WorldSettings`: `List<StartingItem> getStartingItems()`(바꿀 수 없음), `void replaceStartingItems(List<StartingItem>)`. 매핑 `<element-collection name="startingItems">` → `world_starting_item`(`world_settings_id`), `<order-column name="position"/>`, `item_type_id` bigint not null, `count` int not null.
  - `record StartingItemRequest(@NotNull Long itemTypeId, @Min(1) @Max(999_999) int count)`.
  - `WorldSettingsFinder` + `List<StartingItem> findStartingItems()`. `WorldSettingsEditor` + `List<StartingItem> replaceStartingItems(@NotNull @Size(max=30) List<@NotNull @Valid StartingItemRequest> items)` — D38 검사(칸은 `Slots` 로 계산), 400 문구 D38.
  - `InventoryEditor.createWithStartingItems(Long characterId)`: 새 소지품에 시작 소지품을 순서대로 넣고 저장. `CharacterModifyService.create` 가 캐릭터 저장 직후 같은 트랜잭션에서 부른다.
  - `ItemTypeRepository` + `@Query("select count(w) from WorldSettings w join w.startingItems s where s.itemTypeId = :itemTypeId") long countInStartingItems(Long itemTypeId)`.
  - REST: `GET/PUT /api/admin/world/starting-items` — 요청 `StartingItemsSaveRequest(List<StartingItemRequest> items)`, 응답 `StartingItemResponse(Long itemTypeId, String itemName, int count)[]`(이름은 `ItemTypeFinder.findAllById`).

- [ ] **Step 1: 테스트** — `WorldSettingsEditorTest`: `replaceStartingItems`(순서 유지), `duplicateItemFail`, `overCapacityFail`(저물대 없이 영석 30·회춘단 3·비검 1 → `시작 소지품이 칸을 넘습니다. (필요 3칸, 칸 2)`), `fourBagsFail`, `unknownItemFail`. `CharacterCreatorTest`: `createGivesStartingItems`(룰북 넷 → 소지품 3/22칸, 하품 30), `createWithoutStartingItems`(빈 소지품 0/2칸). `ItemTypeEditorTest`: `deleteItemTypeInStartingItemsFail`. `WorldApiWebMvcTest`: `PUT` 200 본문, `count:0` 400, `detail` 그대로. 통합: `manageStartingItems`(트랜잭션 밖 이름 직렬화).
- [ ] **Step 2: 실패 확인 → 구현 → 통과.**
- [ ] **Step 3: 검증** — `./gradlew build` 통과(ArchUnit: `world → item`, `inventory → world`, `character → inventory`, 순환 없음).
- [ ] **Step 4: 커밋** — `feat(world-server): 시작 소지품 지정과 캐릭터 생성 때 지급`.

**완료 조건:** 위 테스트 통과, 1·2차 캐릭터 생성 테스트 그대로 통과.
**progress.md 한 줄:** `WorldSettings.startingItems`(`world_starting_item`), `GET/PUT /api/admin/world/starting-items`, 생성 트랜잭션에서 지급, 기존 캐릭터는 빈 소지품.

---

### Task 5: 방 바닥 물건 — 배치·버린 물건·읽을 때 만료, 관리 REST

**권장 모델:** opus

**목적:** D39·D40(묶음)·D49(바닥 부분)·D54(방 삭제).

**Files:**
- Create: `domain/floor/{FloorPlacement,FloorPlacementSaveInfo,DroppedItem,FloorStack,FloorStacks}.java`
- Modify: `orm.xml`, `application/item/required/ItemTypeRepository.java`(+count 둘·만료 지우기), `ItemTypeModifyService.java`
- Create: `application/floor/required/{FloorPlacementRepository,DroppedItemRepository}.java`, `application/floor/provided/{FloorPlacementFinder,FloorPlacementEditor,FloorPlacementSaveRequest,RoomFloor}.java`, `application/floor/{FloorQueryService,FloorModifyService}.java`
- Create: `adapter/webapi/FloorItemApi.java`, `adapter/webapi/dto/FloorPlacementResponse.java`; Modify: `adapter/webapi/RoomApi.java`, `dto/RoomDetailResponse.java`(`floorItems`)
- Test: `domain/floor/{FloorPlacementTest,DroppedItemTest,FloorStacksTest,FloorFixture}`, `application/floor/required/FloorPlacementRepositoryTest`, `application/floor/provided/{FloorPlacementEditorTest,RoomFloorTest}`, `adapter/webapi/{FloorItemApiWebMvcTest,RoomApiWebMvcTest(추가),AdminApiIntegrationTest(추가)}`, `ItemTypeEditorTest`(추가)

**Interfaces:**
- Consumes: Task 1, `RoomFinder`·`RoomDeletedEvent`, `Clock`.
- Produces:
  - `record FloorPlacementSaveInfo(int count, @Nullable Integer respawnMinutes)`.
  - `FloorPlacement`(`@Entity`): `static FloorPlacement create(Long roomId, ItemType, FloorPlacementSaveInfo)`, `void update(ItemType, FloorPlacementSaveInfo)`(남은 수 = 처음 수, 비워진 시각 없음), `int availableAt(Instant now)`(저장하지 않는 계산), `void refresh(Instant now)`, `void take(int count, Instant now)`(0이 되면 비워진 시각 기록), getter `getRoomId()`·`getItemType()`·`getCount()`·`findRespawnMinutes()`. 검사: 수 1~9,999, 재등장 1~1,440(문구 `수는 1~9,999로 정하십시오.`, `재등장은 1~1,440분으로 정하십시오.`).
  - `DroppedItem`(`@Entity`): `static DroppedItem drop(Long roomId, ItemType, int count, Instant now)`(사라지는 시각 = now + 5분), `boolean isExpiredAt(Instant)`, `void take(int)`, getter.
  - `record FloorStack(ItemType itemType, int count)`, `FloorStacks.of(List<FloorPlacement>, List<DroppedItem>, Instant now)` → `List<FloorStack>`(D40 묶음·순서, 0인 것 뺌).
  - 매핑: `floor_placement`(`room_id` bigint not null FK 없음, `item_type_id` FK not null, `count` int, `respawn_minutes` int null, `remaining` int, `emptied_at` timestamp null), `dropped_item`(`room_id`, `item_type_id` FK, `count`, `expires_at` timestamp not null). 두 리포지토리의 방 조회는 `@EntityGraph(attributePaths = "itemType")`, id 순.
  - `FloorPlacementFinder`(`/** 관리자가 둔 바닥 물건을 찾는다 */`): `FloorPlacement find(Long)`(없으면 `바닥 물건을 찾을 수 없습니다. id: 9`), `List<FloorPlacement> findAllInRoom(Long roomId)`.
  - `record FloorPlacementSaveRequest(@NotNull Long itemTypeId, @Min(1) @Max(9999) int count, @Nullable @Min(1) @Max(1440) Integer respawnMinutes)`.
  - `FloorPlacementEditor`(`/** 관리자가 방 바닥에 물건을 두고 고치고 치운다 */`): `FloorPlacement create(Long roomId, @Valid FloorPlacementSaveRequest)`(방이 없으면 `RoomFinder` 문구 400), `FloorPlacement update(Long id, @Valid …)`, `void delete(Long id)`.
  - `RoomFloor`(`/** 게임에서 방 바닥 물건을 보고 줍고 버린다 */`): `List<FloorStack> look(Long roomId)`(그 방의 사라진 버린 물건을 지우고 재등장할 배치를 채운 뒤 묶음), `int take(Long roomId, Long itemTypeId, int count)`(버린 것 오래된 것부터 → 배치 id 순, 꺼낸 수), `void drop(Long roomId, Long itemTypeId, int count)`.
  - `FloorModifyService` 의 `@EventListener void onRoomDeleted(RoomDeletedEvent)` — 그 방의 배치·버린 물건 지우기(같은 트랜잭션).
  - `ItemTypeRepository` + `countInFloorPlacements`, `countInDroppedItems(Long itemTypeId, Instant now)`(사라지지 않은 것), `@Modifying deleteExpiredDroppedItems(Long itemTypeId, Instant now)` — 지우기 전에 부른다.
  - REST: D49 바닥 세 개(`FloorItemApi`), `RoomDetailResponse.floorItems`(`FloorPlacementResponse.of(FloorPlacement, Instant now)`).

- [ ] **Step 1: 도메인 테스트** — `FloorPlacementTest`: `takeReducesRemaining`, `respawnsAfterMinutes`(3개 모두 줍기 → 9분 59초에 0 → 10분에 3), `noRespawnStaysEmpty`, `partialTakeDoesNotStartRespawn`, `updateResets`. `DroppedItemTest`: `expiresAfterFiveMinutes`(4분 59초 false, 5분 true). `FloorStacksTest`: 배치 가죽 2 + 버린 가죽 1 + 버린 하품 3 → `[가죽 3, 하품 영석 3]`, 사라진 것 뺌.
- [ ] **Step 2: 실패 확인 → 구현.**
- [ ] **Step 3: 서비스 테스트** — `RoomFloorTest`(`MutableClock`): `lookPurgesExpired`(5분 뒤 행이 지워짐), `takeDroppedFirst`, `takeAcrossStacks`, `dropKeepsOwnTimer`. `FloorPlacementEditorTest`: `create`, `unknownRoomFail`, `deleteRoomRemovesFloorItems`(방 삭제 뒤 배치·버린 물건 행 없음), `deleteStartRoomKeepsFloorItems`(시작 방 삭제 409 뒤 그대로). `ItemTypeEditorTest`: `deleteItemTypeOnFloorFail`, `deleteItemTypeWithOnlyExpiredDropsSucceeds`.
- [ ] **Step 4: 웹·통합 테스트** — `FloorItemApiWebMvcTest`: `POST` 201 본문, `count:0` 400, `respawnMinutes:null` 허용, 관리자 아님 403. `RoomApiWebMvcTest` 추가: `floorItems[0].itemName`. 통합 `manageFloorItems`: 방에 두기 → `GET /rooms/{id}` 가 트랜잭션 밖에서 이름 냄 → 고치기 → 지우기.
- [ ] **Step 5: 구현 → 통과.**
- [ ] **Step 6: 검증** — `./gradlew build` 통과(ArchUnit: `floor → item, room`).
- [ ] **Step 7: 커밋** — `feat(world-server): 방 바닥 물건(배치 재등장·버린 물건 5분)과 관리 API`.

**완료 조건:** 위 테스트 통과, 시간에 걸린 테스트가 실제로 기다리지 않음.
**progress.md 한 줄:** `floor` 슬라이스(FloorPlacement·DroppedItem·FloorStacks·RoomFloor), 읽을 때 만료·재등장, 방 삭제 때 정리, 바닥 관리 API 3개와 방 상세 `floorItems`.

---

### Task 6: NPC 상점 역할 — 도메인·관리 서비스·REST (+2차 미룬 NPC Minor)

**권장 모델:** opus

**목적:** D41·D49(상점 부분)·D54(기준가 변경 검사). NPC 도메인을 건드리므로 2차에서 미룬 NPC Minor 셋을 함께 고친다.

**Files:**
- Create: `domain/npc/{ShopKind,ShopItem,ShopOffer}.java`; Modify: `domain/npc/{Npc,NpcSaveInfo}.java`, `orm.xml`
- Modify: `application/npc/provided/{NpcEditor,NpcFinder}.java`, Create `application/npc/provided/{ShopSaveRequest,ShopItemRequest}.java`, Modify `application/npc/{NpcModifyService,NpcQueryService}.java`, `application/npc/required/NpcRepository.java`
- Modify: `application/item/required/ItemTypeRepository.java`(+`countInShops`, `findShopPrices`), `ItemTypeModifyService.java`(기준가 변경 검사)
- Modify: `adapter/webapi/NpcApi.java`, `dto/{NpcDetailResponse,NpcSummaryResponse}.java`, Create `dto/{NpcShopResponse,NpcShopItemResponse}.java`
- Test: `domain/npc/{NpcTest(추가),ShopKindTest}`, `application/npc/required/NpcRepositoryTest`(추가), `application/npc/provided/NpcEditorTest`(추가), `ItemTypeEditorTest`(추가), `adapter/webapi/NpcApiWebMvcTest`(추가), `AdminApiIntegrationTest`(추가)

**Interfaces:**
- Consumes: Task 1 `ItemType`·`ItemCategory`·`Pricing`·`ItemTypeFinder`.
- Produces:
  - `enum ShopKind` — D41 표 순서·코드·이름. `koreanName()`, `boolean sells(ItemCategory)`, `boolean buys(ItemCategory)`, `List<ItemCategory> buyCategories()`, `static Optional<ShopKind> buyerOf(ItemCategory)`(선언 순서 첫째).
  - `record ShopItem(Long itemTypeId, @Nullable Integer price)` embeddable — `long effectivePrice(ItemType)`(price ?? `Pricing.defaultSellPrice`).
  - `record ShopOffer(ItemType itemType, @Nullable Integer price)`.
  - `Npc` + 필드 `@Nullable ShopKind shopKind`, `List<ShopItem> shopItems`; `void openShop(ShopKind, List<ShopOffer>)`(D41 검사 전부, 문구 글자 그대로), `void closeShop()`, `Optional<ShopKind> findShopKind()`, `boolean hasShop()`, `List<ShopItem> getShopItems()`, `String shopName()`(`"왕씨 단약방"`). 매핑: `shop_kind` varchar(16) STRING null, `<element-collection name="shopItems">` → `npc_shop_item`(`npc_id`), `<order-column name="position"/>`, `item_type_id` bigint not null, `price` int null. `NpcRepository` 의 `findById`·`findAllByOrderByNameAsc`·`findAllByRoomIdOrderByIdAsc` 엔티티 그래프에 `shopItems` 를 더한다(둘 다 순서 열 있는 리스트라 여러 bag 문제가 없다).
  - **2차 미룬 Minor:** `Npc.apply` 가 인사말·기본 대답 1~500자와 낱말 30개 한도를 검사(`인사말은 1~500자로 정하십시오.`, `기본 대답은 1~500자로 정하십시오.`, `낱말은 30개까지 넣을 수 있습니다.`). `NpcSaveInfo` 의 쓰이지 않는 `jakarta.validation` 애노테이션을 지운다. `NpcRepository` 의 쓰이지 않는 `existsByName`·`existsByNameAndIdNot` 를 지운다(지우기 전 `grep -rn` 으로 쓰는 곳이 없음을 확인).
  - `record ShopItemRequest(@NotNull Long itemTypeId, @Nullable @Min(1) @Max(100_000_000) Integer price)`, `record ShopSaveRequest(@NotNull ShopKind kind, @NotNull @Size(max=50) List<@NotNull @Valid ShopItemRequest> items)`.
  - `NpcEditor` + `Npc openShop(Long npcId, @Valid ShopSaveRequest)`(물건은 `ItemTypeFinder.find`), `void closeShop(Long npcId)`.
  - `ItemTypeRepository` + `@Query("select count(n) from Npc n join n.shopItems s where s.itemTypeId = :itemTypeId") long countInShops(Long)`, `@Query("select n.name, s.price from Npc n join n.shopItems s where s.itemTypeId = :itemTypeId") List<Object[]> findShopPrices(Long)`(또는 이름·값 투영 record). `ItemTypeModifyService.update` 가 기준가를 바꿀 때 D41 409 문구로 검사.
  - REST: D49 `PUT/DELETE /api/admin/npcs/{npcId}/shop`, `NpcDetailResponse.shop`, `NpcSummaryResponse.shopKindName`.

- [ ] **Step 1: 도메인 테스트** — `ShopKindTest`: D41 표, `buyerOf(MATERIAL) == MATERIAL_SHOP`, `buyerOf(BEAST_EGG)` 비어 있음. `NpcTest` 추가: `openShop`(순서 유지, `shopName`), `sellCategoryFail`(단약방에 법기 → `단약방은 법기를 팔 수 없습니다.`), `spiritStoneFail`, `duplicateItemFail`, `unfairPriceFail`(회춘단 3에 값 1 → `판매가는 매입가의 2배보다 커야 합니다: 회춘단 (매입가 0.75, 판매가 1)`), `zeroBaseNeedsPriceFail`, `nullPriceFollowsBase`, `closeShop`, `greetingTooLongFail`, `topicsOver30Fail`.
- [ ] **Step 2: 실패 확인 → 구현.**
- [ ] **Step 3: 서비스·리포지토리 테스트** — `NpcRepositoryTest`: `saveWithShopItems`(flush·clear 뒤 순서·null 값). `NpcEditorTest`: `openShopAndReplace`, `closeShop`, `unknownItemFail`. `ItemTypeEditorTest`: `deleteItemTypeInShopFail`, `lowerBaseValueKeepsFairPrices`, `raiseBaseValueBreaksFixedPriceFail`(409 문구 D41), `zeroBaseWithNullPriceFail`.
- [ ] **Step 4: 웹·통합 테스트** — `NpcApiWebMvcTest`: `PUT /shop` 200 본문(`shop.kindName`, `items[0].effectivePrice`), 잘못된 `kind` 400, `price:0` 400, `DELETE /shop` 204, 목록 `shopKindName`. 통합 `manageShop`: 아이템 → NPC → 상점 → `GET /npcs/{id}` 트랜잭션 밖 이름 → 상점 없애기.
- [ ] **Step 5: 구현 → 통과.**
- [ ] **Step 6: 검증** — `./gradlew build` 통과(ArchUnit: `npc → item, room`, `item` 은 `npc` 를 쓰지 않음).
- [ ] **Step 7: 커밋** — `feat(world-server): NPC 상점 역할(상점 종류·파는 물건·값·불변식)과 관리 API`.

**완료 조건:** 위 테스트 통과, 2차 NPC 테스트 그대로 통과.
**progress.md 한 줄:** `ShopKind`·`ShopItem`·`Npc.openShop`, `npc_shop_item`, 상점 API 2개, 기준가 변경 불변식 검사. 2차 Minor: Npc 도메인 길이·낱말 수 검사, NpcSaveInfo 애노테이션 제거, NpcRepository `existsByName*` 둘 제거.

---

### Task 7: 게임 — 물건 명령 해석, 방 바닥, 소지품·줍기·버리기·물건 보기

**권장 모델:** opus

**목적:** D40·D45·D46·D47·D48(room·inventory·system item·notice item)을 게임 코어와 WebSocket 직렬화에 넣는다. 상점·주기·귀속은 Task 8.

**Files:**
- Create: `domain/game/{Amount,ItemArguments}.java`; Modify: `domain/game/{Command,CommandParser}.java`
- Modify: `application/game/required/GameMessage.java`(`RoomMessage.items`, `FloorItemView`, `InventoryMessage`, `SystemMessage.item`, `NoticeMessage.item`), `application/game/RoomViewAssembler.java`, `application/game/GameService.java`
- Create: `application/game/ItemCommandHandler.java`(package-private `@Component`)
- Test: `domain/game/{CommandParserTest(추가),ItemArgumentsTest}`, `application/game/provided/{BaseGameTest,GameConnectorTest,GameConnectorConcurrencyTest}`(추가), `adapter/websocket/{WebSocketGameMessageSenderTest,GameWebSocketIntegrationTest}`(추가)

**Interfaces:**
- Consumes: `RoomFloor`(Task 5), `InventoryFinder`·`InventoryEditor`(Task 3), `ItemTypeFinder`, `ItemNames`·`ItemQuery`, 테스트 준비에 `ItemTypeEditor`·`FloorPlacementEditor`·`WorldSettingsEditor`, `MutableClock`.
- Produces:
  - `sealed interface Amount { record Count(int value); record All(); record Invalid(); static Amount one(); }`.
  - `record ItemArguments(@Nullable ItemQuery item, Amount amount)` + `static ItemArguments parse(String text)`(D46). 예: `늑대 가죽 3` → (`늑대 가죽`, Count 3), `가죽 전부` → All, `하품 10` → (`하품 영석`, 10), `가죽 0` → Invalid, `가죽 1234567890` → Invalid, `3` → (null, 3), `""` → (null, Count 1).
  - `Command`: `Inventory()`, `Pick(ItemArguments args)`, `Drop(ItemArguments args)`. 파서: `소지품`·`ㅈ`(인자 있으면 Unknown), `줍기 …`, `버리기 …`. `보기 <인자>` 는 2차 그대로 `Look(argument)`.
  - `RoomMessage(ZoneView zone, boolean zoneEntered, RoomView room, @Nullable OwnerView owner, List<String> exits, List<OccupantView> occupants, List<FloorItemView> items)`, `record FloorItemView(String name, int count)`.
  - `record InventoryMessage(int usedSlots, int totalSlots, List<StoneView> stones, List<ItemView> items, String text, String tone)` + `static InventoryMessage of(…)`(tone `"item"`), `type()` → `"inventory"`; `record StoneView(String name, long count)`, `record ItemView(String name, int count, boolean bound, boolean quest)`.
  - `SystemMessage.item(String text)`(tone `"item"`), `NoticeMessage.item(String text, String actor, String event)`(tone `"item"`).
  - `RoomViewAssembler.assemble(Room, boolean zoneEntered, List<OnlinePlayer>, List<Npc>, List<FloorStack>, Long viewerCharacterId)`.
  - `ItemCommandHandler`: `void handle(Command command, OnlinePlayer player, BiConsumer<String, GameMessage> send)` — `Inventory`·`Pick`·`Drop`·물건 보기를 D45 문구 그대로 처리. `String inventoryText(Inventory)` 는 D45 형식. 알림(D47)은 `PresenceRepository.findAllInRoom` 에서 나를 뺀 사람에게.
  - `GameService`: 생성자에 `RoomFloor`·`ItemCommandHandler` 추가. 방 보기를 만드는 모든 곳이 `roomFloor.look(roomId)` 를 넣는다. `Look(argument)` 가 null이 아니면 `ItemCommandHandler` 의 물건 보기로(D45).

- [ ] **Step 1: 파서 테스트** — `ItemArgumentsTest` 위 예 전부. `CommandParserTest`: `소지품`·`ㅈ` → Inventory, `줍기` → Pick(null, Count 1), `버리기 하품 10`, `줍기 2.가죽`, `소지품 x` → Unknown.
- [ ] **Step 2: 포트 테스트** — `BaseGameTest` 에 `prepareItems()`(아이템: `회춘단(연기 초)` 단약 3, `회기단(연기 초)` 단약 4, `늑대 가죽` 재료 2 묘사 `거칠고 질긴 늑대의 가죽이다.`, `늑대 이빨` 재료 1, `비검` 법기 40, `하급 저물대` 저물대 하급 30, `왕씨의 약초 꾸러미` 퀘스트 0)와 `@BeforeEach` 시계 `reset()` 추가. `GameConnectorTest`:
  - `inventoryShowsStartingItems` — 시작 소지품(하급 저물대 1·하품 영석 30·회춘단 3·회기단 3)으로 만든 캐릭터 `소지품` → `inventory` text `[소지품 3/22칸]  하품 영석 30\n하급 저물대   회춘단(연기 초) x3   회기단(연기 초) x3`, `usedSlots 3`, `totalSlots 22`.
  - `emptyInventory` — `[소지품 0/2칸]\n가진 물건이 없습니다.`
  - `roomShowsFloorItems` — 배치 가죽 3 → `room.items == [{늑대 가죽, 3}]`, NPC 뒤.
  - `pickAll`·`pickSome` — `줍기 가죽 2` → `늑대 가죽 2개를 주웠습니다.`, 같은 방 B가 notice `<A>{이/가} 늑대 가죽 2개를 줍는다.`(event `pick`), 다음 `보기` 에 `{늑대 가죽, 1}`.
  - `pickPartial` — 칸 2(저물대 없음), 하품 1칸 사용, 바닥 비검 3 → `저물대에 자리가 없어 비검 3개 중 1개만 주웠습니다.`, 바닥 비검 2.
  - `pickFails` — `줍기` → `무엇을 주우시겠습니까? (예: 줍기 늑대 가죽)`, `줍기 곰` → `이곳에 '곰'은 없습니다.`, `줍기 가죽 5`(3개) → `늑대 가죽은 3개뿐입니다.`, `줍기 가죽 0` → `수량은 1 이상의 수로 적으십시오.`, 칸 없음 → `저물대에 자리가 없습니다.`, 저물대 3개에 4번째 → `저물대는 세 개까지만 찰 수 있습니다.`
  - `dropAndPickBack` — `버리기 이빨 1` → `늑대 이빨 1개를 버렸습니다.`, B notice `… 버린다.`, 바닥에 묶여 보임, 다시 줍기.
  - `dropStones` — `버리기 하품 10` → `하품 영석 10개를 버렸습니다.`
  - `dropFails` — 없는 물건, `늑대 이빨은 2개뿐입니다.`, 퀘스트 `퀘스트 물건은 버릴 수 없습니다.`
  - `dropBagInUseFail` — 하급 저물대 하나로 칸 21개 쓸 때 `버리기 저물대` → `저물대에 든 물건을 먼저 비우십시오.`, 소지품 그대로.
  - `droppedItemDisappearsAfterFiveMinutes` — 버리고 시계 5분 → 다음 `보기` 에 없음, `줍기` → `이곳에 '이빨'은 없습니다.`
  - `placementRespawns` — 재등장 10분 배치를 다 줍고 10분 → 다시 보임.
  - `lookAtItem` — 바닥 `보기 가죽` → `늑대 가죽 — 거칠고 질긴 늑대의 가죽이다.`, 소지품만 있는 물건도 보임, `보기 왕씨`(NPC) → `아직 자세히 볼 수 없습니다.`, `보기 곰` → `그런 것은 보이지 않습니다.`
  - `oldCharacterGetsEmptyInventory` — 소지품 행이 없는 캐릭터의 `소지품` → `0/2칸`.
  - 2차 테스트의 `room` 기대값에 `items: []` 를 더한다.
  - `GameConnectorConcurrencyTest.concurrentPickNeverDuplicates` — 바닥 가죽 50, A·B가 동시에 `줍기 가죽 전부` 를 여러 번 → 두 소지품 합 50, 바닥 0.
- [ ] **Step 3: WebSocket 테스트** — `WebSocketGameMessageSenderTest`: `inventory` JSON 필드·순서, `room.items`, `system` tone `item`, `notice` event `pick`. `GameWebSocketIntegrationTest.pickAndInventory`(실제 WebSocket으로 `줍기` → `소지품`).
- [ ] **Step 4: 실패 확인 → 구현 → 통과.**
- [ ] **Step 5: 검증** — `./gradlew build` 통과(ArchUnit: game이 floor·inventory·item 의 provided만 씀).
- [ ] **Step 6: 커밋** — `feat(world-server): 방 바닥 물건·소지품·줍기·버리기·물건 보기`.

**완료 조건:** 위 테스트 통과, 문구가 D45와 글자 하나까지 같다. 2차 게임 테스트가 (room `items` 기대값 말고는) 그대로 통과.
**progress.md 한 줄:** `Amount`·`ItemArguments`·`Command.Inventory/Pick/Drop`, `RoomMessage.items`·`InventoryMessage`·tone `item`, `ItemCommandHandler`, 동시 줍기 테스트.

---

### Task 8: 게임 — 상점(목록·사기·팔기·값·확인·취소), 주기, 귀속, 도움말

**권장 모델:** opus

**목적:** D36(확인)·D42·D43·D44·D47(주기)·D56.

**Files:**
- Modify: `domain/game/{Command,CommandParser}.java`, `application/game/{GameService,GameSession,ItemCommandHandler}.java`
- Create: `application/game/ShopCommandHandler.java`(package-private `@Component`)
- Test: `domain/game/CommandParserTest`(추가), `application/game/provided/{BaseGameTest,GameConnectorTest}`(추가), `application/game/GameSessionTest`(있으면 추가, 없으면 생략), `adapter/websocket/GameWebSocketIntegrationTest`(추가)

**Interfaces:**
- Consumes: Task 3 `InventoryEditor.purchase/sell/give/bind/unbind`, Task 6 `Npc.findShopKind/getShopItems/shopName`·`ShopKind`·`ShopItem.effectivePrice`, `Pricing`, `NpcFinder.findAllInRoom`, `Clock`.
- Produces:
  - `Command`: `ShopList(String args)`, `Buy(String args)`, `Sell(String args)`, `Appraise(String args)`, `Give(@Nullable String target, String args)`, `Bind(String args)`, `Unbind(String args)`, `Confirm()`, `Cancel()`. 파서: `목록`·`사기`·`팔기`·`값` → 나머지 그대로(NPC 이름 떼기는 핸들러가 함, 방의 상점 수를 알아야 하므로), `주기` → 첫 낱말 대상·나머지, `귀속 해제 X` → Unbind(`X`), `귀속 X` → Bind(`X`), `확인`·`취소`(인자 있으면 Unknown).
  - `GameSession`: `record PendingPurchase(Long npcId, Long itemTypeId, int count, Instant askedAt)`, `void askConfirm(PendingPurchase)`, `Optional<PendingPurchase> findPending()`, `void clearPending()`. `GameService.handleCommand` 는 `Confirm`·`Cancel` 이 아니면 먼저 `clearPending()`.
  - `ShopCommandHandler`: `void handle(Command command, OnlinePlayer player, GameSession session, BiConsumer<String, GameMessage> send)` — D42 상점 고르기·목록·사기·팔기·값·확인·취소. `ItemCommandHandler` 에 주기·귀속·귀속 해제(D43·D44)를 더한다.
  - `GameService.HELP` = D56 문구.

- [ ] **Step 1: 파서 테스트** — 위 사례 전부(`주기` → Give(null, ""), `주기 한립` → Give("한립", ""), `주기 한립 하품 100`, `귀속 해제 비검`, `귀속` → Bind(""), `확인 x` → Unknown).
- [ ] **Step 2: 포트 테스트** — `BaseGameTest` 에 `prepareShop()`(마을 어귀의 2차 NPC `왕씨` 에 단약방 `[회춘단(연기 초) null, 회기단(연기 초) null]`). `GameConnectorTest`:
  - `shopList` — `목록` → `[왕씨 단약방]  파는 것  값(하품)\n  1. 회춘단(연기 초)  3\n  2. 회기단(연기 초)  4\n사 주는 것: 단약·영약`.
  - `noShopHereFail` — 상점 없는 방 `사기 회춘단` → `이곳에서는 할 수 없습니다.`
  - `twoShopsNeedName` — 같은 방에 상점 NPC 둘 → `사기 회춘단` → `상인이 여럿입니다. 상인 이름을 붙이십시오: 왕씨 · 이씨`, `사기 왕 회춘단` 은 됨. 상점 하나일 때 `사기 왕씨 회춘단` 도 됨.
  - `buy` — 하품 30 → `사기 회춘단 5` → `회춘단(연기 초) 5개를 샀습니다. (하품 영석 -15)`, 소지품 하품 15.
  - `buyFails` — `사기` → `무엇을 사시겠습니까? (예: 사기 회춘단 5)`, `사기 비검` → `왕씨: "그런 물건은 팔지 않소."`, 하품 9로 15 → `영석이 모자랍니다. (필요 15, 가진 것 9)`, 칸 없음 → `저물대에 자리가 없습니다.`(영석 그대로), `사기 회춘단 전부` → 수량 문구.
  - `buyBreaksHighStoneNeedsConfirm` — 하품 0·상품 1로 회춘단 40개(120) → `값을 치르려면 상품 영석 1개를 깨야 합니다. 거스름 하품 9,880개를 받습니다. 계속하려면 '확인'.` → `확인` → 산다(상품 0, 하품 9,880). 다시 같은 질문 뒤 `보기` 를 하고 `확인` → `확인할 것이 없습니다.`; 질문 뒤 11초 → `확인할 것이 없습니다.`; `취소` → `그만두었습니다.`; 대기 없이 `취소` → `그만둘 것이 없습니다.`
  - `buyChangeOverflowFail` — 거스름이 칸에 안 들어감 → `거스름을 받을 자리가 없습니다. (하품 9,880개, 1칸 필요)`.
  - `sell` — 회춘단 2 → `팔기 회춘단 2` → `회춘단(연기 초) 2개를 팔았습니다. (하품 영석 +1)`.
  - `sellFails` — `팔기 하품` → `왕씨: "그런 물건은 받지 않소."`, 퀘스트 → `왕씨: "그건 내가 살 물건이 아니오."`, 귀속 비검 → `왕씨: "귀속된 물건은 사 줄 수 없소."`, 늑대 가죽 → `왕씨: "그런 물건은 받지 않소." (재료는 재료상에 팔 수 있습니다)`, 회춘단 1 → `왕씨: "값이 너무 적어 사 줄 수가 없소. 더 모아 오시오."`, `회춘단(연기 초)는 3개뿐입니다.`
  - `appraise` — `값 회춘단`(3개) → `회춘단(연기 초) 1개에 하품 영석 0.75 — 3개면 2`, `값 회춘단 10` → `… — 10개면 7`.
  - `give` — A `주기 <B> 회춘단 2` → A `<B>에게 회춘단(연기 초) 2개를 주었습니다.`, B `system/item` `<A>{이/가} 회춘단(연기 초) 2개를 건넸습니다.`, 같은 방 C notice `<A>{이/가} <B>에게 회춘단(연기 초) 2개를 건넨다.`(event `give`); `주기 <B> 하품 5` → 영석 이동.
  - `giveToAbsentPlayerKeepsItem` — 다른 방·접속 끊은·없는 이름·NPC 이름 → `여기에 <이름>{은/는} 없습니다.`, 소지품 그대로.
  - `giveToSelfFail` — `자기 자신에게는 줄 수 없습니다.`
  - `giveFails` — 귀속 `귀속된 물건은 남에게 줄 수 없습니다.`, 퀘스트 `퀘스트 물건은 남에게 줄 수 없습니다.`, 거래 불가 `<물건>{은/는} 남에게 줄 수 없는 물건입니다.`, 받는 쪽 칸 없음 `<B>의 저물대에 자리가 없습니다.`, 받는 쪽 저물대 3개 `<B>{은/는} 저물대를 더 찰 수 없습니다.`, 인자 없음 `누구에게 무엇을 주시겠습니까? (예: 주기 한립 회춘단 2)`.
  - `bindAndUnbind` — `귀속 비검` → `비검에 정혈을 한 방울 떨어뜨린다. 비검이 당신에게 귀속되었습니다.`, `소지품` 에 `비검 (귀속)`, `버리기 비검` → `귀속된 물건은 버릴 수 없습니다.`, `귀속 해제 비검` → `비검과 이어진 정혈의 끈을 끊었습니다. 귀속이 풀렸습니다.`
  - `bindFails` — `귀속 회춘단` → `회춘단(연기 초)는 귀속할 수 없습니다.`, 이미 귀속 → `이미 귀속된 물건입니다.`, `귀속 해제 비검`(귀속 아님) → `귀속된 물건이 아닙니다.`, 퀘스트 해제 → `퀘스트 물건과 본명법보는 이렇게 풀 수 없습니다.`, 인자 없음 두 문구.
  - `boundAndUnboundPreferUnbound` — 비검 2개 중 하나 귀속 → `버리기 비검` 은 귀속 안 된 것을 버림.
  - `help` 문구를 D56으로 갱신.
- [ ] **Step 3: WebSocket 통합 테스트** — `giveBetweenTwoSessions`(실제 두 연결, 받는 쪽 메시지).
- [ ] **Step 4: 실패 확인 → 구현 → 통과.**
- [ ] **Step 5: 검증** — `./gradlew build` 통과.
- [ ] **Step 6: 커밋** — `feat(world-server): 상점 사고팔기·값·확인, 주기, 귀속, 도움말`.

**완료 조건:** 위 테스트 통과, 문구가 D42~D44·D56과 글자 하나까지 같다.
**progress.md 한 줄:** `Command` 상점·주기·귀속·확인·취소, `ShopCommandHandler`, `GameSession.PendingPurchase`(10초), 주기 알림, 도움말 D56.

---

### Task 9: admin-console — 아이템 종류와 시작 소지품 화면

**권장 모델:** opus

**목적:** D50 가운데 `/items/`·`/item/`·`/start-items/` 와 공통 바.

**Files:**
- Modify: `admin-console/src/lib/api.ts`(타입·함수), `src/components/TopBar.tsx`
- Create: `admin-console/src/lib/items.ts`, `app/items/page.tsx`, `app/item/page.tsx`, `app/start-items/page.tsx`, `src/components/{ItemForm,StartItemsEditor}.tsx`
- Test: `src/lib/api.test.ts`(추가), `src/components/{ItemForm.test.tsx,StartItemsEditor.test.tsx}`

**Interfaces:**
- Consumes: D49 아이템 종류·시작 소지품 API.
- Produces:
  - `items.ts`: `ITEM_CATEGORIES: {code, name, stackSize, bindable, creatable}[]`(D33 표 순서), `BAG_GRADES: {code: "LOW"|"MIDDLE"|"HIGH", name, slots}[]`, `SHOP_KINDS: {code, name, sells: ItemCategoryCode[]}[]`(D41, Task 10이 씀).
  - `api.ts`: 타입 `ItemCategoryCode`, `BagGradeCode`, `ItemTypeResponse`, `ItemTypeSaveRequest`, `StartingItem = {itemTypeId, count}`, `StartingItemResponse`. 함수 `listItemTypes`, `getItemType`, `createItemType`, `updateItemType`, `deleteItemType`, `getStartingItems`, `saveStartingItems`.
  - 문구 D50 그대로(제목 `아이템`·`시작 소지품`, 버튼 `아이템 만들기`·`저장`·`삭제`·`물건 추가`, 라벨, 안내, 확인 문구).

- [ ] **Step 1: 테스트** — `api.test.ts`: `createItemType` 이 `POST {url}/api/admin/item-types` 본문·`X-MUD-Client: 1`, `saveStartingItems` 가 `PUT …/starting-items` `{items}`. `ItemForm.test`: 종류 `저물대` 를 고르면 `저물대 등급` 이 보이고 저장 본문 `bagGrade:"LOW"`, 다른 종류면 `bagGrade:null`; 종류 선택에 `영석` 없음; 영석 응답이면 칸이 읽기 전용이고 안내 문구; 서버 409 `detail` 이 보임. `StartItemsEditor.test`: `물건 추가` → 행, `삭제`, 저장 본문 순서, 성공 `저장했습니다.`, 400 `detail` 보임.
- [ ] **Step 2: 실패 확인 → 구현 → 통과.**
- [ ] **Step 3: 검증** — `npm ci && npm test && npm run build` 통과, `out/items/index.html`·`out/item/index.html`·`out/start-items/index.html` 생성.
- [ ] **Step 4: 커밋** — `feat(admin-console): 아이템 종류·시작 소지품 화면`.

**완료 조건:** 테스트·정적 빌드 통과, 1·2차 화면 테스트 그대로 통과.
**progress.md 한 줄:** `/items/`·`/item/`·`/start-items/`, `items.ts`(종류·저물대 등급·상점 종류), 공통 바 링크 둘.

---

### Task 10: admin-console — 방 바닥 물건과 NPC 상점 화면

**권장 모델:** sonnet

**목적:** D50 가운데 방 화면 `바닥 물건`, NPC 화면 `상점`, NPC 목록 `상점` 열.

**Files:**
- Modify: `admin-console/src/lib/api.ts`, `app/room/page.tsx`, `app/npc/page.tsx`, `app/npcs/page.tsx`
- Create: `src/components/{FloorItemsEditor,ShopEditor}.tsx`
- Test: `src/lib/api.test.ts`(추가), `src/components/{FloorItemsEditor.test.tsx,ShopEditor.test.tsx}`

**Interfaces:**
- Consumes: D49 바닥·상점 API, Task 9 `items.ts`·`listItemTypes`.
- Produces: `api.ts` 타입 `FloorPlacementResponse`, `FloorPlacementSaveRequest`, `RoomDetailResponse.floorItems`, `ShopKindCode`, `ShopSaveRequest`, `NpcDetailResponse.shop`, `NpcSummaryResponse.shopKindName`; 함수 `createFloorItem(roomId, req)`, `updateFloorItem(id, req)`, `deleteFloorItem(id)`, `saveShop(npcId, req)`, `deleteShop(npcId)`. 문구 D50 그대로.

- [ ] **Step 1: 테스트** — `FloorItemsEditor.test`: 두기 폼 기본 `수 1`·`재등장(분) 10`, 재등장 비우면 본문 `respawnMinutes:null`, 표의 `한 번뿐`·`10분`, `삭제`. `ShopEditor.test`: 종류 `단약방` 이면 물건 선택에 단약만, 값 빈칸 → `price:null`, 자리 표시 `기준가 3`, `상점 아님` 저장 → `deleteShop` 호출, 서버 400 `detail` 보임.
- [ ] **Step 2: 실패 확인 → 구현 → 통과.**
- [ ] **Step 3: 검증** — `npm ci && npm test && npm run build` 통과.
- [ ] **Step 4: 커밋** — `feat(admin-console): 방 바닥 물건·NPC 상점 화면`.

**완료 조건:** 테스트·정적 빌드 통과.
**progress.md 한 줄:** 방 화면 `바닥 물건`, NPC 화면 `상점`, NPC 목록 `상점` 열.

---

### Task 11: play-client — 바닥 물건 줄, 소지품·물건 메시지, 자동완성

**권장 모델:** sonnet

**목적:** D48·D51을 그린다.

**Files:**
- Modify: `play-client/src/lib/{protocol.ts,render.ts,complete.ts}`, `src/components/Terminal.tsx`, `app/globals.css`(`.line-item`)
- Test: `src/lib/{render.test.ts,complete.test.ts}`(추가), `src/components/Terminal.test.tsx`(추가)

**Interfaces:**
- `protocol.ts`: `FloorItem = {name: string; count: number}`, `RoomMessage.items: FloorItem[]`, `SystemMessage.tone: "info" | "error" | "item"`, `NoticeMessage.tone: "movement" | "item"`, `NoticeEvent` 에 `"pick" | "drop" | "give"`, `InventoryMessage = {type: "inventory"; usedSlots; totalSlots; stones: {name; count}[]; items: {name; count; bound; quest}[]; text; tone: "item"}` 를 `ServerMessage` 에.
- `render.ts`: occupant 뒤에 `item.count > 1 ? \`${name} ${count}개\` : name`(style `item`), `inventory` → text 줄별 style `item`.
- `complete.ts`: `COMMANDS` D51 순서.
- `Terminal`: 이름 후보 = occupants 이름 + `room.items` 이름. notice 는 `arrive`·`appear`·`leave`·`disappear` 만 후보를 바꾼다.

- [ ] **Step 1: 테스트** — `render.test.ts`: 바닥 `하품 영석 3개`·`비검`(style `item`), `inventory` 두 줄 style `item`, `system` tone item. `complete.test.ts`: `"줍"` → `"줍기"`, `"귀"` → `"귀속"`, `"줍기 가"` + names `["늑대 가죽"]` 은 후보 없음(낱말 앞부분이 아니라 이름 앞부분으로 맞추므로 `"줍기 늑"` → `"줍기 늑대 가죽"`). `Terminal.test.tsx`: room(바닥 `늑대 가죽`) 뒤 `줍기 늑` + Tab → `줍기 늑대 가죽`, `pick` notice 뒤에도 이름 후보 그대로.
- [ ] **Step 2: 실패 확인 → 구현 → 통과.**
- [ ] **Step 3: 검증** — `npm ci && npm test && npm run build` 통과.
- [ ] **Step 4: 커밋** — `feat(play-client): 바닥 물건·소지품·물건 메시지 표시와 3차 명령 자동완성`.

**완료 조건:** 테스트·정적 빌드 통과.
**progress.md 한 줄:** `room.items`·`InventoryMessage`·tone `item`, `.line-item`, COMMANDS 3차, 이름 후보에 바닥 물건.

---

### Task 12: 스펙 반영 — 제안 결정을 시스템 스펙에 `(제안)` 으로

**권장 모델:** sonnet

**목적:** D32~D56과 구현된 값을 `docs/spec/system/` 에 옮기고 3차 열린 질문에 답을 붙인다. 코드를 바꾸지 않는다.

**Files:**
- Modify: `docs/spec/system/index.md` — `### 3차 범위` 의 "(작성 예정)" 을 계획 링크로, "구현했다(날짜)" 와 진행 기록 링크, `### 3차 이터레이션 열린 질문` 의 질문마다 `→ (제안) …` 답과 링크([열린 질문 대응표](#3차-열린-질문-대응표)).
- Modify: `docs/spec/system/admin-tool.md` — `## 3차 화면`(D50), `## 3차 REST API`(D49 표·응답 모양·예외 문구), 관리 툴 변경의 반영에 D54, 상점 줄.
- Modify: `docs/spec/system/command.md` — `## 3차 명령어` 에 D42~D47·D56(해석·이름 찾기·출력·오류 문구·공개 범위·도움말), 새 문구에 "룰북에 아직 없음" 표시. "`값`·`확인` 을 넣는지는 구현 때" 문장을 결정으로 바꾼다.
- Modify: `docs/spec/system/world-structure.md` — `## 방 바닥 물건` 에 D39·D40 데이터 모델, "3차 구현 때 정한다" 문장을 결정으로.
- Modify: `docs/spec/system/npc.md` — 역할 절에 D41 데이터 모델·불변식.
- Modify: `docs/spec/system/platform.md` — `### 3차 WebSocket 메시지 변경`(D48 표), `### 3차 월드 상태와 저장`(D37 저장·D39 시간 재기·D53 동시성·D55 시계), 플레이 화면(D51), 저장소 구성의 슬라이스·의존(D52).

**규칙:** 2차 Task 8과 같다 — 새 결정 문장 끝에 `(제안)`, 창호 확정 문장은 고치지 않음, **룰북(`docs/spec/game-rule/`)은 고치지 않음**, 값은 **구현된 코드**에서 확인(`GameService`·핸들러 문구, orm.xml, DTO, admin-console 문구). 다르면 코드 값을 적고 progress.md 메모에 차이를 남긴다. 룰북과 다르게 정한 것(D43 정혈 비용 없음, D45 `(꺼냄)`·내구도 없음, D42 목록의 효과 설명 없음)은 "3차는 …, 룰북 규칙은 …이 생길 때 붙인다" 로 적는다.

- [ ] **Step 1: 각 파일 수정.**
- [ ] **Step 2: 검증** — 새·바뀐 상대 링크가 있는 파일·헤딩을 가리키는지 확인, `git diff --stat` 에 `docs/spec/system/*` 와 progress.md만 있음.
- [ ] **Step 3: 커밋** — `docs(spec): 3차 이터레이션 제안 결정(아이템·영석·상점·소지품·바닥·주기·귀속·메시지) 반영`.

**완료 조건:** D32~D56 각각이 스펙 어딘가에 `(제안)` 으로 있고, 3차 열린 질문 모두에 답이 붙음.
**progress.md 한 줄:** 옮긴 파일과 절, 계획과 코드의 문구 차이(없으면 "없음").

---

### Task 13: README — 3차 시나리오와 명령, 도움말 대조

**권장 모델:** sonnet

**목적:** 실행·확인 안내를 3차에 맞춘다.

**Files:**
- Modify: `README.md`

**내용:**
- 모듈 소개에 3차 한 줄(아이템·영석·상점·저물대·귀속·주기).
- `## 3차 시나리오 — 아이템, 영석, 상점, 저물대·귀속, 주기`(2차 시나리오 다음): admin-console `아이템` 에서 `하급 저물대`(저물대·하급, 기준가 30)·`회춘단(연기 초)`(단약, 3)·`회기단(연기 초)`(단약, 4)·`늑대 가죽`(재료, 2)·`비검`(법기, 40) 만들기 → 방 화면 `바닥 물건` 에 `늑대 가죽` 3개 → `시작 소지품` 에 하급 저물대 1·하품 영석 30·회춘단 3·회기단 3(룰북 시작 소지품, 영수대는 아직 없음) → NPC 화면 `상점` 에 단약방·회춘단·회기단(값 빈칸) → play-client에서 새 캐릭터 `소지품`(`[소지품 3/22칸]  하품 영석 30`) → `줍기 가죽 2`·`버리기 가죽 1`·`보기 가죽` → `목록`·`사기 회춘단 2`·`팔기 회춘단 2`·`값 회춘단` → `사기`/`줍기` 로 비검을 얻어 `귀속 비검`·`버리기 비검`(거절)·`귀속 해제 비검` → 두 번째 계정을 같은 방에 들여 `주기 <이름> 회춘단 1`, `주기 <이름> 하품 5`.
- `## 되는 명령` 표에 3차 명령 줄(`소지품`·`ㅈ`, `보기 <물건>`, `줍기`, `버리기`, `주기`, `목록`, `사기`, `팔기`, `값`, `귀속`/`귀속 해제`, `확인`/`취소`)과 `도움말` 줄을 D56 문구로. 수량 문법(마지막 수, `전부`, `2.가죽`, `하품 10`) 한 줄. `보기 <무엇>` 줄 설명을 "물건은 보임, 사람·NPC는 아직" 으로. 앞부분으로 고를 수 있는 것에 물건 이름·주기 대상을 더한다.
- 브라우저 수동 확인 체크리스트에 3차 항목(바닥 물건 줄 색, 소지품 칸, 상점 목록, 상위 영석 `확인`, 귀속 표시, 주기 양쪽 메시지와 제3자 알림, 버린 물건 5분 뒤 사라짐, Tab 으로 바닥 물건 이름).
- 스키마: 기존 `pgdata` 에서 그대로 올리면 `ddl-auto=update` 가 새 테이블(`item_type`·`inventory`·`inventory_entry`·`floor_placement`·`dropped_item`·`npc_shop_item`·`world_starting_item`)과 `npc.shop_kind` 열을 더하고, 서버가 뜰 때 영석 4종을 만든다는 한 줄.

- [ ] **Step 1: 수정.** 명령 이름·문구는 `GameService.HELP`·핸들러의 실제 문구와 대조한다. play-client `COMMANDS` 가 D51과 같은지도 본다(다르면 progress.md 메모).
- [ ] **Step 2: 커밋** — `docs(infra): README에 3차 시나리오·물건 명령 반영`.

**완료 조건:** README의 명령·문구가 코드와 같고, 1·2차 내용은 유지.
**progress.md 한 줄:** README 3차 시나리오·되는 명령·수동 확인·스키마 줄, 도움말 대조 결과.

---

### Task 14: 통합 확인 — compose 한 번 빌드·실행, 스모크 3차 시나리오, 정리

**권장 모델:** opus

**목적:** 실제 컨테이너에서 3차 완료 시나리오(decisions.md 끝)를 자동으로 확인하고 디스크를 정리한다. **이 이터레이션에서 Docker를 쓰는 유일한 태스크다.**

**Files:**
- Modify: `scripts/smoke-test.mjs`, `README.md`(스모크 절 단계 목록), `docs/impl/iteration-3/progress.md`

**시나리오 추가** (2차 단계 1~15 뒤에 이어서. 이름에는 2차처럼 숫자 6자리 실행 번호 `<n>` 을 붙인다 — 숫자로 끝나는 이름은 조사 규칙상 받침 없음으로 본다(`회춘단<n>는`·`<n>를`·`<n>와`·`<n>가`), 기대 문구도 그렇게 만든다):
16. 관리자 — 아이템 종류 `하급 저물대<n>`(저물대·하급, 30), `회춘단<n>`(단약, 3), `늑대 가죽<n>`(재료, 2, 묘사 `거칠고 질긴 늑대의 가죽이다.`), `비검<n>`(법기, 40) 만들기. `GET /api/admin/item-types` 앞 넷이 `하품 영석`~`극품 영석`.
17. 관리자 — 광장 바닥에 `늑대 가죽<n>` 3개(재등장 없음) → `GET /rooms/{광장}` 의 `floorItems[].remaining == 3`.
18. 관리자 — 지금 시작 소지품을 읽어 두고(끝에 되돌림) `[하급 저물대<n> 1, 하품 영석 30, 회춘단<n> 3, 비검<n> 1]` 로 저장. 칸 넘침 요청(저물대 빼고 같은 것) → 400 `detail` `시작 소지품이 칸을 넘습니다. (필요 3칸, 칸 2)`.
19. 관리자 — NPC `상인<n>` 을 광장에 배치하고 상점 단약방 `[회춘단<n> 값 빈칸]` → 응답 `shop.items[0].effectivePrice == 3`. 값 1로 저장 → 400 `detail` `판매가는 매입가의 2배보다 커야 합니다: 회춘단<n> (매입가 0.75, 판매가 1)`. 쓰이는 `회춘단<n>` 삭제 → 409.
20. 새 플레이어 C(새 계정) 접속 → 캐릭터 생성 → `소지품` → `inventory.usedSlots == 3`, `totalSlots == 22`, `stones == [{하품 영석, 30}]`, text 첫 줄 `[소지품 3/22칸]  하품 영석 30`. 2차 플레이어 B도 다시 접속(광장).
21. C `보기` → `room.items` 에 `{늑대 가죽<n>, 3}`. C `줍기 늑대 가죽<n> 2` → `늑대 가죽<n> 2개를 주웠습니다.`, B가 notice event `pick`. C `버리기 늑대 가죽<n> 1` → `늑대 가죽<n> 1개를 버렸습니다.`, B가 event `drop`. C `보기` → `{늑대 가죽<n>, 2}`(배치 1 + 버린 1 묶음). C `보기 늑대 가죽<n>` → `늑대 가죽<n> — 거칠고 질긴 늑대의 가죽이다.`
22. C `목록` → `[상인<n> 단약방]` 과 `  1. 회춘단<n>  3`, `사 주는 것: 단약·영약`. C `사기 회춘단<n> 2` → `회춘단<n> 2개를 샀습니다. (하품 영석 -6)` → `소지품` stones 24. C `팔기 회춘단<n> 2` → `회춘단<n> 2개를 팔았습니다. (하품 영석 +1)` → 25. C `값 회춘단<n>` → `회춘단<n> 1개에 하품 영석 0.75 — 3개면 2`. C `팔기 늑대 가죽<n>` → `상인<n>: "그런 물건은 받지 않소." (재료는 재료상에 팔 수 있습니다)`.
23. C `귀속 비검<n>` → `비검<n>에 정혈을 한 방울 떨어뜨린다. 비검<n>가 당신에게 귀속되었습니다.` → `소지품` items 에 `{비검<n>, bound: true}`. C `버리기 비검<n>` → `귀속된 물건은 버릴 수 없습니다.`, `팔기 비검<n>` → `상인<n>: "귀속된 물건은 사 줄 수 없소."`, `주기 <B> 비검<n>` → `귀속된 물건은 남에게 줄 수 없습니다.`. C `귀속 해제 비검<n>` → `비검<n>와 이어진 정혈의 끈을 끊었습니다. 귀속이 풀렸습니다.`
24. C `주기 <B> 회춘단<n> 1` → C `<B>에게 회춘단<n> 1개를 주었습니다.`, B `system` tone `item` `<C>{이/가} 회춘단<n> 1개를 건넸습니다.`. C `주기 <B> 하품 5` → B `소지품` stones `[{하품 영석, 5}]`. B는 저물대 없이 2칸이므로 C `주기 <B> 늑대 가죽<n> 1` → `<B>의 저물대에 자리가 없습니다.`. C `주기 없는사람 회춘단<n>` → `여기에 없는사람은 없습니다.`. C `도움말` 에 `주기 <대상> <물건> [수량]`·`귀속 해제 <물건>`.
25. 관리자 — 18에서 읽어 둔 시작 소지품으로 되돌린다.

- [ ] **Step 1: 스크립트 수정** — 2차 단계는 그대로 두고 위 단계를 더한다(Node 26 내장 `fetch`·`WebSocket` 만).
- [ ] **Step 2: 모듈 검증** — 세 모듈 검증 명령을 다시 돌려 통과 확인(`./gradlew build`, 두 프론트 `npm ci && npm test && npm run build`).
- [ ] **Step 3: compose 한 번** — 시작 전 `df -h .` 와 `docker system df` 를 progress.md 메모에 적는다. 2차 Task 10과 같은 방식(`.env` 는 고치지 않고 `up` 때만 셸 값으로 `MUD_DEV_LOGIN_ENABLED=true`, `MUD_ADMIN_EMAILS=<기존>,smoke-admin@example.com` 을 덮어씀)으로 `docker compose build` **한 번** → `docker compose up -d` → `source "$NVM_DIR/nvm.sh" && nvm use 26.13.1 && node scripts/smoke-test.mjs` → `SMOKE OK`. 실패하면 원인을 고치고(해당 모듈 테스트 통과) 바뀐 서비스만 `docker compose up -d --build <서비스>` 로 다시 올린다(전체 재빌드 반복 금지). 스모크가 바꾼 시작 방은 끝난 뒤 2차처럼 원래 방으로 되돌린다. `ENOSPC` 면 멈추고 보고.
- [ ] **Step 4: 브라우저 확인(가능하면)** — README 3차 체크리스트. 못 하면 남은 수동 확인으로 적는다.
- [ ] **Step 5: 정리** — `docker compose down`(볼륨 남김) → `docker image prune -f`(dangling만) → `docker builder prune -f` → `world-server/build`, `play-client/{out,.next}`, `admin-console/{out,.next}` 삭제(`git check-ignore` 로 먼저 확인). 이름 붙은 `mud-mk3-*` 이미지·`pgdata`·`node_modules` 는 지우지 않는다. 정리 뒤 `df -h .`·`docker system df` 를 메모에 적는다.
- [ ] **Step 6: progress.md** — 결과(통과 여부, 고친 것, 남은 수동 확인), 상태를 "3차 이터레이션 구현 완료" 로.
- [ ] **Step 7: 커밋** — `test(infra): 3차 스모크 시나리오(아이템·바닥·시작 소지품·상점·귀속·주기)`.

**완료 조건:** compose 환경에서 단계 1~25 `SMOKE OK`, 세 모듈 검증 통과, 정리 뒤 dangling 이미지 없음.
**progress.md 한 줄:** 스모크 단계 16~25, compose 빌드 한 번, 디스크 전후 값, 고친 것.

**완료 시나리오 대응** (decisions.md 끝 → 스모크 단계): 관리자 로그인 1 · 아이템 종류 정의 16 · 바닥 배치 17 · 시작 소지품 18 · 상점 역할 19 · 새 캐릭터 시작 소지품·영석 20 · 줍기·버리기 21 · 물건 보기 21 · 사기·팔기(영석 증감) 22 · 저물대(칸 22, 받는 쪽 2칸 거절)·귀속 20·23·24 · 같은 방 다른 플레이어에게 주기 24.

---

## 태스크 의존 순서

```
1 → 2 ─────────────────────────┐
1 → 3 → 4 ──────────────┐      ├→ 9 (2·4)
1 → 5 ──────────┐       │      │
1 → 6 ──────────┼→ 7 → 8 ──────┼→ 10 (5·6)
   (3·5 → 7)    │  (4·6 → 8)   │
                                └→ 11 (8) → 12 → 13 → 14
```

- 순서대로 하면 된다: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14.
- 9(admin-console 아이템·시작 소지품)는 2·4 뒤, 10(바닥·상점 화면)은 5·6 뒤에 시작할 수 있다. 11(play-client)은 8이 끝나야 실제 메시지와 맞출 수 있다. 12·13은 구현된 값을 옮기므로 11 뒤에 한다. 14는 마지막.
- Task 3~6은 모두 `ItemTypeRepository`·`ItemTypeModifyService.isInUse` 를 고친다. 차례로 하면 충돌하지 않는다(나란히 하지 않는다).

## 2차에서 미룬 Minor

**3차에서 함께 고치는 것** (3차 작업과 직접 부딪침):
- `Npc` 도메인이 인사말 길이·낱말 수를 검사하지 않음, `NpcSaveInfo` 의 쓰이지 않는 검증 애노테이션, 쓰이지 않는 `NpcRepository.existsByName`·`existsByNameAndIdNot` → Task 6(상점 역할로 `Npc`·`NpcRepository` 를 고치므로).

**그대로 둠** (3차 작업과 부딪치지 않음):
- 드문 동시 편집 경합 2건이 409 대신 500(성 삭제↔지역 소유 지정, NPC 이름 대소문자). 3차의 아이템 종류 지우기 경합은 D49의 DB 제약 위반 409 처리로 막지만, 2차 두 건의 원인 코드는 고치지 않는다.
- NPC 목록 방 조회 N+1, 지역 화면이 성·종문 목록 실패 때 함께 안 보임, `RoomPicker` 단독 테스트 없음(3차 화면은 `RoomPicker` 를 새로 쓰지 않음), play-client Tab 순서·`kind` 완전성 검사, 스모크 `runNo` 중복 가능성, 방 삭제 테스트가 남은 roomId 상태를 직접 재현하지 않음.
- 1차에서 이월한 것(이웃한 방 동시 삭제 500, 자동 스크롤, 중간 401 처리, 세션 `secure`·DB화, Flyway, 로그아웃 뒤 WebSocket 유지)도 그대로.
