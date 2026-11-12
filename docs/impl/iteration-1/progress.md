# 1차 이터레이션 진행

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1. world-server 골격 — 빌드·공통 지원 코드·ArchUnit | 0ed3e8e, 88369b5 | Gradle 9.9.1 wrapper, Boot 4.1.2. ArchUnit 순환 규칙은 슬라이스가 생기기 전까지 `allowEmptyShould(true)` |
| 2. 맵 도메인과 영속 — 존(지역)·방·출구·세계 설정 | 12a0cae | ManyToOne은 모두 LAZY. `room_exit.direction` 은 H2가 enum 타입을 만들지 않게 `column-definition="varchar(16)"`. IDENTITY라 save 때 바로 insert |
| 3. 맵 관리 서비스와 관리 REST API | 92fc52c | 아직 인증 없음(Task 4). `RoomFinder` 가 돌려주는 방은 `@EntityGraph` 로 지역·출구·대상 방(지역)까지 읽어 트랜잭션 밖에서 DTO를 만든다(`AdminApiIntegrationTest`). 방 삭제는 들어오는 출구 제거 → flush → 삭제. 지역의 방 수는 `ZoneRepository` JPQL로 세어 zone→room 순환을 피한다. 이름 공백 자르기는 `domain/shared/Names` |
| 4. 인증 — 계정, Google OAuth2 Login, 세션, 관리자, 보안 설정 |  | 세션 principal은 `AccountPrincipal`(Google 로그인은 `MudOidcUser` 가 감쌈, `SessionAuthenticator.principalOf` 로 꺼냄). 401/403·CSRF 거절은 필터 단계에서 ProblemDetail JSON(`ProblemDetailResponses`). advice는 컨트롤러 안 보안 예외를 다시 던져 Security가 401/403 처리. ID 토큰 클레임만 쓰고 UserInfo는 부르지 않음. dev login은 `local` + `MUD_DEV_LOGIN_ENABLED=true` |

## 다음 할 일

- Task 5 이후: plan.md 순서대로
