---
paths:
  - "build.gradle.kts"
  - "settings.gradle.kts"
  - "gradle/**"
---

# 빌드

출처는 [README](README.md).

- Gradle Kotlin DSL(`build.gradle.kts`, `settings.gradle.kts`)과 Gradle Wrapper(`./gradlew`)를 쓴다.
- Java 21 toolchain을 쓴다: `java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }`.
- Spring Boot 3.5.x와 `io.spring.dependency-management` 플러그인을 쓴다. 버전은 참고 레포 기준을 따른다(확정은 열린 질문).
- 기본 의존성: `spring-boot-starter-data-jpa`, `spring-boot-starter-web`, `spring-boot-starter-validation`, Lombok(`compileOnly` + `annotationProcessor`, 테스트도 `testCompileOnly` + `testAnnotationProcessor`).
- 테스트 의존성: `spring-boot-starter-test`, `archunit-junit5`, `mockito-core`(+ Mockito를 `-javaagent` 로 붙이는 `mockitoAgent` configuration), `instancio-core`, `junit-platform-launcher`(testRuntimeOnly).
- 테스트는 `useJUnitPlatform()` 으로 돌린다.
- SpotBugs 플러그인으로 정적 분석을 하고, 예외 필터는 `spotbugs-exclude-filter.xml` 에 둔다. 빌드는 SpotBugs를 통과해야 한다.
- 개발·테스트 DB는 H2(`runtimeOnly`)를 쓴다. 운영 DB는 (미정) — 참고 레포는 MySQL + `spring-boot-docker-compose`(`developmentOnly`).
