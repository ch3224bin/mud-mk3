# 1차 이터레이션 진행

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1. world-server 골격 — 빌드·공통 지원 코드·ArchUnit | 0ed3e8e, 88369b5 | Gradle 9.9.1 wrapper, Boot 4.1.2. ArchUnit 순환 규칙은 슬라이스가 생기기 전까지 `allowEmptyShould(true)` |
| 2. 맵 도메인과 영속 — 존(지역)·방·출구·세계 설정 | 12a0cae | ManyToOne은 모두 LAZY. `room_exit.direction` 은 H2가 enum 타입을 만들지 않게 `column-definition="varchar(16)"`. IDENTITY라 save 때 바로 insert |

## 다음 할 일

- Task 3: 맵 관리 서비스와 관리 REST API (진행 중)
