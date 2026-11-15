# 5차 이터레이션 진행

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1 몹 유형 — 도메인·영속·관리 서비스, 드롭 표의 아이템 사용 검사 | 452d165 (리뷰 ✅, Minor 5) | `mob` 슬라이스(MobType·드롭 표 `MobFixedDrop`·`MobTierDrop`·enum 셋 `MobKind`·`MobTier`·`DropTier`·`MobStats`·`DuplicateMobNameException`), 표 셋(`mob_type`·`mob_fixed_drop`·`mob_tier_drop`, 드롭 물건 FK 없음), `Realm.majorIndex/beastGrade`, 드롭 물건 쓰이는 검사(`ItemTypeRepository.countInMobFixedDrops`·`countInMobTierDrops`). `MobTypeFinder`·`MobTypeEditor`·`MobTypeView`(+`FixedDropView`·`TierDropView`·`PlacementView`, `placements` 빈 목록 — Task 2), `MobTypeModifyService.checkDeletable` 빈 자리(Task 2). 계획과 다른 점: `MobStats.of` 는 `Math.round` 대신 십진(`BigDecimal`) 곱 + HALF_UP(양수라 같은 결과, 연기 6층 두목 0.5·축기 중기 두목 0.7 의 x.5 경계를 double 오차 없이), 공격 계수는 scale 1 로 저장(`0.60` → `0.6`), 요청 필수 enum·드롭 줄 `tier`·`itemTypeId`·목록 안 null 은 컴팩트 생성자 `IllegalArgumentException`(`<필드> 는 꼭 보내야 합니다`·`<필드> 에 빈 값이 있습니다`), 고정 재료 수는 `int`(빠지면 Jackson 공통 400). 계층 드롭 검사는 계층마다 개수(선언 순) 다음 같은 물건. |
| 2 몹 배치와 몹 관리 REST | | `mob_placement`(`room_id` FK 없음, `mob_type_id` FK, `MobPlacement` 수 1~10 `몹 수는 1~10으로 정하십시오.`), `MobPlacementFinder`(`findAll` id 순 — 몹 유형·드롭 표 둘을 `@EntityGraph` 한 번에(순서 열 리스트라 bag 문제 없음, 따로 읽기 대안 안 씀), `findAllInRoom` → `MobPlacementView`)·`MobPlacementEditor`(`create`·`update`·`delete`, 없는 방 → 없는 몹 → 수), `/mobs` 5개·`/rooms/{id}/mobs`·`/mob-placements/{id}` 2개, 방 상세 `mobs`(`floorItems` 다음), 409 둘(`DuplicateMobNameException`·`MobTypeInUseException` `배치된 몹은 지울 수 없습니다: 청모랑`, `countByMobTypeId`), 방 삭제 때 배치 지움(`RoomDeletedEvent` 동기 리스너), `MobTypeView.placements`(배치 id 순, 방 이름은 `RoomFinder.find` 방마다 한 번). 계획과 다른 점: `MobPlacementSaveRequest.mobId` 없음은 컴팩트 생성자 `IllegalArgumentException`(`mobId 는 꼭 보내야 합니다`, JSON 이면 공통 400), 수는 Bean Validation 없이 도메인만 봄(스펙 문구를 내려고). 응답 `stats` 는 DTO `MobDetailResponse.StatsResponse`. 리드 메모: 컴팩트 생성자 IAE → 공통 400 을 `MobApiWebMvcTest.createUnreadableBodyFail`(9가지 본문)로 고정, `required(kind)` 를 빼면 실패함을 확인. |

## 상태

