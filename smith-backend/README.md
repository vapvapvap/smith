# smith-backend

REST-бэкенд ИИ-агента. Принимает запрос с параметрами генерации (промпт,
модель, temperature, top_p, top_k, penalties, max_tokens, stop) и вызывает
DeepSeek API. Поддерживает синхронный ответ (JSON) и стриминг (SSE).

## Стек

Java 25, Spring Boot 4.1.1, Gradle 9.7.1 (wrapper), MyBatis 4.1.0,
HikariCP, Flyway 12.4.0, PostgreSQL 17, Apache HttpClient5 5.6.4,
springdoc-openapi 3.1.1.

## Модули

| Модуль | Назначение |
|---|---|
| `domain` | enum моделей, value objects, порты (`LlmProvider`, `ChatRequestRepository`, `ModelRegistry`) |
| `api` | DTO + контракт `ChatApi` (аннотации OpenAPI) |
| `infrastructure` | MyBatis-маппер, Flyway-миграции, `DeepSeekLlmProvider` |
| `application` | `ChatCompletionService`, `ModelInfoService`, маппинг DTO<->domain |
| `api-impl` | `ChatController`, `AiAgentApplication`, конфигурация, обработка ошибок |

## Требования

- JDK 25 (например `C:\Program Files\Java\jdk-25.0.4`).
- PostgreSQL 17 на `localhost:5432`, БД `ai_agent` (создаётся автоматически
  миграцией при старте, если БД уже существует).
- Переменные окружения:
  - `DB_USER` (по умолчанию `postgres`)
  - `DB_PASSWORD`
  - `DEEPSEEK_API_KEY`

Для локальной разработки можно создать `api-impl/application-local.yml`
(в `.gitignore`) с реальными значениями и запускать с профилем `local`.

## Сборка и запуск

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.4"
.\gradlew.bat build                    # сборка + тесты
.\gradlew.bat :api-impl:bootRun "--args=--spring.profiles.active=local"   # локальный запуск
```

Запуск собранного jar:

```powershell
java -jar api-impl\build\libs\api-impl-0.1.0.jar
```

При старте Flyway применяет миграции (`V1__init.sql`) — создаёт таблицу
`chat_request` для хранения истории запросов.

## API

Swagger UI: http://localhost:8080/swagger-ui.html
Спецификация: http://localhost:8080/v3/api-docs

### POST /api/v1/chat/completions — синхронный ответ (JSON)

```bash
curl -X POST http://localhost:8080/api/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Расскажи анекдот","model":"DEEPSEEK_V4_FLASH"}'
```

### POST /api/v1/chat/completions/stream — ответ потоком (SSE)

```bash
curl -N -X POST http://localhost:8080/api/v1/chat/completions/stream \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Посчитай от 1 до 5","model":"DEEPSEEK_V4_FLASH"}'
```

События: `chunk` (content/reasoningContent), `usage`, `done` (finishReason),
`error`.

### GET /api/v1/models — список моделей

```bash
curl http://localhost:8080/api/v1/models
```

## Параметры запроса

| Поле | Обязательно | По умолчанию | Передаётся в DeepSeek |
|---|---|---|---|
| `prompt` | да | — | `messages[0].content` |
| `model` | да | — | резолв в имя провайдера через `ModelRegistry` |
| `temperature` | нет | 1.0 | да |
| `top_p` | нет | 1.0 | да |
| `top_k` | нет | — | нет |
| `presence_penalty` | нет | 0.0 | нет |
| `frequency_penalty` | нет | 0.0 | нет |
| `max_tokens` | нет | — | да |
| `stop` | нет | — | да (до 16) |
| `stream` | нет | false | — (маршрутизация) |
| `thinking` | нет | enabled | `thinking.type` |
| `reasoning_effort` | нет | high | `thinking.reasoning_effort` |

`top_k`, `presence_penalty`, `frequency_penalty` принимаются в DTO, но не
передаются в DeepSeek (не поддерживаются).

## Модели (алиасы)

| Алиас | Имя у провайдера |
|---|---|
| `DEEPSEEK_V4_PRO` | `deepseek-v4-pro` |
| `DEEPSEEK_V4_FLASH` | `deepseek-v4-flash` |
| `DEEPSEEK_V4_FLASH_VISION_EXP` | `deepseek-v4-flash-vision-exp` |

## Коды ошибок

- `400` — некорректный запрос (валидация) или неизвестная модель.
- `502` — ошибка LLM-провайдера.
- `500` — внутренняя ошибка.

## Документы

- `PLAN.md` — полный план разработки.
- `CONTEXT.md` — решения, проверенные факты, доступы и секреты.
- `AGENTS.md` — инструкции для сессий разработки.
