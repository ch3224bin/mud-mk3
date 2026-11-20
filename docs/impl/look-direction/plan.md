# 방위 보기(look-direction) 구현 계획

**목표:** `보기`·`봐`·`보` 뒤에 방위 낱말(이름·초성 약자·영문)을 쓰면 그 방위 출구 너머 방 이름을 `동쪽으로 숲 입구가 보인다.` 로 보인다.

**스펙:** 결정 요약 [index.md `### 방위 보기 (look-direction)`](../../spec/system/index.md#방위-보기-look-direction), 세부 [command.md `## 방위 보기 (look-direction)`](../../spec/system/command.md#방위-보기-look-direction), 룰북 [이동 · 보기](../../spec/game-rule/commands.md#이동--보기)·[방 보기](../../spec/game-rule/world.md#방-보기).

**새 의존성 없음. DB·메시지 모양 변경 없음.**

## 결정

- 요청: 창호 지시 (2026-11-20). 방위 낱말 셋 다·방 이름만·길 없음 문구·방위 먼저·위아래: 고나 판단 (2026-11-20, 창호 위임). 문체·대상 밖 출구·상태 막힘·메시지 종류·도움말은 (제안).

## 바뀌는 파일

| 파일 | 바뀌는 것 |
|---|---|
| `world-server/.../domain/game/Command.java` | 새 명령 `LookToward(Direction)` |
| `world-server/.../domain/game/CommandParser.java` | `보기`/`봐`/`보` 의 인자 전체가 `Direction.fromCommand` 에 맞으면 `LookToward`, 아니면 지금처럼 `Look(인자)` |
| `world-server/.../application/game/GameService.java` | `LookToward` 처리(출구 있으면 `system/info` `<towardPhrase> <이름><이/가> 보인다.`, 없으면 `NO_EXIT` error), `HELP` 에 `보기 <방위>` |
| `scripts/smoke-test.mjs` | `HELP_6` 문구, 새 단계 56 |
| `README.md` | 명령 표 한 줄·도움말 |

play-client: 코드 변경 없음(`system` 줄을 그대로 그림, 자동완성 그대로).

## 테스트 (RED 먼저)

- `CommandParserTest.lookToward`: `봐 동`·`보 ㄷ`·`보기 e`·`보기 E`·`봐 위`·`보 ㅇㄹ` → `LookToward`. `보기 동 왕씨`·`보기 동쪽` → `Look`. 기존 `lookKeepsDirectionArgument` 는 이번 일로 뜻이 바뀌어 지움(새 테스트가 대신함).
- `GameConnectorTest`: 이름(`보기 북` → `북쪽으로 우물가가 보인다.`, 받침 없음)·약자(`봐 ㅂ` 우물가에서 → `북쪽으로 남문이 보인다.`, 받침 있음)·영문(`보 e`), 위·아래(`위로 …`·`아래로 …`), 출구 없음(error), 방위 이름 NPC `동` 이 있어도 방위 먼저, 보고 나서도 방이 그대로, 교전 중에도 됨. 기존 `lookShortcutsWithTarget` 의 `봐 동`·`보 ㄷ` 기대값(`그런 것은 보이지 않습니다.`)은 길 없음으로 바뀜. `helpText` 새 문구.

## 스모크 단계 56

`SMOKE OK` 앞. 그 시점 광장에 있는 출구는 동(`사냥터<번호>`, 단계 36 에서 만들고 지우지 않음)뿐이다(북쪽 길은 단계 9, `북뜰` 은 단계 55 에서 지움). 단계 56 이 광장 위에 `누각<번호>`(한쪽)를 만들고 끝에 지운다. 새 플레이어 I(광장): `봐 ㄷ`·`보기 e`·`보 동` → `동쪽으로 사냥터<번호>가 보인다.`(info), `보기 위` → `위로 누각<번호>가 보인다.`, `봐 ㄴ` → 길 없음(error), `보` → 광장 room(움직이지 않음), `종료` → 누각 DELETE 204 → 광장에 위 출구 없음.
