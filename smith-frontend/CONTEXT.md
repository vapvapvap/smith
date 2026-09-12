# Итоги сессии (smith-frontend)

Этот файл фиксирует результаты сессии разработки фронтенда: исходные
требования, вопросы, принятые решения и проверенные факты. Для разработки
читать вместе с `README.md` (запуск/деплой) и `AGENTS.md` (инструкции).

## 1. Исходные требования (от заказчика)

- Клиентский UI для ИИ-агента **Smith**, работающий с `smith-backend`.
- Чистый **HTML/CSS/JS** (без сборщика и фреймворков), важно для простого деплоя.
- Стиль — **минимализм / чистый**.
- Первый UI — **полный клиент**: выбор модели, параметры генерации, стриминг.

## 2. Вопросы заказчику и ответы

| Вопрос | Ответ |
|---|---|
| Что за страница | UI для Smith (клиент к существующему бэкенду) |
| Стек | Чистый HTML/CSS/JS; нужна возможность деплоя |
| Стиль | Минимализм / чистый |
| Объём | Полный клиент (модели, параметры, стриминг) |
| CORS / запуск | Настроить CORS в бэкенде |
| Память диалога | Не выбор A/B, а **переключатель «Передавать контекст»** |

## 3. Ключевые решения

- **ES-модули без сборщика.** Требуется HTTP-сервер (через `file://` модули не
  работают).
- **SSE через `fetch` + `ReadableStream`.** `EventSource` не подходит — эндпоинт
  стрима принимает `POST`.
- **Контекст диалога собирается на клиенте.** У бэкенда нет многоходового
  диалога (один `prompt`). Переключатель «Передавать контекст» склеивает историю
  сессии; `reasoningContent` в контекст не включается; лимит
  `CONTEXT_CHAR_LIMIT = 12000` (старые пары отбрасываются).
- **Хранение в `localStorage`:** настройки (`smith.settings`), лента
  (`smith.messages`), тема (`smith.theme`), опционально адрес бэкенда
  (`smith.baseUrl`).
- **Тема** — атрибут `data-theme` на `<html>` + CSS-переменные в `base.css`;
  по умолчанию учитывается `prefers-color-scheme`.
- **Модель по умолчанию — `DEEPSEEK_V4_FLASH`**; список моделей сортируется по
  `alias`, поэтому FLASH идёт первым.
- **CORS в бэкенде** (`CorsConfig` + `app.cors.allowed-origins`), не прокси.

## 4. Проверенные факты

### 4.1 Контракт API бэкенда

- `GET /api/v1/models` -> `[{ alias, providerName }]`.
- `POST /api/v1/chat/completions` -> JSON `ChatCompletionResponse`
  (`id`, `model`, `content`, `reasoningContent`, `finishReason`,
  `usage{promptTokens, completionTokens, totalTokens}`, `createdAt`).
- `POST /api/v1/chat/completions/stream` -> SSE.
- Тело запроса: `prompt`, `model` (алиас), `stream`, `thinking`
  (`enabled|disabled`), `reasoning_effort` (`low|high|max`), а также snake_case
  `top_p`, `top_k`, `max_tokens`, `presence_penalty`, `frequency_penalty`, `stop`.
- Ошибки: `{ status, error, message }` (400 — валидация/модель, 502 — провайдер,
  500 — внутренняя).

### 4.2 Формат SSE (подтверждён живым запросом)

```
event:chunk
data:{"content":"","reasoningContent":"..."}

event:usage
data:{"promptTokens":38,"completionTokens":20,"totalTokens":58}

event:done
data:{"finishReason":"LENGTH"}
```

События: `chunk`, `usage`, `done`, `error`. Spring не ставит пробел после
`data:`; фреймы разделены `\n\n`. При малом `max_tokens` весь бюджет может уйти
в `reasoningContent`, а `content` останется пустым (`finishReason: LENGTH`).

### 4.3 CORS

- `CorsConfig` в `smith-backend` покрывает `/api/**`, методы `GET/POST/OPTIONS`.
- Preflight с `Origin: http://localhost:8000` возвращает
  `Access-Control-Allow-Origin`.
- Список по умолчанию: `localhost`/`127.0.0.1` на портах `8000`, `5500`, `5173`;
  переопределяется env `CORS_ALLOWED_ORIGINS`.

### 4.4 Локальное окружение

