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
| 1 | 장 묶음별 작성 G1~G4 (병렬) | 완료 — 새 장 technique-list·quest·first-region |
| 1r | 통합: 장 간 모순(round-1-contradictions.md)·인계(round-1-handoffs.md)·문체 통일·용어집 | 완료 |
| 1b | 명령어 장 G5 (commands.md) | 완료 — 다른 장 인계는 round-2-handoffs.md |
| 1c | 전체 변경분 모순 검토(A·B) → 수정 | 완료 |
| 2 | 리뷰: round-2-balance.md(수정 필요 H5·M7·L3), round-2-impl.md(부족 P0 3·P1 13) | 완료 |
| 2w | 묶음별 반영 G1~G4 (병렬) → 인계·명령어 동기화 → 모순 검토(round-2-contradictions.md) → 수정 | 완료 |
| 3 | 재검토: round-3-balance.md(H3·M2·L6), round-3-impl.md(P0 0·P1 3) | 완료 |
| 3w | 반영: G1~G3 합본 / G4 (병렬) → 인계(round-3-handoffs.md) → 모순 검토(round-3-contradictions.md) → 커밋 | 완료 |
| 4 | 재검토(balance·impl) | 진행 중 |

## 끝난 장 (Round 1 초안 + 통합)

stats · realm · technique · technique-list(새) · character · combat · recovery · death · party · item · crafting · equipment · world · game-structure · npc · companion · quest(새) · first-region(새) · commands(새) · glossary

## 다음 할 일

- Round 2: balance-reviewer·impl-reviewer 가 전체 룰북 검토 → round-2-balance.md / round-2-impl.md
- 리뷰 지적 + round-2-handoffs.md(명령 출력 누락)를 묶음별 writer 가 반영 → 재검토
