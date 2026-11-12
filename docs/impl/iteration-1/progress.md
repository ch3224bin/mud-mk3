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
| 6. 게임 세션과 명령 처리 — 보기·이동·말하기·누구·멀티유저 | 255b9d6, 36c003d | `GameService` 는 공정 락 안에서 `TransactionTemplate` 으로 트랜잭션을 열고 닫는다(포트 메서드는 `SUPPORTS`) — 커밋 뒤 락을 풀어 같은 이름 동시 생성 경합을 막음. `onRoomDeleted` 는 `@TransactionalEventListener(AFTER_COMMIT)` — 관리자 트랜잭션 안에서 게임 락을 잡지 않고(동시 방 삭제 교착 방지), 커밋 뒤 락+새 트랜잭션에서 옮긴다. 커밋과 반영 사이 명령은 현재 방이 없으면 그 자리에서 시작 방으로 옮긴다. 메시지는 커밋 뒤 락 안에서 보낸다. 같은 계정 재접속은 생성 중 세션도 밀어냄(4409). 생성 확인 전 이름 중복·시작 방을 미리 확인(생성 포트 예외가 공유 트랜잭션을 롤백 전용으로 만들지 않게). 실제 `GameMessageSender` 는 Task 7 — 그때까지 `@SpringBootTest` 들은 `TestFakesConfiguration` 을 import 하고 앱 단독 실행(bootRun)은 sender 빈이 없어 뜨지 않음. ArchUnit에 '다른 application 슬라이스는 provided만' 규칙 추가 |
| 7. WebSocket 어댑터 — `/ws/game`, 세션 쿠키 인증, JSON 직렬화 | 0dad092, 165ad99 | 핸드셰이크 인터셉터가 HTTP 세션(새로 만들지 않음)의 `SecurityContext` → `principalOf` 로 `accountId` 속성을 넣고, Origin은 `setAllowedOrigins(CorsProperties)` 로 403. 미로그인은 연결 뒤 `error AUTH_REQUIRED` + 4401. sender는 연결마다 보내기 줄 + 가상 스레드 보내기 작업 하나(게임 락 안의 send/close는 줄에 넣고 곧장 돌아옴, 순서 유지, close는 앞 메시지 뒤). 못 보낸 것이 512KB를 넘거나 보내기가 실패하면 4500(`SESSION_NOT_RELIABLE`)으로 닫음. 톰캣 `BLOCKING_SEND_TIMEOUT` 5초로 막힌 보내기 작업도 끝나게 함. 바이너리 프레임은 BAD_MESSAGE. JSON은 `ObjectNode` 에 `type` 을 먼저 넣고 record 필드를 붙인다. `TestFakesConfiguration` 은 게임 포트 테스트(`@ApplicationServiceTest`, `GameConnectorConcurrencyTest`)에만 남김(`@Primary`). 통합 테스트는 표를 비우고 presence가 빌 때까지 기다린다 |

## 다음 할 일

- Task 8: admin-console (진행 중)
