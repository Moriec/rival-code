# Спецификация frontend RivalCode

## Назначение

Frontend - отдельное пользовательское приложение платформы RivalCode. Оно должно работать через `gateway-service` и не обращаться напрямую к внутренним микросервисам.

Основная задача frontend: дать пользователю понятный интерфейс для регистрации, просмотра задач, отправки решений, участия в дуэлях, просмотра рейтинга, уведомлений и истории submissions.

```text
Browser
  -> gateway-service
  -> auth-service / problem-service / submission-service / duel-service / notification-service
```

Исходный код frontend лучше держать отдельным модулем, например `frontend/`. `gateway-service` может в production отдавать собранные static files, но не должен содержать UI-логику.

## Общий стиль

Визуальный стиль должен быть единым и вдохновленным Codeforces: плотный, табличный, быстрый для сканирования, с минимумом декоративности. Не нужно копировать бренд, логотип, точные цвета и верстку Codeforces один-в-один; нужна похожая инженерная эстетика соревновательного программирования.

Ключевые признаки:

- светлый интерфейс по умолчанию;
- компактные таблицы;
- синие ссылки;
- тонкие серые границы;
- панели с простыми заголовками;
- минимум крупных marketing-секций;
- высокая плотность информации;
- все страницы должны выглядеть как части одной системы.

Интерфейс должен ощущаться не как SaaS-лендинг, а как рабочая платформа для программистов: быстро найти задачу, открыть условие, отправить код, увидеть verdict, войти в дуэль.

## Design Tokens

Рекомендуемые базовые токены:

| Токен | Значение | Назначение |
|---|---:|---|
| `--color-bg` | `#f5f5f5` | фон страницы |
| `--color-surface` | `#ffffff` | панели, таблицы, формы |
| `--color-border` | `#cfd7e3` | границы блоков |
| `--color-border-soft` | `#e5e7eb` | внутренние разделители |
| `--color-header` | `#eaf0f8` | заголовки панелей и таблиц |
| `--color-link` | `#1f5fbf` | ссылки |
| `--color-link-hover` | `#0f3f8f` | hover ссылок |
| `--color-text` | `#1f2933` | основной текст |
| `--color-muted` | `#6b7280` | вторичный текст |
| `--color-success` | `#198754` | accepted/success |
| `--color-danger` | `#c82333` | wrong answer/error |
| `--color-warning` | `#b7791f` | pending/time limit |
| `--radius-sm` | `3px` | таблицы, inputs, buttons |
| `--radius-md` | `4px` | панели |
| `--space-xs` | `4px` | мелкие отступы |
| `--space-sm` | `8px` | базовый отступ |
| `--space-md` | `12px` | стандартный отступ |
| `--space-lg` | `16px` | крупный отступ |

Шрифты:

- основной: system font stack, например `Arial`, `Helvetica`, `Segoe UI`, sans-serif;
- код: `Consolas`, `Monaco`, `Menlo`, monospace;
- базовый размер текста: `13px` или `14px`;
- размер текста в таблицах: `12px` или `13px`;
- крупные hero-заголовки не использовать.

## Layout

Базовая структура страницы:

```text
top header
  logo / project name
  main navigation
  user menu / notifications

content wrapper
  optional left/main/right columns
  panels and tables

footer
  short technical links
```

Рекомендуемая ширина контента:

- desktop: `1100px`-`1280px`;
- wide desktop: не растягивать текстовые страницы на всю ширину;
- mobile: одноколоночная верстка, таблицы превращать в компактные списки или разрешать горизонтальный scroll.

Панель:

- тонкая рамка;
- заголовок на светло-сером или светло-синем фоне;
- внутри белый фон;
- без тяжелых shadows;
- border radius не больше `4px`.

Таблица:

- заголовок с фоном `--color-header`;
- строки с hover;
- числовые значения выравнивать по правому краю;
- главные ссылки в строках должны быть синими;
- статусы показывать короткими badge/label.

## Навигация

Главное меню:

- `Problems`
- `Submissions`
- `Duels`
- `Leaderboard`
- `Notifications`
- `Profile`

Для неавторизованного пользователя:

- `Login`
- `Register`

Верхняя навигация должна быть компактной. Пользователь всегда должен понимать, где он находится: активный пункт меню подсвечивается.

## Маршруты Frontend

| Route | Назначение |
|---|---|
| `/` | dashboard или список задач |
| `/login` | вход |
| `/register` | регистрация |
| `/profile/me` | мой профиль |
| `/users/:userId` | публичный профиль |
| `/problems` | список задач |
| `/problems/:problemId` | условие задачи |
| `/submissions` | мои/общие submissions |
| `/submissions/:submissionId` | страница submission |
| `/duels` | центр дуэлей |
| `/duels/:duelId` | комната дуэли |
| `/leaderboard` | рейтинг |
| `/notifications` | inbox уведомлений |
| `/admin/problems` | управление задачами, только admin |
| `/admin/duels` | сезоны, queue presets, problem pools, только admin |

