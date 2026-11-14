# 3차 이터레이션 진행

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| Task 1 아이템 종류 도메인·영속·관리 서비스, 영석 심기, 시계 | | `item` 슬라이스(ItemType·ItemCategory·BagGrade·SpiritStoneGrade·ItemQuery·ItemNames·Slots·Pricing), 영석 4종 시작 시 심기, `Josa` 은/는·을/를·과/와, `Numbers`, `Clock` 빈과 `MutableClock`. 계획에 더한 것: `SpiritStoneGrade.description()`(D35 묘사), `ItemNames.normalize`(이름 공백 정리 — 도메인과 `ItemTypeSaveRequest` 컴팩트 생성자가 함께 씀), `ItemQuery.hasOrdinal()`. `isInUse` 는 0을 돌려주는 자리(뒤 태스크가 count를 더함). `ItemTypeFinder.spiritStone` 없음 문구 `영석 종류가 없습니다: 하품 영석`. |
| Task 2 아이템 종류 관리 REST API | | `/api/admin/item-types` 5개(`ItemTypeApi`, `ItemTypeResponse.of`), 409 처리 4개(`DuplicateItemTypeNameException`·`ItemTypeInUseException`·`SpiritStoneChangeException`, DB 제약 위반 `DataIntegrityViolationException` — DB 메시지는 내보내지 않고 고정 문구, warn 로그). 리드 판정대로 영석에 형식이 틀린 `PUT` 은 `@Valid` 가 먼저 400(웹 테스트 `updateSpiritStoneInvalidRequestFail`). 단건 조회 없는 id 400. |
| Task 3 소지품 도메인·결제·영속·소지품 서비스 | | `inventory` 슬라이스(Inventory·InventoryEntry·SpiritStonePayment·AddCheck·RemoveCheck·PurchaseResult·SaleResult·GiveResult), 묶음 유일은 도메인(DB 제약 없음), `InventoryFinder`·`InventoryEditor`, 아이템 종류 사용 검사에 소지품(`countInInventories`). 계획에 더한 것: `SpiritStonePayment.paidValue()`. 도메인은 바꿀 내용을 묶음별 증감으로 모아 한 번에 계산·적용하므로 결제에서 하품을 다 내고 거스름을 받으면 같은 묶음의 순증감 하나로 처리된다. `give` 는 귀속된 묶음(`bound=true`)·자기 자신이면 `IllegalStateException`(게임이 먼저 거름), 받는 쪽은 늘 귀속 안 된 묶음. `add(…, bound=true)` 로 영석을 귀속할 수 없다. 관리 API 통합 테스트에 소지품에 든 종류 삭제·종류 변경 409. |

## 상태

- 계획 작성(2026-11-14). 구현 전.

## 리드 판정 (Ruling)

## 다음 할 일
