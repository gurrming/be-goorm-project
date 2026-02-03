# 1. 자바 25 환경 설정 (Amazon Corretto 또는 Eclipse Temurin 추천)
FROM eclipse-temurin:25-jdk-alpine

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 빌드된 .jar 파일 위치 지정
ARG JAR_FILE=build/libs/*.jar

ARG CACHE_BUST=1

# 4. 파일을 컨테이너 내부로 복사
COPY ${JAR_FILE} app.jar

# 5. 애플리케이션 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]

ENV LOG_LEVEL WARN