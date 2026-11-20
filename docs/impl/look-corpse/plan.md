# 시체 보기(look-corpse) 구현 계획

**목표:** `보기`·`봐`·`보 <시체>` 로 시체에 남은(수습할 수 있는) 물건을 `청모랑의 시체 — 거둘 것: 청모랑 가죽 1, 늑대 이빨 1` 로 보인다.

**스펙:** 결정 요약 [index.md `### 시체 보기 (look-corpse)`](../../spec/system/index.md#시체-보기-look-corpse), 세부 [command.md `## 시체 보기 (look-corpse)`](../../spec/system/command.md#시체-보기-look-corpse), 룰북 [시체와 수습](../../spec/game-rule/npc.md#시체와-수습)·[이동 · 보기](../../spec/game-rule/commands.md#이동--보기).

**새 의존성 없음. DB·메시지 모양 변경 없음.** 창호 요청 2번(여러 시체 고르기)은 이번 범위 밖.

## 바뀌는 파일

| 파일 | 바뀌는 것 |
|---|---|
| `world-server/.../application/game/ItemCommandHandler.java` | `LookTarget` 에 시체를 담고, 시체면 `CorpseHandler.look` 결과를 보낸다 |
| `world-server/.../application/game/CorpseHandler.java` | `look(Corpse, 보는 사람, now)`: 지금 종류 이름으로 남은 줄(지워진 종류 빼고), 내 몫이 아니면 둘째 줄 |
| `world-server/.../application/game/CombatTexts.java` | `lookCorpse(이름, 줄들)`·빈 시체 문구 |
| `scripts/smoke-test.mjs` | 시체 보기 단계 |
| `README.md` | 보기 줄 |

## 테스트 (RED 먼저, `CombatRewardTest`)

- 처치자 `봐 시체` → 거둘 것 줄, 시체·소지품 그대로, 다른 사람 알림 없음. 기존 `lookCorpseNotYet`(아직 자세히 볼 수 없음)은 뜻이 바뀌어 이 테스트로 바꿈.
- 다른 사람 `보 시체`(26초) → 둘째 줄 `(34초 뒤 …)`, 60초 뒤 → 첫 줄만.
- 빈 시체 → `— 거둘 것이 없다.`, 시체는 남음.
- 일부 수습 뒤 → 남은 것만.
- 시체 둘: `봐 시체` 첫째, `봐 2.시체` 둘째.
- 지워진 종류는 안 보임.