- Node/deno/bun **отсутствуют** — синтаксис JS проверяется в браузере.
- Python **3.14.7** установлен; запуск статики: `python -m http.server 8000`.
- JDK 25: `C:\Program Files\Java\jdk-25.0.4` (для запуска бэкенда).
- Бэкенд запускается профилем `local` (см. `smith-backend/AGENTS.md`).

### 4.5 Проверка через headless Chrome

Chrome/Edge доступны. Рабочий вызов (иначе возможен конфликт профиля и пустой
вывод):

```
chrome.exe --headless=new --disable-gpu --no-sandbox --no-first-run \
  --user-data-dir=<temp> --window-size=W,H \
  --virtual-time-budget=5000 --dump-dom|--screenshot=... <url>
```

Особенности:

- при `--dump-dom` `window.innerHeight` меньше `--window-size` примерно на 151px
  (учитывается хром браузера); при `--screenshot` вьюпорт равен `--window-size`;
- для проверки раскрытого блока параметров временно добавлялся атрибут `open` к
  `<details class="params">`, затем убирался;
- отладочные значения удобно писать в `document.body.dataset.debug` и читать
  через `--dump-dom`.

## 5. Соглашения

- Отвечать и комментировать по-русски.
- Не добавлять лишних комментариев в код.
- Не подключать фреймворки/сборщики/npm без согласования.
- Ключи тела запроса — snake_case там, где так у бэкенда.
- Секреты не коммитить.

## 6. Что уже сделано

- Структура `smith-frontend`: `index.html`, `css/base.css`, `css/app.css`,
  `js/{config,api,state,render,app}.js`, `README.md`, `AGENTS.md`.
- Дизайн: минимализм, светлая/тёмная темы, адаптив (выезжающий сайдбар на
  мобильных), SVG-иконки.
- Функции: загрузка моделей и статус соединения, стриминг (SSE) и синхронный
  режим, блок «Рассуждения», токены и `finishReason`, панель параметров,
  переключатели «Стриминг ответа» и «Передавать контекст», markdown-lite
  (код-фенсы и inline-код), `localStorage`, тосты об ошибках.
- CORS в `smith-backend`: `api-impl/.../config/CorsConfig.java`, проп
  `app.cors.allowed-origins` в `application.yml`.
- Прокрутка блока «Параметры генерации» на низких экранах: тонкий скроллбар,
  появляется только при нехватке высоты.
- Модель по умолчанию `DEEPSEEK_V4_FLASH`, список сортируется по `alias`.
- Системный промпт: поле `#system-prompt` (textarea, Enter — перенос строки)
  отдельным блоком под «Параметрами генерации»; растягивается до низа сайдбара,
  но не менее 200px (`.field--grow`); значение в `state.settings.systemPrompt`,
  в запрос уходит как `system_prompt` только если непустое.
- Поля `presence_penalty`/`frequency_penalty` убраны из UI (deprecated у DeepSeek).
- У каждого параметра генерации и системного промпта — иконка `.hint` с
  краткой подсказкой в `title`.

## 7. Отступления / известные ограничения

- **Прокрутка параметров — через `max-height` с числом `470px`**:
  `.params__grid { max-height: max(160px, calc(100dvh - 470px)); overflow-y:auto }`.
  Значение подобрано эмпирически (высота элементов над сеткой + summary + футер).
  Если менять высоты сайдбара, число нужно перепроверить.
- **Вложенный flex-shrink внутри `<details>` в Chrome не сработал** — сетка не
  сжималась, её клипал `overflow:hidden`. Поэтому используется `max-height`.
- **`.params { flex-shrink: 0 }`** обязателен: `overflow:hidden` обнуляет
  автоматический `min-height` flex-элемента, иначе блок схлопывается до summary.
- Нет автотестов и линтера; проверка ручная (браузер/DevTools).
- После правок CSS нужен сброс кэша (Ctrl+F5).
- В корне `smith-frontend` лежит пользовательский `screen.png` (скриншот бага) —
  не относится к сборке.

## 8. Следующие возможные шаги (не в текущей итерации)

- Показ ошибок с учётом кодов (400/502/500) отдельными состояниями.
- Кнопка «Стоп» для прерывания стрима (`AbortController` уже пробрасывается в
  `streamCompletion`).
- Подсветка синтаксиса в блоках кода.
- Автотесты/линтер (нужна установка Node) или минимальный CI-скрипт.
- Хранение адреса бэкенда в UI (сейчас только через `config.js`/`localStorage`).
