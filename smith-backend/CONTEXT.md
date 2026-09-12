# Итоги сессии планирования (smith-backend)

Этот файл фиксирует результаты планировочной сессии: исходные требования,
вопросы, принятые решения и проверенные факты. Для разработки читать вместе
с `PLAN.md` (полный план) и `AGENTS.md` (инструкции для будущих сессий).

## 1. Исходные требования (от заказчика)

- Бэкенд ИИ-агента на **Java 25 + Spring Boot 3** (в итоге Spring Boot 4, см. п.3).
- Стек: Gradle, HikariCP, MyBatis, Apache HttpClient, OpenAPI Swagger.
- Подход: DDD, hexagonal architecture, чистый код.
- Модули: `domain`, `infrastructure`, `application`, `api-impl`, `api`.
  Позже добавится `tg-api` / `tg-impl` (Telegram) — в текущей итерации НЕ делаем.
- Зависимости модулей:
  - `domain` — ни от чего;
  - `api` — ни от чего;
  - `infrastructure` -> `domain`;
  - `application` -> `domain`, `api`, `infrastructure`;
  - `api-impl` -> `api`, `application`.
- REST-контроллер принимает параметры нейросети: текстовый запрос, model,
  temperature, top_p, top_k, Presence Penalty, Frequency Penalty, Max Tokens,
  Stop Sequences. Обязательные: запрос и model; остальные — с дефолтами.
- Приложение вызывает DeepSeek API, передаёт параметры, возвращает ответ.
- DeepSeek API-ключ — в properties-файле.
- Нужна система имён моделей (универсальные алиасы), чтобы добавлять провайдеров.

## 2. Вопросы заказчику и ответы

| Вопрос | Ответ |
|---|---|
| СУБД и что хранить | **PostgreSQL** |
| Обработка top_k / penalties (не поддержаны DeepSeek) | **Принимать в DTO, НЕ слать в DeepSeek** |
| Стриминг ответа | **Оба режима** (JSON + SSE) |
| Как клиент указывает модель | **Универсальные алиасы** (enum с маппингом) |
| Пароль PostgreSQL | задан в `application-local.yml` (в `.gitignore`, в VCS не попадает) |
| БД для приложения | **`ai_agent`**, пользователь `postgres` |

## 3. Ключевое решение: Spring Boot 4 вместо 3

Конфликт версий, выявленный при проверке совместимости:

- Spring Boot 3.5.x официально поддерживает **Gradle 7.6.4+ / 8.4+** (не 9.x).
- Gradle 8.x поддерживает Java **только до 24**; Java 25 требует **Gradle 9.1.0+**.
- Gradle 9.x поддерживается только Spring Boot 4.x.

Итог: для полноценного **Java 25** выбран **Spring Boot 4.1.1 + Gradle 9.7.1**
(вариант B из предложенных). Требование «Spring Boot 3» отменено заказчиком.

## 4. Проверенные факты

### 4.1 Совместимость (официальные источники)

- Gradle compatibility matrix: Java 25 — toolchain и runtime с **9.1.0+**.
- Spring Boot 4.1.1: Java 17–26; Gradle 8.x (8.14+) и 9.x; Spring Framework 7.0.9.
- MyBatis spring-boot-starter: **4.1.0** = Spring Boot 4.1 (latest).
- springdoc-openapi: **3.1.1** поддерживает Spring Boot 4 (Jakarta).

### 4.2 Управляемые Spring Boot 4.1.1 версии (BOM)

HikariCP 7.0.2, Jackson 2.21.5, Flyway 12.4.0, PostgreSQL JDBC 42.7.13,
Apache HttpClient5 5.6.4 (httpcore5 5.4.3).

### 4.3 DeepSeek API

- `base_url = https://api.deepseek.com`, `POST /chat/completions` (OpenAI-совместимо).
- Модели (2026): `deepseek-v4-flash`, `deepseek-v4-pro`, `deepseek-v4-flash-vision-exp`.
- `top_k` **не поддерживается**; `presence_penalty` и `frequency_penalty` — **deprecated**.
- Есть специфичные: `thinking{type: enabled|disabled}` (default enabled) и
  `reasoning_effort{low|high|max}` (default high).
- Ответ: `choices[0].message.content`, `reasoning_content` (thinking mode),
  `finish_reason`, `usage`.

### 4.4 Локальное окружение

- PostgreSQL **17.2** установлен в `C:\Program Files\PostgreSQL\17`, порт **5432**.
- `psql` НЕ в PATH (вызывать по полному пути `C:\Program Files\PostgreSQL\17\bin\psql.exe`).
- `pg_hba.conf`: все подключения `scram-sha-256`.
- БД `ai_agent` на момент планирования **не существовала**.

## 5. Доступы и секреты (НЕ коммитить в VCS)

| Назначение | Значение |
|---|---|
| PostgreSQL хост/порт | `localhost:5432` |
| PostgreSQL пользователь | `postgres` |
| PostgreSQL пароль | <локальное значение, в VCS не коммитить> |
| PostgreSQL целевая БД | `ai_agent` |
| DeepSeek base-url | `https://api.deepseek.com` |
| DeepSeek API key | <локальное значение, в VCS не коммитить> |

