FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY payment-service/target/payment-service-0.0.1-SNAPSHOT.jar /app/payment-service.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/payment-service.jar"]
