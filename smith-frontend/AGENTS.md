# AGENTS.md — smith-frontend

Инструкции для будущих сессий разработки этого проекта. Читать в первую очередь.

## О проекте

`smith-frontend` — минималистичный веб-клиент ИИ-агента **Smith**. Общается с
REST-бэкендом `smith-backend`: список моделей, генерация ответа (JSON) и
стриминг (SSE). Написан на чистом HTML/CSS/JS (ES-модули), **без сборщика и
фреймворков**.

## Документы

- `README.md` — запуск, настройка адреса бэкенда, CORS, деплой.
- `CONTEXT.md` — итоги сессии: требования, решения, проверенные факты,
  ограничения, особенности проверки через headless Chrome.

## Правила

- Отвечать и комментировать по-русски.
- Не добавлять лишних комментариев в код.
- Не подключать фреймворки, сборщики и npm-зависимости без явного согласования —
  проект сознательно на vanilla JS.
- Секреты и ключи не коммитить.

## Стек

HTML5, CSS3 (custom properties, `color-mix`, `prefers-color-scheme`),
JavaScript ES-модули (ES2020+), Fetch API, `ReadableStream` для разбора SSE.
Без зависимостей и без шага сборки.

## Структура

```
smith-frontend/
├── index.html          # разметка: сайдбар, чат-лента, composer
├── login.html          # страница входа (логин/пароль)
├── css/
│   ├── base.css        # reset, CSS-переменные тем, типографика
│   ├── app.css         # layout, компоненты, адаптив
│   └── login.css       # стили страницы входа
├── js/
│   ├── config.js       # базовый URL бэкенда, apiUrl/authUrl, лимит контекста
│   ├── api.js          # запросы к API + SSE-парсер + login/logout/me
│   ├── state.js        # состояние и localStorage
│   ├── render.js       # рендер сообщений, markdown-lite
│   ├── attachments.js  # вложения: скрепка, drag-n-drop, чтение файлов
│   ├── login.js        # логика страницы входа
│   └── app.js          # точка входа, обработчики, связка модулей
└── README.md
```

## Ключевые соглашения

- Адрес бэкенда вычисляется в `js/config.js` от хоста страницы
  (`http(s)://<hostname>:8080`), переопределяется через
  `window.SMITH_API_BASE_URL` или `localStorage['smith.baseUrl']`. Благодаря
  этому cookie сессии остаётся same-site и при `localhost`, и при `127.0.0.1`.
- Префикс API — `/api/v1` (константа в `config.js`).
- Стриминг: `EventSource` **не подходит** (эндпоинт POST), поэтому SSE
  читается вручную из `response.body.getReader()` и разбирается в `api.js`.
- События SSE: `chunk` (`{content, reasoningContent}`), `usage`, `done`
  (`{finishReason}`), `error` (`{message}`).
- `POST /api/v1/chat/summarize` (`{text, model}`) -> `{summary, usage}` —
  саммаризация контекста (sync JSON, `summarizeCompletion()` в `api.js`).
- `POST /api/v1/chat/facts` (`{text, facts, model}`) -> `{facts, usage}` —
  обновление key-value памяти (sync JSON, `factsCompletion()` в `api.js`).
- Панель статистики токенов (`#token-stats`) под лентой диалога: «текущий запрос»
  (promptTokens последнего ответа), «история диалога» (сумма prompt+completion
  по всем ответам за сессию), «ответ модели» (completionTokens последнего
  ответа). Расчёт в `updateTokenStats()` (app.js), обновляется по `usage` и
  восстанавливается из `localStorage` (usage сохраняется в сообщении).
- Ключи тела запроса — snake_case там, где так у бэкенда: `top_p`, `top_k`,
  `max_tokens`, `presence_penalty`, `frequency_penalty`, `reasoning_effort`,
  `system_prompt`.
- Системный промпт — поле под «Параметрами генерации» (`#system-prompt`),
  хранится в `state.settings.systemPrompt`; при непустом значении уходит как
  `system_prompt`. Поле растягивается до низа сайдбара (не менее 150px), Enter
  переносит строку.
