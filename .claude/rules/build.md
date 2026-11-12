---
paths:
  - "build.gradle.kts"
  - "settings.gradle.kts"
  - "gradle/**"
  - "compose.yaml"
---

# 빌드

출처는 [README](README.md). 버전은 2026-11-12에 공식 저장소에서 확인한 최신 안정판이다(아래 "버전 출처").

## 버전 (확정)

| 대상 | 버전 |
|---|---|
| Java (toolchain) | 25 (최신 LTS) |
| Gradle (Wrapper) | 9.9.1 |
| Spring Boot (`org.springframework.boot` 플러그인) | 4.1.2 |
| `io.spring.dependency-management` 플러그인 | 1.1.7 |
| `com.github.spotbugs` 플러그인 | 6.5.13 |
| ArchUnit (`archunit-junit5`) | 1.5.1 |
| Instancio (`instancio-core`) | 6.2.1 |

- Spring Boot BOM이 관리하는 라이브러리(Spring Framework, Hibernate, Jackson, JUnit, Mockito, AssertJ, H2, PostgreSQL 드라이버 등)는 버전을 적지 않고 BOM을 따른다.

## 설정

- Gradle Kotlin DSL(`build.gradle.kts`, `settings.gradle.kts`)과 Gradle Wrapper(`./gradlew`)를 쓴다.
- `java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }`.
- 메인 의존성: `spring-boot-starter-data-jpa`, `spring-boot-starter-webmvc`, `spring-boot-starter-websocket`, `spring-boot-starter-validation`, Lombok(`compileOnly` + `annotationProcessor`, 테스트도 `testCompileOnly` + `testAnnotationProcessor`).
  - Boot 4는 스타터가 기술별로 나뉘었다. 웹은 `spring-boot-starter-web` 대신 `spring-boot-starter-webmvc` 를 쓴다.
- 테스트 의존성: `spring-boot-starter-test`, `spring-boot-starter-webmvc-test`(`@WebMvcTest`), `spring-boot-starter-data-jpa-test`(`@DataJpaTest`), `archunit-junit5`, `instancio-core`, `junit-platform-launcher`(testRuntimeOnly).
- Mockito는 `-javaagent` 로 붙인다: `mockitoAgent` configuration에 `org.mockito:mockito-core`(`isTransitive = false`, 버전은 BOM)를 넣고 `tasks.withType<Test> { useJUnitPlatform(); jvmArgs("-javaagent:${mockitoAgent.asPath}") }`.
- SpotBugs 플러그인으로 정적 분석을 하고, 예외 필터는 `spotbugs-exclude-filter.xml` 에 둔다. 빌드는 SpotBugs를 통과해야 한다.

## DB

- 운영 DB는 PostgreSQL이다: `runtimeOnly("org.postgresql:postgresql")`.
- 로컬 실행은 PostgreSQL을 `compose.yaml` + `developmentOnly("org.springframework.boot:spring-boot-docker-compose")` 로 띄워 쓴다.
- 테스트(`@DataJpaTest`, `@SpringBootTest`)는 H2 메모리 DB로 돌린다: `testRuntimeOnly("com.h2database:h2")`. H2는 메인 런타임에 넣지 않는다.

## 버전 출처 (2026-11-12 확인)

- Java: Adoptium API `available_releases` — `most_recent_lts: 25`
- Gradle: services.gradle.org `versions/current` — 9.9.1
- Spring Boot: Maven Central `spring-boot` 메타데이터 — 정식 최신 4.1.2(4.2.0은 RC). Boot 4.1 요구 사항: Java 17 이상(27까지 호환), Gradle 8.14 이상 또는 9.x (spring-boot `system-requirements.adoc`, v4.1.2)
- Spring Boot 4.1.2 BOM(`spring-boot-dependencies-4.1.2.pom`): Spring Framework 7.0.10, Hibernate 7.4.13.Final, Jackson 3.1.7, JUnit Jupiter 6.0.3, Mockito 5.23.0, H2 2.4.240, PostgreSQL 42.7.14, JSpecify 1.0.1
- 플러그인: Gradle Plugin Portal 메타데이터 — dependency-management 1.1.7, spotbugs 6.5.13
- ArchUnit 1.5.1, Instancio 6.2.1: Maven Central 메타데이터
