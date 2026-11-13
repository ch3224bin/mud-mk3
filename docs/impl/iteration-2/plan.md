# 2차 이터레이션 구현 계획 — 성·종문 정의 + 지역 소유 + NPC 배치 + 기본 대화

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 관리자가 admin-console에서 성·종문(이름·묘사)을 정의하고 지역마다 소유(성 / 종문 / 없음)를 지정하며, 유니크 NPC를 만들어 대사(인사말·낱말별 대답·기본 대답)와 LLM 켬/끔 값을 넣고 방 하나에 배치한다. 플레이어는 play-client에서 방 머리의 소유 표시(`[청석성]`)와 방의 NPC를 보고, `대화 <NPC> [낱말]` 로 NPC와 정해진 대사로 대화한다.

**Architecture:** 1차 구조를 그대로 넓힌다. `world-server`(헥사고날)에 슬라이스 `faction`(성·종문)과 `npc` 를 더하고, `zone` 이 `faction` 을, `npc` 가 `room` 을, `character`·`game` 이 `npc` 를 쓴다(순환 없음). 성·종문·NPC는 PostgreSQL에 두고, 게임은 1차처럼 명령마다 DB에서 읽으므로 관리 변경이 바로 반영된다. NPC의 배치 방은 `room_id`(FK 없음)로 두고, 방을 지우면 같은 트랜잭션에서 배치가 풀린다. play-client에는 WebSocket `room` 메시지의 `owner`·`occupants[kind=npc]` 와 새 메시지 `talk` 가 생긴다. 서버 틱·LLM 호출은 없다.

**Tech Stack:** 1차와 같다 — Java 25 · Spring Boot 4.1.2 · Gradle 9.9.1 Kotlin DSL · PostgreSQL 18.6 / H2(테스트) · Next.js 16.4.6 · React 19.3.1 · TypeScript 7.0.2 · Vitest 5.1.0 · Docker Compose. **새 의존성 없음.**

**Spec:** 아래 `## Spec` 참고. 계획은 스펙을 근거로 하고, 스펙이 정하지 않은 것(2차 열린 질문 포함)은 `## 제안 결정 목록` 에서 정했다.

---

## Spec

- 시스템 스펙: `docs/spec/system/index.md`(`### 2차 범위` 와 `### 2차 이터레이션 열린 질문`), `npc.md`(생성과 배치, 고유 대사, 캐릭터 챗), `world-structure.md`(성 · 종문 소유), `admin-tool.md`(관리 화면에서 하는 일, 1차 화면·REST API), `command.md`(1차 명령어, 2차 명령어), `platform.md`(1차 WebSocket 메시지, 1차 월드 상태와 저장, 저장소 구성)
- 룰북(게임 규칙): `docs/spec/game-rule/npc.md`(말 걸기, 유니크 NPC), `docs/spec/game-rule/commands.md`(공통 규칙·대상 지정, NPC · 상점 · 거래의 `대화 <NPC> [낱말]`, NPC 말 문체 `이름: "…"`), `docs/spec/game-rule/world.md`(방 보기 — 대상 목록은 플레이어 → 유니크 NPC 순, 방 이름 줄 뒤 두 칸 띄우고 덧붙임)
- 1차 계획·진행: `docs/impl/iteration-1/plan.md`(D1~D18), `docs/impl/iteration-1/progress.md`(리드 판정·미룬 Minor)
- 용어: `docs/spec/glossary.md`
- world-server 코딩 규칙: `.claude/rules/*.md` (README, build, architecture, domain-model, application, adapter, persistence, testing, code-style). **world-server 태스크는 시작 전에 모두 읽는다.**
- 2차 범위 밖(만들지 않는다): 조건으로 열리는 낱말, NPC가 알아서 말하기·서버 틱, LLM 호출·캐릭터 챗, NPC 장기 메모리, 동료 합류, NPC 역할(상점·퀘스트 등)과 역할로 이어지는 낱말, 몹·전투, 퀘스트·공적, 성·종문 가입, NPC 소속, 같은 NPC 여러 마리, 돌아다니는 NPC, `보기 <대상>` 자세히 보기, 대상 번호(`2.왕씨`), 안전 장소 규칙.

## Global Constraints

모든 태스크의 요구 사항에 이 절과 `## 제안 결정 목록` 이 포함된다. 태스크를 받은 구현자는 이 두 절을 먼저 읽는다.

**버전 (추측 금지)** — 1차 계획 `docs/impl/iteration-1/plan.md` 의 `## Global Constraints` 버전 표를 그대로 쓴다. 이 이터레이션은 **새 라이브러리·플러그인·이미지를 넣지 않는다.** `build.gradle.kts`, `package.json`, `package-lock.json`, Dockerfile, `compose.yaml` 의 버전 줄을 바꾸지 않는다. 꼭 필요해 보이면 멈추고 리드에게 보고한다.

**로컬 환경 (1차와 같음)**

- macOS arm64, 기본 JDK Temurin 25. world-server는 항상 `./gradlew`.
- 프론트 태스크는 Node 26.13.1: 명령 앞에 `source "$NVM_DIR/nvm.sh" && nvm use` (모듈의 `.nvmrc`).
- **디스크가 모자라다(남은 공간 약 6GB).** Docker 빌드·실행은 **Task 10에서 한 번만** 한다. Task 1~9는 Docker 명령(`docker build`, `docker compose build/up`)을 쓰지 않는다. `ENOSPC`(No space left on device)가 나면 아무것도 지우지 말고 멈춰 리드에게 보고한다.

**저장소·규칙**

- 모듈·루트 파일 구성은 1차와 같다(`world-server/`, `play-client/`, `admin-console/`, 루트 `compose.yaml`·`.env.example`·`README.md`·`scripts/`).
- world-server 코딩 규칙은 `.claude/rules/*.md` 를 따른다. 루트 패키지는 `mudmk3`. 아래 경로는 `world-server/src/{main,test}/java/mudmk3/` 기준이다.
- open-in-view가 꺼져 있다(`spring.jpa.open-in-view=false`). REST 어댑터는 트랜잭션 밖에서 DTO를 만들므로, 응답에 쓰는 지연 연관(`Zone.owner` 등)은 리포지토리 `@EntityGraph` 로 미리 읽는다(1차 `RoomRepository` 관례).
- Next.js 앱 규칙은 1차와 같다(정적 export, `"use client"`, 상대 경로 import, 쿼리스트링 id).
- 화면 문구·서버 메시지·예외 메시지는 한국어. 룰북 문장을 그대로 쓰고, 룰북에 없는 문구는 이 계획에 적힌 문장을 **글자 그대로** 쓴다.
- 자동 테스트는 Google 자격 증명·네트워크 없이 돈다.

**커밋**

