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
| 8. 스펙 반영 — 제안 결정을 시스템 스펙에 `(제안)` 으로 | 916eb43 | D19~D31을 `index.md`(2차 범위에 계획 링크·구현 날짜, 2차 열린 질문 9개에 `→ (제안)` 답)·`npc.md`(`## 2차 데이터 모델`, LLM 켬 동작, 캐릭터 설정 3차로 미룸)·`world-structure.md`(`### 2차 데이터 모델`)·`admin-tool.md`(`## 2차 화면`·`## 2차 REST API`·반영)·`command.md`(`대화` 해석과 출력 절, 금지어 줄 P7)·`platform.md`(`### 2차 WebSocket 메시지 변경`, 방 그림, 슬라이스)에 옮김. 구현 판정도 반영: NPC 이름 대소문자 무시(P6), `llmEnabled` 필수(T4-1), `대화 왕씨 []` → 인사(T5-1), 지역만 고르면 배치 안 함(T6-2), 잘못된 `kind` → 400 공통 문구. 계획과 코드의 문구 차이 없음(배치 방 없음은 `방을 찾을 수 없습니다. id: 9`) |
| 9. README — 2차 시나리오와 명령 | 916eb43 | 모듈 소개에 2차 한 줄, `## 2차 시나리오`(성 → 지역 소유 → NPC → play-client 확인 → `대화`), `### 되는 명령`(`대화 <NPC> [낱말]`·도움말 문구·`말하기` 와 `대화` 구분), 브라우저 수동 확인 2차 항목, `pgdata` 에서 `ddl-auto=update` 한 줄. world-server 도움말에 `대화 <NPC> [낱말]` 이 이미 있음을 확인 |
| 10. 통합 확인 — compose 한 번 빌드·실행, 스모크 2차 시나리오, 정리 | TBD | `smoke-test.mjs` 에 단계 11~15(성·종문·지역 소유 → NPC 상세 → 방 머리 `owner`·occupants NPC → `대화` 인사·낱말·기본 대답·도움말 → 성 삭제 409·방 삭제 뒤 NPC 배치 풀림). 이름에 숫자 6자리 실행 번호(NPC 12자 제한). 모듈 코드 변경 없음, 스모크 첫 실행에 통과. README: 소유 열 `청석성 (성)`, 예시 방을 1차 `청석숲 오솔길` 로, `## 되는 명령` 을 2차 시나리오 밖 최상위 절로, 스모크 절 2차 항목. command.md 인사 줄 모양이 룰북 서술형 예시와 다르다는 메모 |

## 상태

- **2차 이터레이션 구현 완료** (2026-11-14). 남은 것: 브라우저 수동 확인(README 체크리스트 2차 항목), 리드 최종 리뷰.
- 검증 (Task 10):
  - world-server `./gradlew clean build` — BUILD SUCCESSFUL, 테스트 377개 통과(실패·건너뜀 0), SpotBugs 포함.
  - play-client `npm ci && npm test && npm run build` — 6 파일 67개 통과, 정적 export 빌드 성공.
  - admin-console `npm ci && npm test && npm run build` — 8 파일 41개 통과, 정적 export 빌드 성공.
  - compose: `docker compose build` 한 번(세 이미지) → `docker compose up -d` → `node scripts/smoke-test.mjs` 단계 1~15 `SMOKE OK`(첫 실행에 통과, 다시 빌드 없음).
  - `.env` 는 고치지 않았다(개발용 로그인 꺼짐·관리자 본인 이메일). 빌드는 `.env` 그대로(두 클라이언트는 개발용 로그인 폼 없음), `up` 때만 셸 값으로 `MUD_DEV_LOGIN_ENABLED=true`, `MUD_ADMIN_EMAILS=<기존>,smoke-admin@example.com` 을 덮어썼다. 스모크가 바꾼 시작 방은 끝난 뒤 원래 방(`태초 마을 / 태초 객잔`, id 1)으로 되돌렸다. 스모크 데이터(지역 `스모크 mwfouj8g`, 성 `청석성372896` 등)는 `pgdata` 에 남아 있다.
- 디스크 (Task 10):
  - 시작 전 `df -h .` 남은 공간 55Gi. `docker system df`: 이미지 2.382GB, 빌드 캐시 0B.
  - 스모크 뒤(정리 전) `docker system df`: 이미지 2.444GB, 빌드 캐시 985.5MB.
  - 정리: `docker compose down`(볼륨 `pgdata` 남김) → `docker image prune -f`(이번 빌드로 dangling 이 된 이전 `mud-mk3-*` 이미지 층, 62.4MB) → `docker builder prune -f`(922.9MB) → `world-server/build`, `play-client/{.next,out}`, `admin-console/{.next,out}` 삭제(`git check-ignore` 로 확인). `node_modules` 는 남김.
  - 정리 뒤 dangling 이미지 0개, `docker system df`: 이미지 2.382GB, 빌드 캐시 62.55MB(회수 가능 0B). `df -h .` 남은 공간 53Gi(`npm ci` 로 다시 설치한 `node_modules`·Gradle 캐시 차이).

## 리드 판정 (Ruling)

- (없음)

## 다음 할 일

- 리드 최종 리뷰, 브라우저 수동 확인(README `수동 확인 — 브라우저` 2차 항목).
