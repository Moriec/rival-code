# RivalCode: краткий обзор проекта

RivalCode - это backend-платформа для соревновательного программирования: пользователи решают задачи, отправляют код на проверку, участвуют в дуэлях, получают рейтинг и уведомления. Система строится как набор микросервисов вокруг общего Java-модуля `contracts`, где лежат DTO/enums для обмена данными между сервисами.

Главная идея архитектуры: пользовательские HTTP-запросы идут через gateway, а тяжелая и асинхронная работа проверки кода идет через Kafka. Каждый доменный сервис владеет только своими данными и не ходит напрямую в базы других сервисов.

Подробные спецификации:

- [Взаимодействия микросервисов](microservice-interactions.md)
- [Хранение данных](data-storage-specification.md)

## Из чего состоит сервер

Платформа состоит из семи backend-компонентов:

| Компонент | Роль |
|---|---|
| `gateway-service` | Единая точка входа для frontend/API clients. |
| `auth-service` | Регистрация, логин, JWT, профиль пользователя, роли. |
| `problem-service` | Архив задач, условия, теги, версии, лимиты и тесты. |
| `duel-service` | Матчмейкинг, комнаты дуэлей, рейтинг, leaderboard. |
| `submission-service` | Прием решений, сохранение verdict, orchestration judge flow. |
| `online-judge` | Компиляция и запуск кода в sandbox. |
| `notification-service` | Inbox уведомлений и live-доставка пользователю. |

## Как проходит обычный запрос

Клиент не должен обращаться к внутренним сервисам напрямую. Внешний вход один: `gateway-service`.

```text
Frontend / API client
  -> gateway-service
  -> нужный доменный сервис
  -> response клиенту
```

Gateway отвечает за маршрутизацию, CORS, базовые ограничения запроса, correlation id и, при необходимости, rate limit. Доменные сервисы все равно валидируют JWT локально как resource servers, потому что gateway не должен быть единственной линией доверия.

## Как работает проверка решения

Проверка кода - главный асинхронный сценарий системы.

```text
1. Клиент отправляет CreateSubmissionRequest.
2. gateway-service проксирует запрос в submission-service.
3. submission-service создает submission и immutable execution snapshot.
4. submission-service публикует ComputingTask в Kafka topic submissions.
5. online-judge читает ComputingTask.
6. online-judge компилирует и запускает код через isolate sandbox.
7. online-judge публикует JudgeResult в Kafka topic submission-results.
8. submission-service сохраняет полный JudgeResult.
9. submission-service формирует PublicSubmissionVerdict для UI.
10. submission-service публикует SubmissionEvaluatedEvent в submission-events.v1.
```

Полный `JudgeResult` считается внутренними данными. Для rated-режима frontend получает только `PublicSubmissionVerdict`, чтобы не раскрывать скрытые тесты, input, expected output и actual output.

## Как работает дуэль

Дуэль строится поверх задач, submissions и событий judge.

```text
1. Пользователь создает matchmaking ticket.
2. duel-service ищет подходящего соперника по preset/mode/rating.
3. duel-service выбирает задачу через problem-service.
4. Участники подключаются к WebSocket-комнате дуэли.
5. Клиенты отправляют CodeSnapshotMessage для live-состояния редактора.
6. Решение отправляется как обычный submission с mode = DUEL.
7. submission-service проводит judge flow.
8. duel-service получает SubmissionEvaluatedEvent.
9. duel-service завершает дуэль, считает outcome и рейтинг.
10. duel-service публикует DuelFinishedEvent и команды уведомлений.
```

`duel-service` не проверяет код сам. Он реагирует на результат, который пришел от `submission-service` после работы `online-judge`.

## Транспорты и хранилища

| Технология | Где используется | Зачем |
|---|---|---|
| HTTP | Gateway и публичные/internal API сервисов | Запросы клиента и синхронные internal calls. |
| WebSocket | `duel-service` | Live-комнаты дуэлей и code snapshots. |
| Kafka | `submission-service`, `online-judge`, `duel-service`, `notification-service` | Проверка кода, доменные события и команды уведомлений. |
| PostgreSQL | Доменные сервисы | Основное долговременное хранение. |
| Redis | Gateway/problem/duel/submission/notification опционально | Rate limit, cache, matchmaking, presence, live state. |
| Object Storage | Auth/problem/submission опционально | Аватары, assets задач, архивы тестов, judge logs. |

