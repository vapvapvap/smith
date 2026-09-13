# AGENTS.md — smith-backend

Инструкции для будущих сессий разработки этого проекта. Читать в первую очередь.

## О проекте

`smith-backend` — REST-бэкенд ИИ-агента. Принимает запрос с параметрами
генерации (промпт, модель, temperature, top_p, top_k, penalties, max_tokens,
stop) и вызывает DeepSeek API, возвращая ответ (JSON) либо стрим (SSE).
Позже добавится Telegram-интеграция (`tg-api` / `tg-impl`).

## Документы

- `PLAN.md` — полный план разработки (архитектура, модули, DTO, шаги).
- `CONTEXT.md` — итоги сессии: решения, версии, проверенные факты, доступы.

## Правила

- Отвечать и комментировать по-русски.
- Соблюдать DDD + hexagonal architecture (ядро не зависит от инфраструктуры).
- Не добавлять лишних комментариев в код.
- Секреты (пароль БД, DeepSeek API-ключ) НЕ коммитить — использовать env-переменные.

## Стек (согласован)

Java 25, Gradle 9.7.1, Spring Boot 4.1.1, Spring Security 7.1.1, MyBatis 4.1.0,
HikariCP 7.0.2, Flyway 12.4.0, PostgreSQL 17, Apache HttpClient5 5.6.4,
springdoc-openapi 3.1.1.

## Модули и зависимости

- `domain` — сущности, value objects, порты. Ни от чего не зависит.
- `api` — DTO + интерфейс `ChatApi`. Ни от чего не зависит.
- `infrastructure` -> `domain` (MyBatis, DeepSeek HttpClient).
- `application` -> `domain`, `api`, `infrastructure`.
- `api-impl` -> `api`, `application` (контроллеры, main, конфиг).

## Ключевые соглашения

- Модели — enum `LlmModel` с алиасами (`DEEPSEEK_V4_PRO` и т.д.) и `ModelRegistry`
  для маппинга на имена провайдера.
- Порт `LlmProvider` (`complete`/`stream`) абстрагирует вызов LLM.
- Необязательный `system_prompt` подставляется провайдером как сообщение
  `role=system` перед пользовательским `role=user`; хранится в `chat_request.system_prompt`.
- `top_k`, `presence_penalty`, `frequency_penalty` принимаются в DTO, но НЕ
  передаются в DeepSeek (не поддерживаются).
- Вложения (`attachments`, только изображения) передаются base64 в JSON запроса
  и уходят в vision-модель (`DEEPSEEK_FLASH`) как `image_url`
  (data URI). `GET /api/v1/models` возвращает флаг `vision`. Не-image вложение
  или не-vision модель -> `400`.
- Режимы ответа: `stream=false` -> JSON, `stream=true` -> SSE (`SseEmitter`).
- Саммаризация контекста: `POST /api/v1/chat/summarize` (`{text, model}`) ->
  `{summary, usage}`. Вызывает LLM с фиксированным системным промптом
  саммаризации и `thinking=disabled`; НЕ пишет в `chat_request` (внутренняя
  операция), возвращает `usage` для учёта расхода на клиенте.
- Авторизация — session cookie (`JSESSIONID`) + Spring Security form login на
  `POST /api/auth/login` (`username`/`password`, `application/x-www-form-urlencoded`).
  Пользователи — таблица `app_user` (BCrypt-хэши), порт `UserRepository`,
  `AppUserDetailsService`. Без сессии `/api/**` -> `401`. CSRF отключён.
  Открыты: `/api/auth/login`, `/api/auth/logout`, Swagger, `/error`.
  `GET /api/auth/me` возвращает `{username}` (требует сессии).

## Доступы (локальная разработка)

- PostgreSQL: `localhost:5432`, БД `ai_agent`, user `postgres`.
  Пароль и DeepSeek API-ключ — в `CONTEXT.md` (раздел «Доступы и секреты»).
- Логины/пароли пользователей приложения: `CREDENTIALS.local.md` в корне
  репозитория (в `.gitignore`, не коммитится).
- `psql`: `C:\Program Files\PostgreSQL\17\bin\psql.exe` (не в PATH).
- JDK 25: `C:\Program Files\Java\jdk-25.0.4` (JAVA_HOME может быть не задан —
  выставлять в командах: `$env:JAVA_HOME="C:\Program Files\Java\jdk-25.0.4"`).
- Gradle 9.7.1: скачан в `C:\gradle-9.7.1` (НЕ через wrapper, bin не в PATH):
  запуск `& "C:\gradle-9.7.1\bin\gradle.bat" ...`. После генерации wrapper
  использовать `.\gradlew.bat`.

## С чего начать (текущее состояние)

Проект реализован (шаги 0–9 PLAN.md). Команды:
- Полный build с тестами: `$env:JAVA_HOME="C:\Program Files\Java\jdk-25.0.4"; .\gradlew.bat build`
- Локальный запуск (профиль `local`, секреты в `api-impl/application-local.yml`):
  `$env:JAVA_HOME="C:\Program Files\Java\jdk-25.0.4"; .\gradlew.bat :api-impl:bootRun "--args=--spring.profiles.active=local"`
- Проверка: Swagger `http://localhost:8080/swagger-ui.html`;
  модели `GET /api/v1/models`; sync `POST /api/v1/chat/completions`;
  SSE `POST /api/v1/chat/completions/stream`;
  саммаризация `POST /api/v1/chat/summarize`.
- Авторизация: `POST /api/auth/login` (form-urlencoded), `POST /api/auth/logout`,
  `GET /api/auth/me`. Пользователи: `vap`, `lex`, `max`, `heh`, `art`
  (пароли — BCrypt в `V4__seed_users.sql`, см. итоги сессии).
