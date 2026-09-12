# smith-frontend

Минималистичный клиент ИИ-агента **Smith** на чистом HTML/CSS/JS (ES-модули,
без сборщика). Работает с REST-бэкендом `smith-backend`.

## Возможности

- Выбор модели из `GET /api/v1/models`.
- Стриминг ответа (SSE) и синхронный режим (JSON) — переключатель «Стриминг ответа».
- Отдельный сворачиваемый блок «Рассуждения» (`reasoningContent`, thinking mode).
- Показ использования токенов и причины завершения.
- Панель параметров генерации: `temperature`, `top_p`, `top_k`, `max_tokens`,
  `presence_penalty`, `frequency_penalty`, `stop`, `thinking`, `reasoning_effort`.
- Переключатель «Передавать контекст» — собирает историю сессии в один `prompt`
  (у бэкенда нет многоходового диалога), с ограничением ~12000 символов.
- Светлая/тёмная тема, адаптивная вёрстка, сохранение настроек и ленты в `localStorage`.

## Структура

```
smith-frontend/
├── index.html
├── css/
│   ├── base.css      # reset, CSS-переменные тем, типографика
│   └── app.css       # layout, компоненты, адаптив
└── js/
    ├── config.js     # базовый URL бэкенда, лимит контекста
    ├── api.js        # модели, JSON-запрос, SSE-парсер (fetch + ReadableStream)
    ├── state.js      # состояние и localStorage
    ├── render.js     # рендер сообщений и markdown-lite
    └── app.js        # точка входа
```

## Запуск

Модули ES требуют HTTP-сервера (не открывать через `file://`).

```powershell
# из папки smith-frontend
python -m http.server 8000
# или расширение Live Server в VS Code (порт 5500)
```

Открыть http://localhost:8000.

Бэкенд должен быть запущен на `http://localhost:8080` (см. `smith-backend`).

## Настройка адреса бэкенда

По умолчанию используется `http://localhost:8080`. Изменить можно:

1. в `js/config.js` — константа `DEFAULT_BASE_URL`;
2. разово через `localStorage`: `localStorage.setItem('smith.baseUrl', 'https://api.example.com')`;
3. глобально до загрузки модуля: `window.SMITH_API_BASE_URL = 'https://api.example.com'`.

## CORS

Бэкенд разрешает источники из `app.cors.allowed-origins` (`application.yml`).
По умолчанию: `localhost/127.0.0.1` на портах `8000`, `5500`, `5173`.
Для своего домена задайте переменную окружения:

```powershell
$env:CORS_ALLOWED_ORIGINS = "https://your-frontend.example.com"
```

## Деплой

Статика без сборки — подходит любой хостинг статики:

- GitHub Pages, Netlify, Vercel, Cloudflare Pages — загрузить содержимое папки.
- Перед деплоем указать публичный адрес бэкенда (см. «Настройка адреса бэкенда»).
- На бэкенде добавить домен фронтенда в `CORS_ALLOWED_ORIGINS`.
