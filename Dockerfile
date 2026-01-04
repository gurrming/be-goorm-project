# 1. 빌드 스테이지
FROM amazoncorretto:17-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew clean build -x test

# 2. 실행 스테이지
FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]