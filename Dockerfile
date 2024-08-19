FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/RealBirthdayNotifierBot-1.0.0.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
CMD ["java", "-jar", "app.jar"]