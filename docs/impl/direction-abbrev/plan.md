# 방위 초성 약자(direction-abbrev) 구현 계획

**목표:** 방위 이동을 한글 초성 약자(`ㄷ` `ㅅ` `ㄴ` `ㅂ` `ㅂㄷ` `ㅂㅅ` `ㄴㄷ` `ㄴㅅ` `ㅇ` `ㅇㄹ`)로도 하고, `보기` 줄임말을 `ㅂ` 에서 `봐`·`보` 로, `상태` 줄임말을 `ㅅ` 에서 `ㅅㅌ` 으로 바꾼다.

**스펙:** 결정 요약 [index.md `### 방위 초성 약자 (direction-abbrev)`](../../spec/system/index.md#방위-초성-약자-direction-abbrev), 세부 [command.md `## 방위 초성 약자 (direction-abbrev)`](../../spec/system/command.md#방위-초성-약자-direction-abbrev), 룰북 [줄임말](../../spec/game-rule/commands.md#줄임말).

**새 의존성 없음. DB·메시지 모양 변경 없음.**

## 결정

- 요청: 창호 지시 (2026-11-20). 방위 전부에 초성, 겹치면 방위가 먼저, `상태` 는 `ㅅㅌ`: 고나 판단 (2026-11-20, 창호 위임). `ㅇ`·`ㅇㄹ`·`ㅅㅌ`·도움말 문구는 (제안).
- `봐 동`(방위 보기)은 다음 일. 이번에는 `보기`/`봐`/`보` 인자를 그대로 `Look` 대상으로 넘긴다(지금 동작 유지).

## 바뀌는 파일

| 파일 | 바뀌는 것 |
|---|---|
| `world-server/.../domain/room/Direction.java` | 방위마다 초성 약자(`initials`) 필드, `fromCommand` 가 이름·영문·초성을 한곳에서 읽음(`봐 동` 이 나중에 재사용) |
| `world-server/.../domain/game/CommandParser.java` | `보기` 줄임말 `봐`·`보`(`ㅂ` 뺌), `상태` 줄임말 `ㅅㅌ`(`ㅅ` 뺌). `ㅂ`·`ㅅ` 은 `parseOther` 의 방위 풀이로 감 |
| `world-server/.../domain/technique/ReservedWords.java` | 새 낱말 11개(105 → 116) |
| `world-server/.../application/game/GameService.java` | `HELP` 문구 |
| `scripts/smoke-test.mjs` | `HELP_6` 문구, 새 단계 55 |
| `README.md`, 스펙·룰북 | 줄임말 표·도움말 |

play-client: 코드 변경 없음. `COMMANDS` 에는 전에도 줄임말이 없고, 자모는 음절 앞부분이 아니라 Tab 과 겹치지 않으며, IME 조합 중 Enter 는 이미 건너뛴다(`Terminal.handleKeyDown`).

## 테스트 (RED 먼저)

- `DirectionTest.fromCommandInitials`: 열 방위 초성 → 방위, `fromCommandFail` 에 `ㅂㅂ`·`ㄱ`.
- `CommandParserTest`: `moveByInitials`(`ㄷ`→EAST … `ㅇㄹ`→DOWN, `ㄷ 한립`→Unknown), `look`(`봐`·`보`, `봐 늑대 가죽`, `보 동`→`Look("동")`), `status`(`ㅅㅌ`, `ㅅㅌ 한립`→Unknown).
- `ReservedWordsTest`: 새 낱말 포함, 개수 116.
- `GameConnectorTest`: `ㅂ`→북 이동, `보`·`봐` 방 보기, `ㅅㅌ` 상태, 도움말 새 문구.

## 스모크 단계 추가안

`SMOKE OK` 앞 단계 55: 새 플레이어 H 생성(광장) → `ㅂ` 북쪽 길 → `ㄴ` 광장 → `ㄷ` 사냥터 → `ㅅ` 광장 → `보`·`봐` 광장 room → `ㅅㅌ` status → `도움말` 새 문구 → `종료`. `HELP_6` 를 새 문구로.

## 리드 판정 후보

- **L-1 `상태` 새 줄임말 `ㅅㅌ`.** 다른 후보는 `ㅅㄷ`(이미 없음)나 줄임말 없음. 초성 그대로라 `ㅅㅌ`.
- **L-2 위·아래 `ㅇ`·`ㅇㄹ`.** 초성 그대로. `ㅇ` 은 채팅의 '응' 으로 칠 수 있지만 위 출구가 없으면 길 없음 줄만 나온다.
- **L-3 도움말 모양.** 방위 괄호에 `ㄷ … ㅇㄹ / e … d` 로 초성과 영문을 `/` 로 나눔(` · ` 는 명령 구분이라 쓰지 않음).
- **L-4 기존 신통 이름.** 이미 `보`·`봐`·`ㅇ` 같은 이름으로 저장된 신통이 있으면 명령이 먼저라 그 신통은 쓸 수 없게 된다. 이주는 하지 않는다(관리 툴에서 다시 저장하면 예약어로 걸림).
- **L-5 `봐 동` 지금 동작.** 지금은 `동` 이라는 대상을 찾아 보통 `그런 것은 보이지 않습니다.` 다(스펙 `보기 <대상>` 그대로). 스모크에서는 보지 않는다(다음 일에서 바뀜).
