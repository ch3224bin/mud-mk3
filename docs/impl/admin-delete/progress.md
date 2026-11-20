# 관리 툴 삭제(admin-delete) 진행

상태: **진행 중**

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1. 몹 삭제 — 배치와 함께 지우고 로그 (world-server) | | 지우는 순서: `MobTypeModifyService.delete` 가 `mobTypeFinder.find`(없으면 400) → `MobPlacementRepository.deleteByMobTypeId`(파생 삭제, 지운 수) → `mobTypeRepository.delete`(드롭 표 값 컬렉션 함께) — 배치를 먼저 지워 `mob_placement.mob_type_id` 외래 키에 걸리지 않음(실제 커밋하는 `AdminApiIntegrationTest` 로 확인). 반환 `DeletedMobType(id, name, placementCount)`. `MobApi.delete` 가 포트가 돌아온 뒤(커밋 뒤) `SessionAuthenticator.principalOf(authentication).orElseThrow()` 로 관리자를 읽어 `info` 로그 `관리자 삭제 — 몹 id: …, 이름: …, 함께 지운 배치: …, 관리자 계정 id: …, 이메일: …`. `MobTypeInUseException`·`checkDeletable` 지움(`ApiControllerAdvice` 목록에서도 뺌). 바뀐 기존 테스트: `MobTypeEditorTest.deletePlacedFail` → `deletePlaced`(배치 둘, 반환값·몹 400·방 배치 빔), `delete` 에 `placementCount` 0 단언, `MobApiWebMvcTest.deletePlacedFail` → `deleteLogs`, `delete` 에 stub(F-1), `AdminApiIntegrationTest.manageMobs` 의 몹 409 단언과 배치 DELETE 204 줄을 지워 배치가 있는 채 몹 DELETE 204 → 방 `$.mobs` 빔 → 몹 GET 400(F-2, 물건 409·배치 PUT 은 이미 앞). 더한 테스트 `MobApiWebMvcTest.deleteUnknownFail`(거절은 로그 없음). 교전 중 몹: `CombatSpawnTest.mobTypeDeletedRemovesLivingMob` — `공격 청모랑` 으로 교전 중 몹 유형 삭제 → `advanceAndTick(1)` 에 quiet room 하나(청모랑 없음)·알림 없음·몹 인스턴스 없음, `북` 으로 움직이고 다음 틱도 예외 없음. `countByMobTypeId` 는 main 에서 안 쓰이나 `MobPlacementRepositoryTest` 가 써서 남김. |
