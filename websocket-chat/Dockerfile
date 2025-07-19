# Use OpenJDK 17 runtime
FROM eclipse-temurin:17-jre-jammy

# Set working directory
WORKDIR /app

# Copy the packaged JAR file (shaded JAR from Maven build)
COPY target/websocket-chat-1.0.0.jar app.jar

# Copy Firebase service account key (if needed)
# COPY firebase-service-account.json .

# Create non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /app
USER appuser

# Expose port (Cloud Run will set PORT env variable)
EXPOSE 8080

# Install curl for health check
USER root
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]