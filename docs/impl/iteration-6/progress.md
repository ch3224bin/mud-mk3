# 6차 이터레이션 진행

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1. 신통 정의 보강 — 단계별 값·이펙트 문장 도메인·영속·관리 서비스·REST | | `AbilityStagePower`(embeddable)·이펙트 문장 값 컬렉션(표 `ability_effect_line`·`ability_stage_power`, 둘 다 `position` 순서 열), 6차 검사(D99, 이펙트 문장은 검사 우선 F17), `usableInGame`, 요청·응답 세 칸(`StagePowerRequest` 세 칸 필수 → JSON 400), 픽스처 기본값(D117: `ability()` 공격은 배우는 단계~9성 값, `changchunWithStages` 는 단계 수에 맞춤 F3, 도우미 `stagePowers`·`withEffectLines`·`withStagePowers`·`withCast`·`withCost`, 더해서 `upToStage`). 계획과 다른 점: `RequestValues.list` 를 더해 `effectLines`·`stagePowers` 안의 null 을 `IllegalArgumentException("<필드> 에 빈 값이 있습니다")` 로 막음. 9단계가 아닌 공법을 쓰던 1~5차 테스트(TechniqueTest·AbilityTest·CultivationTextsTest·TechniqueReaderTest·TechniqueEditorTest·TechniqueRepositoryTest)는 `upToStage`/`withStagePowers` 로 고침. 스모크 단계 26·admin-console 저장은 아직 새 칸을 안 보내 400 — Task 8·11 |
