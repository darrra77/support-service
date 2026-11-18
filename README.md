# Support Service — сервис заявок технической поддержки

REST API для регистрации и обработки заявок на техническую поддержку. Основной функционал реализован на **Java / Spring Boot**, блок статистики — на **Kotlin/JVM** в том же приложении. Данные хранятся в **PostgreSQL**, схема создаётся миграциями **Flyway**.

## Документы проекта

| Документ | Содержание |
|---|---|
| [Спецификация](docs/requirements.md) | Проблема, роли, пользовательские истории, критерии приёмки, требования |
| [Backlog](docs/backlog.md) | Задачи, приоритеты и оценки трудозатрат |
| [Архитектура и диаграммы](docs/architecture.md) | Обработка заявки, компоненты приложения, ER-диаграмма |
| [Спецификация OpenAPI](docs/openapi.json) | Машиночитаемое описание REST API |
| [GitHub Actions](.github/workflows/ci.yml) | Сборка, тестирование, публикация Docker-образов |
| [План тестирования](docs/test-plan.md) | Тестовые сценарии и критерии проверки |
| [Результаты тестирования](docs/test-results.md) | Результаты 27 тестов, JaCoCo и проверок |
| [Журнал дефектов](docs/defects.md) | BUG-001 и регрессионное тестирование |
| [Развёртывание и откат](docs/deployment.md) | CI/CD, Docker и rollback |
## Технологии

| Компонент | Технология |
|---|---|
| Основное приложение | Java 21, Spring Boot 3.5.16 |
| Статистика | Kotlin/JVM 2.2.21 |
| Сборка | Gradle Wrapper (9.8.2) |
| База данных | PostgreSQL 16 |
| Миграции | Flyway |
| Тестирование | JUnit 5, Mockito, Testcontainers |
| Покрытие | JaCoCo |
| Форматирование | Spotless (Java и Kotlin) |
| API | REST, OpenAPI, Swagger UI |
| Развёртывание | Docker, Docker Compose, GitHub Actions, GHCR |

## Функциональность

- Создание, просмотр и редактирование заявок.
- Категории, приоритеты `LOW`, `MEDIUM`, `HIGH` и сроки исполнения.
- Контролируемые переходы статусов: `NEW → IN_PROGRESS → RESOLVED`; отмена из `NEW` или `IN_PROGRESS`.
- Запрет редактирования и повторного открытия завершённых/отменённых заявок (HTTP 409).
- История изменения статусов и транзакционное сохранение переходов.
- Поиск и фильтрация с пагинацией.
- Сводка за период: созданные заявки, распределение по статусам/приоритетам, просроченные и среднее время решения.

## Быстрый запуск

**Требуется:** Git, Docker Desktop или Docker Engine с Compose. Для локальной Gradle-сборки — JDK 21.

```bash
# В корне репозитория
cp .env.example .env
```

Откройте `.env` и задайте значение `DB_PASSWORD`. Не добавляйте этот файл в Git.

```bash
docker compose up -d --build
docker compose ps
```

После запуска:

| Адрес | Назначение |
|---|---|
| `http://localhost:8080/actuator/health` | Проверка состояния приложения |
| `http://localhost:8080/swagger-ui/index.html` | Интерактивная документация API |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON |
| `http://localhost:8080/api/categories` | Категории |

Проверка в терминале:

```bash
curl -fsS http://localhost:8080/actuator/health
curl -fsS http://localhost:8080/api/categories
```

Остановка **без удаления данных**:

```bash
docker compose down
```

**Не запускайте `docker compose down -v`**, если нужно сохранить заявки: этот флаг удаляет том с PostgreSQL.

## Основные методы API

| Метод | URL | Действие |
|---|---|---|
| `GET` | `/api/categories` | Список категорий |
| `POST` | `/api/tickets` | Создание заявки |
| `GET` | `/api/tickets/{id}` | Получение заявки |
| `GET` | `/api/tickets` | Список с фильтрацией и пагинацией |
| `PATCH` | `/api/tickets/{id}` | Редактирование заявки |
| `PATCH` | `/api/tickets/{id}/status` | Изменение статуса |
| `GET` | `/api/tickets/{id}/history` | История статусов |
| `GET` | `/api/reports/summary` | Статистика за период (`from`, `to`) |

Пример создания заявки:

```bash
curl -i -X POST http://localhost:8080/api/tickets \
  -H 'Content-Type: application/json' \
  -d '{"title":"Не работает компьютер","description":"Не включается","categoryId":1,"priority":"HIGH"}'
```

Точные схемы запросов и ответов доступны в Swagger и в [`docs/openapi.json`](docs/openapi.json).

## Тестирование и покрытие