- **계획 작성** (2026-11-15). 범위는 창호가 정함(2026-11-15, 기본안대로) → [index.md 5차 범위](../../spec/system/index.md#5차-범위-기본-전투). 스펙 5차 절(`(제안)`)을 계획과 함께 썼다: index.md(범위·열린 질문), admin-tool.md, command.md, platform.md, npc.md.
- 태스크와 권장 모델: 1 몹 유형 — 도메인·영속·관리 서비스(opus) · 2 몹 배치와 몹 관리 REST(opus) · 3 현재 기혈 저장(opus) · 4 교전 도메인 — 메모리 개념·피해·드롭 굴림(opus) · 5 서버 틱과 몹 나타나기(opus) · 6 공격과 틱 교전(opus) · 7 처치 보상·질 때·교전 뒤 기혈(opus) · 8 admin-console 몹 화면(opus) · 9 play-client 몹·교전 줄(opus) · 10 문서 — 스펙 (제안)·README(sonnet) · 11 통합 스모크·compose 한 번(opus) · 12 최종 리뷰(opus). 구현 태스크는 모두 opus, sonnet 은 문서 옮기기(10)만.

## 리드 판정 (Ruling)

5차 계획 리드 판정 (2026-11-15):

- R5-1 룰북에는 기본 공격이 없지만(신통·법기 명령만, 틱 자동 공격 없음) 창호가 '공격 명령' 을 5차 범위에 넣었으므로 5차 임시 `공격` 과 3초 자동 평타를 둔다. (제안) — 틀리면: 신통·법기가 생길 때 `공격` 을 없애거나 법기 기본 공격으로 바꾼다.
- R5-2 계획 때 남긴 QUESTION 1~5(아래 창호 확인 항목)는 구현을 막지 않고 `(제안)` 으로 진행하고, 창호 검토 항목으로 남긴다.
- 계획 리뷰(opus) 반영(2026-11-15): 질 때 시작 방 `room` 은 `CombatEngine` 이 `CombatOutcome` 을 돌려주고 `GameService.wakeAtStart` 가 보냄(순환 없앰, D86·D88), 몹 종류별 드롭 차이를 5차가 줄였다고 스펙에 `(제안)`, 깎인 기혈은 그 열만 쓰는 네이티브 쿼리(엔티티는 읽기 전용 매핑, D83·D95, 테스트 `vitalsWriteDoesNotTouchRealm`·`designateDoesNotOverwriteLost`), WebSocket 테스트는 실제 시계·난수라 축기 한 방 처치·메모리 정리(Task 7 Step 4), 그 밖 Minor(방 보기 번호 순서, `보기` 의 시체 자리, 지워진 드롭 물건 건너뜀, 공격 계수 소수 자리, `collectTierDrop` 난수 순서, `placeMob`/틱 전 상태, `advanceAndTick` 은 최종 상태만 같음, `Command.Collect` 임시 자리, `MobTypeSaveRequest` 이름, `MobTypeView.PlacementView`, 스모크 `waitFor`·시작 소지품 대비·칸 준비).

## 다음 할 일

- 창호 확인이 필요한 `(제안)` 검토 항목 (계획 작성 때 정함, R5-2):
  - 몹 스탯(기혈·육신·위력)은 관리 툴에서 입력하지 않고 기준 경지·종류·체급으로 룰북 몹 스탯 식대로 계산한다.
  - (QUESTION 1) 룰북에 없는 5차 임시 `공격` 명령과 플레이어 3초 자동 공격(룰북 전투는 틱 자동 공격 없음·신통 명령, R5-1).
  - (QUESTION 2) 교전 뒤 10초에 기혈이 다 차는 임시 규칙(룰북은 기혈 자연 회복 없음).
  - (QUESTION 3) 도주가 없어 교전 중 이동·`종료` 가 막히고 교전은 한쪽이 쓰러질 때까지 이어짐(강한 몹과 붙으면 반드시 짐).
  - (QUESTION 4) 질 때 최소 처리(페널티·5초·부활 장소·보호 시간 없이 시작 방·기혈 1).
  - (QUESTION 5) 선공·배회·무리·안전 장소 없음(모든 몹은 공격받아야 싸움).
