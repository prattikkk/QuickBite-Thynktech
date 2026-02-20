# ── Stage 1: build ──────────────────────────────────────────────
FROM maven:3.9.3-eclipse-temurin-17 AS build
WORKDIR /app

# Copy dependency descriptors first (layer cache)
COPY pom.xml ./
RUN mvn -B dependency:go-offline

# Copy source and build
COPY src src
RUN mvn -B -DskipTests package

# ── Stage 2: runtime ───────────────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app

# Create non-root user
RUN addgroup --system app && adduser --system --ingroup app app

COPY --from=build /app/target/*.jar app.jar

# JVM options via env (tune for your deployment)
ENV JAVA_OPTS="-Xms512m -Xmx1g -Duser.timezone=UTC"

USER app
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
