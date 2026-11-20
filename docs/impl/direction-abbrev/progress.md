# 방위 초성 약자(direction-abbrev) 진행

상태: **구현 끝, 스모크 실행은 리드**

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1. 스펙·룰북·계획 | 00675a3 | command.md `## 방위 초성 약자 (direction-abbrev)`, index.md 결정 요약, 룰북 줄임말 표·출구·방 보기(`ㅂ`·`ㅅ` 고침), admin-tool.md 예약어 수(116), README 명령 표·도움말 |
| 2. world-server 명령 해석 | 0ef1f7f | `Direction` 에 `initials`(`ㄷ`…`ㅇㄹ`), `fromCommand` 가 이름·영문·초성을 한곳에서 읽음. `CommandParser`: `보기`/`봐`/`보` → `Look(인자 그대로)`, `상태`/`ㅅㅌ`, `ㅂ`·`ㅅ` 은 `parseOther` 방위로. 예약어 11개 더함(105 → 116, 끝은 `공격` 그대로). `GameService.HELP` 새 문구. RED 16 실패 → GREEN, `./gradlew build` 1562 통과(SpotBugs·ArchUnit). 바뀐 기존 기대값: `CommandParserTest.look`(`ㅂ`→`봐`·`보`)·`notCommandIsAbility`(`보`→`보다`)·`status`·`statusWithArgumentFail`(`ㅅ`→`ㅅㅌ`), `ReservedWordsTest.listMatchesPlan`(105→116), `GameConnectorTest.lookShowsOthersExcludingSelf`(`ㅂ`→`보`·`봐`)·`statusShortcut`(`ㅅ`→`ㅅㅌ`)·`helpText`·`unknownCommand`(`보`→`보다`) |
| 3. play-client | b109a9f | 코드 변경 없음. 지키는 테스트만: `Terminal.test` 초성 그대로 보냄·조합 중 Enter 무시(가드를 빼면 실패함 확인), `complete.test` 초성 + Tab 은 그대로. 133 통과·tsc·build |
| 4. 스모크 | 919f271 | `HELP_6` 새 문구, 단계 55(H: `ㅂ`→북쪽 길, `ㄴ`→광장, `ㄷ`→사냥터, `ㅅ`→광장, `보`·`봐` room, `ㅅㅌ` status, 도움말) `SMOKE OK` 앞. `node --check` 만, 실행은 리드 |
| 5. 세 모듈 검증 | — | world-server build 1562, play-client 133, admin-console 209, 둘 다 tsc·build (Node 26.13.1) |

## 알려진 한계

- 이미 `보`·`봐`·`ㅇ` 같은 이름으로 저장된 신통이 있으면 명령이 먼저라 쓸 수 없다(L-4, 이주 없음).
- `봐 동` 은 다음 일. 지금은 `동` 이라는 대상을 찾는다.
