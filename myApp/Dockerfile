# Используем официальный образ OpenJDK
FROM eclipse-temurin:17-jdk-jammy as builder

# Рабочая директория
WORKDIR /app

# Копируем файлы для сборки
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# Собираем приложение
RUN chmod +x gradlew
RUN ./gradlew clean build -x test

# Финальный образ
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Копируем собранный JAR из builder
COPY --from=builder /app/build/libs/*.jar app.jar

# Открываем порт
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]