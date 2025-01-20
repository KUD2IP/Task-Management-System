FROM openjdk:17-jdk-slim

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем файл JAR
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Устанавливаем переменные среды
ENV SPRING_PROFILES_ACTIVE=docker

# Открываем порт
EXPOSE 8222

# Команда для запуска приложения
ENTRYPOINT ["java", "-jar", "app.jar"]