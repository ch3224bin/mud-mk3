---
paths:
  - "src/**/*.java"
---

# 코드 스타일

출처는 [README](README.md).

## 네이밍

| 대상 | 형식 | 예 |
|---|---|---|
| provided 포트 | 동작 주체 명사 | `MemberRegister`, `MemberFinder`, `MemberAuthenticator` |
| 서비스 구현 | `<Aggregate><책임>Service` | `MemberModifyService`, `MemberQueryService` |
| required 포트 | `<Aggregate>Repository`, `<기능>Sender` | `MemberRepository`, `EmailSender` |
| 요청 DTO | `<Aggregate><동작>Request` | `MemberRegisterRequest` |
| 도메인 입력 | `<Aggregate><동작>Info` | `MemberRegisterInfo` |
| 응답 DTO | `<Aggregate><동작>Response` | `MemberRegisterResponse` |
| 예외 | `<상황>Exception` | `DuplicateEmailException`, `LoginFailedException` |
| 상태 | `<Aggregate>Status` (enum) | `MemberStatus` |

## null

- `domain`, `application` 패키지 루트의 `package-info.java` 에 JSpecify `@org.jspecify.annotations.NullMarked` 를 붙인다. null이 될 수 있는 곳에만 `@org.jspecify.annotations.Nullable` 을 붙인다.
  - 참고 레포의 Spring `@NonNullApi`·`@Nullable`(`org.springframework.lang`)은 Spring Framework 7에서 deprecated라 쓰지 않는다.

## Lombok

- 쓰는 것: `@Getter`, `@ToString`(연관 필드는 `exclude`, 상속 시 `callSuper = true`), `@NoArgsConstructor(access = PROTECTED)`, `@RequiredArgsConstructor`.
- `@Setter`, `@Data` 는 쓰지 않는다.

## 모양

- 들여쓰기는 공백 4칸.
- `Objects.requireNonNull`, `Assert.state`, AssertJ 단언은 static import 한다.
- 예외 메시지와 도메인 설명은 한국어로 쓴다.
- 데이터를 나르는 타입(DTO, 값 객체, 도메인 입력)은 `record` 로 만든다.
