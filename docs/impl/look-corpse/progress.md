# 시체 보기(look-corpse) 진행

상태: **시체 보기 완료 (통합 확인 통과)**

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1. 스펙·룰북·계획 | 717be1e | command.md `## 시체 보기 (look-corpse)`, index.md 결정 요약, 룰북 시체와 수습·이동 · 보기 표, 5차 '시체는 자세히 볼 수 없다' 에 바뀜 표시, README 보기 줄 |
| 2. world-server | 0098cff | `CorpseHandler.look`(지금 종류 이름, 지워진 종류는 안 보이고 빼지 않음, 내 몫이 아니면 둘째 줄 `otherShare`), `CombatTexts.lookCorpse`, `ItemCommandHandler` 의 `LookTarget.corpse`. RED: 새 테스트 6 실패 → GREEN. 바뀐 기존 테스트: `CombatRewardTest.lookCorpseNotYet` 을 `lookCorpseShowsDrops` 로 바꿈, `CombatSpawnTest.lookOrderPrefersMob` 의 시체 기대값(아직 자세히 볼 수 없음 → 거둘 것이 없다) |
| 3. 스모크 | 31c9aff | 단계 39 에 E `봐 시체`(한 줄, `토끼 가죽 1`)·D `보 시체`(같은 첫 줄, 60초 안이면 둘째 줄 남은 시간). 단계 수는 그대로 |
| 4. 리뷰(opus) Minor | d84888b | Critical·Important 없음. 시체 목록을 `corpseHandler.inRoom` 으로, 지워진 종류만 남은 시체 테스트, 스모크 수 검사(`1` 뒤 `,` 또는 끝). 교전 중 보기·처치자 없는 시체 테스트는 안 더함(보기는 막힘 검사가 없고, 권한 판정은 수습과 같은 `collectibleBy`) |
| 5. 세 모듈 검증 | — | world-server `./gradlew build` 1574 통과, play-client 133, admin-console 209, 둘 다 tsc (Node 22.15.1). play-client·admin-console 코드 변경 없음 |
| 6. 통합 확인 | 이 커밋 | 원래 stack 은 이미 멈춰 있었고(창호가 멈춤) 건드리지 않음. 사본 project `mud-mk3-wt-look-corpse`, 저장소 밖 override(포트 18080·13000·13001 `!override`, CORS 에 스모크 Origin 3000·3001). `config` 로 호스트 포트·볼륨 `mud-mk3-wt-look-corpse_pgdata` 만 확인. 원래 postgres 가 멈춰 있어 pg_dump 대신 `mud-mk3_pgdata` 를 읽기 전용으로 붙인 일회용 컨테이너에서 사본 볼륨으로 `cp -a`(64.6M). postgres·world-server 만 올림(클라이언트 변경 없음, 스모크는 world-server 만 씀). 기동 로그 ERROR/Exception 0 → 스모크 단계 1~56 `SMOKE OK`(실행 id `mwonh6e3`), 스모크 뒤 로그 ERROR/Exception 0. 정리: `down -v --rmi local`, `.env` 사본·override 삭제. `mud-mk3_pgdata` 남음, 원래 컨테이너 Exited 그대로, 원래 디렉토리 `git status` 비어 있음 |

## 알려진 한계

- `봐 청모랑` 은 살아 있는 청모랑이 있으면 몹이 먼저다(지금 대상 고르기 순서). 시체는 `봐 시체`·`봐 청모랑의`·번호로 본다.
- 여러 시체 고르기 개선(창호 요청 2번)은 다음 일.
