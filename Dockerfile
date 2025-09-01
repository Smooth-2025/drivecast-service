FROM eclipse-temurin:21-jre-alpine
LABEL description="Docker image for drivecast service"
EXPOSE 8080
COPY build/libs/drivecast-service-0.0.1-SNAPSHOT-plain.jar app.jar
CMD ["java", "-jar", "/app.jar"]
