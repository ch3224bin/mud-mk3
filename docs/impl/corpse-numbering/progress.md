# 시체 번호(corpse-numbering) 진행

상태: **시체 번호 완료 (통합 확인 통과)**

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1. 스펙·룰북·계획 | 67abec9 | command.md `## 시체 번호 (corpse-numbering)`, platform.md `### 시체 번호 WebSocket 메시지 변경`, index.md 결정 요약, 룰북 방 보기·대상 지정, README 확인 목록 |
| 2. world-server·play-client | d4278fc | `CorpseView.ordinal`(시체 둘 이상이면 쓰러진 순 1 부터, 하나면 null) — `RoomViewAssembler.corpseViews`. play-client `Corpse.ordinal`, `render.ts` `corpseLine`(`N.이름`). RED: Java 컴파일 실패·render 1 실패 → GREEN. 바뀐 기존 테스트: `CorpseView` 2 인자 → `null` 셋째 인자(5 곳), WebSocket 직렬화 테스트에 `ordinal`, play-client fixture 에 `ordinal: null`. 새 테스트: `numbersCorpsesMatchOrdinalSelection`(일부 수습으로 두 시체를 가르고 `봐·수습 2.시체` 가 2 번 줄), `numbersMixedCorpsesTogether`, render 번호 줄 |
| 3. 스모크 | f183596 | 단계 39 기대값에 `ordinal: null`. 새 단계 57: 사냥터에 드롭 없는 `들개<번호>` 둘 → J(축기 초기) 둘 다 처치 → `보기` 시체 `ordinal` 1·2 → `봐 2.시체`(item)·`봐 3.시체`(error) → `수습 2.시체` → `보기` 시체 하나 `ordinal: null` → 몹 삭제 |
| 4. 리뷰(opus) Minor | e66da8f | Critical·Important 없음(번호 순서와 고르기 순서가 같은 저장소 순서임을 확인). `ordinal == null`(필드 없음도 이름만), 스펙에 `시체` 로 시작하는 사람·NPC·몹 이름이면 `보기` 번호가 밀림, `@see CorpseHandler#inRoom` |
| 5. 세 모듈 검증 | — | world-server build 1576, play-client 133→134·tsc·build, admin-console 209·tsc (Node 22.15.1) |
| 6. 통합 확인 | 이 커밋 | 원래 stack 은 멈춰 있었고 건드리지 않음. 사본 project `mud-mk3-wt-corpse-numbering`, 저장소 밖 override(포트 18080·13000·13001), `config` 로 포트·볼륨 확인, `mud-mk3_pgdata` 를 읽기 전용으로 붙여 사본 볼륨에 `cp -a`. 네 서비스 build·up(play-client 이미지 빌드 포함), 기동 로그 ERROR/Exception 0 → 스모크 단계 1~57 `SMOKE OK`(실행 id `mwoobxde`), 뒤 로그 ERROR/Exception 0. 리뷰 Minor(주석·play-client 표시)는 스모크 뒤라 스모크를 다시 돌리지 않음(스모크는 world-server 메시지만 봄, 서버 쪽은 주석만 바뀜). 정리: `down -v --rmi local`, `.env` 사본·override 삭제. `mud-mk3_pgdata` 남음 |

## 알려진 한계

- 번호는 `N.시체` 용이다. 이름이 다른 시체가 섞이면 `2.청모랑` 처럼 이름으로 센 번호와 화면 번호가 다를 수 있다.
- 사람·NPC·몹 이름에 `시체` 로 시작하는 낱말이 있으면 `보기` 의 번호가 밀린다(`수습` 은 그대로).
- 시체가 사라지면 번호가 당겨지는데 화면 줄은 다시 그려지지 않는다(조용한 방 상태는 후보만 바꿈). `보기` 를 다시 하면 맞는다.
