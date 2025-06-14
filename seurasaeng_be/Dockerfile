FROM maven:3.9.4-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy dependency files first for better caching
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B -T 1C

FROM eclipse-temurin:21-jre-alpine

# Add non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -S appuser -u 1001 -G appgroup

WORKDIR /app

# Copy jar with specific name
COPY --from=builder /app/target/*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

CMD ["java", "-server", "-XX:+UseContainerSupport", "-jar", "app.jar"]