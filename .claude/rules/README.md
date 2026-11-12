# mud-mk3 코딩 규칙

- 이 폴더는 코드를 **어떻게** 쓰는지(관례·구조·스타일·테스트 방법)만 다룬다.
- 기술 선택(무엇을 쓰나: 언어·프레임워크·버전, DB, 통신 방식, 월드 상태 관리)은 [docs/spec/system/platform.md](../../docs/spec/system/platform.md)를 따른다. 그곳이 단일 원본이다.
- 이 폴더의 규칙은 참고 레포의 관례를 바탕으로 정리했다. 참고 레포에서 실제로 확인한 관례만 적었다.
  - 출처: https://github.com/tobyspringboot/splearn-1-part2 (커밋 `2d1acdffdd0ad2aa4d400d0ed84544fd07ca0401`)
  - 참고 레포는 Spring Boot 3을 쓴다. mud-mk3가 쓰는 Spring Boot 4에서 바뀐 부분(스타터 이름, 테스트 애노테이션 패키지, null 애노테이션, Jackson 3)은 규칙에 맞춰 고쳤다.
- WebSocket 어댑터·메모리 월드 상태 관례는 참고 레포가 아니라 [platform.md](../../docs/spec/system/platform.md#실시간-통신과-월드-상태)의 결정에서 나온 것이다.
- 이 규칙은 `world-server`(Java Spring) 모듈의 규칙이다. 경로는 `world-server/` 기준이다. play-client·admin-console(Next.js) 규칙은 아직 없다.
- 아래에서 `<base>` 는 mud-mk3의 루트 패키지 `mudmk3` 다.
- 시스템 스펙: [docs/spec/system](../../docs/spec/system/index.md). 게임 규칙: [docs/spec/game-rule](../../docs/spec/game-rule/index.md).

| 파일 | 다루는 것 |
|---|---|
| [build.md](build.md) | Gradle 설정, 의존성 넣는 법, DB 의존성(테스트 H2), 정적 분석 |
| [architecture.md](architecture.md) | 헥사고날 계층, 패키지 구조, 포트(provided/required), 의존 방향, 메모리 월드 상태, ArchUnit |
| [domain-model.md](domain-model.md) | 엔티티·값 객체·팩터리 메서드·불변식 검증 |
| [application.md](application.md) | 애플리케이션 서비스, 요청 DTO, 입력 검증, 예외 |
| [adapter.md](adapter.md) | 웹(REST) 어댑터, WebSocket 어댑터, 응답 DTO, 예외 응답 |
| [persistence.md](persistence.md) | 테스트 DB, JPA 매핑(orm.xml), 리포지토리 |
| [testing.md](testing.md) | 테스트 종류, 픽스처, 테스트 이름, AssertJ·Mockito |
| [code-style.md](code-style.md) | 네이밍, Lombok, null 처리, 코드 모양 |