## API Access

Frontend должен обращаться только к `gateway-service`.

Пример:

```text
GET /api/problems
POST /api/submissions
GET /api/duels/{duelId}
GET /api/notifications
```

Нельзя обращаться из браузера напрямую к:

- `auth-service:8081`;
- `problem-service:8082`;
- `submission-service:8083`;
- `notification-service:8084`;
- `duel-service:8085`;
- Kafka;
- PostgreSQL;
- MinIO internal buckets.

JWT access token передается в `Authorization: Bearer <token>`. Refresh token должен храниться безопасно: предпочтительно httpOnly cookie, если auth/gateway будут это поддерживать. Если в первой версии используется localStorage, нужно понимать риск XSS и не хранить там лишние данные.

## Auth UX

Страницы:

- `Login`
- `Register`
- `Profile`
- `Edit Profile`

Поведение:

- после login сохранить session state и перейти на предыдущую страницу или `/problems`;
- при `401` попытаться refresh token, если механизм доступен;
- при неудачном refresh перевести пользователя на `/login`;
- роли из JWT использовать только для UI-подсказок, но backend все равно должен проверять права сам.

## Problems UI

### Список задач

Route: `/problems`

Источник:

```text
GET /api/problems
GET /api/problems/tags
```

Основной вид: таблица.

Колонки:

- title/slug;
- difficulty;
- tags;
- accepted/attempts;
- status;
- actions.

Фильтры:

- search;
- difficulty;
- tag;
- status;
- solved/unsolved, если backend позже даст такую read-model.

### Страница задачи

Route: `/problems/:problemId`

Источник:

```text
GET /api/problems/{problemId}
```

Блоки:

- title;
- limits;
- tags;
- statement;
- input/output specification;
- examples;
- submit form;
- recent submissions по задаче, если endpoint будет добавлен позже.

Условие задачи должно быть читаемым, но компактным. Примеры показывать в monospace-блоках с кнопкой copy.

## Submission UI

### Создание submission

Источник:

```text
POST /api/submissions
```

Форма:

- language select;
- code editor;
- submit button;
- mode: `PRACTICE` или `DUEL`, если отправка идет из комнаты дуэли;
- problemId и duelId подставляются из текущего route/context.

Редактор:

- monospace;
- line numbers;
- tab size 4;
- базовая подсветка синтаксиса;
- autosave в local state по problemId/language.

### Просмотр результата

Источники:

```text
GET /api/submissions/{submissionId}
GET /api/submissions/{submissionId}/verdict
```

UI должен показывать публичный verdict, а не внутренний `JudgeResult`.

Статусы:

| Status | Цвет |
|---|---|
| `ACCEPTED` | green |
| `WRONG_ANSWER` | red |
| `TIME_LIMIT_EXCEEDED` | orange/brown |
| `MEMORY_LIMIT_EXCEEDED` | orange/brown |
| `COMPILATION_ERROR` | gray/red |
| `RUNTIME_ERROR` | red |
| `SYSTEM_ERROR` | dark red |
| `QUEUED` / `RUNNING` | muted blue/gray |

В rated/duel режиме нельзя показывать скрытые input, expected output и actual output.

## Duels UI

### Центр дуэлей

Route: `/duels`

Источники:

```text
POST /api/duels/matchmaking/tickets
DELETE /api/duels/matchmaking/tickets/{ticketId}
GET /api/duels/profile/{userId}
```

Блоки:

- мой duel profile;
- текущий рейтинг;
- кнопка `Find rated duel`;
- кнопка `Find unrated duel`;
- текущий matchmaking ticket;
- recent duels.

Пока пользователь ждет соперника, показывать компактную pending-панель с временем ожидания и кнопкой cancel.

### Комната дуэли

Route: `/duels/:duelId`

Источники:

```text
GET /api/duels/{duelId}
WS /ws/duels
POST /api/submissions
```

Экран:

- верхняя панель: timer, status, problem title;
- левая/основная колонка: statement;
- правая колонка: участники, рейтинг, outcome;
- нижняя/отдельная зона: code editor и submit;
- live code snapshot соперника, если включено.

WebSocket:

```text
client -> /app/duels/{duelId}/code
server -> /topic/duels/{duelId}/state
server -> /topic/duels/{duelId}/code/{userId}
server -> /topic/duels/{duelId}/finished
```

При `DUEL_FINISHED` UI должен:

- остановить timer;
- показать outcome;
- обновить рейтинг;
- предложить перейти к leaderboard или начать новую дуэль.