> Эти значения — только для локальной разработки. В `application.yml`
> использовать плейсхолдеры `${DB_PASSWORD}` / `${DEEPSEEK_API_KEY}`,
> реальные значения держать в env или `application-local.yml` в `.gitignore`.

## 6. Соглашения

- Отвечать и комментировать по-русски.
- DDD + hexagonal: ядро (`domain`, `application`) не зависит от инфраструктуры.
- Порт `LlmProvider` абстрагирует провайдера LLM (для будущих моделей).
- Без лишних комментариев в коде (только где действительно нужно).
- Названия пакетов/классов универсальные (не привязаны к конкретному провайдеру).

## 7. Что уже сделано

### Шаг 0 — БД
- БД `ai_agent` создана (PostgreSQL 17, localhost:5432).

### Шаги 1–9 — реализация (2026-09-09)
- Gradle-каркас: `settings.gradle.kts`, `gradle/libs.versions.toml`,
  `build.gradle.kts`, 5 модулей (`domain`, `api`, `infrastructure`,
  `application`, `api-impl`), wrapper 9.7.1. `.\gradlew.bat build` зелёный.
- `domain`: `LlmModel`, `ThinkingMode`, `ReasoningEffort`, `FinishReason`,
  `ChatStatus`, value objects (`Prompt`, `ModelName`, `GenerationParams`,
  `Usage`, `ChatCompletion`, `ChatRequest`, `ChatRecord`), порты
  (`LlmProvider`, `ChatRequestRepository`, `ModelRegistry`, `ChatStreamListener`),
  исключения, `DefaultModelRegistry`.
- `api`: DTO (`ChatCompletionRequest`, `ChatCompletionResponse`, `UsageDto`,
  `ChatModelDto`, enum `Thinking`/`ReasoningEffort`), контракт `ChatApi`
  с OpenAPI-аннотациями. Базовый пакет — `com.smith` (решение заказчика).
- `infrastructure`: Flyway `V1__init.sql` (таблица `chat_request`),
  MyBatis `ChatRequestMapper` (аннотационные @Insert/@Select),
  `ChatRecordPo`, `PersistenceChatRequestRepository`,
  `DeepSeekLlmProvider` + `DeepSeekConfig` + DTO тела запроса/ответа
  (Jackson SNAKE_CASE, `stream_options.include_usage`, thinking).
- `application`: `ChatCompletionService` (sync + stream, дефолты параметров,
  сохранение истории в `chat_request`), `ModelInfoService`.
- `api-impl`: `AiAgentApplication` (@MapperScan), `ChatController`
  (POST /chat/completions — JSON, POST /chat/completions/stream — SSE,
  GET /models), `SseStreamListener`, `AppConfig` (бины), `DeepSeekProperties`
  (@ConfigurationProperties), `GlobalExceptionHandler` + `ApiError`,
  `application.yml`, локальный `application-local.yml` (в .gitignore).
- Springdoc UI: `/swagger-ui.html`, спецификация `/v3/api-docs`.
- Тесты: `DefaultModelRegistryTest` (domain), `ChatCompletionServiceTest`
  (application, Mockito), `ChatControllerTest` (standalone MockMvc: sync JSON,
  ошибки 400, SSE-поток), `SseStreamListenerTest` (протокол SSE-событий).

### Системный промпт (2026-09-12)
- `ChatCompletionRequest.system_prompt` (необязательный) -> `ChatRequest.systemPrompt`.
- `DeepSeekLlmProvider` при непустом значении добавляет `{role:system}` перед
  `{role:user}` в `messages`.
- Flyway `V2__add_system_prompt.sql`: `chat_request.system_prompt TEXT`;
  обновлены `ChatRecordPo`, `ChatRequestMapper`, `PersistenceChatRequestRepository`.
- `ChatControllerTest.streamStartsSseAndEmitsEvents` был флейки (гонка с
  виртуальным потоком): добавлено `mvcResult.getAsyncResult(10_000L)` перед
  `asyncDispatch`.

### Проверка e2e (локально, профиль `local`)
- `GET /api/v1/models` — 3 модели (алиас + имя провайдера).
- `POST /chat/completions` — реальный ответ DeepSeek (DEEPSEEK_V4_FLASH),
  кириллица корректна, запись попадает в `chat_request`.
- `POST /chat/completions/stream` — SSE: события `chunk`/`usage`/`done`.
- Неизвестная модель и пустой prompt -> `400` с JSON-телом ошибки.

### Отступления от PLAN.md (зафиксировать)
- `api-impl` дополнительно зависит от `:infrastructure` (нужно для сборки
  бинов провайдера/репозитория в конфигурации).
- SSE выделен в отдельный endpoint `POST /api/v1/chat/completions/stream`
  (единый метод с двумя return types невозможен). Флаг `stream` в DTO
  сохранён, провайдеру/записи истории выставляется контроллером.
- В Boot 4 Flyway-autoconfig в отдельном модуле `spring-boot-flyway`;
  стартеры web разбиты (`spring-boot-starter-webmvc` вместо `starter-web`).
- `top_k`/penalties принимаются в DTO и не передаются провайдеру (по плану).

Следующие возможные шаги (не в текущей итерации): интеграционный тест
контроллера с моком провайдера, Telegram-интеграция (`tg-api`/`tg-impl`).
