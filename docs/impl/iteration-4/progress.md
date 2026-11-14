# 4차 이터레이션 진행

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1 경지·오행·영근 도메인과 캐릭터 저장 | fb63e93 (리뷰 ✅, Minor 3) | `Realm`·`Element`(shared), `SpiritRoot`(record, 엔트리 오행 순 정렬, `of(List)` 더함, 없는 오행 `aptitude` 0)·`SpiritRootEntry`, `player_character.realm`(nullable)·`character_spirit_root`(FK `character_id`, 순서 열 없음 — 도메인이 정렬), 시작 채우기(`@Order(0)`, 영석 심기 `@Order(10)`), `Josa` 으로/로, `CharacterFinder.find`, `CharacterCreator.create(…, SpiritRoot)`·`chooseSpiritRoot`. 경지 null 읽기 문구 `캐릭터의 경지가 비어 있습니다`(IllegalStateException). |
| 2 공법·신통 도메인·영속·관리 서비스 | dad0eaa, b127328 (리뷰 ✅, 고침 1회: 소모 양 상한 10,000) | `technique` 슬라이스(Technique·TechniqueStage·Ability·enum 넷(AbilityKind·AbilityTarget·CostStat·UsableWhen)·ReservedWords(104낱말, 대소문자 무시)), 표 다섯(`technique`·`technique_element`·`technique_stage`·`ability`·`ability_element`), `TechniqueFinder`·`TechniqueEditor`·`TechniqueView`(+`AbilityView`·`ScrollItem`, `learnerCount` 0·`scrollItems` 빈 목록), 검사 순서 D61·D62. 계획과 다른 점: 공법·신통 속성은 순서 열 없는 집합(`Set<Element>`, PK `(…_id, element)`)으로 저장하고 도메인이 오행 순으로 정렬(여러 bag 문제 회피), `DuplicateTechniqueNameException` 은 `domain/technique`(npc 관례), 소모 양·시간은 저장 모양과 같게 scale 1(`5.0`), 요청 필수 enum·`elements` 안 null 은 컴팩트 생성자 `IllegalArgumentException`(`<필드> 는 꼭 보내야 합니다`). |
| 3 공법·경지 관리 REST와 옥간·서책의 담긴 공법 | f781b14, 955afd8 (리뷰 ✅, 고침 1회: 담긴 공법 있는 물건 종류 바꾸기 400) | `GET /realms`, `/techniques` 5개, `item_type.technique_id`, 아이템 요청·응답 `techniqueId`/`techniqueName`, 담긴 물건 있는 공법 삭제 409. 계획과 다른 점: `ScrollItem` 은 `TechniqueView` 안 record 에서 `application/technique/required/ScrollItem` 으로 옮김(JPQL 생성자 식), `ItemTypeSaveRequest.toInfo(@Nullable Technique)`(서비스가 `TechniqueFinder.find` 로 찾아 넘김). 신통 `bodyRefining`·`costPerTick`(boolean)이 빠지면 Jackson 3 기본값(null→primitive 실패)으로 공통 400 `요청 본문을 읽을 수 없습니다`. |
| 4 저물탁 — 등급·한도·경지에 따른 칸, 게임 연결 | 8f82d97 (리뷰 ✅, Minor 3) | `BagGrade` 저물탁 셋(`BRACELET_LOW/MIDDLE/HIGH` 100·200·400)·`displayName`(`하급 저물대`·`하급 저물탁`)·`isBracelet`, `ItemType.isBracelet`, `ItemCategory.BAG` 이름 `저물대·저물탁`, `Slots.MAX_BRACELETS`, `braceletOpen` 인자(`Inventory` 칸 메서드 전부·`InventoryEditor` 다섯, `give` 는 주는 쪽·받는 쪽 둘, `createWithStartingItems` 는 안에서 `false`), 저물탁 한도·문구, 넘은 소지품 규칙(D70: 늘리기는 변경 뒤 칸 안, 줄이기는 그것 또는 쓰는 칸이 늘지 않고 칸이 줄지 않으면, `roomFor` 는 넘으면 0), 소지품 `(축기 초기 필요)`, 시작 소지품 저물탁 2개 400·저물탁 칸 0, `bagGradeName` = `displayName()`. game은 `CharacterFinder.find(id).getRealm().opensBracelet()` 를 명령마다 읽음(`ItemCommandHandler`·`ShopCommandHandler` 에 `CharacterFinder`). 계획과 다른 점: `Inventory.capacity()`(인자 없음)는 지우고 `capacity(boolean)` 만 둠, `braceletCount()` 더함, 줍기 실패 문구는 `checkAdd(…, 1)` 원인으로 고름, `InventoryFinder` 는 칸 메서드가 없어 그대로. 3차 기대값 고침: `bagGradeName` `하급 저물대`, `categoryName`·팔기 안내 `저물대·저물탁은 잡화점에 팔 수 있습니다`. 테스트 `braceletClosesAfterDemotion` 은 120칸을 채우려고 중급 저물탁(202칸)을 씀. |
| 5 수련 슬라이스 — 익힌 공법·스탯 계산·읽기 서비스·경지 지정 | | `cultivation` 슬라이스(`CharacterTechnique`(표 `character_technique`, FK 둘, UK (캐릭터, 공법), 연관 LAZY)·`StatSheet`·`TechniqueReader`·`CultivationFinder`), `CharacterRealmEditor`(`designate` 바뀔 때만 `RealmChangedEvent`, `initiate` 범인만·이벤트 없음)·`RealmChangedEvent`, 공법 익힌 캐릭터 검사 두 가지(삭제는 익힌 캐릭터 → 담긴 물건 순, 단계 수 줄이기는 `maxLearnedStage` 보다 작으면 409), `TechniqueView.learnerCount` 채움. 계획과 다른 점: 단계 합 값 `domain/cultivation/StageBonusSum`(record) 더함, `StatSheet.of` 의 영근은 `@Nullable`(영근 없으면 자질 빈 맵 — `SpiritRoot` 는 빈 값을 받지 않음), 캐릭터별 개수는 `required/LearnedCount`(JPQL 생성자 식), `CharacterFinder.findAllByName()`(영근 `@EntityGraph`, DB 이름순) 더함, 이미 익힘 검사는 `findLearned` 에서 골라 단계를 읽음(`existsBy…` 는 선언만). |

