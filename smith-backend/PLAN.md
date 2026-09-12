# План разработки: smith-backend (бэкенд ИИ-агента)

## 1. Назначение

REST-бэкенд ИИ-агента. Принимает запрос пользователя с параметрами генерации
нейросети, вызывает DeepSeek API, возвращает ответ. Поддерживает два режима:
синхронный JSON и стриминг (SSE). Позже добавится интеграция с Telegram
(`tg-api` / `tg-impl`) — в текущей итерации НЕ выполняется.

## 2. Стек (версии согласованы и проверены на совместимость)

| Технология | Версия | Примечание |
|---|---|---|
| Java (JDK) | 25 | toolchain + runtime |
| Gradle | 9.7.1 | через wrapper |
| Spring Boot | 4.1.1 | Spring Framework 7.0.9, Tomcat 11, Servlet 6.1 |
| Spring Boot Gradle Plugin | 4.1.1 | `org.springframework.boot` |
| MyBatis | mybatis-spring-boot-starter 4.1.0 | ветка под SB 4.1 |
| springdoc-openapi | springdoc-openapi-starter-webmvc-ui 3.1.1 | ветка под SB 4 |
| HikariCP | 7.0.2 | управляется SB (транзитивно через starter-jdbc) |
| Flyway | flyway-core 12.4.0 + flyway-database-postgresql 12.4.0 | PG-модуль обязателен с Flyway 10+ |
| PostgreSQL JDBC | org.postgresql:postgresql 42.7.13 | scope runtimeOnly |
| Apache HttpClient | httpclient5 5.6.4 (+ httpcore5 5.4.3) | управляется SB |
| Jackson | 2.21.5 | транзитивно |

> Версии MyBatis и springdoc фиксируются в version catalog; остальное
> управляется BOM Spring Boot 4.1.1 (вершины можно не указывать).

## 3. Архитектура (hexagonal + DDD)

```
client -> api-impl (Spring Boot) -> application (use cases) -> domain (сущности/порты)
                                          |
                                          +-- infrastructure (MyBatis + DeepSeek HttpClient)
```

### Модули и зависимости

| Модуль | Зависит от | Содержимое |
|---|---|---|
| `domain` | — | enum моделей, value objects, порты (`LlmProvider`, `ChatRequestRepository`, `ModelRegistry`) |
| `api` | — | DTO + интерфейс `ChatApi` (контракт, аннотации OpenAPI) |
| `infrastructure` | `domain` | MyBatis-мапперы, PO, `DeepSeekLlmProvider` |
| `application` | `domain`, `api`, `infrastructure` | `ChatCompletionService`, `ModelInfoService`, маппинг DTO<->domain |
| `api-impl` | `api`, `application` | `ChatController`, `AiAgentApplication`, конфигурация |

> `application -> api` — по ТЗ (неканонично для гексагона, принято).

## 4. Gradle-каркас (Kotlin DSL)

```
ai-agent-backend/
├── settings.gradle.kts          # include: domain, api, infrastructure, application, api-impl
├── build.gradle.kts             # common config для subprojects
├── gradle/libs.versions.toml
└── <modules>/
```

`gradle/libs.versions.toml` (ключевые записи):

```toml
[versions]
spring-boot = "4.1.1"
mybatis = "4.1.0"
springdoc = "3.1.1"

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }

[libraries]
spring-boot-web        = { module = "org.springframework.boot:spring-boot-starter-web" }
spring-boot-validation = { module = "org.springframework.boot:spring-boot-starter-validation" }
spring-boot-jdbc       = { module = "org.springframework.boot:spring-boot-starter-jdbc" }
spring-boot-test       = { module = "org.springframework.boot:spring-boot-starter-test" }
mybatis-starter        = { module = "org.mybatis.spring.boot:mybatis-spring-boot-starter", version.ref = "mybatis" }
springdoc-ui           = { module = "org.springdoc:springdoc-openapi-starter-webmvc-ui", version.ref = "springdoc" }
flyway-core            = { module = "org.flywaydb:flyway-core" }
flyway-postgresql      = { module = "org.flywaydb:flyway-database-postgresql" }
postgresql             = { module = "org.postgresql:postgresql" }
httpclient5            = { module = "org.apache.httpcomponents.client5:httpclient5" }
```