## Leaderboard UI

Route: `/leaderboard`

Источник:

```text
GET /api/duels/leaderboard?seasonId=...
```

Вид: таблица.

Колонки:

- rank;
- user;
- rating;
- wins;
- losses;
- draws.

Первые места можно слегка выделить, но без декоративной перегрузки.

## Notifications UI

Route: `/notifications`

Источники:

```text
GET /api/notifications
POST /api/notifications/read
GET /api/notifications/stream
```

UI:

- icon/badge в header;
- список уведомлений;
- unread highlight;
- кнопка mark read;
- live обновления через SSE.

SSE используется только для получения событий от сервера. Для дуэлей и live code используется WebSocket.

## Admin UI

Admin-раздел не должен быть виден обычным пользователям в навигации.

Страницы:

- управление задачами;
- upload assets/test archives, если frontend будет поддерживать это;
- seasons;
- queue presets;
- duel problem pools.

Backend все равно обязан проверять роль `ADMIN`; frontend скрывает admin UI только для удобства.

## Компоненты

Базовые компоненты должны быть переиспользуемыми:

- `AppLayout`;
- `TopNav`;
- `Panel`;
- `DataTable`;
- `StatusBadge`;
- `DifficultyBadge`;
- `TagList`;
- `Pagination`;
- `CodeEditor`;
- `SubmissionVerdict`;
- `ProblemStatement`;
- `UserLink`;
- `NotificationBell`;
- `DuelTimer`;
- `DuelParticipantCard`.

Все компоненты должны использовать общие tokens, а не локальные случайные цвета и размеры.

## Forms

Правила:

- labels всегда видимы;
- error text рядом с полем;
- disabled/loading состояние у submit button;
- backend validation errors показывать пользователю понятным текстом;
- destructive actions подтверждать.

Кнопки:

- primary: синий;
- secondary: серый;
- danger: красный;
- высота компактная, без больших rounded-pill форм.

## State Management

Рекомендуемые состояния:

- auth/session state;
- cached API queries;
- current user profile;
- notification unread count;
- active duel state;
- local code drafts.

Для API удобно использовать query/cache библиотеку, например TanStack Query. Для глобального UI-state достаточно легкого store. Не нужно складывать весь backend response в один большой глобальный store.

## Error Handling

Frontend должен единообразно обрабатывать:

- `400`: показать ошибку формы;
- `401`: refresh или redirect на login;
- `403`: показать страницу/сообщение "нет доступа";
- `404`: not found;
- `409`: конфликт состояния, например уже есть active matchmaking ticket;
- `500`: общая ошибка сервера с traceId, если gateway/starter его возвращают.

Если backend возвращает `traceId`, показывать его в compact details, чтобы легче искать ошибку в логах.

## Loading And Empty States

Loading:

- таблицы: skeleton rows или компактный spinner;
- forms: disabled submit button;
- duel room: отдельное состояние подключения к WebSocket.

Empty states:

- нет задач по фильтру;
- нет submissions;
- нет уведомлений;
- нет recent duels.

Empty states должны быть короткими и рабочими, без больших иллюстраций.

## Responsive

Desktop - основной сценарий, потому что пользователи пишут код.

Mobile требования:

- можно смотреть задачи, профиль, leaderboard, notifications;
- отправка кода допустима, но не обязана быть идеальной;
- таблицы должны иметь горизонтальный scroll или адаптивные карточки;
- duel room на mobile может быть упрощенным.

## Security Rules

Frontend не должен:

- показывать hidden test input/output;
- хранить refresh token в обычном JS state, если есть cookie-вариант;
- доверять роли пользователя только на клиенте;
- принимать HTML из statement без sanitization;
- показывать внутренние stack traces;
- обращаться к внутренним сервисам напрямую.

Problem statement может содержать форматирование. Если используется Markdown/HTML, его нужно безопасно рендерить.

## Первый MVP

Минимальный полезный frontend:

1. Login/register.
2. Список задач.
3. Страница задачи.
4. Отправка решения.
5. Просмотр public verdict.
6. Matchmaking для дуэли.
7. Комната дуэли с timer, statement и submit.
8. Leaderboard.
9. Notifications inbox.

Admin UI, advanced filters, profile projections и красивый live editor можно расширять после MVP.

## Definition Of Done

Frontend считается готовым для первой интеграции, если:

- все запросы идут через `gateway-service`;
- UI использует единые tokens и компоненты;
- основные страницы не выглядят как разные приложения;
- можно пройти flow `login -> problems -> submit -> verdict`;
- можно пройти flow `login -> duel matchmaking -> duel room -> submit -> duel result`;
- hidden test data нигде не показывается;
- при `401/403/404/409/500` пользователь видит понятное состояние;
- desktop layout удобен для написания кода.
