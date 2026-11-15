# 6차 이터레이션 진행

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1. 신통 정의 보강 — 단계별 값·이펙트 문장 도메인·영속·관리 서비스·REST | | `AbilityStagePower`(embeddable)·이펙트 문장 값 컬렉션(표 `ability_effect_line`·`ability_stage_power`, 둘 다 `position` 순서 열), 6차 검사(D99, 이펙트 문장은 검사 우선 F17), `usableInGame`, 요청·응답 세 칸(`StagePowerRequest` 세 칸 필수 → JSON 400), 픽스처 기본값(D117: `ability()` 공격은 배우는 단계~9성 값, `changchunWithStages` 는 단계 수에 맞춤 F3, 도우미 `stagePowers`·`withEffectLines`·`withStagePowers`·`withCast`·`withCost`, 더해서 `upToStage`). 계획과 다른 점: `RequestValues.list` 를 더해 `effectLines`·`stagePowers` 안의 null 을 `IllegalArgumentException("<필드> 에 빈 값이 있습니다")` 로 막음. 9단계가 아닌 공법을 쓰던 1~5차 테스트(TechniqueTest·AbilityTest·CultivationTextsTest·TechniqueReaderTest·TechniqueEditorTest·TechniqueRepositoryTest)는 `upToStage`/`withStagePowers` 로 고침. 스모크 단계 26·admin-console 저장은 아직 새 칸을 안 보내 400 — Task 8·11 |
| 2. 기존 신통 채우기 — 서버가 뜰 때 | | `TechniqueModifyService.fillMissingAbilityData()`(`@EventListener(ApplicationReadyEvent.class) @Order(20) @Transactional`, 경지 채우기 0·영석 심기 10 다음)가 `findAllByOrderByNameAsc` 로 모든 공법을 읽어 `Technique.fillMissingAbilityData() → int` 를 부르고, 채운 공법만 저장한 뒤 `기존 신통 {}개의 이펙트 문장·단계별 값을 채웠습니다.` 로그. `Ability.fillMissing(int stageCount)`(package-private): 이펙트 문장이 비면 `SINGLE` 은 `{대상}에게 <이름>을/를 펼친다!`, 그 밖은 `<이름>을/를 펼친다.`(`Josa.withEulReul`) 한 줄; 공격이고 단계별 값이 비면 배우는 단계~마지막 단계를 `BigDecimal` 로(F15) 오행 2·1.2·+0.2, 연체 5·2.2·+0.5(기본 위력 scale 2, 계수 scale 3). 멱등(빈 칸만 채움), 검사 우회(6차 검사를 거치지 않음 — 이름에 `{` 가 든 옛 신통도 그대로 채움), 공격이 아니면 단계별 값 없음. `TechniqueRepository` 는 고치지 않음(이미 있는 `findAllByOrderByNameAsc` 사용). 테스트: `TechniqueFillTest`(9개 + 리스너 애노테이션 확인 `fillOnStartupListener`), `TechniqueTest.fillMissingAbilityDataCount` |
