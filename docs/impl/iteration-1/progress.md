# 1차 이터레이션 진행

계획: [plan.md](plan.md)

## 끝난 태스크

| 태스크 | 커밋 | 메모 |
|---|---|---|
| 1. world-server 골격 — 빌드·공통 지원 코드·ArchUnit | 0ed3e8e, 88369b5 | Gradle 9.9.1 wrapper, Boot 4.1.2. ArchUnit 순환 규칙은 슬라이스가 생기기 전까지 `allowEmptyShould(true)` |
| 2. 맵 도메인과 영속 — 존(지역)·방·출구·세계 설정 | 12a0cae | ManyToOne은 모두 LAZY. `room_exit.direction` 은 H2가 enum 타입을 만들지 않게 `column-definition="varchar(16)"`. IDENTITY라 save 때 바로 insert |
| 3. 맵 관리 서비스와 관리 REST API | 92fc52c | 아직 인증 없음(Task 4). `RoomFinder` 가 돌려주는 방은 `@EntityGraph` 로 지역·출구·대상 방(지역)까지 읽어 트랜잭션 밖에서 DTO를 만든다(`AdminApiIntegrationTest`). 방 삭제는 들어오는 출구 제거 → flush → 삭제. 지역의 방 수는 `ZoneRepository` JPQL로 세어 zone→room 순환을 피한다. 이름 공백 자르기는 `domain/shared/Names` |
| 4. 인증 — 계정, Google OAuth2 Login, 세션, 관리자, 보안 설정 | f34c4d6 | 세션 principal은 `AccountPrincipal`(Google 로그인은 `MudOidcUser` 가 감쌈, `SessionAuthenticator.principalOf` 로 꺼냄). 401/403·CSRF 거절은 필터 단계에서 ProblemDetail JSON(`ProblemDetailResponses`). advice는 컨트롤러 안 보안 예외를 다시 던져 Security가 401/403 처리. ID 토큰 클레임만 쓰고 UserInfo는 부르지 않음. dev login은 `local` + `MUD_DEV_LOGIN_ENABLED=true` |
| 5. 캐릭터 — 이름 규칙, 계정당 하나, 시작 방, 위치 저장 | 96f4302 | `player_character` 는 `room_id` 만 갖고 방 FK 없음. 이름은 `CharacterName` embeddable(`name` varchar(6)). 생성 검사 순서: 이름 규칙 → 계정 존재(`AccountFinder`) → 계정에 이미 있음 → 이름 중복 → 시작 방. `/api/auth/me`·dev login 응답의 `characterName` 은 `SessionAuthenticator.principalOf(...).accountId()` 로 찾는다(Principal.getName() 쓰지 않음). 방 삭제 이벤트로 `relocateAll` 을 부르는 리스너는 아직 없음(Task 6). Task 4 Minor: dev login 가드 분리 테스트, `Email` 예외 메시지·Google 로그인 WARN 로그에서 이메일 제거 |

## 다음 할 일

- Task 6: 게임 세션과 명령 처리 (진행 중)
