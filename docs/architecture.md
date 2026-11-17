# Архитектура и диаграммы

## 1. Состав системы

Приложение представляет собой один Spring Boot-сервис с Java-контроллерами и бизнес-логикой и Kotlin-сервисом статистики. Данные хранятся в PostgreSQL. Flyway выполняет версионированные SQL-миграции при запуске.

```mermaid
flowchart LR
    Client[Swagger / HTTP-клиент] --> API[Spring Boot REST API на Java]
    API --> Service[TicketService на Java]
    API --> Report[ReportController на Java]
    Report --> Kotlin[ReportService на Kotlin]
    Service --> Repo[JPA-репозитории]
    Kotlin --> Repo
    Repo --> DB[(PostgreSQL)]
    Flyway[Flyway] --> DB
```

## 2. Процесс обработки заявки

```mermaid
flowchart TD
    A[Создать заявку] --> B[Статус NEW]
    B -->|Начать работу| C[IN_PROGRESS]
    B -->|Отменить| E[CANCELLED]
    C -->|Решить| D[RESOLVED]
    C -->|Отменить| E
    D --> F[Закрыта: правки запрещены]
    E --> F
    B -. Недопустимый переход .-> G[HTTP 409]
    C -. Недопустимый переход .-> G
    F -. Изменение .-> G
```

Каждый допустимый переход записывается в `ticket_status_history` в одной транзакции с обновлением `tickets`.

## 3. ER-диаграмма

```mermaid
erDiagram
    CATEGORIES ||--o{ TICKETS : "category_id"
    TICKETS ||--o{ TICKET_STATUS_HISTORY : "ticket_id"

    CATEGORIES {
        bigint id PK
        string name
    }
    TICKETS {
        bigint id PK
        string title
        string description
        bigint category_id FK
        string priority
        string status
        timestamp created_at
        timestamp due_at
        timestamp closed_at
    }
    TICKET_STATUS_HISTORY {
        bigint id PK
        bigint ticket_id FK
        string old_status
        string new_status
        timestamp changed_at
    }
```

Схема отражает сущности и логические связи. Точные SQL-типы и ограничения определяются `src/main/resources/db/migration/V1__create_tables.sql`.

## 4. Отчётный период

Для сводки используются заявки, удовлетворяющие условию `createdAt >= from AND createdAt < to`. Статусы и приоритеты считаются по этой же выборке. Среднее время решения вычисляется по завершённым заявкам как `closedAt - createdAt`. Если завершённых нет, возвращается `null`.

## 5. Развёртывание

Локальный `compose.yaml` собирает приложение и запускает PostgreSQL с постоянным томом. `compose.deploy.yaml` скачивает опубликованный образ из GHCR по полному SHA коммита и разворачивает его с отдельным томом БД на порту 8081.