- `java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }`
- Плагины: `java-library` для всех; `spring-boot` + `application` только в `api-impl`.
- Зависимости распределяются строго по таблице модулей (п.3).

## 5. Domain-слой

### Модели (enum + реестр)

```java
enum LlmModel {
    DEEPSEEK_V4_PRO("deepseek-v4-pro"),
    DEEPSEEK_V4_FLASH("deepseek-v4-flash"),
    DEEPSEEK_V4_FLASH_VISION_EXP("deepseek-v4-flash-vision-exp");
}
```

Клиент передаёт алиас (напр. `DEEPSEEK_V4_PRO`); `ModelRegistry` резолвит в имя
провайдера. Неизвестный алиас -> 400 с перечнем доступных.

### Value objects

`Prompt`, `ModelName`, `GenerationParams` (temperature, topP, topK,
presencePenalty, frequencyPenalty, maxTokens, stopSequences, thinking,
reasoningEffort), `Usage`, `ChatCompletion`.

### Порты (интерфейсы)

- `LlmProvider` -> `complete(...)` / `stream(...)`
- `ChatRequestRepository` -> `save`, `findById`
- `ModelRegistry` -> резолв алиасов

## 6. API-слой (DTO + контракт)

`ChatCompletionRequest` (Jakarta Validation + `@Schema`):

| Поле | Тип | Обязат. | Default | Действие в DeepSeek |
|---|---|---|---|---|
| `prompt` | String | да | — | -> `messages[0].content` |
| `model` | String (алиас) | да | — | резолв через `ModelRegistry` |
| `temperature` | Double | — | 1.0 | передаётся |
| `top_p` | Double | — | 1.0 | передаётся |
| `top_k` | Integer | — | — | принимается, НЕ передаётся |
| `presence_penalty` | Double | — | 0.0 | принимается, НЕ передаётся (deprecated) |
| `frequency_penalty` | Double | — | 0.0 | принимается, НЕ передаётся (deprecated) |
| `max_tokens` | Integer | — | — | передаётся |
| `stop` | List<String> | — | — | передаётся (до 16) |
| `stream` | Boolean | — | false | SSE vs JSON |
| `thinking` | enum enabled/disabled | — | enabled | -> `thinking.type` |
| `reasoning_effort` | enum low/high/max | — | high | -> `thinking.reasoning_effort` |

`ChatCompletionResponse`: `id`, `model`, `content`, `reasoningContent`,
`finishReason`, `usage{promptTokens, completionTokens, totalTokens}`, `createdAt`.

`ChatApi` — интерфейс с `@Operation`/`@ApiResponse` (описывает методы для Swagger):
- `POST /api/v1/chat/completions`
- `GET /api/v1/models`

## 7. Application-слой

- `ChatCompletionService` (фасад): резолв модели -> построение `ChatRequest` с
  дефолтами -> вызов порта `LlmProvider` (sync/stream) -> сохранение истории ->
  маппинг в DTO.
- `ModelInfoService` — список доступных моделей.

## 8. Infrastructure-слой

### 8.1 DeepSeek-адаптер (`DeepSeekLlmProvider`)

- `httpclient5` (`CloseableHttpClient`), `baseUrl=https://api.deepseek.com`,
  `POST /chat/completions`.
- Заголовки: `Authorization: Bearer <key>`, `Content-Type: application/json`.
- Маппинг тела: `model`, `messages=[{role:user,content:prompt}]`, `temperature`,
  `top_p`, `max_tokens`, `stop`, `stream`,
  `stream_options.include_usage=true`, `thinking{type,reasoning_effort}`.
- **НЕ отправляются:** `top_k`, `presence_penalty`, `frequency_penalty`.
- Разбор ответа: `choices[0].message.content`, `reasoning_content`,
  `finish_reason`, `usage`.
