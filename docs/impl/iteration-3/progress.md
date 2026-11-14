# 3차 이터레이션 진행

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| Task 1 아이템 종류 도메인·영속·관리 서비스, 영석 심기, 시계 | | `item` 슬라이스(ItemType·ItemCategory·BagGrade·SpiritStoneGrade·ItemQuery·ItemNames·Slots·Pricing), 영석 4종 시작 시 심기, `Josa` 은/는·을/를·과/와, `Numbers`, `Clock` 빈과 `MutableClock`. 계획에 더한 것: `SpiritStoneGrade.description()`(D35 묘사), `ItemNames.normalize`(이름 공백 정리 — 도메인과 `ItemTypeSaveRequest` 컴팩트 생성자가 함께 씀), `ItemQuery.hasOrdinal()`. `isInUse` 는 0을 돌려주는 자리(뒤 태스크가 count를 더함). `ItemTypeFinder.spiritStone` 없음 문구 `영석 종류가 없습니다: 하품 영석`. |

## 상태

- 계획 작성(2026-11-14). 구현 전.

## 리드 판정 (Ruling)

## 다음 할 일
