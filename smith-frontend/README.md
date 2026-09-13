# smith-frontend

Минималистичный клиент ИИ-агента **Smith** на чистом HTML/CSS/JS (ES-модули,
без сборщика). Работает с REST-бэкендом `smith-backend`.

## Возможности

- Авторизация по логину/паролю (страница `login.html`, серверная сессия).
- Выбор модели из `GET /api/v1/models`.
- Стриминг ответа (SSE) в реальном времени.
- Отдельный сворачиваемый блок «Рассуждения» (`reasoningContent`, thinking mode).
- Показ использования токенов и причины завершения.
- Панель параметров генерации: `temperature`, `top_p`, `top_k`, `max_tokens`,
  `presence_penalty`, `frequency_penalty`, `stop`, `thinking`, `reasoning_effort`.
- Переключатель «Передавать контекст» — собирает историю сессии в один `prompt`
  (у бэкенда нет многоходового диалога), с ограничением ~12000 символов.
- Стратегии управления контекстом (при включённом «Передавать контекст»):
  «Как есть» (по умолчанию), скользящее окно, закреплённые факты (key-value),
  ветки диалога, саммаризация.
- Вложения: кнопка-скрепка и drag-n-drop. Изображения отправляются в vision-модель
  (`attachments` в base64, модель переключается автоматически), текстовые файлы
  подмешиваются в промпт.
- Светлая/тёмная тема, адаптивная вёрстка, сохранение настроек и ленты в `localStorage`.

## Структура

```
smith-frontend/
├── index.html
├── login.html
├── css/
│   ├── base.css      # reset, CSS-переменные тем, типографика
│   ├── app.css       # layout, компоненты, адаптив
│   └── login.css     # стили страницы входа
└── js/
    ├── config.js     # базовый URL бэкенда, apiUrl/authUrl, лимит контекста
    ├── api.js        # модели, JSON-запрос, SSE-парсер, login/logout/me
    ├── state.js      # состояние и localStorage
    ├── render.js     # рендер сообщений и markdown-lite
    ├── login.js      # логика страницы входа
    └── app.js        # точка входа
```

## Авторизация

Бэкенд использует серверную сессию (cookie `JSESSIONID`) и Spring Security.
При старте `index.html` проверяет сессию (`GET /api/auth/me`); если её нет —
редирект на `login.html`. При любом ответе `401` клиент также переходит на
страницу входа. Выход — кнопка «Выйти» в сайдбаре (`POST /api/auth/logout`).

Все запросы идут с `credentials: 'include'`, поэтому CORS на бэкенде разрешает
credentials. Адрес бэкенда берётся от хоста страницы (`http://<host>:8080`),
поэтому cookie сессии работает и с `localhost`, и с `127.0.0.1`.

Пользователи создаются миграцией бэкенда: `vap`, `lex`, `max`, `heh`, `art`.

## Запуск

Модули ES требуют HTTP-сервера (не открывать через `file://`).

```powershell
# из папки smith-frontend
python -m http.server 8000
# или расширение Live Server в VS Code (порт 5500)
```

Открыть http://localhost:8000 (или http://127.0.0.1:8000).

Бэкенд должен быть запущен на том же хосте, порт `8080` (см. `smith-backend`).

## Настройка адреса бэкенда

По умолчанию адрес вычисляется от хоста страницы: `http(s)://<hostname>:8080`.
Изменить можно:

1. в `js/config.js` — функция `defaultBaseUrl()`;
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