## 상태

- 계획 작성(2026-11-14). 계획 리뷰 반영(2026-11-14): 필수 enum null 400 문구를 스펙대로, L1 상점 준비는 알림 없는 `setRealm`, 스모크 단계 16 저물대 이름, enum 열 `column-definition`, Task 8 익히기 준비, 관리 화면 안내 문구, 모델. 구현 전.
- 태스크와 권장 모델: 1 경지·오행·영근 도메인과 캐릭터 저장(opus) · 2 공법·신통 도메인·영속·관리 서비스(opus) · 3 공법·경지 관리 REST와 옥간·서책의 담긴 공법(opus) · 4 저물탁(opus) · 5 수련 슬라이스 — 익힌 공법·스탯·읽기·경지 지정(opus) · 6 캐릭터 관리 REST(opus) · 7 게임 — 생성 영근 단계(opus) · 8 게임 — 상태·공법·신통(opus) · 9 게임 — 읽기·범인 상점 막힘·경지 알림·도움말(opus) · 10 admin-console 공법·아이템(opus) · 11 admin-console 캐릭터(opus) · 12 play-client(opus) · 13 문서 — 스펙 (제안)·README(sonnet) · 14 통합 스모크·compose 한 번(opus) · 15 최종 리뷰(opus). 구현 태스크는 모두 opus, sonnet 은 문서 옮기기(13)만(창호 모델 규칙).

## 리드 판정 (Ruling)

4차 스펙 리드 판정 (2026-11-14):

- L1 새 캐릭터는 룰북대로 범인, 범인은 상점(`목록`·`사기`·`팔기`) 막힘(character.md 범인 상태, commands.md 상태별 금지). 3차 스모크·E2E 는 상점 쓰기 전에 그 캐릭터를 관리 툴로 연기 1층으로 지정하거나 입문 옥간을 읽게 고친다. — 룰북 우선. 틀리면: 상점 막힘 한 검사만 빼면 됨.
- L2 서재가 없으므로 조건이 안 맞는 옥간은 거절만 하고 물건은 남김(읽기 확인 없음). 서재와 함께 5차 이후. — 범위 밖 기능을 끌어오지 않음.
- L3 관리 툴 경지 지정은 경지 안정 기간 없이 즉시 반영(영력 포함). — '임의 경지 상승'이 목적, 정식 돌파는 범위 밖.
- L4 현재 수치(기혈 등)는 저장하지 않고 4차엔 늘 최대치. 5차(전투)에서 현재 수치 열을 더한다. — 4차에 깎이는 길이 없음.
- L5 기존 캐릭터는 연기 1층으로 채우고, 영근이 없으면 다음 접속 때 세계에 들어가기 전에 고른다(생성 영근 단계를 다시 씀). — 영근은 플레이어가 고르는 룰북 규칙을 지킴, 관리 툴로 바꾸지 않음. 기존 캐릭터는 개발 데이터뿐.

## 다음 할 일

- 리드: 계획 검토 뒤 Task 1부터 차례로 진행.
