# 자율 작성 진행 상황

리드: mud-1 · 작업 모드: 자율 작성 (팀 공통 규칙 → [team-brief.md](team-brief.md))

## 상태: 완료

- balance-reviewer 판정: **수치 OK** ([round-4-balance.md](round-4-balance.md))
- impl-reviewer 판정: **구현에 부족함 없음** — MVP(첫 지역, 연기 1~13층 + 축기 돌파) 기준 ([round-4-impl.md](round-4-impl.md))
- 창호 결정 필요: 26건 → [decisions-needed.md](decisions-needed.md)
- 에이전트가 정한 규칙·수치는 문장 끝 `(제안)` (룰북 전체 약 990곳)

## 팀

| 역할 | 하는 일 |
|---|---|
| balance-reviewer | 페이싱·경제 기준선 작성, 수치의 게임성 검토(시뮬레이션) |
| impl-reviewer | 룰북만으로 구현 가능한지 검토(공식·상태 전이·경계값·명령어 입출력·장 간 참조, MVP 플레이 루프 따라가기) |
| spec-writer (장 묶음별) | G1 성장: stats·realm·technique·technique-list·character / G2 전투: combat·recovery·death·party / G3 경제·물건: item·crafting·equipment / G4 세계·콘텐츠: world·game-structure·npc·companion·quest·first-region / G5 명령어: commands |
| 통합(reconciler) | 장 간 모순·인계 반영, 용어집, 명령어 장 동기화 |
| 모순 검토 | 커밋 전 변경분 모순 검토(.claude/skills/mud-spec/references/review-agent.md) |

## 라운드

| 라운드 | 내용 | 결과 |
|---|---|---|
| 0 | 기준선([design-baseline.md](design-baseline.md)) + 구현 공백([round-0-impl-gaps.md](round-0-impl-gaps.md)) | P0 90개 |
| 1 | 묶음별 작성 G1~G4, 명령어 장 G5, 통합·용어집, 모순 검토([contradictions](round-1-contradictions.md), [handoffs](round-1-handoffs.md)) | 새 장 4개 |
| 2 | 리뷰 [balance](round-2-balance.md) H5·M7·L3 / [impl](round-2-impl.md) P0 3·P1 13 → 반영 → [모순 검토](round-2-contradictions.md) | 반영 |
| 3 | 리뷰 [balance](round-3-balance.md) H3·M2 / [impl](round-3-impl.md) P1 3 → 반영 → [모순 검토](round-3-contradictions.md) | 반영 |
| 4 | 리뷰 [balance](round-4-balance.md) H1·M1 / [impl](round-4-impl.md) P0·P1 0 (P2 6) → 반영 → 모순 없음 → 재시뮬레이션 확인 | **수치 OK · 구현에 부족함 없음** |

## 바뀐 장

stats · realm · technique · technique-list(새) · character · combat · recovery · death · party · item · crafting · equipment · world · game-structure · npc · companion · quest(새) · first-region(새) · commands(새) · index · glossary

## 이어서 할 수 있는 것

- 창호가 [decisions-needed.md](decisions-needed.md) 항목을 고르면 해당 (제안) 문장을 확정하거나 되돌린다.
- 남은 미정 약 24줄: PvP(D9) 3줄, 나머지는 후반(축기 이후·화신 등) P2.
- 축기기 지역(두 번째 지역) 콘텐츠는 아직 없다. 규칙(경지대별 공식)은 일반화돼 있어 같은 방식으로 만들 수 있다.
- 각 리뷰의 L 항목(기록만)은 리뷰 파일에 남아 있다.
