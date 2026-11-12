# Java 21로 실행 파일 빌드
FROM amazoncorretto:21 AS build
WORKDIR /app

RUN yum install -y findutils && yum clean all

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x gradlew \
    && ./gradlew --no-daemon bootJar

# 빌드된 실행 파일로 앱 실행
FROM amazoncorretto:21
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
