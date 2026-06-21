FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY services/driver-service/target/driver-service-0.0.1-SNAPSHOT.jar /app/services/driver-service.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/services/driver-service.jar"]
