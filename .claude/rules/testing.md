---
paths:
  - "src/test/**"
---

# 테스트

출처는 [README](README.md).

## 종류와 위치

| 대상 | 방식 | 위치 |
|---|---|---|
| 도메인 | 순수 단위 테스트(Spring 없음) | `domain/<aggregate>/<Class>Test` |
| provided 포트 | `@ApplicationServiceTest`(= `@SpringBootTest` + `@Transactional` + 테스트 설정 `@Import`) | `application/<aggregate>/provided/<Port>Test` |
| required 포트(리포지토리) | `@DataJpaTest` | `application/<aggregate>/required/<Repository>Test` |
| 웹 어댑터 | `@WebMvcTest(<Api>.class)` + `MockMvcTester`, 포트는 `@MockitoBean` | `adapter/webapi/<Api>WebMvcTest` |
| 아키텍처 | ArchUnit `@AnalyzeClasses` + `@ArchTest` | `HexagonalArchitectureTest` |

- 테스트 패키지는 테스트 대상의 패키지(`provided`/`required` 포함)를 그대로 따른다.
- 외부 연동 포트(메일 등)는 `@TestConfiguration` 클래스에서 람다·가짜 구현 빈으로 바꾼다.
- 여러 테스트가 같은 준비 과정을 쓰면 `BaseApplicationServiceTest`, `BaseRepositoryTest` 같은 기반 클래스에 `prepare~()` 메서드로 모은다.

## 주입

- `src/test/resources/junit-platform.properties` 에 `spring.test.constructor.autowire.mode=all` 을 둔다.
- 테스트 클래스는 생성자 주입으로 받는다: `record XxxTest(Port port, EntityManager em)` 또는 `final` 필드 + `@RequiredArgsConstructor`.

## 픽스처

- 테스트 데이터는 도메인 패키지와 같은 테스트 패키지의 `<Aggregate>Fixture` 클래스 정적 메서드로 만든다(`createMember()`, `createActiveMember()`, `createMemberRegisterRequest(email)` 등).
- 요청 DTO 같은 임의 값은 Instancio로 만들고(`Instancio.of(...).set(field(...), 값).create()`), `src/main/resources/instancio.properties` 에 `bean.validation.enabled=true` 를 둔다.
- id가 필요한 픽스처는 `ReflectionTestUtils.setField(entity, "id", id)` 로 넣는다.

## 쓰는 법

- 단언은 AssertJ(`assertThat`, `assertThatThrownBy(...).isInstanceOf(...)`)를 static import 해서 쓴다.
- 영속 상태를 확인할 때는 `entityManager.flush(); entityManager.clear();` 후 다시 조회한다.
- 서비스·리포지토리 테스트는 목 없이 실제 빈으로 돌린다. 목은 웹 어댑터 테스트(`@MockitoBean` + `when(...)`/`verify(...)`)와 `main` 실행 확인(`Mockito.mockStatic`)에만 쓴다.
- 테스트 메서드 이름은 영문 camelCase로 동작을 쓰고, 실패 경우는 `Fail` 을 붙인다(`activate`, `activateFail`, `duplicateEmailFail`). `@DisplayName` 은 쓰지 않는다.
