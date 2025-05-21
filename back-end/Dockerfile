FROM openjdk:21-jdk-slim

WORKDIR /app

COPY . /app

RUN chmod +x gradlew

RUN ./gradlew build -x test

EXPOSE 8000

CMD ["java", "-jar", "build/libs/back-end-0.0.1-SNAPSHOT.jar"]
