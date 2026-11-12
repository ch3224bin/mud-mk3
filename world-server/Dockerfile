# 빌드 단계: JDK로 실행 가능한 jar를 만든다(테스트는 빌드 파이프라인에서 따로 돌린다)
FROM eclipse-temurin:25.0.5_8-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --version --no-daemon
COPY spotbugs-exclude-filter.xml ./
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# 실행 단계: JRE만 두고 비루트 사용자로 띄운다
FROM eclipse-temurin:25.0.5_8-jre
RUN groupadd --system mud && useradd --system --gid mud --no-create-home mud
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
USER mud
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
