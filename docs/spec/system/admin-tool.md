# 관리 툴

머드 게임 관리 화면. → [목차](index.md)

- 관리 툴은 `admin-console` 모듈로, 플레이 화면(`play-client`)과 분리된 Next.js 앱이다. `world-server` 와 REST로 통신한다. → [플랫폼: 저장소 구성](platform.md#저장소-구성)
- 관리 툴은 Google 로그인으로 들어오고, 서버 설정에 적힌 관리자 Google 이메일 목록에 있는 계정만 쓸 수 있다. → [인증](auth.md#관리자)

## 데이터 주도 콘텐츠

- 시스템(엔진 코드)은 틀을 만든다. 그 틀에 담기는 내용은 관리 툴에서 입력받는다.
  - 예: "NPC가 대사를 한다"는 틀은 엔진이, 어떤 NPC가 어떤 대사를 하는지는 관리 툴에서 입력한 데이터가 정한다.
- 하드 코딩은 최대한 피한다. 다만 어쩔 수 없이 하드 코딩이 필요한 경우도 있을 것으로 보이며, 무조건 관리 툴로 다루는 것은 아니다.
- 당장 해볼 수 있는 것은 관리 툴로 만들고, 이벤트처럼 애매한 부분은 코딩한다. → [이벤트 · 시나리오](event.md)
- 어디까지가 엔진이고 어디부터가 데이터인지는 시스템마다 문서에서 정한다. 아직 정하지 않은 경계는 각 문서의 열린 질문에 남긴다.

## 관리 화면에서 하는 일

| 대상 | 하는 일 | 관련 문서 |
|---|---|---|
| 성 · 종문 | 정의한다. 맵보다 먼저 정의되어야 한다 | [월드 구조](world-structure.md#성--종문-소유), [퀘스트 · 공적 교환](quest.md), [룰북: 게임 구성](../game-rule/game-structure.md) |
| NPC | 생성하고 배치한다. 대사(인사말, 낱말마다 대답, 기본 대답)를 입력한다. LLM 대화 켬/끔을 NPC마다 설정한다 | [NPC](npc.md), [룰북: 유니크 NPC](../game-rule/npc.md#유니크-npc) |
| 몹 | 생성하고 배치한다 | [룰북: 몹](../game-rule/npc.md#몹) |
| 맵 | 만든다 (방·출구). 어떤 존(지역)은 성 또는 종문 소유로 지정한다 | [월드 구조](world-structure.md) |
| 아이템 · 장비 | 입력한다 | [룰북: 아이템](../game-rule/item.md), [룰북: 장비](../game-rule/equipment.md) |
| 공법 · 신통 | 입력한다 | [룰북: 공법](../game-rule/technique.md) |
| 퀘스트 · 공적 교환 | 입력한다 | [퀘스트 · 공적 교환](quest.md) |
| 이벤트 | 보류 — 우선 코딩으로 해결한다 | [이벤트 · 시나리오](event.md) |
| 비경 | 입력한다 | [월드 구조](world-structure.md), [룰북: 비경 탐사](../game-rule/game-structure.md#비경-탐사) |
| 상점 | 입력한다 | [룰북: 상점](../game-rule/game-structure.md#상점) |

- 1차 이터레이션에서는 맵(방·출구) 만들기만 구현한다. 성·종문 정의와 소유 지정은 다음 이터레이션에서 한다. → [이터레이션 계획](index.md#이터레이션-계획)

## 1차 화면

admin-console은 정적 export이고, 경로는 쿼리스트링으로 id를 받는다. (제안)

- `/` 로그인: `Google로 로그인` 버튼(= `${worldServerUrl}/api/auth/login?client=admin` 으로 이동), 개발용 로그인 폼, `?loginError=1` 이면 `로그인하지 못했습니다. 다시 시도하십시오.`. 로그인 뒤 관리자가 아니면 `관리자만 쓸 수 있습니다.` 와 로그아웃 버튼을 보인다. → [인증](auth.md#1차-로그인-흐름) (제안)
- `/zones/` 지역 목록(이름·방 수), 지역 만들기 폼, 지역마다 수정·삭제. (제안)
- `/zone/?id=1` 지역 하나: 방 목록(이름·출구 수, 시작 방 표시)과 방 만들기 폼. (제안)
- `/room/?id=3` 방 하나: 이름·묘사·지역 수정, 삭제, `시작 방으로 지정` 버튼, 출구 표(방위·대상 방(지역)·삭제), 출구 추가 폼(방위 선택 → 대상 지역 선택 → 대상 방 선택, `반대 방향도` 체크박스 기본 켜짐). (제안)
- 위쪽 공통 바: 현재 시작 방, 지역 목록 링크, 로그아웃. (제안)

## 1차 REST API

모두 `/api/admin/**` 이고 `ADMIN` 권한이 필요하다(로그인 안 됨 401, 관리자 아님 403). 변경 요청에는 헤더 `X-MUD-Client: 1` 이 있어야 한다. → [인증](auth.md#오리진corscsrf) 오류는 `ProblemDetail`(`title`, `status`, `detail`=한국어 메시지, `timestamp`, `exception`)이다. 검증 실패·잘못된 값·없는 id는 400, 이름 중복·규칙 충돌은 409다. (제안)

| 메서드 · 경로 | 요청 | 응답 |
|---|---|---|
| `GET /api/admin/zones` | — | 200 `ZoneResponse[]`(이름순) |
| `POST /api/admin/zones` | `{name, description}` | 201 `ZoneResponse` |
| `PUT /api/admin/zones/{zoneId}` | `{name, description}` | 200 `ZoneResponse` |
| `DELETE /api/admin/zones/{zoneId}` | — | 204. 방이 있으면 409 |
| `GET /api/admin/zones/{zoneId}/rooms` | — | 200 `RoomSummaryResponse[]`(id순) |
| `POST /api/admin/rooms` | `{zoneId, name, description}` | 201 `RoomDetailResponse` |
| `GET /api/admin/rooms/{roomId}` | — | 200 `RoomDetailResponse` |
| `PUT /api/admin/rooms/{roomId}` | `{zoneId, name, description}` | 200 `RoomDetailResponse` |
| `DELETE /api/admin/rooms/{roomId}` | — | 204. 이 방으로 오는 출구도 지운다. 시작 방이면 409 |
| `PUT /api/admin/rooms/{roomId}/exits/{direction}` | `{targetRoomId, twoWay}` | 200 `RoomDetailResponse` |
| `DELETE /api/admin/rooms/{roomId}/exits/{direction}?twoWay=true` | — | 204. `twoWay` 기본값은 false |
| `GET /api/admin/world` | — | 200 `WorldResponse` |
| `PUT /api/admin/world/start-room` | `{roomId}` | 200 `WorldResponse` |

- 응답 모양 (제안): `ZoneResponse {id, name, description, roomCount}`, `RoomSummaryResponse {id, name, exitCount}`, `RoomDetailResponse {id, zoneId, zoneName, name, description, start, exits: [{direction, directionName, targetRoomId, targetRoomName, targetZoneName}]}`(exits는 방위 순서), `WorldResponse {startRoomId, startRoomName, startZoneName}`(시작 방이 없으면 셋 다 null).
- `{direction}` 경로 값은 `EAST WEST SOUTH NORTH NORTHEAST NORTHWEST SOUTHEAST SOUTHWEST UP DOWN` 이다. (제안)

### 출구 만들기·지우기 규칙

- 반대 방향 출구 자동 생성: `twoWay=true` 면 대상 방에 반대 방위 출구를 같이 만든다. 대상 방의 반대 방위 출구가 이미 이 방을 가리키면 그대로 두고, 다른 방을 가리키면 아무것도 바꾸지 않고 409다. admin-console은 폼의 `반대 방향도` 체크(기본 켜짐)를 따라 `twoWay` 를 보낸다. (제안)
- 출구 삭제에서 `twoWay=true` 면 대상 방의 반대 출구가 이 방을 가리킬 때만 같이 지운다. (제안)
- 출구의 대상을 바꿔도 옛 대상 방의 반대 출구는 그대로 남는다. (제안)
- 없는 방위의 출구를 지우는 요청은 아무것도 하지 않고 204다. (제안)

### 관리 툴 변경의 반영

- 맵(지역·방·출구)의 변경은 실행 중인 게임에 바로 반영된다. 게임이 맵을 캐시하지 않고 명령마다 DB에서 읽기 때문이다. → [플랫폼: 1차 월드 상태와 저장](platform.md#1차-월드-상태와-저장) (제안)
- 플레이어가 서 있는 방을 지우면 그 플레이어는 시작 방으로 옮겨진다. (제안)
- 2차의 성·종문·지역 소유·NPC(대사·배치) 변경도 맵처럼 실행 중인 게임에 바로 반영된다. 게임이 명령마다 DB에서 읽기 때문이다. 방 머리 소유와 방의 NPC는 다음 방 보기부터 바뀌고, 같은 방 사람에게 따로 알리지 않는다. (제안)
- 2차 스키마도 1차처럼 `ddl-auto=update` 로 새 테이블(`faction`, `npc`, `npc_topic`)과 `zone.owner_id` 열이 생긴다. 마이그레이션 도구는 여전히 없다. (제안)

## 2차 화면

1차 화면에 더한다. (제안)

- 위쪽 공통 바: `지역 목록` 옆에 `성·종문`, `NPC` 링크. (제안)
- `/factions/` 성·종문 목록(종류·이름·소유 지역 수·수정·삭제)과 `성·종문 만들기` 폼(종류 선택 `성`/`종문`, 이름, 묘사). 삭제 확인은 `'청석성' 성을(를) 지울까요?`. (제안)
- `/zones/` 지역 만들기·수정 폼에 `소유` 선택(`없음` + 성·종문 목록, 예 `청석성 (성)`), 지역 목록에 `소유` 열(없으면 `-`). (제안)
- `/npcs/` NPC 목록(이름·배치(`청우촌 / 마을 어귀` 또는 `배치 안 됨`)·낱말 수·LLM(`켬`/`끔`))과 `NPC 만들기` 링크(`/npc/`). (제안)
- `/npc/` (id 없음 = 새로 만들기, `/npc/?id=4` = 고치기): 이름, 인사말, 기본 대답(새 NPC 기본값 `글쎄, 잘 모르겠구려.`), 낱말 표(낱말·대답·`삭제`)와 `낱말 추가` 버튼(낱말과 대답이 둘 다 빈 행은 저장할 때 뺀다), `LLM 대화` 체크박스(기본 꺼짐)와 안내 `켜 두어도 지금은 정해진 대사로 답합니다.`, 배치(지역 선택 → 방 선택. 지역 선택의 첫 항목 `배치 안 함`, 방 선택의 첫 항목 `방 선택`. 지역만 고르고 방을 고르지 않으면 배치 안 함으로 저장), `저장`, 고칠 때 `삭제`(확인 `'왕씨' NPC를 지울까요?`). 만들기에 성공하면 `/npc/?id=<새 id>` 로 간다. 저장 전 클라이언트 검사는 하지 않고 서버의 `detail` 을 보인다. (제안)
- `/room/?id=` 방 화면에 `이 방의 NPC` 목록(이름 링크 → `/npc/?id=`, 없으면 `없음`). (제안)

## 2차 REST API

1차 REST API 규칙(경로·권한·`X-MUD-Client`·`ProblemDetail`·400/409 구분)을 그대로 따른다. (제안)

| 메서드 · 경로 | 요청 | 응답 |
|---|---|---|
| `GET /api/admin/factions` | — | 200 `FactionResponse[]`(종류 성 → 종문, 같은 종류는 이름순) |
| `POST /api/admin/factions` | `{kind, name, description}` | 201 `FactionResponse` |
| `PUT /api/admin/factions/{factionId}` | `{kind, name, description}` | 200 `FactionResponse` |
| `DELETE /api/admin/factions/{factionId}` | — | 204. 소유 지역이 있으면 409 |
| `POST /api/admin/zones`, `PUT /api/admin/zones/{zoneId}` | 1차 `{name, description}` + `ownerId`(number \| null, 빠지면 null = 소유 없음) | 1차와 같음 |
| `GET /api/admin/npcs` | — | 200 `NpcSummaryResponse[]`(이름순) |
| `POST /api/admin/npcs` | `NpcSaveRequest` | 201 `NpcDetailResponse` |
| `GET /api/admin/npcs/{npcId}` | — | 200 `NpcDetailResponse` |
| `PUT /api/admin/npcs/{npcId}` | `NpcSaveRequest`(낱말 목록은 통째로 바꿈) | 200 `NpcDetailResponse` |
| `DELETE /api/admin/npcs/{npcId}` | — | 204 |

- `kind` 값은 `CASTLE` \| `SECT` 다. 그 밖의 값은 400이고 `detail` 은 공통 문구 `요청 본문을 읽을 수 없습니다` 다. (제안)
- 응답 모양 (제안):
  - `FactionResponse {id, kind, kindName, name, description, zoneCount}` — `kindName` 은 `성`·`종문`.
  - `ZoneResponse` 에 `ownerId, ownerName, ownerKind`(`CASTLE`·`SECT`)를 더한다. 소유가 없으면 셋 다 null.
  - `NpcSummaryResponse {id, name, llmEnabled, topicCount, roomId, roomName, zoneName}` — 배치 안 됐으면 room·zone 셋 다 null.
  - `NpcDetailResponse {id, name, greeting, defaultAnswer, topics: [{word, answer}], llmEnabled, roomId, roomName, zoneId, zoneName}` — `topics` 는 입력 순서.
  - `RoomDetailResponse` 에 `npcs: [{id, name}]`(그 방에 배치된 NPC, 만든 순서)를 더한다.
- `NpcSaveRequest {name, greeting, defaultAnswer, topics: [{word, answer}], llmEnabled, roomId}` — `roomId` 가 null이면 배치 안 함, `topics` 가 빠지면 빈 목록. `llmEnabled` 는 꼭 보내야 한다(빠지면 400 `요청 본문을 읽을 수 없습니다`). admin-console은 늘 보낸다. 값 규칙은 [NPC: 2차 데이터 모델](npc.md#2차-데이터-모델)을 따른다. (제안)
- 예외 문구 (제안):

| 경우 | 상태 | `detail` |
|---|---|---|
| 성·종문 이름 중복 | 409 | `이미 있는 성·종문 이름입니다: 청석성` |
| 소유 지역이 있는 성·종문 삭제 | 409 | `소유한 지역이 있는 성·종문은 지울 수 없습니다` |
| 없는 성·종문(`factionId`, 지역의 `ownerId`) | 400 | `성·종문을 찾을 수 없습니다. id: 9` |
| NPC 이름 중복(영문 대소문자 무시) | 409 | `이미 있는 NPC 이름입니다: 왕씨` |
| 한 NPC 안의 같은 낱말 | 400 | `같은 낱말이 두 번 있습니다: 단약` |
| 없는 NPC | 400 | `NPC를 찾을 수 없습니다. id: 9` |
| 없는 배치 방 | 400 | `방을 찾을 수 없습니다. id: 9`(1차 문구) |

## 열린 질문

- 위 대상 가운데 각각 어디까지 관리 툴로 다루고 어디를 하드 코딩하는가? (예: 공법·신통의 효과)
- "배치" 는 방 하나에 두는 것인가, 구역을 정해 돌아다니게 하는 것까지 포함하는가? (룰북에는 특정 방에만 있거나 정해진 구역 안을 돌아다님) → [룰북: 몹](../game-rule/npc.md#몹) — 2차 NPC는 방 하나에 고정한다(창호, 2026-11-14). 돌아다니는 배치는 몹·이후 이터레이션에서 정한다. → [NPC: 생성과 배치](npc.md#생성과-배치)
- 관리 툴에서 바꾼 내용이 실행 중인 게임에 바로 반영되는가, 다시 시작해야 하는가? → (제안) 맵은 바로 반영된다. 2차의 성·종문·지역 소유·NPC도 바로 반영된다. 다른 콘텐츠는 그것이 생기는 이터레이션에서 정한다. → [관리 툴 변경의 반영](#관리-툴-변경의-반영)
- 관리자 사이에 권한 구분(예: 일부 기능만 쓰는 관리자)이 필요한가?
- 관리 툴에서 운영 기능(접속자 보기, 플레이어 캐릭터 수정 등)도 하는가?
