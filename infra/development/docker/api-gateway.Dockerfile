FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY services/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar /app/services/api-gateway.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/services/api-gateway.jar"]
