FROM openjdk:17-jdk-slim

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN chmod +x gradlew

COPY src src

RUN ./gradlew build -x test

RUN mkdir -p data

EXPOSE 8080

CMD ["./gradlew", "run", "-b"]