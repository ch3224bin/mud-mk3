# 플랫폼

게임이 돌아가는 바탕. → [목차](index.md)

## 웹 기반

- 게임은 웹 기반이다. 플레이어는 웹에서 접속해 텍스트로 플레이한다.
- 관리 화면도 있다. → [관리 툴](admin-tool.md)
- 백엔드는 Java Spring으로 구현한다. → [기술 스택](#기술-스택)
- 프론트엔드는 Next.js로 구현한다. → [기술 스택](#기술-스택)

## 기술 스택

기술 선택의 단일 원본이다. 코드를 어떻게 쓰는지(관례·구조·스타일)는 [`.claude/rules`](../../../.claude/rules/README.md)에 둔다.

| 대상 | 선택 · 버전 |
|---|---|
| 언어 | Java 25 (최신 LTS) |
| 프레임워크 | Spring Boot 4.1.2 (BOM 기준 Spring Framework 7.0.10, Hibernate 7.4.13.Final, Jackson 3.1.7, JUnit Jupiter 6.0.3, Mockito 5.23.0) |
| 빌드 | Gradle 9.9.1, Kotlin DSL |
| Gradle 플러그인 | `io.spring.dependency-management` 1.1.7, `com.github.spotbugs` 6.5.13 |
| 테스트 라이브러리 (BOM 밖) | ArchUnit 1.5.1, Instancio 6.2.1 |
| 운영 DB | PostgreSQL (드라이버는 BOM 기준 42.7.14) |
| 테스트 DB | H2 메모리 DB (BOM 기준 2.4.240). 테스트용으로만 쓴다 |
| 프론트엔드 | Next.js (버전 미정) |
| 게임 접속 | WebSocket, 메시지는 JSON → [실시간 통신과 월드 상태](#실시간-통신과-월드-상태) |
| 배포 | Docker(Dockerfile) + docker-compose → 추후 Kubernetes → [배포](#배포) |

- 버전은 "최신 버전으로" 라는 결정에 따라 2026-11-12 기준 최신 안정판으로 확정했다.
- NPC와 내러티브(NPC 장기 메모리 포함)도 PostgreSQL에 둔다. 저장 방식은 (미정). → [NPC](npc.md#데이터-관리)
- 버전 출처 (2026-11-12 확인):
  - Java: Adoptium API `available_releases` — `most_recent_lts: 25`
  - Gradle: services.gradle.org `versions/current` — 9.9.1
  - Spring Boot: Maven Central `spring-boot` 메타데이터 — 정식 최신 4.1.2(4.2.0은 RC). Boot 4.1 요구 사항: Java 17 이상(27까지 호환), Gradle 8.14 이상 또는 9.x (spring-boot 레포 `system-requirements.adoc`, v4.1.2)
  - BOM 관리 버전: Maven Central `spring-boot-dependencies-4.1.2.pom`
  - Gradle 플러그인: Gradle Plugin Portal 메타데이터
  - ArchUnit, Instancio: Maven Central 메타데이터

## 실시간 통신과 월드 상태

- 게임 접속은 WebSocket으로 한다.
- WebSocket으로 오가는 데이터는 JSON이다. 프론트엔드는 JSON을 받아 텍스트로 표현한다.
- JSON 메시지 구조(종류·필드)는 (미정).
- 월드 상태(방에 누가 있는지, 전투 등)는 메모리에 두고, 주기적으로 또는 중요한 시점에 DB에 저장한다.
- 저장 주기, 어떤 시점이 "중요한 시점" 인지, 틱은 (미정).

## 저장소 구성

- mud-mk3 저장소 아래 모듈이 백엔드와 프론트엔드로 나뉜다.
- 모듈 이름은 `backend`, `frontend` 가 아니라 자기 역할이 드러나는 이름으로 짓는다. 이름은 (미정).

## 배포

- 모듈마다 Dockerfile을 만든다.
- 처음에는 맥북에서 docker-compose로 운영해 본다.
- 추후 Kubernetes 환경으로 배포한다. 시기와 구성은 (미정).

## 멀티 유저

- 여러 플레이어가 같은 월드에 동시에 접속해 함께 플레이한다.
- 플레이어끼리의 대화(말·귓속말·외치기)는 [명령어 처리](command.md#대화형-명령어)에서 다룬다.

## 단일 월드

- 게임은 거의 창호 또는 소수의 사람만 할 것으로 보여, 월드는 하나만 운영한다. → [월드 구조](world-structure.md)

## 열린 질문

- 루트 패키지 이름은?
- 월드 상태를 DB에 얼마나 자주 저장하는가? 어떤 시점이 "중요한 시점" 인가? (예: 접속 종료, 돌파, 아이템 획득 등)
- 게임 틱(전투·회복 등이 도는 주기)을 어떻게 돌리는가?
- 백엔드·프론트엔드 모듈 이름은? 관리 툴은 프론트엔드 모듈에 같이 두는가, 따로 두는가?
- JSON 메시지 구조(종류·필드, 색·강조 정보를 넣는지)는?
- Next.js 버전, 그리고 SSR 등 Next.js 서버 기능을 쓰는가, 정적 클라이언트로 쓰는가?
- 계정·로그인은 어떤 방식인가? 계정 하나에 캐릭터 여럿을 둘 수 있는가? (미정) → [룰북: 캐릭터 생성](../game-rule/character.md#캐릭터-생성)
- 관리 화면은 게임 화면과 같은 웹 앱 안에 있는가, 따로인가? (미정)
