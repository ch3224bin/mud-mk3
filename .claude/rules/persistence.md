---
paths:
  - "src/main/java/**/domain/**/*.java"
  - "src/main/java/**/required/*Repository.java"
  - "src/main/resources/META-INF/orm.xml"
---

# 영속성

출처는 [README](README.md).

## DB

- 운영 DB는 PostgreSQL, 테스트는 H2 메모리 DB다. → [build.md](build.md#db)
- 메모리 월드 상태를 DB에 저장하는 일도 이 계층(JPA)이 맡는다. 저장 주기·시점은 (미정). → [architecture.md](architecture.md#메모리-월드-상태)

## 매핑

- 도메인 클래스에는 JPA 애노테이션을 최소한으로 둔다(`@Entity`, 관계 `@OneToOne`/`@OneToMany`/`@ManyToOne`, `@NaturalId`).
- 테이블 이름, 컬럼 이름·길이·nullable, 유니크 제약, enum 매핑(`<enumerated>STRING</enumerated>`), fetch·cascade, `@Embeddable` 값 객체는 `src/main/resources/META-INF/orm.xml` 에 적는다. 접근 방식은 `<access>FIELD</access>`.
- 유니크 제약 이름은 `UK_<TABLE>_<COLUMN>` 으로 짓는다(예: `UK_MEMBER_EMAIL_ADDRESS`).
- 테이블·컬럼 이름은 snake_case로 한다.
- 값 객체 record는 embeddable로 매핑한다(예: `Email`, `Profile`).

## 리포지토리

- 리포지토리는 `application/<aggregate>/required` 에 둔다.
- `JpaRepository` 가 아니라 Spring Data `Repository<T, ID>` 를 상속하고 필요한 메서드만 선언한다.
- 하나를 찾는 메서드는 `Optional` 을 돌려준다. 메서드 이름 규칙으로 못 쓰는 조회만 `@Query` 로 적는다.

```java
public interface MemberRepository extends Repository<Member, Long> {
    Member save(Member member);
    Optional<Member> findByEmail(Email email);
}
```
