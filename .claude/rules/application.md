---
paths:
  - "src/main/java/**/application/**/*.java"
  - "src/main/java/**/support/**/*.java"
---

# 애플리케이션 서비스

출처는 [README](README.md).

## 스테레오타입

- 서비스 클래스에는 Spring 애노테이션을 직접 나열하지 않고 `support.stereotype` 의 메타 애노테이션을 붙인다.
  - `@ApplicationService` = `@Service` + `@Transactional`
  - `@ValidatedApplicationService` = `@ApplicationService` + `@Validated` (포트에 `@Valid` 입력이 있는 서비스)
- 의존성은 `private final` 필드 + `@RequiredArgsConstructor` 생성자 주입으로 받는다.

## 서비스 나누기

- 조회 서비스와 변경 서비스를 나눈다: `<Aggregate>QueryService`(조회 포트 구현, 예: `MemberFinder`), `<Aggregate>ModifyService`(변경 포트 구현, 예: `MemberRegister`). 검증만 하는 서비스는 `<Aggregate>ValidationService`.
- 도메인 규칙은 도메인에 두고, 서비스는 조율·중복 검사·트랜잭션 경계만 맡는다. 변경 서비스는 "찾기 → 도메인 메서드 호출 → 저장" 흐름으로 쓴다.

```java
public Member activate(Long memberId) {
    Member member = memberFinder.find(memberId);
    member.activate();
    return memberRepository.save(member);
}
```

- 같은 슬라이스의 조회가 필요하면 리포지토리 대신 조회 포트(예: `MemberFinder`)를 주입해 쓴다.
- 찾는 대상이 없으면 `orElseThrow(() -> new IllegalArgumentException("... id: " + id))` 로 던진다.

## 요청 DTO와 입력 검증

- 요청 DTO는 `provided` 패키지의 `record` 로 만들고 Bean Validation 제약(`@Email`, `@Size` 등)을 붙인다.
- 요청 DTO는 `toInfo()` 로 도메인 입력 객체로 바꿔 도메인에 넘긴다.
- 포트 메서드 파라미터에 `@Valid` 를 붙여 검증한다.

## 예외

- 여러 검증 오류를 모아 알려야 하면 `List<String> errors` 에 모은 뒤 `support.exception.ValidationException(errors)` 을 던진다.
- 서비스에서 감지한 중복 같은 규칙 위반은 이름 있는 예외(`<상황>Exception`)로 던진다. 포트(provided)에 속한 예외는 provided 패키지에 둔다(예: `LoginFailedException`).
