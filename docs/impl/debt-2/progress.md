# 기술 부채 정리 2차(debt-2) 진행

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1. 이펙트 문장 조사는 뒤가 끊길 때만 (#19) | | 조사 판정: 자리표시 바로 뒤 조사 글자(`으로` 먼저) 다음이 문장 끝 또는 `!Character.isLetterOrDigit` 이면 조사, 아니면 쌍별 허용 꼬리(`으로/로`: 부터·써·서·도·는·만·의, `과/와`: 도·는·의·만) 뒤가 경계일 때만 조사(앞 조사만 고침), 그 밖은 그대로 둠. `EffectLine.render`·`Particle` record(+`tails`)·`isParticle`/`isBoundary` 추가. 테스트 `EffectLineTest.particleOnlyBeforeBoundary`·`allowedTails`. F-11: 경계는 계획대로 `isLetterOrDigit`(한자·가나 뒤도 '글자'로 봐 조사 아님 — 스펙 '한글·영문' 과 다르나 실제 차이 거의 없음). |