Docker Engine должен быть запущен: интеграционные тесты используют PostgreSQL через Testcontainers. Для загрузки настроек локальной базы из `.env` на macOS/Linux:

```bash
set -a
source .env
set +a
./gradlew clean test jacocoTestReport
./gradlew spotlessCheck
```

Результаты:

- JUnit: `build/reports/tests/test/index.html`;
- JaCoCo HTML: `build/reports/jacoco/test/html/index.html`;
- JaCoCo XML: `build/reports/jacoco/test/jacocoTestReport.xml`.

По результатам тестирования успешно выполнены 27 автоматизированных тестов. Ошибок и пропущенных тестов нет. Общее покрытие JaCoCo составляет 87% инструкций и 66% ветвлений. Регрессионное тестирование BUG-001 завершено успешно.

## Контейнеры, данные и резервные копии

`compose.yaml` запускает приложение и PostgreSQL с постоянным томом. Flyway автоматически выполняет миграции при запуске.

Создать SQL-копию основной базы:

```bash
mkdir -p backups
docker compose exec -T postgres pg_dump -U support -d supportdb > backups/supportdb_backup.sql
```

Восстановить копию **в отдельную** базу (основную `supportdb` не затрагивать):

```bash
docker compose exec -T postgres psql -U support -d postgres -c 'CREATE DATABASE supportdb_restore;'
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U support -d supportdb_restore < backups/supportdb_backup.sql
```

Команда `CREATE DATABASE` выполняется однократно. Файлы `backups/` исключены из Git.

## Развёртывание и версии образов

GitHub Actions после успешной проверки исходников публикует образ `ghcr.io/darrra77/support-service:<полный-SHA-коммита>` для `linux/amd64` и `linux/arm64`.

Для **ручного** запуска опубликованного образа используйте `compose.deploy.yaml` (отдельная база и порт `8081`):

```bash
export IMAGE_TAG=<полный-SHA-опубликованного-образа>
# DB_PASSWORD должен быть задан в .env или окружении
docker compose -p support-deploy -f compose.deploy.yaml up -d --no-build --pull always
curl -fsS http://localhost:8081/actuator/health
```

Развёртывание на временном GitHub-hosted runner (если соответствующий шаг включён и успешно прошёл) подтверждает запуск и smoke-проверку **в рамках pipeline**, но не предоставляет постоянно работающий сервер. Ручной откат Docker-образа успешно проверен на локальном стенде. Выполнено переключение с версии 91068de на c462ee7 и последующее восстановление актуальной версии. Все проверки работоспособности завершились успешно. Подробности приведены в docs/deployment.md. 

## Автоматическое развёртывание (CD)

GitHub Actions выполняет автоматическое развёртывание приложения после успешной сборки, тестирования и публикации Docker-образа в GitHub Container Registry.

Для развёртывания используется GitHub-hosted runner с операционной системой Ubuntu. Отдельная виртуальная машина или собственный сервер не требуются.

Процесс развёртывания включает:

1. Загрузку опубликованного Docker-образа из GHCR.
2. Запуск Spring Boot и PostgreSQL через Docker Compose.
3. Проверку работоспособности приложения через /actuator/health.
4. Проверку REST API через /api/categories.
5. Сохранение логов выполнения в артефактах GitHub Actions.

Развёртывание выполняется во временном окружении GitHub Actions. После завершения задания контейнеры останавливаются, поэтому этот вариант предназначен для автоматизированной проверки развёртывания, а не для постоянного размещения приложения.

## Диагностика

| Проблема | Проверка | Что исправить |
|---|---|---|
| База недоступна | `docker compose ps`, `docker compose logs postgres` | Проверить `DB_PASSWORD`, готовность PostgreSQL, адрес `postgres:5432` внутри сети Compose |
| Приложение не отвечает на health check | `docker compose logs app --tail=100` | Проверить запуск Spring Boot, порт `8080`, настройки БД |
| Flyway не применил миграцию | `docker compose logs app` | Найти ошибку и версию SQL-файла в `src/main/resources/db/migration`; не редактировать уже применённые миграции |

## Структура репозитория

```text
.github/workflows/       CI/CD
src/main/java/           Java: API, модели, репозитории и бизнес-логика
src/main/kotlin/         Kotlin: статистика
src/main/resources/      Конфигурация и SQL-миграции
src/test/java/           Тесты Java и интеграционные сценарии
src/test/kotlin/         Тесты Kotlin и взаимодействия с Java
docs/                   Аналитика, диаграммы, контракт API
Dockerfile               Многоэтапная сборка приложения
compose.yaml             Локальный запуск приложения и базы
compose.deploy.yaml      Запуск готового образа из GHCR
```
