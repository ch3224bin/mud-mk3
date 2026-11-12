---
paths:
  - "world-server/build.gradle.kts"
  - "world-server/settings.gradle.kts"
  - "world-server/gradle/**"
  - "world-server/compose.yaml"
---

# 빌드

출처는 [README](README.md). 기술 선택과 버전은 [docs/spec/system/platform.md](../../docs/spec/system/platform.md#기술-스택)를 따른다. 이 문서는 빌드 설정을 어떻게 쓰는지만 다룬다.

## 설정

- Gradle Kotlin DSL(`build.gradle.kts`, `settings.gradle.kts`)과 Gradle Wrapper(`./gradlew`)를 쓴다.
- Java 버전은 toolchain으로 정한다: `java { toolchain { languageVersion = JavaLanguageVersion.of(<Java 버전>) } }`.
- 플러그인은 `org.springframework.boot`, `io.spring.dependency-management`, `com.github.spotbugs` 를 쓴다.
- Spring Boot BOM이 관리하는 라이브러리(Spring Framework, Hibernate, Jackson, JUnit, Mockito, AssertJ, DB 드라이버 등)는 버전을 적지 않고 BOM을 따른다. BOM 밖의 라이브러리(ArchUnit, Instancio)만 버전을 적는다.

## 의존성

- 메인: `spring-boot-starter-data-jpa`, `spring-boot-starter-webmvc`, `spring-boot-starter-websocket`, `spring-boot-starter-validation`, Lombok(`compileOnly` + `annotationProcessor`, 테스트도 `testCompileOnly` + `testAnnotationProcessor`).
  - Spring Boot 4는 스타터가 기술별로 나뉘었다. 웹은 `spring-boot-starter-web` 대신 `spring-boot-starter-webmvc` 를 쓴다.
- 테스트: `spring-boot-starter-test`, `spring-boot-starter-webmvc-test`(`@WebMvcTest`), `spring-boot-starter-data-jpa-test`(`@DataJpaTest`), `archunit-junit5`, `instancio-core`, `junit-platform-launcher`(testRuntimeOnly).
- Mockito는 `-javaagent` 로 붙인다: `mockitoAgent` configuration에 `org.mockito:mockito-core`(`isTransitive = false`, 버전은 BOM)를 넣고 `tasks.withType<Test> { useJUnitPlatform(); jvmArgs("-javaagent:${mockitoAgent.asPath}") }`.
- SpotBugs로 정적 분석을 하고, 예외 필터는 `spotbugs-exclude-filter.xml` 에 둔다. 빌드는 SpotBugs를 통과해야 한다.

## DB 의존성

- 운영 DB 드라이버는 `runtimeOnly` 로 넣는다.
- 로컬 실행용 DB는 `compose.yaml` + `developmentOnly("org.springframework.boot:spring-boot-docker-compose")` 로 띄운다.
- 테스트(`@DataJpaTest`, `@SpringBootTest`)는 H2 메모리 DB로 돌린다: `testRuntimeOnly("com.h2database:h2")`. H2는 메인 런타임에 넣지 않는다.
