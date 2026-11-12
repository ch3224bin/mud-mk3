---
paths:
  - "src/**/*.java"
---

# 아키텍처

출처는 [README](README.md).

## 계층

- 헥사고날 아키텍처 + 도메인 모델 패턴으로 만든다.
- 의존은 안쪽으로만 향한다: adapter → application → domain.
  - domain은 application·adapter를 모른다. application은 adapter를 모른다. adapter는 아무 계층도 의존하지 않는다.
- domain은 핵심 규칙만 담는다. application은 포트를 정의하고 구현한다. adapter는 웹·DB·외부 시스템 연동을 맡는다.

## 패키지

```
<base>
├── domain/<aggregate>              // 애그리거트 하나당 패키지 하나
├── domain/shared                   // 여러 애그리거트가 쓰는 값 객체
├── application/<aggregate>         // 같은 이름 애그리거트의 서비스 구현
│   ├── provided                    // 드라이빙 포트(어댑터가 호출) + 요청 DTO
│   └── required                    // 드리븐 포트(서비스가 필요로 함, 예: Repository)
├── adapter/{webapi,websocket,persistence,integration,security}
└── support/{stereotype,exception}  // 스테레오타입 애노테이션, 공통 예외
```

- `domain` 바로 아래 패키지 하나가 애그리거트 하나다. `application` 바로 아래 패키지는 같은 이름 애그리거트의 서비스 패키지다. 이 단위를 슬라이스라 부른다.

## 포트

- provided 포트: 어댑터가 호출하는 인터페이스. 구현은 `application.<aggregate>` 의 `~Service` 클래스가 한다.
- 컨트롤러 등 어댑터는 구현 클래스가 아니라 provided 포트 인터페이스에 의존한다.
- required 포트: 서비스가 필요로 하는 인터페이스(`~Repository`, `~Sender`). 구현은 adapter가 한다.
- 도메인이 필요로 하는 기술 추상화(예: `PasswordEncoder`)는 도메인 패키지에 인터페이스로 두고, 구현은 adapter에 둔다.
- 포트 인터페이스에는 한 줄 Javadoc으로 역할을 적는다(예: `/** 회원의 등록과 관련된 기능을 제공한다 */`).

## 메모리 월드 상태

- 월드 상태를 메모리에 두기로 한 결정과 저장 시점은 [platform.md](../../docs/spec/system/platform.md#실시간-통신과-월드-상태)를 따른다. 여기서는 코드 배치만 정한다.
- 월드 상태를 이루는 개념(방, 방 안의 캐릭터, 전투 등)은 domain에 둔다. 도메인 규칙은 메모리에 있든 DB에 있든 같은 도메인 객체가 지킨다.
- 메모리에 들고 있는 저장소는 application의 required 포트로 정의하고, 메모리 구현은 adapter에 둔다. DB 저장은 지금처럼 persistence(JPA) 쪽이 맡는다.
- 메모리 상태를 다루는 구체 구조(동시성 처리, 저장 포트 이름 등)는 아직 정하지 않았다.

## 슬라이스 간 의존

- 애그리거트는 다른 애그리거트 루트를 직접 참조할 수 있지만, 다른 애그리거트의 getter(`get~`/`is~`/`ensure~`)·record·enum 메서드만 호출한다. 다른 애그리거트를 바꾸는 메서드는 호출하지 않는다.
- 애플리케이션 서비스는 다른 슬라이스의 provided 포트 인터페이스와 DTO만 쓴다. 구현 클래스에 직접 접근하지 않는다.
- domain 슬라이스끼리, application 슬라이스끼리 순환 의존을 만들지 않는다.

## 검증

- 위 규칙은 ArchUnit 테스트(`HexagonalArchitectureTest`)로 강제한다. `layeredArchitecture().consideringAllDependencies()` 로 계층을, `slices().matching("<base>.domain.(*)..").should().beFreeOfCycles()` 로 순환을 검사한다(application도 같게).