- У бэкенда **нет многоходового диалога** (принимает один `prompt`). Контекст
  собирается на клиенте (переключатель «Передавать контекст»), лимит —
  `CONTEXT_CHAR_LIMIT`.
- **Стратегии управления контекстом** (под переключателем «Передавать контекст»,
  select `#context-strategy`, активен только при включённом контексте). Значения
  `state.settings.contextStrategy` (`CONTEXT_STRATEGIES` в `state.js`):
  - `as_is` («Как есть», дефолт) — весь диалог без сжатия;
  - `sliding_window` — последние `slidingWindowSize` сообщений;
  - `sticky_facts` — блок `facts` + последние `slidingWindowSize` сообщений;
  - `branching` — контекст активной ветки как есть;
  - `summarize` — саммаризация каждые `summaryInterval` сообщений (rolling).
  `buildPrompt()` — диспетчер по стратегии. Контекстные контролы показываются
  по стратегии (`#sliding-window-ctl`, `#summary-interval-ctl`, `#facts-panel`).
- **Саммаризация** (`summarize`): `maybeSummarize()` каждые N сообщений вызывает
  `POST /chat/summarize`, добавляет системное сообщение (`role=system`, класс
  `msg--system`), сохраняет `state.summary` (`text`, `coveredCount`, `usage`) в
  `localStorage['smith.summary']`; контекст — `саммари + сообщения после него`.
- **Факты** (`sticky_facts`): `maybeUpdateFacts()` после каждого хода вызывает
  `POST /chat/facts`, парсит «ключ: значение» в `state.facts` (`[{key,value}]`,
  `localStorage['smith.facts']`), обновляет `factsCoveredCount` и накопленный
  `factsUsage`. В промпт идут `Известные факты о задаче: …` + последние N
  сообщений. Панель `#facts-panel`/`#facts-list` показывает факты. Эксперимент
  показал склонность стратегии к «дрейфу фактов» (см.
  `experiments/context-strategies/report.md`).
- **Ветки диалога** (`branching`): `state.branches` (`[{id, name, checkpointId,
  messages}]`) + `state.activeBranchId` (`localStorage['smith.branches']`;
  миграция из старого `smith.messages`). Кнопка `.msg__fork` у сообщения (видна
  при `data-strategy="branching"`) вызывает `forkBranchAt(id)` — создаёт ветку
  с копией сообщений до checkpoint. `#branch-switcher` в шапке чата и блок
  `#branches-panel` в сайдбаре (список `#branches-list` + кнопка `#fork-branch`,
  разветвляет от последнего сообщения активной ветки) переключают ветки
  (`switchBranch` + перерисовка). `state.messages` — ссылка на активную ветку.
- Показ полей по стратегии — `updateContextControls()` (app.js): `as_is` — ничего;
  `sliding_window` — `#sliding-window-ctl`; `sticky_facts` — `#sliding-window-ctl`
  + `#facts-panel`; `branching` — `#branches-panel` + `#branch-switcher`;
  `summarize` — `#summary-interval-ctl`.
- Токены сервисных вызовов учитываются в панели `#token-stats`: пункт
  «Саммаризация» (`state.summary.usage`) и «Факты» (`state.factsUsage`) + входят
  в «Историю диалога».
- Настройки, лента и тема хранятся в `localStorage` (`state.js`).
- Тема — через атрибут `data-theme` на `<html>` и CSS-переменные.
- `hidden` + `display:flex/grid`: в `base.css` есть
  `[hidden] { display: none !important; }` — обязательно для скрытия блоков
  стратегий/панелей (`element.hidden = true`), т.к. авторский `display` иначе
  перебивает UA-правило `[hidden]`.
- **Вложения:** кнопка-скрепка (`#attach`) и drag-n-drop (`js/attachments.js`).
  Изображения уходят в `attachments` запроса (`{name, mime_type, data}` base64) и
  автоматически переключают модель на vision (`model.vision === true` из
  `GET /models`). Текстовые файлы (txt/md/csv/json/…, до 50k символов) читаются
  на клиенте и вставляются в `prompt`; прочие типы отклоняются. Вложения не
  персистятся в `localStorage` (в сообщении хранятся только имя/тип/размер).
