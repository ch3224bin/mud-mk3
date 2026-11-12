---
paths:
  - "src/main/java/**/domain/**/*.java"
---

# 도메인 모델

출처는 [README](README.md).

## 엔티티

- 엔티티는 `AbstractEntity` 를 상속한다. `AbstractEntity` 는 `@MappedSuperclass` 로 `Long id`(IDENTITY, getter는 JSpecify `@Nullable`)와 HibernateProxy에 안전한 `final equals/hashCode` 를 제공한다.
- JPA용 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 로 막는다.
- 생성은 의미 있는 이름의 정적 팩터리 메서드로 한다(예: `Member.register(info, encoder)`). 팩터리 안에서 필드를 채우고 초기 상태를 정한다.
- 상태 변경은 의미 있는 도메인 메서드로만 한다(`activate()`, `deactivate()`, `updateInfo(...)`). setter를 만들지 않는다.
- 클래스에 `@Getter` 를 붙이고, 컬렉션 필드는 `@Getter(AccessLevel.NONE)` 로 막은 뒤 `Collections.unmodifiableList(...)` 를 돌려주는 getter를 직접 만든다.
- 애그리거트 내부 엔티티의 변경 메서드는 package-private으로 두어 루트를 통해서만 바뀌게 한다.
- 다른 애그리거트가 상태를 확인할 수 있게 `isActive()` 같은 조회 메서드와 `ensureActive()` 같은 확인 메서드를 둔다.

```java
public void activate() {
    state(status == MemberStatus.PENDING, "PENDING 상태가 아닙니다");
    this.status = MemberStatus.ACTIVE;
}
```

## 불변식 검증

- 상태 전이 조건은 메서드 첫머리에서 `org.springframework.util.Assert.state(...)` 로 검증한다(어기면 `IllegalStateException`).
- 필수 값은 `Objects.requireNonNull(...)` 로 막는다. `state`, `requireNonNull` 은 static import 해서 쓴다.
- 도메인 규칙 위반을 따로 구분해야 하면 `RuntimeException` 을 상속한 `<상황>Exception` 을 애그리거트 패키지에 둔다(예: `DuplicateEmailException`, `InvalidCurriculumException`).

## 값 객체

- 값 객체는 `record` 로 만들고, 컴팩트 생성자에서 검증한다. 어기면 `IllegalArgumentException`.
- 여러 애그리거트가 쓰는 값 객체는 `domain/shared` 에 둔다.

```java
public record Email(String address) {
    public Email {
        if (!EMAIL_PATTERN.matcher(address).matches())
            throw new IllegalArgumentException("이메일 형식이 바르지 않습니다: " + address);
    }
}
```

## 도메인 입력 객체

- 도메인 팩터리·변경 메서드는 웹 DTO가 아니라 도메인 패키지의 입력 record(예: `MemberRegisterInfo`)를 받는다. 이 record에도 Bean Validation 제약을 붙인다.

## 상태

- 상태는 `enum` 으로 둔다(예: `MemberStatus`).
