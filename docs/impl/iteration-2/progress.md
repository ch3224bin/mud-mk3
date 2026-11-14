# 2차 이터레이션 진행

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1. 성·종문 도메인·영속과 지역 소유 — 관리 서비스까지 | 5e91716 | `faction` 슬라이스(도메인·orm.xml·리포지토리·FactionFinder/Editor), `Zone.owner`·`ZoneSaveRequest.ownerId`. `ZoneSaveInfo` 는 바꾸지 않음(소유는 `create/update` 의 따로 받는 인자) |
| 2. 성·종문 관리 REST API와 지역 소유 지정 |  | `/api/admin/factions` 4개(`FactionApi`·`FactionResponse`), `ZoneResponse` 에 `ownerId`·`ownerName`·`ownerKind`, 409 처리 2개. 성·종문 삭제의 동시 소유 지정 경합은 1차 지역 삭제와 같게 두었다 |

## 상태

- 계획 작성 (2026-11-14). 구현은 아직 시작하지 않았다.

## 리드 판정 (Ruling)

- (없음)

## 다음 할 일

- Task 1: 성·종문 도메인·영속과 지역 소유 — 관리 서비스까지.
