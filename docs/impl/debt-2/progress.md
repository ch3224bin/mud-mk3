# 기술 부채 정리 2차(debt-2) 진행

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1. 이펙트 문장 조사는 뒤가 끊길 때만 (#19) | | 조사 판정: 자리표시 바로 뒤 조사 글자(`으로` 먼저) 다음이 문장 끝 또는 `!Character.isLetterOrDigit` 이면 조사, 아니면 쌍별 허용 꼬리(`으로/로`: 부터·써·서·도·는·만·의, `과/와`: 도·는·의·만) 뒤가 경계일 때만 조사(앞 조사만 고침), 그 밖은 그대로 둠. `EffectLine.render`·`Particle` record(+`tails`)·`isParticle`/`isBoundary` 추가. 테스트 `EffectLineTest.particleOnlyBeforeBoundary`·`allowedTails`. F-11: 경계는 계획대로 `isLetterOrDigit`(한자·가나 뒤도 '글자'로 봐 조사 아님 — 스펙 '한글·영문' 과 다르나 실제 차이 거의 없음). |
| 2. 시전 발동 때 영력 0 재확인 (#24-1) | | `AbilityCaster.fire` 의 재확인: 시전 지움 → 세계에 없음 → 신통·대상(흩어짐) → 영력 0 이고 연체 아님이면 `error` `AbilityTexts.NO_SPIRIT` 하고 끝(소모·재사용 대기·교전 시작 없음) → 법력 → `cast`. 능력치는 한 번 읽어 법력 확인에도 씀. 테스트 `AbilityCastingTest.castingNoSpiritAtFire`·`castingNoSpiritZeroCostElementalAtFire`·`castingBodyRefiningAtMortalFires`(F-7: `TechniqueFixture.withCost`·`withCast`). 소모 없는 오행 신통은 영력 0 이면 위력 0 이라 고치기 전에도 기혈이 그대로였음 — 차이는 거절 줄·교전 시작으로 봄. #24(2) 재사용 대기 시작 시각은 기록만(L-3). |
