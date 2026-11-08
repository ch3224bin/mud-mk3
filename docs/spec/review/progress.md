# 자율 작성 진행 상황

리드: mud-1 · 작업 모드: 자율 작성 (팀 공통 규칙 → [team-brief.md](team-brief.md))

## 팀

| 역할 | 하는 일 |
|---|---|
| balance-reviewer | 페이싱·경제 기준선 작성, 수치의 게임성 검토(성장 곡선·보상 주기·리스크-보상·sink/faucet·지배 전략) |
| impl-reviewer | 룰북만으로 구현 가능한지 검토(공식·상태 전이·경계값·명령어 입출력·장 간 참조) |
| spec-writer (장 묶음별) | G1 성장: stats·realm·technique·character / G2 전투: combat·recovery·death·party / G3 경제·물건: item·crafting·equipment / G4 세계·콘텐츠: world·game-structure·npc·companion / G5 명령어 장(새 장) |

## 라운드

| 라운드 | 내용 | 상태 |
|---|---|---|
| 0 | 기준선(design-baseline.md) + 구현 공백 목록(round-0-impl-gaps.md, P0 90개) | 완료 |
| 1 | 장 묶음별 작성 G1~G4 (병렬) | 진행 중 |
| 1b | 명령어 장 G5 | 대기 |
| 2+ | 리뷰(balance·impl) → 반영 반복 | 대기 |

## 끝난 장

(없음)

## 다음 할 일

- G1~G4 결과 받으면: 용어집·index·decisions 반영 → 묶음별 모순 검토 → commit & push
- 그다음 G5 명령어 장 → Round 2 리뷰
