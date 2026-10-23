FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl rclone \
    && rm -rf /var/lib/apt/lists/*
RUN useradd --system --create-home --uid 10001 gibiteca
WORKDIR /app
COPY --from=build /workspace/target/minha-gibiteca-0.1.0-SNAPSHOT.jar app.jar
RUN mkdir -p /app/data/capas /app/data/rclone && chmod 700 /app/data/rclone && chown -R gibiteca:gibiteca /app/data
USER gibiteca
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
