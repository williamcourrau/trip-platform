FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY driver-service/target/driver-service-0.0.1-SNAPSHOT.jar /app/driver-service.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/driver-service.jar"]
