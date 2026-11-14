# 2차 이터레이션 진행

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1. 성·종문 도메인·영속과 지역 소유 — 관리 서비스까지 | 5e91716 | `faction` 슬라이스(도메인·orm.xml·리포지토리·FactionFinder/Editor), `Zone.owner`·`ZoneSaveRequest.ownerId`. `ZoneSaveInfo` 는 바꾸지 않음(소유는 `create/update` 의 따로 받는 인자) |
| 2. 성·종문 관리 REST API와 지역 소유 지정 | 97153d0 | `/api/admin/factions` 4개(`FactionApi`·`FactionResponse`), `ZoneResponse` 에 `ownerId`·`ownerName`·`ownerKind`, 409 처리 2개. 성·종문 삭제의 동시 소유 지정 경합은 1차 지역 삭제와 같게 두었다 |
| 3. NPC 도메인과 영속 | 0f99476 | `npc` 슬라이스(`Npc`·`NpcTopic`·`NpcSaveInfo`·`DuplicateNpcNameException`·`NpcRepository`), orm.xml `npc`·`npc_topic`(`position` 순서 열, 낱말 DB 유니크 없음). `[ ]` 떼기는 공용 `Names.stripBrackets`(Task 5 `CommandParser` 가 다시 씀). 도메인 이름 규칙 위반 문구 `NPC 이름은 띄어쓰기 없이 1~12자로 정하십시오: <이름>` (제안) |
| 4. NPC 관리 서비스·REST API, 방 삭제 시 배치 풀기, 캐릭터 이름 검사 | 0f3cd47 | `NpcFinder`·`NpcEditor`(이름 유일 대소문자 무시, `existsByNameIgnoreCaseAndIdNot` 추가), `/api/admin/npcs` 5개, `RoomDetailResponse.npcs`, `RoomDeletedEvent` 동기 리스너로 배치 풀기, `isNameTaken` 이 NPC 이름도 봄(`create` 가 재사용). Task 3 잔여: `Npc` 의 `@ElementCollection` 제거(orm.xml만), 낱말 중복 검사를 `NpcTopic.matches` 와 같은 규칙으로. `llmEnabled` 는 원시 boolean이라 요청에서 빠지면 400(Jackson 3) |
| 5. 게임 — 방 머리 소유, 방의 NPC, `대화` 명령, 도움말, WebSocket 메시지 모양 | e38ce17 | `Command.Talk`·`CommandParser`(낱말은 `Names.stripBrackets`, `대화 왕씨 []` 는 인사로 봄 (제안)), `RoomMessage.owner`(`OwnerView`)·`OccupantView.npc`·`TalkMessage`, `GameService` 가 방 보기마다 `NpcFinder.findAllInRoom` 을 읽음. `WebSocketGameMessageSender` 는 고치지 않음(`owner: null` 이 그대로 나감, 테스트로 확인). 테스트 준비는 `BaseGameTest.prepareNpcs(map)`(P3), `prepareMap` 은 그대로 |
| 6. admin-console — 성·종문, 지역 소유, NPC 화면 | b0d1b40 | `/factions/`·`/npcs/`·`/npc/` 화면, 지역 폼 `소유` 선택·목록 `소유` 열, 방 화면 `이 방의 NPC`. 지역→방 고르기를 `RoomPicker` 로 뽑아 `ExitEditor` 도 씀(P5, 동작·문구 그대로). 빈 낱말 행 빼기는 `TopicEditor` 의 `withoutEmptyRows`. NPC 배치는 지역 선택 첫 항목 `배치 안 함`, 방 선택 첫 항목 `방 선택` (제안). 고친 뒤 폼을 서버 값으로 다시 연다 |
| 7. play-client — 소유 머리, NPC 줄, `talk`, 자동완성 | 0963b92 | `protocol.ts` 에 `RoomOwner`·`Occupant.kind` `npc`·`TalkMessage`, 방 이름 줄 `청석성 남문  [청석성]`(style `room-name`), NPC 줄 style `npc`, `talk` 는 줄마다 style `talk`, `COMMANDS` 에 `대화`(`말하기` 다음). `Terminal` 이름 후보는 1차부터 kind를 거르지 않아 주석만 고침. 색 `--npc: #d9a3c7`·`--talk: #e8b48a` (제안) |

## 상태

- 계획 작성 (2026-11-14). 구현은 아직 시작하지 않았다.

## 리드 판정 (Ruling)

- (없음)

## 다음 할 일

- Task 1: 성·종문 도메인·영속과 지역 소유 — 관리 서비스까지.
