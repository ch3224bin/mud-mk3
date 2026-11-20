# 시체 번호(corpse-numbering) 구현 계획

**목표:** 방에 시체가 둘 이상이면 방 보기 시체 줄에 `1.` `2.` … 번호를 붙여 `봐 2.시체`·`수습 2.시체` 에 쓸 번호를 보인다.

**스펙:** [index.md `### 시체 번호`](../../spec/system/index.md#시체-번호-corpse-numbering), [command.md `## 시체 번호`](../../spec/system/command.md#시체-번호-corpse-numbering), [platform.md 메시지 변경](../../spec/system/platform.md#시체-번호-websocket-메시지-변경), 룰북 방 보기·대상 지정. 결정: 창호 결정 (2026-11-20).

**새 의존성·DB 변경 없음.** `room.corpses[]` 에 `ordinal` 한 칸.

## 바뀌는 파일

| 파일 | 바뀌는 것 |
|---|---|
| `world-server/.../GameMessage.java` | `CorpseView(name, collectible, @Nullable Integer ordinal)` |
| `world-server/.../RoomViewAssembler.java` | 시체가 둘 이상이면 1 부터 번호 |
| `play-client/src/lib/protocol.ts`·`render.ts` | `ordinal` 이 있으면 `N.이름` |
| `scripts/smoke-test.mjs` | 시체 둘 → 번호 → `봐 2.시체`·`수습 2.시체` 단계 |

## 테스트 (RED 먼저)

- world-server: 시체 하나면 `ordinal` null, 둘이면 1·2, `봐 2.시체`·`수습 2.시체` 가 `ordinal` 2 줄의 시체를 가리킴(일부 수습으로 두 시체를 가름), 다른 이름 섞임. WebSocket 직렬화에 `ordinal`.
- play-client `render.test.ts`: 번호 있음·없음.