- 태스크 하나 = 커밋 하나 이상. 구현자는 커밋만 하고 **push 하지 않는다**(리드가 한다). 구현자는 서브에이전트를 띄우지 않는다.
- 커밋 메시지: `<type>(<module>): <한국어 요약>` (type: feat, test, build, docs, chore, fix / module: world-server, play-client, admin-console, infra, spec).
- 커밋 메시지 끝에 빈 줄 다음 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` 를 붙인다.
- 태스크를 끝내면 `docs/impl/iteration-2/progress.md` 의 표에 한 줄(태스크 번호·제목, 커밋 해시는 리드가 채움, 메모)을 더하고 같은 커밋에 넣는다.

**검증 명령 (각 태스크 완료 조건에 포함)**

- world-server: `cd world-server && ./gradlew build` (테스트 + SpotBugs + ArchUnit 통과)
- play-client / admin-console: `cd <module> && source "$NVM_DIR/nvm.sh" && nvm use && npm ci && npm test && npm run build`

## 제안 결정 목록

스펙이 정하지 않아 이 계획에서 정한 것. 번호는 1차(D1~D18)에 이어 붙인다. Task 8이 `docs/spec/system/` 해당 문서에 `(제안)` 표시로 옮긴다.

**D19. 성·종문 모델** — 도메인 `Faction`(슬라이스 `faction`): 종류 `FactionKind { CASTLE("성"), SECT("종문") }`, 이름(1~30자, 앞뒤 공백 자름, **성과 종문을 통틀어 유일** — 방 머리 `[청석성]` 이 무엇을 가리키는지 헷갈리지 않게), 묘사(0~500자, null은 `""`). 성과 종문은 종류만 다르고 같은 틀이다. 2차에 가입·퀘스트·안전 장소 등 규칙은 없다.

**D20. 지역 소유** — `Zone` 에 `owner`(성·종문 하나 또는 없음, `zone.owner_id` FK nullable)를 더한다. 소유는 지역 저장 요청의 `ownerId`(null이면 없음)로 정한다. 없는 `ownerId` 는 400 `성·종문을 찾을 수 없습니다. id: 9`. **소유한 지역이 있는 성·종문은 지울 수 없다**(409 `소유한 지역이 있는 성·종문은 지울 수 없습니다`). 성·종문의 이름을 바꾸면 그 소유 지역의 방 머리 표시도 다음 방 보기부터 바뀐다.

**D21. NPC 항목** — 도메인 `Npc`(슬라이스 `npc`). 2차 NPC는 모두 유니크 NPC다.
- 이름: 띄어쓰기 없이 1~12자(`^\S{1,12}$`), NPC끼리 유일(`UK_NPC_NAME`). 띄어쓰기를 막는 까닭: `대화 <NPC> <낱말>` 을 공백으로 나누기 때문이다.
- 인사말: 1~500자. 기본 대답(모르는 낱말에): 1~500자 — admin-console 새 NPC 폼의 기본값은 룰북 예 `글쎄, 잘 모르겠구려.`.
- 낱말 대답 목록: 최대 30개, 입력 순서 유지. 낱말은 띄어쓰기 없이 1~20자이고 앞뒤 `[` `]` 는 떼고 저장한다. 같은 NPC 안에서 같은 낱말이 둘이면 400 `같은 낱말이 두 번 있습니다: 단약`. 대답은 1~1000자.
- LLM 대화 켬/끔(`llmEnabled`, 기본 끔): 저장만 한다.
- 배치 방(`roomId`, 없으면 배치 안 됨 = 세계에 없음).
- 2차에 넣지 않는 항목: 한 줄 묘사·자세히 볼 때 묘사·호칭(`보기 <대상>` 이 생길 때), LLM용 캐릭터 설정 칸(성격·말투·배경 — 3차 LLM 이터레이션으로 미룸), 소속 성·종문(가입·종문 규칙이 생길 때).

**D22. 배치와 방 삭제** — NPC는 방 하나에 고정한다. `npc.room_id` 는 bigint nullable이고 방 FK를 두지 않는다(1차 `player_character.room_id` 와 같은 까닭). NPC가 있는 방을 지우면 **배치만 풀린다**(NPC는 남고 `roomId=null`, 관리 목록에 `배치 안 됨`). 방 삭제와 같은 트랜잭션에서 `RoomDeletedEvent` 를 동기 `@EventListener` 로 받아 푼다(방 삭제가 롤백되면 같이 롤백). 시작 방 삭제 금지 등 1차 규칙은 그대로.

**D23. 캐릭터 이름과 NPC 이름** — 1차에서 미룬 "NPC 이름 검사"를 한다: 캐릭터를 만들 때 이름이 NPC 이름과 같으면 1차 중복 문구 그대로 `이미 있는 이름입니다. 다른 이름을 정하십시오.`. 반대로 NPC를 만들 때 캐릭터 이름과의 중복은 검사하지 않는다(`대화` 는 NPC만 가리키므로 헷갈리지 않는다). 금지어 검사는 여전히 없다.

**D24. 방 보기 — 소유 머리와 NPC 목록**
- `room` 메시지에 `owner` 를 더한다: `{kind: "castle" | "sect", name}` 또는 `null`(지역 소유 없음).
- `occupants` 에 NPC를 더한다: 플레이어(들어온 순서) 다음에 그 방에 배치된 NPC(만든 순서 = id 순)를 `{name, kind: "npc"}` 로. 룰북 대상 목록 순서(플레이어 → 유니크 NPC)를 따른다.
- 클라이언트 그림(D8 갱신): 소유가 있으면 방 이름 줄 뒤에 두 칸 띄우고 `[소유 이름]` 을 붙인다(룰북 `청석숲 오솔길  (영기: 보통)` 처럼 방 이름 줄에 덧붙이는 모양).
```
◇ 청석숲                 ← zoneEntered일 때만 (1차와 같음)
청석성 남문  [청석성]     ← owner 있을 때만 "  [이름]"
돌로 쌓은 성문이 우뚝 서 있다.
출구: 북 남
한립                     ← occupant player (style occupant)
왕씨                     ← occupant npc (style npc)
```
- NPC가 방에 생기거나 사라져도(관리 변경) 같은 방 사람에게 알림은 없다. 다음 방 보기에 보인다.

**D25. `대화` 명령** (룰북 `대화 <NPC> [낱말]`)
- 해석: 첫 낱말이 `대화` 면 다음 낱말이 NPC, 그 뒤 나머지 전부가 낱말(앞뒤 `[` `]` 를 뗌). `Command.Talk(@Nullable String target, @Nullable String word)`. 줄임말 없음. `말` 은 1차처럼 알 수 없는 명령이다.
- 대상 찾기: 같은 방에 배치된 NPC 가운데서만(플레이어는 대상이 아니다). 이름 전체가 같은 NPC, 없으면 이름이 입력으로 시작하는 NPC 가운데 방 보기 순서(id 순) 첫째. 영문은 대소문자를 가리지 않는다. 번호(`2.왕씨`)는 2차에 없다.
- 낱말 찾기: 저장된 낱말과 글자 그대로 같을 때(영문 대소문자 무시). 앞부분 일치는 하지 않는다(룰북: 앞부분 일치는 대상·물건 이름에만).
- 출력은 **묻는 플레이어에게만** 간다(같은 방 다른 사람은 보지 않는다). 새 메시지 `talk`(D26).
  - `대화 왕씨` → `왕씨: "어서 오시오."` 그리고 낱말이 있으면 다음 줄 `물어볼 수 있는 낱말: [단약] [일거리]`(입력 순서, 낱말마다 `[ ]`, 사이 공백 하나). 낱말이 없으면 첫 줄만.
  - `대화 왕씨 일거리` → `왕씨: "<그 낱말의 대답>"`.
  - 모르는 낱말 → `왕씨: "<기본 대답>"`.
  - 오류(`system/error`): `대화` 만 → `누구에게 말을 거시겠습니까?` / 같은 방에 맞는 NPC가 없음 → `여기에 그런 상대는 없습니다.`
- LLM 켬 NPC도 같은 대사로 답한다(2차는 LLM을 부르지 않는다).
- `보기 <NPC>` 는 1차 그대로 `아직 자세히 볼 수 없습니다.` 다.
- `도움말` 문구: `명령: 보기(ㅂ) · 동 서 남 북 북동 북서 남동 남서 위 아래 (e w s n ne nw se sw u d) · 말하기(') · 대화 <NPC> [낱말] · 누구 · 도움말 · 종료`
- 위 문구 가운데 `물어볼 수 있는 낱말:`, `누구에게 말을 거시겠습니까?`, `여기에 그런 상대는 없습니다.` 는 룰북에 아직 없다.

**D26. WebSocket 메시지 변경** (1차 D5에 더함, 다른 메시지는 그대로)

| type | 필드 | 뜻 |
|---|---|---|
| `room` | 1차 필드 + `owner`: `{kind: "castle" \| "sect", name}` \| `null`. `occupants[].kind`: `"player"` \| `"npc"` | 방 보기 |
| `talk` | `npc`(NPC 이름), `word`(물은 낱말, 인사면 `null`), `text`(그린 줄들, `\n` 으로 나뉨), `tone`: `"talk"` | NPC 대답(묻는 사람에게만) |

- 새 `tone` 값 `talk`. 클라이언트 스타일 이름에 `npc`(방의 NPC 줄)·`owner` 는 쓰지 않는다 — 소유는 방 이름 줄에 붙으므로 `room-name` 스타일을 그대로 쓴다.

**D27. 관리 REST API 추가** (1차 D12 규칙 그대로: `/api/admin/**`, ADMIN, 변경에 `X-MUD-Client: 1`, `ProblemDetail`, 검증 실패·잘못된 값·없는 id 400, 이름 중복·규칙 충돌 409)

| 메서드 · 경로 | 요청 | 응답 |
|---|---|---|
| `GET /api/admin/factions` | — | 200 `FactionResponse[]` (종류 성 → 종문, 같은 종류는 이름순) |
| `POST /api/admin/factions` | `{kind, name, description}` | 201 `FactionResponse` |
| `PUT /api/admin/factions/{factionId}` | `{kind, name, description}` | 200 `FactionResponse` |
| `DELETE /api/admin/factions/{factionId}` | — | 204. 소유 지역이 있으면 409 |
| `POST /api/admin/zones`, `PUT /api/admin/zones/{zoneId}` | 1차 `{name, description}` + `ownerId`(number \| null, 빠지면 null) | 1차와 같음 |
| `GET /api/admin/npcs` | — | 200 `NpcSummaryResponse[]` (이름순) |
| `POST /api/admin/npcs` | `NpcSaveRequest` | 201 `NpcDetailResponse` |
| `GET /api/admin/npcs/{npcId}` | — | 200 `NpcDetailResponse` |
| `PUT /api/admin/npcs/{npcId}` | `NpcSaveRequest` (낱말 목록은 통째로 바꿈) | 200 `NpcDetailResponse` |
| `DELETE /api/admin/npcs/{npcId}` | — | 204 |

- `kind` 값은 `CASTLE` \| `SECT`.
- `FactionResponse {id, kind, kindName, name, description, zoneCount}` (`kindName` 은 `성`·`종문`).
- `ZoneResponse` 에 `ownerId, ownerName, ownerKind`(`CASTLE`·`SECT`, 없으면 셋 다 null)를 더한다.
- `NpcSaveRequest {name, greeting, defaultAnswer, topics: [{word, answer}], llmEnabled, roomId}` (`roomId` null = 배치 안 함, `topics` 빠지면 빈 목록).
- `NpcSummaryResponse {id, name, llmEnabled, topicCount, roomId, roomName, zoneName}` (배치 안 됐으면 room 셋 null).
- `NpcDetailResponse {id, name, greeting, defaultAnswer, topics: [{word, answer}], llmEnabled, roomId, roomName, zoneId, zoneName}`.
- `RoomDetailResponse` 에 `npcs: [{id, name}]`(그 방에 배치된 NPC, id 순)를 더한다.
- 예외 문구: `이미 있는 성·종문 이름입니다: 청석성`(409), `소유한 지역이 있는 성·종문은 지울 수 없습니다`(409), `성·종문을 찾을 수 없습니다. id: 9`(400), `이미 있는 NPC 이름입니다: 왕씨`(409), `같은 낱말이 두 번 있습니다: 단약`(400), `NPC를 찾을 수 없습니다. id: 9`(400), 배치 방이 없으면 1차 `RoomFinder` 문구(400).

**D28. admin-console 화면 추가** (1차 D14에 더함)
- 위쪽 공통 바 링크: `지역 목록` 옆에 `성·종문`, `NPC`.
- `/factions/` 성·종문 목록(종류·이름·소유 지역 수·수정·삭제) + 만들기 폼(종류 선택 `성`/`종문`, 이름, 묘사).
- `/zones/` 지역 만들기·수정 폼에 `소유` 선택(`없음` + 성·종문 목록 `청석성 (성)`), 목록에 `소유` 열(없으면 `-`).
- `/npcs/` NPC 목록(이름·배치(`청우촌 / 마을 어귀` 또는 `배치 안 됨`)·낱말 수·LLM(`켬`/`끔`)) + `NPC 만들기` 링크(`/npc/`).
- `/npc/` (id 없음 = 새로 만들기, `/npc/?id=4` = 고치기): 이름, 인사말(textarea), 기본 대답(textarea, 새 NPC 기본값 `글쎄, 잘 모르겠구려.`), 낱말 표(낱말·대답·`삭제` 버튼) + `낱말 추가` 버튼, `LLM 대화` 체크박스와 안내 `켜 두어도 지금은 정해진 대사로 답합니다.`, 배치(지역 선택 → 방 선택, 첫 항목 `배치 안 함`), `저장`, (고칠 때) `삭제`(확인 `confirm()`). 만들기 성공 시 `/npc/?id=<새 id>` 로.
- `/room/?id=` 방 화면에 `이 방의 NPC` 목록(이름 링크 → `/npc/?id=`, 없으면 `없음`).

**D29. play-client** (1차 D8·D18에 더함)
- 방 그림은 D24. NPC 줄은 style `npc`(CSS `.line-npc`, 플레이어 줄과 다른 색). `talk` 는 `text` 를 줄로 나눠 style `talk`(CSS `.line-talk`).
- 자동완성 명령 목록에 `대화` 를 `말하기` 다음에 넣는다. 이름 후보는 마지막 `room.occupants` 전부(플레이어 + NPC)이고, `notice` 로 플레이어만 더하고 뺀다(1차 그대로).

**D30. 슬라이스와 의존** — 새 애그리거트(슬라이스): `faction`, `npc`. 의존: `zone → faction`, `room → zone`, `world → room`, `npc → room`, `character → world, account, npc`, `game → character, room, world, npc`. 순환 없음. 성·종문의 소유 지역 수는 `FactionRepository` 의 JPQL로 센다(`faction → zone` 의존을 만들지 않게 — 1차 `ZoneRepository.countRooms` 관례).

**D31. 관리 변경 반영** — 성·종문·지역 소유·NPC(대사·배치)의 변경은 1차 맵처럼 실행 중 게임에 바로 반영된다(게임이 명령마다 DB에서 읽음). 스키마는 1차처럼 `ddl-auto=update` 로 새 테이블·`zone.owner_id` 열이 생긴다(마이그레이션 도구는 여전히 없음).

## Review Focus

스펙이 직접 말하지 않지만 실제로 부딪칠 만한 것. 각 줄의 테스트는 담당 태스크에 들어 있다.

1. NPC가 배치된 방을 지움 → 방 삭제 204, NPC는 남고 `roomId=null`, 그 NPC를 다시 다른 방에 배치할 수 있다. 방 삭제가 시작 방이라 409면 NPC 배치도 그대로. (Task 4 `deleteRoomUnplacesNpcs`, `deleteStartRoomKeepsNpcPlacement`)
2. 소유 지역이 있는 성·종문 삭제 → 409이고 아무것도 바뀌지 않음, 소유를 `없음` 으로 바꾼 뒤에는 지워짐. (Task 1 `deleteOwningFactionFail`)
3. open-in-view 꺼짐 + 지연 연관: `GET /api/admin/zones`·`/factions`·`/npcs`·`/rooms/{id}` 가 실제 트랜잭션 밖 직렬화에서 `LazyInitializationException` 없이 소유·배치 이름을 낸다. (Task 2·4 `AdminApiIntegrationTest` 확장)
4. 낱말 목록을 순서만 바꿔 저장하거나 같은 낱말을 지웠다 다시 넣어도 DB 오류가 없고 순서가 유지됨. (Task 3 `NpcRepositoryTest.replaceTopicsKeepsOrder`)
5. `대화 <플레이어 이름>`·다른 방 NPC·배치 안 된 NPC → `여기에 그런 상대는 없습니다.`; NPC 이름과 같은 캐릭터 이름 → 거절. (Task 5 `talkToPlayerOrAbsentNpcFail`, Task 4 `createCharacterWithNpcNameFail`)
6. 관리자가 지역 소유·NPC 배치를 바꾼 직후 게임의 다음 `보기` 에 반영. (Task 5 `ownerAndNpcChangesShowOnNextLook`)

---

## 파일 구조 (요약, 새로 생기거나 바뀌는 것)

```
world-server/src/main/java/mudmk3/
  domain/faction/{Faction,FactionKind,FactionSaveInfo,DuplicateFactionNameException}.java      (새)
  domain/zone/{Zone,ZoneSaveInfo}.java                                                        (owner)
  domain/npc/{Npc,NpcTopic,NpcSaveInfo,DuplicateNpcNameException}.java                         (새)
  domain/game/{Command,CommandParser}.java                                                    (Talk)
  application/faction/{FactionQueryService,FactionModifyService}.java, provided/{FactionFinder,FactionEditor,FactionSaveRequest,FactionInUseException}.java, required/FactionRepository.java  (새)
  application/zone/...                                                                        (ownerId)
  application/npc/{NpcQueryService,NpcModifyService}.java, provided/{NpcFinder,NpcEditor,NpcSaveRequest,NpcTopicRequest}.java, required/NpcRepository.java  (새)
  application/character/{CharacterQueryService,CharacterModifyService}.java                   (NPC 이름 검사)
  application/game/{GameService,RoomViewAssembler}.java, required/GameMessage.java            (owner·npc·talk)
  adapter/webapi/{FactionApi,NpcApi,ZoneApi,RoomApi,ApiControllerAdvice}.java, dto/{FactionResponse,NpcSummaryResponse,NpcDetailResponse,NpcTopicResponse,RoomNpcResponse,ZoneResponse,RoomDetailResponse}.java
  resources/META-INF/orm.xml
admin-console/  app/{factions,npcs,npc}/page.tsx, app/{zones,room}/page.tsx, src/lib/api.ts, src/components/{TopBar,ZoneForm,FactionForm,NpcForm,TopicEditor,RoomPicker}.tsx
play-client/    src/lib/{protocol,render,complete}.ts, src/components/Terminal.tsx, app/globals.css
docs/spec/system/*.md  README.md  scripts/smoke-test.mjs
```

---

### Task 1: 성·종문 도메인·영속과 지역 소유 — 관리 서비스까지

**권장 모델:** opus

**목적:** D19·D20·D30의 `faction` 슬라이스와 `Zone.owner` 를 도메인·JPA 매핑·리포지토리·provided 포트(서비스)까지 만든다. REST는 Task 2.

**Files:**
- Create: `domain/faction/{Faction,FactionKind,FactionSaveInfo,DuplicateFactionNameException}.java`
- Modify: `domain/zone/{Zone,ZoneSaveInfo}.java`(소유), `src/main/resources/META-INF/orm.xml`
- Create: `application/faction/required/FactionRepository.java`, `application/faction/provided/{FactionFinder,FactionEditor,FactionSaveRequest,FactionInUseException}.java`, `application/faction/{FactionQueryService,FactionModifyService}.java`
- Modify: `application/zone/provided/ZoneSaveRequest.java`(`@Nullable Long ownerId`), `application/zone/ZoneModifyService.java`, `application/zone/required/ZoneRepository.java`(`@EntityGraph(attributePaths = "owner")` 를 `findById`·`findAllByOrderByNameAsc` 에)
- Test: `domain/faction/{FactionTest,FactionFixture}`, `domain/zone/ZoneTest`(추가), `application/faction/required/FactionRepositoryTest`, `application/faction/provided/FactionEditorTest`, `application/zone/provided/ZoneEditorTest`(추가)

**Interfaces:**
- Produces:
  - `enum FactionKind { CASTLE("성"), SECT("종문") }` — `String koreanName()`. 선언 순서가 목록 순서.
  - `record FactionSaveInfo(@NotNull FactionKind kind, @NotBlank @Size(max=30) String name, @Size(max=500) @Nullable String description)`.
  - `Faction`(`@Entity`, `AbstractEntity`): `static Faction create(FactionSaveInfo)`, `void update(FactionSaveInfo)`, getter `getKind()`, `getName()`(앞뒤 공백 자름), `getDescription()`(null → `""`).
  - `DuplicateFactionNameException(String message)`.
  - `Zone`: 필드 `@Nullable Faction owner`; `static Zone create(ZoneSaveInfo, @Nullable Faction owner)`, `void update(ZoneSaveInfo, @Nullable Faction owner)`, `Optional<Faction> findOwner()`. (1차 `create(ZoneSaveInfo)` 를 부르는 곳·테스트는 `create(info, null)` 로 고친다.)
  - 매핑: 테이블 `faction`(`kind` varchar(16) STRING `column-definition="varchar(16)"` not null, `name` varchar(30) not null `UK_FACTION_NAME`, `description` varchar(500) not null). `zone.owner_id` many-to-one LAZY, nullable, FK.
  - `FactionRepository extends Repository<Faction, Long>`: `save`, `Optional<Faction> findById(Long)`, `List<Faction> findAll()`, `boolean existsByName(String)`, `boolean existsByNameAndIdNot(String, Long)`, `void delete(Faction)`, `@Query("select count(z) from Zone z where z.owner.id = :factionId") long countZones(Long factionId)`.
  - `record FactionSaveRequest(@NotNull FactionKind kind, @NotBlank @Size(max=30) String name, @Size(max=500) @Nullable String description)` + `toInfo()`.
  - `FactionFinder`(`/** 성·종문을 찾는다 */`): `Faction find(Long factionId)`(없으면 `IllegalArgumentException("성·종문을 찾을 수 없습니다. id: " + id)`), `List<Faction> findAll()`(종류 선언 순서 → 이름순), `long countZones(Long factionId)`.
  - `FactionEditor`(`/** 성·종문을 만들고 고치고 지운다 */`): `Faction create(@Valid FactionSaveRequest)`, `Faction update(Long, @Valid FactionSaveRequest)`, `void delete(Long)`. 이름 중복 → `DuplicateFactionNameException("이미 있는 성·종문 이름입니다: " + name)`(자른 이름으로 비교), 소유 지역이 있으면 `FactionInUseException("소유한 지역이 있는 성·종문은 지울 수 없습니다")`.
  - `ZoneSaveRequest(@NotBlank @Size(max=30) String name, @Size(max=500) @Nullable String description, @Nullable Long ownerId)`. `ZoneModifyService` 는 `ownerId` 가 있으면 `FactionFinder.find` 로 찾아 넘긴다.

- [ ] **Step 1: 도메인 테스트** — `FactionTest`: `create`(이름 공백 자름, 묘사 null → `""`), `update`(종류 바꾸기 포함). `ZoneTest` 추가: `createWithOwner`, `updateRemovesOwner`(owner null → `findOwner()` empty).
- [ ] **Step 2: 실패 확인 → 도메인·orm.xml·리포지토리 구현.**
- [ ] **Step 3: 리포지토리 테스트(`@DataJpaTest`)** — `FactionRepositoryTest`: `duplicateNameFail`(성과 종문이 같은 이름이어도 flush 때 `DataIntegrityViolationException`), `countZones`(지역 둘이 소유하면 2), `ZoneRepositoryTest` 추가 `ownerPersisted`(flush·clear 후 `findById` 의 owner 이름).
- [ ] **Step 4: 서비스 테스트(`@ApplicationServiceTest`)** — `FactionEditorTest`: `create`, `duplicateNameFail`, `updateToOtherNameFail`, `updateKeepsOwnName`, `delete`, `deleteOwningFactionFail`(예외 + 성·종문 그대로), `deleteAfterOwnerRemoved`. `ZoneEditorTest` 추가: `createWithOwner`, `updateOwnerToNone`, `unknownOwnerFail`(`IllegalArgumentException`, 메시지 `성·종문을 찾을 수 없습니다. id: 999`). `FactionFinder.findAll` 순서: 성 `청석성`·`남원성`, 종문 `황엽곡` → `[남원성, 청석성, 황엽곡]`.
- [ ] **Step 5: 구현 → 통과.** ArchUnit이 `zone → faction` 만 있고 `faction → zone` 클래스 의존이 없음을 확인(JPQL 문자열은 의존이 아님).
- [ ] **Step 6: 검증** — `cd world-server && ./gradlew build` 통과.
- [ ] **Step 7: 커밋** — `feat(world-server): 성·종문 도메인·영속과 지역 소유`.

**완료 조건:** 위 테스트 통과, orm.xml 테이블·열·제약 이름이 위 값대로, 1차 테스트 전부 통과.

---

### Task 2: 성·종문 관리 REST API와 지역 소유 지정

**권장 모델:** opus

**목적:** D27의 성·종문 엔드포인트와 지역 요청·응답의 소유 필드를 만든다.

**Files:**
- Create: `adapter/webapi/FactionApi.java`, `adapter/webapi/dto/FactionResponse.java`
- Modify: `adapter/webapi/ZoneApi.java`, `adapter/webapi/dto/ZoneResponse.java`, `adapter/webapi/ApiControllerAdvice.java`(`DuplicateFactionNameException`·`FactionInUseException` → 409)
- Test: `adapter/webapi/FactionApiWebMvcTest`, `adapter/webapi/ZoneApiWebMvcTest`(추가), `adapter/webapi/AdminApiIntegrationTest`(추가), `adapter/security/SecurityIntegrationTest`(추가 한 줄)

**Interfaces:**
- Consumes: Task 1 포트.
- Produces: D27 표의 `/api/admin/factions` 4개와 지역 `ownerId`. `FactionResponse.of(Faction, long zoneCount)`. `ZoneResponse(Long id, String name, String description, long roomCount, @Nullable Long ownerId, @Nullable String ownerName, @Nullable String ownerKind)` — `ownerKind` 는 enum 이름(`CASTLE`·`SECT`). 잘못된 `kind` 값(`"CITY"`)은 400.

- [ ] **Step 1: 웹 테스트(`@WebMvcTest` + 보안 켠 상태, 1차 관례대로 관리자 인증·`X-MUD-Client: 1`)** — `FactionApiWebMvcTest`: `POST` 201 본문 `{id, kind:"CASTLE", kindName:"성", name, description, zoneCount:0}`, 빈 이름 400, 잘못된 kind 400, 중복 409 `detail` 이 `이미 있는 성·종문 이름입니다: 청석성`, `DELETE` 소유 지역 있음 409 `detail` 이 `소유한 지역이 있는 성·종문은 지울 수 없습니다`, `GET` 순서 그대로, 관리자 아님 403. `ZoneApiWebMvcTest` 추가: `POST` 에 `ownerId` 가 포트 요청으로 넘어감, 응답 `ownerName`·`ownerKind`, `ownerId` 없는 1차 요청 본문도 201.
- [ ] **Step 2: 실패 확인 → 구현 → 통과.**
- [ ] **Step 3: 통합 테스트** — `AdminApiIntegrationTest` 에 `manageFactionsAndOwnership`: 성 만들기 → 지역 만들기(`ownerId`) → `GET /api/admin/zones` 의 그 지역 `ownerName=="청석성"`(지연 로딩 오류 없음) → `GET /api/admin/factions` 의 `zoneCount==1` → 성 삭제 409 → 지역 소유 `null` 로 PUT → 성 삭제 204.
- [ ] **Step 4: 검증** — `./gradlew build` 통과.
- [ ] **Step 5: 커밋** — `feat(world-server): 성·종문 관리 API와 지역 소유 지정`.

**완료 조건:** 위 테스트 통과, 1차 지역 API 테스트가 그대로 통과(`ownerId` 생략 허용).

---

### Task 3: NPC 도메인과 영속

**권장 모델:** opus

**목적:** D21의 `Npc` 애그리거트(낱말 대답 포함)를 도메인 규칙·JPA 매핑·리포지토리로 만든다.

**Files:**
- Create: `domain/npc/{Npc,NpcTopic,NpcSaveInfo,DuplicateNpcNameException}.java`
- Modify: `src/main/resources/META-INF/orm.xml`
- Create: `application/npc/required/NpcRepository.java`
- Test: `domain/npc/{NpcTest,NpcFixture}`, `application/npc/required/NpcRepositoryTest`

**Interfaces:**
- Produces:
  - `record NpcTopic(String word, String answer)` — embeddable 값 객체. 컴팩트 생성자: 낱말 앞뒤 공백과 앞뒤 `[` `]` 를 떼고(`[단약]` → `단약`), 낱말이 `^\S{1,20}$` 가 아니면 `IllegalArgumentException("낱말은 띄어쓰기 없이 1~20자로 정하십시오: " + word)`, 대답이 비었거나 1000자 넘으면 `IllegalArgumentException("대답은 1~1000자로 정하십시오: " + word)`. `boolean matches(String input)` — 입력도 같은 방식으로 다듬어 `equalsIgnoreCase`.
  - `record NpcSaveInfo(@NotBlank @Pattern(regexp="^\\S{1,12}$") String name, @NotBlank @Size(max=500) String greeting, @NotBlank @Size(max=500) String defaultAnswer, @Size(max=30) List<NpcTopic> topics, boolean llmEnabled)`.
  - `Npc`(`@Entity`): `static Npc create(NpcSaveInfo)`, `void update(NpcSaveInfo)`(낱말 목록 통째로 바꿈), `void placeIn(Long roomId)`, `void unplace()`, `Optional<Long> findRoomId()`, `List<NpcTopic> getTopics()`(불변, 입력 순서), `Optional<NpcTopic> findTopic(String word)`, `String answerTo(String word)`(맞는 낱말의 대답, 없으면 기본 대답), `boolean isCalled(String input)`(이름 전체 같음, 영문 대소문자 무시), `boolean nameStartsWith(String input)`, getter `getName()`·`getGreeting()`·`getDefaultAnswer()`·`isLlmEnabled()`. 생성·변경 때 같은 낱말(대소문자 무시) 둘이면 `IllegalArgumentException("같은 낱말이 두 번 있습니다: " + word)`, 이름·인사말·기본 대답은 앞뒤 공백을 자른다.
  - `DuplicateNpcNameException(String message)` — Task 4의 서비스가 던진다.
  - 매핑: 테이블 `npc`(`name` varchar(12) not null `UK_NPC_NAME`, `greeting` varchar(500) not null, `default_answer` varchar(500) not null, `llm_enabled` boolean not null, `room_id` bigint **nullable, FK 없음**). `topics` 는 `<element-collection>` → `<collection-table name="npc_topic">`(`npc_id` join column), `<order-column name="position"/>`, embeddable `NpcTopic`(`word` varchar(20) not null, `answer` varchar(1000) not null). 낱말 유일은 도메인이 지키고 DB 유니크 제약은 두지 않는다(목록 통째 교체 때 위치 갱신 순서로 생기는 일시 충돌을 피함).
  - `NpcRepository extends Repository<Npc, Long>`: `save`, `Optional<Npc> findById(Long)`, `List<Npc> findAllByOrderByNameAsc()`, `List<Npc> findAllByRoomIdOrderByIdAsc(Long roomId)`, `boolean existsByName(String)`, `boolean existsByNameAndIdNot(String, Long)`, `boolean existsByNameIgnoreCase(String)`(캐릭터 이름 검사용), `void delete(Npc)`. 응답·게임이 트랜잭션 밖에서 낱말을 읽을 수 있게 `findById`·`findAllByRoomIdOrderByIdAsc` 에 `@EntityGraph(attributePaths = "topics")`.

- [ ] **Step 1: 도메인 테스트** — `NpcTest`: `create`(공백 자름, 기본 `llmEnabled=false` 픽스처), `topicBracketsStripped`, `duplicateTopicFail`(`단약`·`단약`, `Herb`·`herb`), `topicWithSpaceFail`(`늑대 퇴치`), `answerToKnownWord`, `answerToUnknownWordIsDefault`, `answerIgnoresBracketsAndLatinCase`(`[단약]`), `noPrefixMatchForWord`(`단` → 기본 대답), `placeAndUnplace`, `updateReplacesTopics`(순서 유지), `nameMatching`(`isCalled("왕씨")`, `nameStartsWith("왕")`, `isCalled("왕")==false`).
- [ ] **Step 2: 실패 확인 → 구현.**
- [ ] **Step 3: 리포지토리 테스트(`@DataJpaTest`)** — `NpcRepositoryTest`: `saveWithTopics`(flush·clear 후 낱말·순서 유지), `replaceTopicsKeepsOrder`(`[단약, 일거리]` → `[일거리, 단약, 길]` 로 바꿔 flush·clear 후 그 순서, 예외 없음), `duplicateNameFail`, `findAllByRoomIdOrderByIdAsc`(다른 방·배치 안 된 NPC 제외), `existsByNameIgnoreCase`.
- [ ] **Step 4: 검증** — `./gradlew build` 통과.
- [ ] **Step 5: 커밋** — `feat(world-server): NPC 도메인(대사·낱말 대답·배치)과 영속 매핑`.

**완료 조건:** 위 테스트 통과, orm.xml 이름이 위 값대로, ArchUnit 통과.

---

### Task 4: NPC 관리 서비스·REST API, 방 삭제 시 배치 풀기, 캐릭터 이름 검사

**권장 모델:** opus

**목적:** D22·D23·D27(NPC 부분)을 구현한다.

**Files:**
- Create: `application/npc/provided/{NpcFinder,NpcEditor,NpcSaveRequest,NpcTopicRequest}.java`, `application/npc/{NpcQueryService,NpcModifyService}.java`
- Create: `adapter/webapi/NpcApi.java`, `adapter/webapi/dto/{NpcSummaryResponse,NpcDetailResponse,NpcTopicResponse,RoomNpcResponse}.java`
- Modify: `adapter/webapi/RoomApi.java`, `adapter/webapi/dto/RoomDetailResponse.java`(`npcs`), `adapter/webapi/ApiControllerAdvice.java`(`DuplicateNpcNameException` → 409)
- Modify: `application/character/{CharacterQueryService,CharacterModifyService}.java`(NPC 이름 검사)
- Test: `application/npc/provided/{NpcEditorTest,NpcFinderTest}`, `application/character/provided/CharacterCreatorTest`(추가), `adapter/webapi/{NpcApiWebMvcTest,RoomApiWebMvcTest(추가),AdminApiIntegrationTest(추가)}`, `application/game/provided/GameConnectorTest`(추가 한 개)

**Interfaces:**
- Consumes: Task 3, `RoomFinder`·`RoomDeletedEvent`(1차).
- Produces:
  - `record NpcTopicRequest(@NotBlank @Size(max=22) String word, @NotBlank @Size(max=1000) String answer)`(괄호 포함 길이 여유; 정규화·규칙은 `NpcTopic`).
  - `record NpcSaveRequest(@NotBlank @Size(max=12) String name, @NotBlank @Size(max=500) String greeting, @NotBlank @Size(max=500) String defaultAnswer, @Nullable @Size(max=30) List<@Valid NpcTopicRequest> topics, boolean llmEnabled, @Nullable Long roomId)` + `toInfo()`(topics null → 빈 목록).
  - `NpcFinder`(`/** NPC를 찾는다 */`): `Npc find(Long npcId)`(없으면 `IllegalArgumentException("NPC를 찾을 수 없습니다. id: " + id)`), `List<Npc> findAll()`(이름순), `List<Npc> findAllInRoom(Long roomId)`(id 순), `boolean existsByName(String name)`(대소문자 무시).
  - `NpcEditor`(`/** NPC를 만들고 고치고 배치하고 지운다 */`): `Npc create(@Valid NpcSaveRequest)`, `Npc update(Long, @Valid NpcSaveRequest)`, `void delete(Long)`. 이름 중복 → `DuplicateNpcNameException("이미 있는 NPC 이름입니다: " + name)`. `roomId` 가 있으면 `RoomFinder.find(roomId)` 로 있는지 확인(없으면 그 `IllegalArgumentException` → 400) 후 `placeIn`, null이면 `unplace`. 도메인의 `IllegalArgumentException`(낱말 중복·형식)은 그대로 400.
  - `NpcModifyService` 의 `@EventListener void onRoomDeleted(RoomDeletedEvent)` — 같은 트랜잭션에서 `findAllByRoomIdOrderByIdAsc(roomId)` 의 NPC를 모두 `unplace`. (1차 `RoomModifyService.delete` 는 바꾸지 않는다. 게임 쪽 `@TransactionalEventListener(AFTER_COMMIT)` 와 따로 돈다.)
  - 캐릭터: `CharacterQueryService.isNameTaken(name)` 이 캐릭터 이름 **또는** `NpcFinder.existsByName(name)` 이면 true. `CharacterModifyService.create` 의 이름 중복 검사도 NPC 이름을 보고 같은 `DuplicateCharacterNameException("이미 있는 이름입니다. 다른 이름을 정하십시오.")`.
  - REST: D27 표의 `/api/admin/npcs` 5개. `NpcApi` 가 응답을 만들 때 배치 방 이름·지역은 `RoomFinder.findOptional(roomId)` 로 채운다(어댑터가 포트 둘을 쓴다). `RoomApi.get/put...` 의 `RoomDetailResponse.npcs` 는 `NpcFinder.findAllInRoom` 으로 채운다.

- [ ] **Step 1: 서비스 테스트(`@ApplicationServiceTest`)** — `NpcEditorTest`: `createPlaced`, `createUnplaced`, `duplicateNameFail`, `updateToOtherNpcNameFail`, `updateReplacesTopicsAndPlacement`, `unknownRoomFail`, `duplicateTopicFail`, `delete`, `deleteRoomUnplacesNpcs`(`RoomEditor.delete` 뒤 flush·clear → NPC 남고 `findRoomId()` empty), `deleteStartRoomKeepsNpcPlacement`(시작 방 삭제 예외 뒤 배치 그대로), `llmFlagStored`. `NpcFinderTest`: `findAllInRoom` id 순, `existsByName` 대소문자 무시. `CharacterCreatorTest` 추가: `createCharacterWithNpcNameFail`(NPC `왕씨` 가 있으면 캐릭터 `왕씨` 거절). `GameConnectorTest` 추가: `createCharacterWithNpcNameFail` — 생성 흐름에서 `왕씨` 입력 → `system/error` `이미 있는 이름입니다. 다른 이름을 정하십시오.` 후 이름 prompt.
- [ ] **Step 2: 실패 확인 → 구현 → 통과.**
- [ ] **Step 3: 웹 테스트** — `NpcApiWebMvcTest`: `POST` 201 본문(`topics` 순서, `llmEnabled`, `roomName`, `zoneName`), 이름에 공백 400, 31번째 낱말 400, 중복 409 `detail` `이미 있는 NPC 이름입니다: 왕씨`, `GET` 목록 `roomId:null` 이면 room 셋 null, `DELETE` 204, 관리자 아님 403, `X-MUD-Client` 없음 403. `RoomApiWebMvcTest` 추가: `GET /api/admin/rooms/{id}` 에 `npcs[0].name`.
- [ ] **Step 4: 통합 테스트** — `AdminApiIntegrationTest` 에 `manageNpcs`: 지역·방 → NPC 만들기(배치, 낱말 둘) → `GET /api/admin/npcs/{id}`·목록·`/rooms/{id}` 가 지연 로딩 오류 없이 이름을 냄 → 방 삭제 → NPC 상세 `roomId==null`.
- [ ] **Step 5: 검증** — `./gradlew build` 통과(ArchUnit: `npc → room`, `character → npc`, 순환 없음).
- [ ] **Step 6: 커밋** — `feat(world-server): NPC 관리 API·방 삭제 시 배치 풀기·캐릭터 이름의 NPC 이름 검사`.

**완료 조건:** 위 테스트 통과, D27 NPC 엔드포인트 5개 동작, 1차 테스트 전부 통과.

---

### Task 5: 게임 — 방 머리 소유, 방의 NPC, `대화` 명령, 도움말, WebSocket 메시지 모양

**권장 모델:** opus

**목적:** D24·D25·D26을 게임 코어와 WebSocket 직렬화에 넣는다.

**Files:**
- Modify: `domain/game/{Command,CommandParser}.java`(`Talk`)
- Modify: `application/game/required/GameMessage.java`(`RoomMessage.owner`, `OwnerView`, `OccupantView.npc`, `TalkMessage`), `application/game/RoomViewAssembler.java`, `application/game/GameService.java`(NPC 조회, `Talk` 처리, `HELP` 문구)
- Test: `domain/game/CommandParserTest`(추가), `application/game/provided/{BaseGameTest,GameConnectorTest}`(추가), `adapter/websocket/{WebSocketGameMessageSenderTest,GameWebSocketIntegrationTest}`(추가)

**Interfaces:**
- Consumes: `NpcFinder.findAllInRoom`(Task 4), `Zone.findOwner()`·`FactionKind`(Task 1), `NpcEditor`·`FactionEditor`·`ZoneEditor`(테스트 준비).
- Produces:
  - `record Command.Talk(@Nullable String target, @Nullable String word)`. 파서: `대화` → `Talk(null, null)`, `대화 왕씨` → `Talk("왕씨", null)`, `대화 왕씨 일거리` → `Talk("왕씨", "일거리")`, `대화   왕씨   [일거리]` → `Talk("왕씨", "일거리")`, `대화 왕씨 늑대 퇴치` → `Talk("왕씨", "늑대 퇴치")`, `대화왕씨` → `Unknown("대화왕씨")`, `말 왕씨` → `Unknown("말")`(1차 그대로).
  - `RoomMessage(ZoneView zone, boolean zoneEntered, RoomView room, @Nullable OwnerView owner, List<String> exits, List<OccupantView> occupants)` — 필드 순서도 이대로(JSON 순서). `record OwnerView(String kind, String name)`(`kind`: `CASTLE` → `"castle"`, `SECT` → `"sect"`). `OccupantView.npc(String name)` → kind `"npc"`.
  - `record TalkMessage(String npc, @Nullable String word, String text, String tone)` + `static TalkMessage talk(String npc, @Nullable String word, String text)`(tone `"talk"`), `type()` → `"talk"`.
  - `RoomViewAssembler.assemble(Room, boolean zoneEntered, List<OnlinePlayer> occupants, List<Npc> npcs, Long viewerCharacterId)` — 플레이어(나 빼고, 들어온 순서) 다음 NPC(id 순).
  - `GameService`: 생성자에 `NpcFinder` 추가. 방 보기를 만드는 모든 곳(접속·보기·이동·방 삭제 반영·생성 완료)이 NPC를 넣는다. `Talk` 처리(D25 문구 그대로): 대상 null → `누구에게 말을 거시겠습니까?`; 같은 방 NPC에서 `isCalled` 첫째, 없으면 `nameStartsWith` 첫째; 없으면 `여기에 그런 상대는 없습니다.`; word null → `이름: "인사말"` (+ `\n물어볼 수 있는 낱말: [a] [b]`); 있으면 `이름: "answerTo(word)"`. 묻는 세션에만 보낸다. `HELP` = D25 문구.
  - WebSocket: `WebSocketGameMessageSender` 는 record 필드를 그대로 붙이므로 코드 변경이 필요 없을 것이다 — `owner: null` 이 JSON에 `null` 로 나가는지 테스트로 확인하고, 빠지면 null도 쓰도록 고친다.

- [ ] **Step 1: 파서 테스트** — 위 `Talk` 사례 전부.
- [ ] **Step 2: 포트 테스트(`GameConnectorTest`, `BaseGameTest.prepareMap()` 에 성 `청석성` 과 그 소유 지역 `청석성` 의 방 `남문`(우물가 북↔남), NPC `왕씨`(마을 어귀, 인사말 `어서 오시오.`, 낱말 `단약`→`회춘단이 좋소.`, `일거리`→`늑대가 말썽이오.`, 기본 대답 `글쎄, 잘 모르겠구려.`)를 더하는 `prepareNpcs()` 추가)**
  - `roomShowsNpcsAfterPlayers` — 마을 어귀에 A·B: A `보기` → occupants `[{B, player}, {왕씨, npc}]`, owner null.
  - `roomShowsOwner` — `남문` 으로 이동 → `owner == {castle, 청석성}`.
  - `talkGreetingListsWords` — `대화 왕씨` → `talk` text `왕씨: "어서 오시오."\n물어볼 수 있는 낱말: [단약] [일거리]`, `word==null`.
  - `talkGreetingWithoutWords` — 낱말 없는 NPC → 첫 줄만.
  - `talkKnownWord` — `대화 왕씨 일거리` → `왕씨: "늑대가 말썽이오."`; `대화 왕 [단약]` → `왕씨: "회춘단이 좋소."`(앞부분 이름, 괄호).
  - `talkUnknownWordIsDefault` — `대화 왕씨 비석` → `왕씨: "글쎄, 잘 모르겠구려."`.
  - `talkWithoutTargetFail` — `대화` → `system/error` `누구에게 말을 거시겠습니까?`.
  - `talkToPlayerOrAbsentNpcFail` — `대화 <B 이름>`, 다른 방에서 `대화 왕씨`, 배치 안 된 NPC 이름 → 모두 `여기에 그런 상대는 없습니다.`
  - `talkIsPrivate` — A가 `대화 왕씨` 할 때 같은 방 B는 아무 메시지도 받지 않음.
  - `llmEnabledNpcAnswersSame` — `llmEnabled=true` NPC도 같은 대사.
  - `ownerAndNpcChangesShowOnNextLook` — 접속 중 관리자가 지역 소유 지정·NPC 배치 → 다음 `보기` 에 owner·NPC 보임, 그 사이 알림 없음.
  - `roomDeletedWithNpcStillWorks` — NPC 있는 우물가를 지워도 플레이어 옮김(1차) 정상, 시작 방 보기에 그 NPC 없음.
  - `help` 문구를 D25로 갱신.
- [ ] **Step 3: WebSocket 테스트** — `WebSocketGameMessageSenderTest`: `talk` JSON `{"type":"talk","npc":"왕씨","word":null,"text":...,"tone":"talk"}`, `room` 의 `owner` null·객체 둘 다, `occupants[].kind=="npc"`. `GameWebSocketIntegrationTest` 에 `talkToNpc`(NPC를 포트로 준비, 실제 WebSocket으로 `대화 왕씨 단약` → `talk`).
- [ ] **Step 4: 실패 확인 → 구현 → 통과.**
- [ ] **Step 5: 검증** — `./gradlew build` 통과(ArchUnit: game이 npc의 provided만 씀).
- [ ] **Step 6: 커밋** — `feat(world-server): 방 머리 소유·방의 NPC·대화 명령`.

**완료 조건:** 위 테스트 통과, 문구가 D25와 글자 하나까지 같다. 1차 게임 테스트가 (occupants·help 기대값 갱신 말고는) 그대로 통과.

---

### Task 6: admin-console — 성·종문, 지역 소유, NPC 화면

**권장 모델:** opus

**목적:** D28 화면으로 D27 API를 쓴다.

**Files:**
- Modify: `admin-console/src/lib/api.ts`(타입·함수), `src/components/{TopBar,ZoneForm}.tsx`, `app/zones/page.tsx`, `app/room/page.tsx`
- Create: `admin-console/app/factions/page.tsx`, `app/npcs/page.tsx`, `app/npc/page.tsx`, `src/components/{FactionForm,NpcForm,TopicEditor,RoomPicker}.tsx`, `src/lib/factions.ts`(`FACTION_KINDS: {code:"CASTLE"|"SECT", name:"성"|"종문"}[]`)
- Test: `src/lib/api.test.ts`(추가), `src/components/{FactionForm.test.tsx,NpcForm.test.tsx,TopicEditor.test.tsx,ZoneForm.test.tsx(추가)}`

**Interfaces:**
- Consumes: D27.
- Produces:
  - `api.ts`: 타입 `FactionKind = "CASTLE" | "SECT"`, `FactionResponse`, `FactionSaveRequest`, `ZoneResponse`(+`ownerId`,`ownerName`,`ownerKind`), `ZoneSaveRequest`(+`ownerId: number | null`), `NpcTopic = {word, answer}`, `NpcSaveRequest`, `NpcSummaryResponse`, `NpcDetailResponse`, `RoomDetailResponse`(+`npcs: {id, name}[]`). 함수 `listFactions`, `createFaction`, `updateFaction`, `deleteFaction`, `listNpcs`, `getNpc`, `createNpc`, `updateNpc`, `deleteNpc`.
  - `RoomPicker`: 지역 선택 → 방 선택. 값 `roomId: number | null`, 첫 항목 `배치 안 함`. 기존 배치가 있으면 그 지역·방이 선택된 채로 열린다. (1차 `ExitEditor` 의 지역→방 고르기와 같은 동작이지만 `ExitEditor` 는 고치지 않는다.)
  - `TopicEditor`: 낱말·대답 행 목록, `낱말 추가`, 행마다 `삭제`. 빈 행(낱말·대답 둘 다 빔)은 저장할 때 뺀다.
  - `NpcForm`: D28 항목. 저장 전 클라이언트 확인은 하지 않고 서버 `detail` 을 보인다(1차 관례).
  - 문구: 제목 `성·종문`, `NPC`; 버튼 `성·종문 만들기`, `NPC 만들기`, `낱말 추가`, `저장`, `삭제`; 라벨 `종류`, `이름`, `묘사`, `소유`, `인사말`, `기본 대답`, `낱말`, `대답`, `LLM 대화`, `배치`; 안내 `켜 두어도 지금은 정해진 대사로 답합니다.`; 목록 빈 상태 `아직 성·종문이 없습니다.`, `아직 NPC가 없습니다.`; 배치 없음 표시 `배치 안 됨`; 방 화면 `이 방의 NPC`, 없으면 `없음`.

- [ ] **Step 1: 테스트 작성** — `api.test.ts`: `createNpc` 가 `POST {url}/api/admin/npcs` 본문(`topics`, `roomId:null`)과 `X-MUD-Client: 1`, `updateZone` 본문에 `ownerId`. `FactionForm.test`: 종류 선택 `종문` 후 저장 → `{kind:"SECT", ...}`. `TopicEditor.test`: `낱말 추가` 로 행 생김, `삭제` 로 사라짐, 빈 행 빠짐. `NpcForm.test`: 새 NPC 기본 대답이 `글쎄, 잘 모르겠구려.`, LLM 체크 → `llmEnabled:true`, 배치 `배치 안 함` → `roomId:null`, 서버 409 `detail` 이 화면에 보임. `ZoneForm.test` 추가: 소유 `청석성 (성)` 선택 → `ownerId`.
- [ ] **Step 2: 실패 확인 → 구현 → 통과.**
- [ ] **Step 3: 검증** — `npm ci && npm test && npm run build` 통과, `out/factions/index.html`·`out/npcs/index.html`·`out/npc/index.html` 생성 확인.
- [ ] **Step 4: 커밋** — `feat(admin-console): 성·종문·지역 소유·NPC(대사·배치·LLM 값) 화면`.

**완료 조건:** 테스트·정적 빌드 통과. 1차 화면 테스트 그대로 통과.

---

### Task 7: play-client — 소유 머리, NPC 줄, `talk`, 자동완성

**권장 모델:** sonnet

**목적:** D26·D29를 그린다.

**Files:**
- Modify: `play-client/src/lib/{protocol.ts,render.ts,complete.ts}`, `src/components/Terminal.tsx`(이름 후보에 NPC 포함 확인), `app/globals.css`(`.line-npc`, `.line-talk`)
- Test: `src/lib/{render.test.ts,complete.test.ts}`(추가), `src/components/Terminal.test.tsx`(추가)

**Interfaces:**
- `protocol.ts`: `Occupant = { name: string; kind: "player" | "npc" }`, `RoomMessage.owner: { kind: "castle" | "sect"; name: string } | null`, `TalkMessage = { type: "talk"; npc: string; word: string | null; text: string; tone: "talk" }` 를 `ServerMessage` 에 더한다.
- `render.ts`: 방 이름 줄 = `owner ? \`${room.name}  [${owner.name}]\` : room.name`(style `room-name`), occupant는 `kind === "npc"` 면 style `npc`, 아니면 `occupant`. `talk` → `text` 줄별 style `talk`.
- `complete.ts`: `COMMANDS` 에 `대화` 를 `말하기` 다음에.
- `Terminal`: 이름 후보는 `room.occupants` 전부의 `name`(1차 코드가 kind를 거르지 않는지 확인만; 거르면 고친다).

- [ ] **Step 1: 테스트** — `render.test.ts`: owner 있음 `청석성 남문  [청석성]`, owner null 은 1차 그대로, NPC 줄 style `npc`, `talk` 두 줄 style `talk`. `complete.test.ts`: `"대"` → `"대화"`, `"대화 왕"` + names `["한립","왕씨"]` → `"대화 왕씨"`. `Terminal.test.tsx`: `room`(NPC 포함) 받은 뒤 `대화 왕` + Tab → `대화 왕씨`.
- [ ] **Step 2: 실패 확인 → 구현 → 통과.**
- [ ] **Step 3: 검증** — `npm ci && npm test && npm run build` 통과.
- [ ] **Step 4: 커밋** — `feat(play-client): 방 머리 소유·NPC 표시·대화 출력·대화 자동완성`.

**완료 조건:** 테스트·정적 빌드 통과.

---

### Task 8: 스펙 반영 — 제안 결정을 시스템 스펙에 `(제안)` 으로

**권장 모델:** sonnet

**목적:** D19~D31과 구현된 값을 `docs/spec/system/` 에 옮기고 2차 열린 질문을 정리한다. 코드를 바꾸지 않는다.

**Files:**
- Modify: `docs/spec/system/index.md` — `### 2차 범위` 에 "구현 계획: `docs/impl/iteration-2/plan.md`" 링크와 "구현했다(날짜)" 한 줄, `### 2차 이터레이션 열린 질문` 의 각 질문 뒤에 `→ (제안) …` 답(D21 항목·D22 방 삭제·D25 이름 줄임·동명/대화 공개 범위/`보기 <NPC>`·D25 LLM 켬 응답·D21 LLM 설정 칸 3차·D20 성·종문 삭제·D21 소속 없음)과 링크.
- Modify: `docs/spec/system/npc.md` — `## 2차 데이터 모델` 절(D21·D22·D23), 캐릭터 챗 절에 D25 LLM 켬 동작, 열린 질문의 "캐릭터 설정 입력" 에 "3차로 미룸 (제안)".
- Modify: `docs/spec/system/world-structure.md` — 성 · 종문 소유 절에 D19·D20 데이터 모델·삭제 규칙.
- Modify: `docs/spec/system/admin-tool.md` — `## 2차 화면`(D28), `## 2차 REST API`(D27 표·응답 모양·예외 문구), 관리 툴 변경의 반영에 D31.
- Modify: `docs/spec/system/command.md` — `## 2차 명령어` 에 D25(해석·대상 찾기·낱말 찾기·출력 문구·오류 문구·공개 범위·도움말 문구), "룰북에 아직 없음" 표시. 마지막 줄의 "아직 열려 있다" 문장을 결정으로 바꾼다.
- Modify: `docs/spec/system/platform.md` — `## 2차 WebSocket 메시지 변경`(D26 표, tone `talk`), 플레이 화면 방 그림(D24), 저장소 구성의 슬라이스·의존(D30).

**규칙:** 1차 Task 11과 같다 — 새 결정 문장 끝에 `(제안)`, 확정 문장은 고치지 않음, 룰북(`docs/spec/game-rule/`)은 고치지 않음, 값은 **구현된 코드**에서 확인(`GameService` 문구, orm.xml, DTO, admin-console 문구). 다르면 코드 값을 적고 progress.md 메모에 차이를 남긴다.

- [ ] **Step 1: 각 파일 수정.**
- [ ] **Step 2: 검증** — 새·바뀐 상대 링크가 있는 파일·헤딩을 가리키는지 확인, `git diff --stat` 에 `docs/spec/system/*` 와 progress.md만 있음.
- [ ] **Step 3: 커밋** — `docs(spec): 2차 이터레이션 제안 결정(성·종문·지역 소유·NPC·대화 명령·메시지) 반영`.

**완료 조건:** D19~D31 각각이 스펙 어딘가에 `(제안)` 으로 있고, 2차 열린 질문 9개 모두에 답이나 "미룸" 이 붙음.

---

### Task 9: README — 2차 시나리오와 명령

**권장 모델:** sonnet

**목적:** 실행·확인 안내를 2차에 맞춘다.

**Files:**
- Modify: `README.md`

**내용:**
- 모듈 소개에 2차 기능 한 줄(성·종문, 지역 소유, NPC, 대화).
- `## 처음 할 일` 절에 2차 시나리오: admin-console에서 `성·종문` → 성 만들기 → `지역 목록` 에서 지역 소유 지정 → `NPC` → NPC 만들기(인사말·낱말·대답·기본 대답·LLM 값·배치) → play-client에서 그 방으로 가 방 머리 `[성 이름]` 과 NPC 줄 확인 → `대화 <NPC>`, `대화 <NPC> <낱말>`, 모르는 낱말.
- 명령 절 제목을 `### 되는 명령` 으로 바꾸고 `대화 <NPC> [낱말]` 줄과 도움말 문구(D25) 반영. `말하기` 는 플레이어끼리 채팅이고 NPC에게는 `대화` 라는 설명 한 줄.
- 브라우저 수동 확인 체크리스트에 2차 항목(소유 머리, NPC 줄 색, `대화` 탭 자동완성, LLM 켬 NPC도 같은 대사, NPC 있는 방 삭제 → NPC `배치 안 됨`).
- 스키마: 기존 compose 볼륨(`pgdata`)에서 그대로 올리면 `ddl-auto=update` 가 새 테이블·열을 더한다는 한 줄.

- [ ] **Step 1: 수정.** 명령 이름·문구는 `GameService` 의 실제 문구와 대조한다.
- [ ] **Step 2: 커밋** — `docs(infra): README에 2차 시나리오·대화 명령 반영`.

**완료 조건:** README의 명령·문구가 코드와 같고, 1차 내용(로그인·compose·스모크)은 유지.

---

### Task 10: 통합 확인 — compose 한 번 빌드·실행, 스모크 시나리오 확장, 정리

**권장 모델:** opus

**목적:** 실제 컨테이너에서 2차 완료 시나리오를 자동으로 확인하고, 디스크를 정리한다. **이 이터레이션에서 Docker를 쓰는 유일한 태스크다.**

**Files:**
- Modify: `scripts/smoke-test.mjs`, `README.md`(스모크 절 단계 목록이 바뀌면), `docs/impl/iteration-2/progress.md`

**시나리오 추가** (1차 단계 1~10 뒤에 이어서, 같은 실행 번호를 섞어 겹치지 않게):
11. 관리자: 성 `청석성<n>` 만들기 → 1차 단계에서 만든 지역 소유를 그 성으로 PUT → `GET /api/admin/zones` 의 `ownerName`.
12. 관리자: NPC `왕씨<n>`(이름은 12자 이내, 실행 번호는 숫자 몇 자리) 만들기 — 인사말 `어서 오시오.`, 낱말 `단약`→`회춘단이 좋소.`, `일거리`→`늑대가 말썽이오.`, 기본 대답 `글쎄, 잘 모르겠구려.`, `llmEnabled:true`, 배치 `광장` → `GET /api/admin/npcs/{id}` 로 값 확인.
13. 플레이어 B(광장에 있음) `보기` → `room.owner.name == 청석성<n>`, occupants에 `{name: 왕씨<n>, kind: "npc"}`.
14. B `대화 왕씨<n>` → `talk` text가 `왕씨<n>: "어서 오시오."\n물어볼 수 있는 낱말: [단약] [일거리]`; `대화 왕씨<n> 일거리` → `왕씨<n>: "늑대가 말썽이오."`; `대화 왕씨<n> 비석` → `왕씨<n>: "글쎄, 잘 모르겠구려."`; `도움말` 에 `대화 <NPC> [낱말]`.
15. 관리자: 성 삭제 → 409. NPC가 있는 방을 만들어(새 방 + NPC 배치) 그 방 삭제 → NPC 상세 `roomId == null`.

- [ ] **Step 1: 스크립트 수정** — 1차 단계는 그대로 두고 위 단계를 더한다(Node 26 내장 `fetch`·`WebSocket` 만).
- [ ] **Step 2: 모듈 검증** — 세 모듈 검증 명령을 다시 돌려 통과 확인(`./gradlew build`, 두 프론트 `npm ci && npm test && npm run build`).
- [ ] **Step 3: compose 한 번** — 시작 전 `df -h .` 로 남은 공간을 progress.md 메모에 적는다. 1차 Task 10 `.env`(없으면 `.env.example` 복사 후 `POSTGRES_PASSWORD=mud-local`, `SPRING_PROFILES_ACTIVE=local`, `MUD_DEV_LOGIN_ENABLED=true`, `MUD_ADMIN_EMAILS=smoke-admin@example.com`)로 `docker compose up -d --build` **한 번** → `source "$NVM_DIR/nvm.sh" && nvm use 26.13.1 && node scripts/smoke-test.mjs` → `SMOKE OK`. 실패하면 원인을 고치고(해당 모듈 테스트 통과) 바뀐 서비스만 `docker compose up -d --build <서비스>` 로 다시 올린다(전체 재빌드 반복 금지). `ENOSPC` 면 멈추고 보고.
- [ ] **Step 4: 브라우저 확인(가능하면)** — admin-console에서 성·종문·소유·NPC 만들기, play-client에서 방 머리·NPC·`대화`·Tab. 못 하면 README 체크리스트로 남긴다.
- [ ] **Step 5: 정리** — `docker compose down`(볼륨은 남김) → `docker image prune -f`(dangling 이미지만) → `docker builder prune -f`(빌드 캐시) → 저장소 빌드 산출물 삭제: `world-server/build`, `play-client/{out,.next}`, `admin-console/{out,.next}`(모두 `.gitignore` 대상인지 `git check-ignore` 로 먼저 확인). 이름 붙은 이미지(`mud-mk3-*`)와 `pgdata` 볼륨, `node_modules` 는 지우지 않는다. 정리 뒤 `df -h .` 를 progress.md 메모에 적는다.
- [ ] **Step 6: progress.md** — 결과(통과 여부, 고친 것, 남은 수동 확인), 상태를 "2차 이터레이션 구현 완료" 로.
- [ ] **Step 7: 커밋** — `test(infra): 2차 스모크 시나리오(성·종문·NPC·대화)`.

**완료 조건:** compose 환경에서 `SMOKE OK`, 세 모듈 검증 통과, 정리 뒤 dangling 이미지 없음.

---

## 태스크 의존 순서

```
1 → 2 ──────────────┐
1 → 3 → 4 → 5 ──────┼→ 6 (2·4 필요)
               5 → 7 ┤
                     └→ 8 → 9 → 10
```

- 순서대로 하면 된다: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10.
- 6(admin-console)은 2와 4가 끝나면 시작할 수 있고, 7(play-client)은 5가 끝나야 실제 메시지와 맞출 수 있다. 8·9는 구현된 값을 옮기므로 7 뒤에 한다. 10은 마지막.

## 손대지 않는 1차 이월 항목

2차 작업과 부딪치지 않아 이번에 고치지 않는다(1차 `progress.md` 의 미룬 Minor 약 45건 가운데 대표).

- 이웃한 방 두 개를 동시에 지울 때 한쪽 관리 요청 500(DB 교착) — 1차 수용 판정 그대로.
- play-client 강제 자동 스크롤, admin-console 중간 401 시 로그인 화면 이동, 출구 삭제 확인 문구.
- 세션 쿠키 `secure`(HTTPS 때), 세션 저장소 DB화(Spring Session JDBC), 마이그레이션 도구(Flyway).
- 열린 WebSocket이 HTTP 세션 로그아웃·만료 뒤에도 유지되는 것.

2차에서 함께 다루는 1차 항목: 방 삭제와 NPC 배치(D22), 캐릭터 이름의 NPC 이름 검사(D23, 1차 D9에서 미룸).
