# mud-mk3 코딩 규칙

- 백엔드는 Java Spring(Spring Boot)으로 구현한다.
- 이 폴더의 규칙은 참고 레포의 관례를 바탕으로 정리했다. 참고 레포에서 실제로 확인한 관례만 적었다.
  - 출처: https://github.com/tobyspringboot/splearn-1-part2 (커밋 `2d1acdffdd0ad2aa4d400d0ed84544fd07ca0401`)
- 아래에서 `<base>` 는 mud-mk3의 루트 패키지다. 루트 패키지 이름은 아직 정하지 않았다(미정).
- 시스템 스펙: [docs/spec/system](../../docs/spec/system/index.md). 게임 규칙: [docs/spec/game-rule](../../docs/spec/game-rule/index.md).

| 파일 | 다루는 것 |
|---|---|
| [build.md](build.md) | 빌드 도구, Java·Spring Boot 버전, 의존성, 정적 분석 |
| [architecture.md](architecture.md) | 헥사고날 계층, 패키지 구조, 포트(provided/required), 의존 방향, ArchUnit |
| [domain-model.md](domain-model.md) | 엔티티·값 객체·팩터리 메서드·불변식 검증 |
| [application.md](application.md) | 애플리케이션 서비스, 요청 DTO, 입력 검증, 예외 |
| [adapter.md](adapter.md) | 웹 어댑터, 응답 DTO, 예외 응답 |
| [persistence.md](persistence.md) | JPA 매핑(orm.xml), 리포지토리 |
| [testing.md](testing.md) | 테스트 종류, 픽스처, 테스트 이름, AssertJ·Mockito |
| [code-style.md](code-style.md) | 네이밍, Lombok, null 처리, 코드 모양 |