- Стриминг: чтение `InputStream`, эмиттер SSE (`data: ...` до `[DONE]`).
- Ошибки non-2xx -> доменное исключение `LlmProviderException`.

### 8.2 PostgreSQL + MyBatis

- Flyway `V1__init.sql`:

```sql
CREATE TABLE chat_request (
  id UUID PRIMARY KEY,
  prompt TEXT NOT NULL,
  model VARCHAR(64) NOT NULL,
  content TEXT,
  reasoning_content TEXT,
  finish_reason VARCHAR(32),
  prompt_tokens INT, completion_tokens INT, total_tokens INT,
  streamed BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(32) NOT NULL,
  error_message TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

- `ChatRequestMapper` (MyBatis) -> реализация `ChatRequestRepository`.
- HikariCP — настройки пула в `application.yml`.

## 9. api-impl (приложение)

- `AiAgentApplication` (`@SpringBootApplication`, `@MapperScan`).
- `ChatController implements ChatApi`:
  - `POST /api/v1/chat/completions` — `stream=false` -> JSON; `stream=true` ->
    `SseEmitter` (`text/event-stream`).
  - `GET /api/v1/models`.
- `@RestControllerAdvice` — ошибки валидации/провайдера.
- `@ConfigurationProperties(prefix = "deepseek")` для ключа.
- springdoc: UI `/swagger-ui.html`, спецификация `/v3/api-docs`.

`application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ai_agent
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
      connection-timeout: 30000
  flyway:
    enabled: true
    locations: classpath:db/migration

deepseek:
  base-url: https://api.deepseek.com
  api-key: ${DEEPSEEK_API_KEY}
  connect-timeout: 10s
  read-timeout: 120s
```

> Реальные значения секретов — в env / локальном `application-local.yml`
> (в `.gitignore`). НЕ коммитить в VCS.

## 10. База данных

- Сервер: PostgreSQL 17.2, `localhost:5432`, аутентификация `scram-sha-256`.
- Пользователь `postgres` / пароль см. в `CONTEXT.md` (раздел «Доступы»).
- Целевая БД: `ai_agent` (создать; на момент планирования отсутствовала).

Команда создания (PowerShell, `psql` не в PATH — по полному пути):

```powershell
$env:PGPASSWORD="<пароль из CONTEXT.md>"
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -h localhost -U postgres -w -c "CREATE DATABASE ai_agent WITH ENCODING 'UTF8' TEMPLATE template0;"
```

## 11. Порядок выполнения

0. Создать БД `ai_agent` (п.10).
1. Каркас: `settings.gradle.kts`, `libs.versions.toml`, 5 модулей -> `gradlew build` зелёный.
2. `domain`: enum моделей + `ModelRegistry`, value objects, порты.
3. `api`: DTO + `ChatApi` (OpenAPI-аннотации).
4. `infrastructure` (БД): Flyway `V1__init.sql`, MyBatis маппер, `ChatRequestRepository`, HikariCP.
5. `infrastructure` (LLM): `DeepSeekLlmProvider` на httpclient5, DTO тела запроса/ответа, маппинг, обработка ошибок, SSE.
6. `application`: `ChatCompletionService`, `ModelInfoService`, маппинг и дефолты.
7. `api-impl`: контроллер (sync + SSE), `@RestControllerAdvice`, main, `@ConfigurationProperties`, springdoc.
8. Проверка: реальный запрос через Swagger; unit-тесты маппинга/резолва; интеграционный тест контроллера с моком провайдера.
9. README (запуск, env, примеры запросов).

## 12. Замечания / риски

- `application -> api` зависимость — по ТЗ (принято).
- springdoc 3.1.1 официально поддерживает Spring Boot 4 / Jakarta — риск снят.
- Apache HttpClient5 управляется SB 4.1.1 (5.6.4).
- SSE в Swagger UI показывается как `text/event-stream` (без «Try it out» стриминга) — помечаем `produces`.
