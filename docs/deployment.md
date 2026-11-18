# Развёртывание и откат Support Service

## 1. Схема CI/CD

Исходный код хранится в GitHub-репозитории `darrra77/support-service`.

Workflow `.github/workflows/ci.yml` выполняет последовательность:

1. **Build and Test** - получение исходного кода, проверка форматирования Java/Kotlin, сборка Gradle, запуск тестов и формирование отчёта JaCoCo.
2. **Publish Docker image** - сборка Docker-образа для `linux/amd64` и `linux/arm64`, публикация в GitHub Container Registry (GHCR) с тегом полного SHA коммита.
3. **Deploy and Verify** - загрузка опубликованного образа, запуск приложения и PostgreSQL через `compose.deploy.yaml`, проверка `/actuator/health` и `/api/categories`, сохранение логов.

Публикация и развёртывание выполняются после успешных проверок при отправке изменений в `main`. При ошибке тестов дальнейшие этапы не выполняются. Секрет `DEPLOY_DB_PASSWORD` хранится в GitHub Actions Secrets, а не в репозитории.

**Особенность стенда:** `Deploy and Verify` запускается на GitHub-hosted runner (`ubuntu-latest`), который существует только во время выполнения задания. По завершении контейнеры останавливаются. Это автоматизированная проверка развёртывания, **не постоянный учебный сервер**.

## 2. Локальный стенд для проверки опубликованных версий

Для отдельной проверки развёртывания используется Docker Desktop на macOS и файл `compose.deploy.yaml`:

- приложение доступно на `http://localhost:8081`;
- PostgreSQL работает в отдельном контейнере;
- данные PostgreSQL хранятся в именованном Docker volume `support-deploy_deploy_postgres_data`;
- образ приложения загружается из `ghcr.io/darrra77/support-service` по SHA-тегу.

Для запуска нужны Docker Desktop, файл `.env` с переменной `DB_PASSWORD` и переменная окружения `IMAGE_TAG`, соответствующая опубликованному образу.

```bash
export IMAGE_TAG=$(git rev-parse HEAD)
docker compose -p support-deploy -f compose.deploy.yaml up -d --no-build
curl -fsS http://localhost:8081/actuator/health
curl -fsS http://localhost:8081/api/categories
```

Не выполнять `docker compose down -v` при проверке сохранности данных: флаг `-v` удаляет тома базы данных.

## 3. Проведённая проверка rollback

### Использованные версии

| Назначение | Коммит (полный SHA) | Примечание |
|---|---|---|
| Новая версия | `91068de81ef350a6bd40c30928b58c2d206c8da2` | С исправлением BUG-001 |
| Предыдущая версия | `c462ee767d408881996efb35c3343c37bbdcb5f5` | До исправления BUG-001 |

Оба образа успешно загружены из GHCR через `docker pull` на компьютере с Apple Silicon.

### Последовательность и результаты

| Действие | Проверка | Фактический результат |
|---|---|---|
| Запуск новой версии `91068de` | `GET /actuator/health`, `GET /api/categories` | HTTP 200, `{"status":"UP"}`, 4 категории |
| Откат на `c462ee7` | `docker compose ... ps`, `GET /actuator/health`, `GET /api/categories` | Запущен образ с SHA `c462ee7…`, HTTP 200, `{"status":"UP"}`, 4 категории |
| Возврат на новую версию `91068de` | `docker compose ... ps`, `GET /actuator/health` | Запущен образ с SHA `91068de…`, HTTP 200, `{"status":"UP"}` |

При каждой смене версии пересоздавался **только** сервис `app`:

```bash
export NEW_IMAGE=$(git rev-parse 91068de)
export OLD_IMAGE=$(git rev-parse c462ee7)

# Новая версия
export IMAGE_TAG=$NEW_IMAGE
docker compose -p support-deploy -f compose.deploy.yaml up -d --no-deps --no-build app
curl -fsS http://localhost:8081/actuator/health

# Откат
export IMAGE_TAG=$OLD_IMAGE
docker compose -p support-deploy -f compose.deploy.yaml up -d --no-deps --no-build app
curl -fsS http://localhost:8081/actuator/health

# Восстановление актуальной версии
export IMAGE_TAG=$NEW_IMAGE
docker compose -p support-deploy -f compose.deploy.yaml up -d --no-deps --no-build app
curl -fsS http://localhost:8081/actuator/health
```

### Состояние после завершения

По последнему `docker compose ... ps`:

- `support-deploy-app-1`: образ `ghcr.io/darrra77/support-service:91068de81ef350a6bd40c30928b58c2d206c8da2`, статус `Up`, порт `8081:8080`;
- `support-deploy-postgres-1`: образ `postgres:16`, статус `Up (healthy)`.

Контейнер PostgreSQL на протяжении переключений образа не пересоздавался (в выводе `ps` дата создания контейнера оставалась прежней). **Отдельная проверка сохранности конкретной тестовой записи до и после отката не проводилась в этом сценарии**; сохранность данных при перезапуске Compose проверялась ранее.

**Вывод:** ручной откат на ранее опубликованный образ и возвращение актуальной версии успешно проверены на локальном Docker-стенде. Автоматический pipeline GitHub Actions проверяет развёртывание на временном runner, но описанный здесь rollback выполнялся отдельно, вручную.

## 4. Ограничения и меры предосторожности

- Схема PostgreSQL должна оставаться совместимой с предыдущим образом; не следует менять уже применённые миграции Flyway.
- Откат образа не равен откату схемы базы данных.
- При сбое прежде всего проверяются `docker compose ... ps`, `docker compose ... logs app` и `/actuator/health`.
- Секреты не публикуются в README или логах; локальный `.env` не коммитится в Git.
