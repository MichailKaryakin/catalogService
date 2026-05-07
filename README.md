# Catalog Service

Микросервис управления каталогом товаров: продукты, категории, складские остатки.

## Быстрый старт

### Dev

Запустить инфраструктуру:

```bash
docker-compose up -d postgres zookeeper kafka redis
```

Запустить сервис:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Prod

> Перед запуском замените `jwt.secret` в `application.yml` на надёжный секрет (≥32 символа).

```bash
docker-compose --profile app up -d
```

## Адреса

| Сервис          | URL                                         |
|-----------------|---------------------------------------------|
| Catalog Service | http://localhost:8081                       |
| Swagger UI      | http://localhost:8081/swagger-ui/index.html |
| PostgreSQL      | localhost:5434                              |
| Kafka           | localhost:9092                              |
| Redis           | localhost:6379                              |
| Prometheus      | http://localhost:9090                       |
| Grafana         | http://localhost:3000                       |

## Тесты

```bash
./gradlew test                # unit
./gradlew integrationTest     # integration (требует Docker)
```