# 방위 보기(look-direction) 진행

상태: **구현 완료 (통합 확인 남음 — 스모크 실제 실행은 리드가 따로)**

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1. 스펙·룰북·계획 | b92cd80 | command.md `## 방위 보기 (look-direction)`, index.md 결정 요약, 룰북 이동·보기 표·방 보기 |
| 2. world-server | b5a4273 | `Command.LookToward(Direction)`, `CommandParser.parseLook`(인자 전체가 `Direction.fromCommand` 에 맞으면 방위 보기), `GameService.lookToward`(`system/info` `<towardPhrase> <이름><이/가> 보인다.`, 없으면 `NO_EXIT`), `HELP` 에 `보기 <방위>`, README. RED: 컴파일 실패 → 스텁 뒤 8 실패 → GREEN. `./gradlew build` 1568 통과. 바뀐 기존 테스트: `CommandParserTest.lookKeepsDirectionArgument` 지움(뜻이 바뀜, `lookToward`·`lookTowardNeedsWholeArgumentToBeDirection` 이 대신), `GameConnectorTest.lookShortcutsWithTarget` 의 `봐 동`·`보 ㄷ` 기대값(대상 없음 → 길 없음), `helpText` |
| 3. 스모크 | 8c58070 | `HELP_6` 새 문구, 단계 56 `SMOKE OK` 앞(광장 위 `누각<번호>` 를 만들고 끝에 지움, 동은 단계 36 의 사냥터). `node --check` 만 함 |
| 4. 세 모듈 검증 | — | world-server build 1568, play-client 133, admin-console 209, 둘 다 tsc·build (Node 26.13.1). play-client 코드 변경 없음 |

## 알려진 한계

- 방위 낱말과 같은 이름(`동`·`e`·`ㄷ` 등)의 NPC·물건은 `보기 <그 이름>` 으로 볼 수 없다(방위가 먼저). 관리 툴은 그런 이름을 막지 않는다.
- 이름 있는 출구·비밀문이 생기면 방위 보기 대상 밖으로 두고 `보기 <출구 이름>` 은 그때 정한다.
