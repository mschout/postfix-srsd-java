FROM eclipse-temurin:25-jdk-alpine AS build

WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties gradlew ./
ADD gradle ./gradle
RUN ./gradlew --no-daemon --version || true
COPY src /app/src
RUN ./gradlew --no-daemon clean jar

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar /app/postfix-srsd.jar
ENV LOCAL_ALIAS=bounces.localhost
ENV SECRET_NAME=postfix-srsd-secrets
CMD java -jar /app/postfix-srsd.jar --log-file=- --socket=tcp:2510 --local-alias $LOCAL_ALIAS --secret-file /run/secrets/$SECRET_NAME
