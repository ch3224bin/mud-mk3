---
paths:
  - "src/main/java/**/adapter/**/*.java"
---

# 어댑터

출처는 [README](README.md).

- 어댑터 클래스에는 `support.stereotype` 의 메타 애노테이션을 붙인다: `@Adapter`(일반), `@WebApiAdapter`(= `@Adapter` + `@RestController`).
- 어댑터는 `@RequiredArgsConstructor` 로 provided 포트 인터페이스를 주입받는다. 서비스 구현 클래스를 주입하지 않는다.
- 하위 패키지로 나눈다: `webapi`(REST 컨트롤러, `webapi/dto`), `websocket`(게임 접속), `persistence`(JPA — 리포지토리는 Spring Data 인터페이스로 `required` 에 두므로 별도 구현이 없으면 비어 있음), `integration`(외부 시스템, 예: 메일), `security`(암호화 등 기술 구현).
- REST 컨트롤러 이름은 `<Aggregate>Api` 로 한다. 요청 본문은 `@RequestBody @Valid` 로 받는다.
- 응답 DTO는 `record` 로 만들고 정적 팩터리 `of(도메인 객체)` 로 바꾼다.

```java
public record MemberRegisterResponse(Long memberId, String email) {
    public static MemberRegisterResponse of(Member member) { ... }
}
```

- REST 예외 응답은 `ResponseEntityExceptionHandler` 를 상속한 `@ControllerAdvice` 하나(`ApiControllerAdvice`)에서 `ProblemDetail` 로 통일한다. 예외 종류별로 HTTP 상태를 정하고(예: 중복 → `CONFLICT`, 나머지 → `INTERNAL_SERVER_ERROR`), `timestamp`·`exception`(예외 클래스 이름) 속성을 넣는다.

## WebSocket 어댑터

- 게임 접속은 WebSocket으로 한다. WebSocket 어댑터는 `adapter/websocket` 에 둔다.
- 다른 어댑터와 같이 provided 포트 인터페이스에만 의존한다. 플레이어 입력을 받아 provided 포트를 호출한다.
- 메시지 형식, 세션 관리, 플레이어에게 출력을 밀어 보내는 포트 구조는 (미정).
