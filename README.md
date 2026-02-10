# Blog Backend API

REST API для блога на Spring Boot и PostgreSQL.

## Технологии

Java 21 • Spring Boot 4.0.2 • PostgreSQL 15 • Maven

## Установка и запуск

**1. Создайте базу данных:**
```
psql -U postgres
CREATE DATABASE blogdb;
CREATE USER bloguser WITH PASSWORD 'password';
GRANT ALL PRIVILEGES ON DATABASE blogdb TO bloguser;
```
2. Настройте подключение в application.properties

Сборка и запуск
```
mvn clean package
java -jar target/my-blog-back-app-1.0.0.jar
```
Тесты
```
mvn test
```
API Endpoints
Posts:

GET /api/posts - список постов

GET /api/posts/{id} - получить пост

POST /api/posts - создать пост

PUT /api/posts/{id} - обновить пост

DELETE /api/posts/{id} - удалить пост

POST /api/posts/{id}/likes - добавить лайк

GET/PUT /api/posts/{id}/image - работа с изображением

Comments:

GET /api/posts/{postId}/comments - комментарии поста

POST /api/posts/{postId}/comments - создать комментарий

PUT /api/posts/{postId}/comments/{id} - обновить комментарий

DELETE /api/posts/{postId}/comments/{id} - удалить комментарий

Примеры запросов:
# Получить посты
```
curl "http://localhost:8080/api/posts?search=java&pageNumber=1&pageSize=10"
```

# Создать пост
```
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{"title":"Hello","text":"World","tags":["java"]}'
```
# Добавить лайк
```
curl -X POST http://localhost:8080/api/posts/1/likes
```

Структура

```
src/main/java/com/blog/
├── controller/    # REST endpoints
├── service/       # Business logic
├── repository/    # Database access
├── dto/           # Data transfer objects
└── entity/        # Domain models
```