- **Авторизация:** серверная сессия (cookie `JSESSIONID`), все fetch идут с
  `credentials: 'include'`. Вход — `login.html` (`js/login.js`), при `401`
  `api.js` делает `window.location.replace('login.html')`. При старте `app.js`
  вызывает `me()`; без сессии — редирект на логин. В сайдбаре — имя
  пользователя и кнопка «Выйти» (`logout()`). Адрес бэкенда берётся от хоста
  страницы, поэтому cookie `JSESSIONID` отправляется и с `localhost`, и с
  `127.0.0.1` (иначе `SameSite=Lax` блокирует кросс-сайтовые запросы).

## Бэкенд и CORS

- Бэкенд по умолчанию: тот же хост, порт `8080` (см. `js/config.js`).
- CORS настраивается в `smith-backend` (`app.cors.allowed-origins` в
  `application.yml`, env `CORS_ALLOWED_ORIGINS`). По умолчанию разрешены
  `localhost`/`127.0.0.1` на портах `8000`, `5500`, `5173`.
- Запуск бэкенда (профиль `local`) — см. `smith-backend/AGENTS.md`.

## Запуск

Нужен HTTP-сервер (ES-модули не работают через `file://`):

```powershell
# из папки smith-frontend
python -m http.server 8000 --bind 127.0.0.1
# открыть http://localhost:8000 или http://127.0.0.1:8000
```

> Адрес бэкенда определяется по хосту страницы, поэтому оба варианта
> (`localhost`/`127.0.0.1`) работают с сессионной cookie.

## Мониторинг запуска (обязательно)

`python -m http.server` при старте **не пишет ничего** в лог — строки появляются
только при запросах. Поэтому о готовности судить **не по логу**, а по признакам:

- слушающий порт: `Get-NetTCPConnection -LocalPort 8000 -State Listen`;
- ответ `Invoke-WebRequest http://127.0.0.1:8000/` -> `200`;
- жив ли процесс: `Get-Process -Id <PID> -ErrorAction SilentlyContinue`.

Правила:

- Запускать в фоне с перенаправлением вывода в лог-файл и **`-WindowStyle Hidden`**:
  `Start-Process ... -RedirectStandardOutput/-RedirectStandardError -WindowStyle Hidden`.
  **НЕ использовать `-NoNewWindow`:** при нём команда не возвращает управление
  до завершения процесса — общение «зависает» (проверено). С `-WindowStyle
  Hidden` команда возвращает PID сразу.
- Опрашивать каждые ~30 c (или чаще) и продолжать сразу после появления порта,
  **не выжидая фиксированную паузу**.
- Отсутствие строк в логе — не признак зависания; признак зависания — мёртвый
  процесс или порт, который не поднялся за разумное время.

## Проверка

Автотестов и линтера нет. После изменений проверять вручную в браузере
(консоль DevTools, сеть). Node в окружении может отсутствовать — синтаксис JS
проверять в браузере. Полезные сценарии:

- загрузка моделей и статус соединения в сайдбаре;
- стриминг ответа и блок «Рассуждения»;
- ошибка провайдера (`error`/HTTP 502) отображается в сообщении и тостом;
- темы, адаптив (панель на мобильном), сохранение настроек после перезагрузки.

## Текущее состояние

Реализован полный клиент: модели, JSON и SSE, параметры генерации, контекст,
темы, адаптив, `localStorage`, авторизация (страница входа, сессия, выход).
Реализованы 5 стратегий управления контекстом (select `#context-strategy`):
`as_is` (дефолт), `sliding_window`, `sticky_facts` (бэкенд `POST /chat/facts`),
`branching` (ветки диалога), `summarize`. Результаты сравнения стратегий —
`experiments/context-strategies/report.md`.
Проверено вживую: `GET /api/v1/models`, CORS preflight, реальный SSE-поток
DeepSeek; e2e входа/выхода через headless Chrome.
