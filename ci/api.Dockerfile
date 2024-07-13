FROM maven:3.9.5-eclipse-temurin-17 AS builder

RUN curl -sSL https://get.daml.com/ | sh
ENV PATH="/root/.daml/bin:$PATH"

COPY daml /app/daml
COPY daml.yaml /app/daml.yaml

COPY src /app/src
COPY pom.xml /app/pom.xml

WORKDIR /app

RUN mvn clean package

FROM openjdk:17

COPY --from=builder /app/target/daml-spring-boot-example-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/app.jar"]