FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY trip-service/target/trip-service-0.0.1-SNAPSHOT.jar /app/trip-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/trip-service.jar"]
