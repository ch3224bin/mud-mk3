# 2차 이터레이션 진행

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1. 성·종문 도메인·영속과 지역 소유 — 관리 서비스까지 | 5e91716 | `faction` 슬라이스(도메인·orm.xml·리포지토리·FactionFinder/Editor), `Zone.owner`·`ZoneSaveRequest.ownerId`. `ZoneSaveInfo` 는 바꾸지 않음(소유는 `create/update` 의 따로 받는 인자) |
| 2. 성·종문 관리 REST API와 지역 소유 지정 | 97153d0 | `/api/admin/factions` 4개(`FactionApi`·`FactionResponse`), `ZoneResponse` 에 `ownerId`·`ownerName`·`ownerKind`, 409 처리 2개. 성·종문 삭제의 동시 소유 지정 경합은 1차 지역 삭제와 같게 두었다 |
| 3. NPC 도메인과 영속 | 0f99476 | `npc` 슬라이스(`Npc`·`NpcTopic`·`NpcSaveInfo`·`DuplicateNpcNameException`·`NpcRepository`), orm.xml `npc`·`npc_topic`(`position` 순서 열, 낱말 DB 유니크 없음). `[ ]` 떼기는 공용 `Names.stripBrackets`(Task 5 `CommandParser` 가 다시 씀). 도메인 이름 규칙 위반 문구 `NPC 이름은 띄어쓰기 없이 1~12자로 정하십시오: <이름>` (제안) |
| 4. NPC 관리 서비스·REST API, 방 삭제 시 배치 풀기, 캐릭터 이름 검사 | TBD | `NpcFinder`·`NpcEditor`(이름 유일 대소문자 무시, `existsByNameIgnoreCaseAndIdNot` 추가), `/api/admin/npcs` 5개, `RoomDetailResponse.npcs`, `RoomDeletedEvent` 동기 리스너로 배치 풀기, `isNameTaken` 이 NPC 이름도 봄(`create` 가 재사용). Task 3 잔여: `Npc` 의 `@ElementCollection` 제거(orm.xml만), 낱말 중복 검사를 `NpcTopic.matches` 와 같은 규칙으로. `llmEnabled` 는 원시 boolean이라 요청에서 빠지면 400(Jackson 3) |

## 상태

- 계획 작성 (2026-11-14). 구현은 아직 시작하지 않았다.

## 리드 판정 (Ruling)

- (없음)

## 다음 할 일

- Task 1: 성·종문 도메인·영속과 지역 소유 — 관리 서비스까지.