## Ключевые правила архитектуры

- `contracts` - общая библиотека DTO/enums, но не место для бизнес-логики.
- `rivalcode-service-starter` - общий Spring Boot starter для инфраструктурных вещей: trace id, JSON config, единый формат ошибок, service info и фабрика event envelope.
- Каждый микросервис владеет своей БД или schema.
- Один сервис не читает таблицы другого сервиса напрямую.
- Внешние ID в DTO остаются строками, но в БД их удобно хранить как `uuid`.
- Kafka используется для judge/domain events и пользовательских уведомлений.
- `online-judge` не имеет публичного пользовательского API.
- `auth-service` не знает про рейтинг, дуэли, задачи и submissions.
- `problem-service` не знает про матчмейкинг, рейтинг и историю решений.
- `submission-service` хранит полный результат judge, но наружу отдает безопасный public verdict.
- `duel-service` считает рейтинг и историю дуэлей, но не исполняет код.

## Текущий статус реализации

В текущем репозитории уже есть рабочий контур `online-judge`:

- Kafka consumer читает задачи из topic `submissions`.
- `DefaultJudgeService` управляет процессом проверки.
- `IsolateSandbox` компилирует и запускает код с лимитами времени/памяти.
- `StandardChecker` сравнивает output с expected output.
- Kafka producer отправляет `JudgeResult` в topic `submission-results`.
- `test-online-judge` генерирует тестовые submissions и проверяет ответы judge.

Также есть модуль `contracts`, который описывает DTO/enums для остальных сервисов. Остальные микросервисы должны реализовываться по контрактам и документации в `docs`.

## Основной end-to-end поток

```text
Пользователь
  -> gateway-service
  -> auth-service / problem-service / duel-service / submission-service
  -> Kafka submissions
  -> online-judge
  -> Kafka submission-results
  -> submission-service
  -> Kafka submission-events.v1
  -> duel-service
  -> Kafka notification-commands.v1
  -> notification-service
  -> пользователь видит verdict, итог дуэли и уведомления
```

## Кратко по микросервисам

### `gateway-service`

Единая точка входа в backend. Маршрутизирует `/api/auth/**`, `/api/users/**`, `/api/problems/**`, `/api/duels/**`, `/api/submissions/**`, `/api/notifications/**` и `/ws/duels/**` во внутренние сервисы. Не хранит доменные данные и не собирает сложные read-models.

### `auth-service`

Сервис identity. Отвечает за регистрацию, логин, refresh tokens, JWT, роли, профиль и аватар пользователя. Источник истины для `userId`, `email`, `username`, `displayName`, `roles`, `enabled`. Не хранит рейтинг, дуэли, submissions и solved statistics.

### `problem-service`

Сервис архива задач. Хранит задачи, версии условий, теги, примеры, лимиты, checker type и ссылки на архивы тестов. Отдает задачи для обычного решения и подбирает задачу для дуэли через internal API. Не знает, кто победил в дуэли и какие submissions уже прошли judge.

### `duel-service`

Сервис соревновательной логики. Управляет matchmaking tickets, комнатами дуэлей, WebSocket-состоянием, сезонами, рейтингом, leaderboard и историей матчей. Получает `SubmissionEvaluatedEvent`, решает исход дуэли и публикует события/уведомления о результате.

### `submission-service`

Сервис жизненного цикла решения. Принимает `CreateSubmissionRequest`, сохраняет submission и execution snapshot, публикует `ComputingTask` для judge, принимает `JudgeResult`, сохраняет полный результат и формирует безопасный `PublicSubmissionVerdict` для UI.

### `online-judge`

Worker без публичного API. Получает `ComputingTask`, компилирует пользовательский код, запускает его в isolate sandbox, проверяет output и возвращает `JudgeResult`. Не меняет рейтинг, профиль, задачи, историю дуэлей или публичный verdict.

### `notification-service`

Сервис inbox-уведомлений. Принимает `NotificationCommand` из Kafka topic `notification-commands.v1`, сохраняет уведомления, отдает список уведомлений пользователю, помечает их прочитанными и может стримить live-события через SSE/WebSocket. Не принимает доменных решений, только доставляет уже сформированные сообщения.
