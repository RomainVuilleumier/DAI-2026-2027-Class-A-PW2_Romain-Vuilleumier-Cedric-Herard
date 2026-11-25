#  Build with Maven
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime image
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

COPY --from=build /app/target/Dactilo-CHD-RMV-1.0-SNAPSHOT.jar ./Dactliorace.jar

ENV TERM=xterm-256color

EXPOSE 5000/tcp
EXPOSE 5001/udp

ENTRYPOINT ["java", "-jar", "/app/Dactilorace.jar"]