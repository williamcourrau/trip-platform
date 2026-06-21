FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY services/trip-service/target/trip-service-0.0.1-SNAPSHOT.jar /app/services/trip-service.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/services/trip-service.jar"]
