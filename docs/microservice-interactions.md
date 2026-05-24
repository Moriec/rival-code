# Взаимодействия микросервисов RivalCode

Документ фиксирует рабочие контракты между микросервисами платформы. Его цель — дать понятную карту для дальнейшей реализации сервисов: какие запросы должен принимать каждый сервис, что он возвращает, какие сообщения публикует и за какие данные отвечает.

Базовый Java package контрактов: `com.rivalcode.contracts.*`.

## Общая схема

Целевая система состоит из семи backend-компонентов:

| Компонент | Назначение | Основной транспорт |
|---|---|---|
| `gateway-service` | единая точка входа, маршрутизация, CORS, rate limit, auth propagation | HTTP |
| `auth-service` | регистрация, вход, refresh, профиль, аватар, роли | HTTP |
| `problem-service` | архив задач, условия, теги, версии, лимиты, test manifests | HTTP |
| `duel-service` | матчмейкинг, дуэли, рейтинг, лидерборд, WebSocket-комнаты | HTTP, WebSocket, Kafka, RabbitMQ |
| `submission-service` | создание submissions, orchestration judge, хранение verdict | HTTP, Kafka |
| `online-judge` | sandbox-компиляция и запуск пользовательского кода | Kafka |
| `notification-service` | inbox уведомлений, read-status, live notification stream | HTTP, RabbitMQ |

Ключевой end-to-end поток:

```text
client
  -> gateway-service
  -> submission-service
  -> Kafka topic submissions
  -> online-judge
  -> Kafka topic submission-results
  -> submission-service
  -> Kafka topic submission-events.v1
  -> duel-service
  -> RabbitMQ notifications.topic
  -> notification-service
```

## Gateway Service

`gateway-service` не владеет доменными данными. Он маршрутизирует запросы во внутренние сервисы и может отдавать диагностическую информацию о маршрутах.

### HTTP API

| Method | Path | Request | Response | Назначение |
|---|---|---|---|---|
| `GET` | `/api/gateway/routes` | - | `List<GatewayRouteDto>` | список активных маршрутов gateway |
| `GET` | `/api/gateway/services/status` | - | `List<GatewayServiceStatusDto>` | агрегированный health/status backend-сервисов |

### Route Mapping

| External path | Target service |
|---|---|
| `/api/auth/**` | `auth-service` |
| `/api/users/**` | `auth-service` |
| `/api/problems/**` | `problem-service` |
| `/api/duels/**` | `duel-service` |
| `/ws/duels/**` | `duel-service` |
| `/api/submissions/**` | `submission-service` |
| `/api/notifications/**` | `notification-service` |

### Implementation Notes

- Gateway проверяет базовые cross-cutting concerns: CORS, request size, rate limit, correlation id.
- JWT можно валидировать в gateway, но доменные сервисы все равно должны валидировать токен локально как resource servers.
- Gateway не должен собирать сложные доменные read-models. Профиль, история, leaderboard и verdict должны приходить из профильных сервисов.

## Auth Service

`auth-service` отвечает только за identity и профиль пользователя: регистрацию, вход, refresh-токены, роли, публичный профиль и аватар. Он не знает про submissions, рейтинг, матчи и задачи.

### HTTP API

| Method | Path | Request | Response | Назначение |
|---|---|---|---|---|
| `POST` | `/api/auth/register` | `RegisterRequest` | `AuthTokensResponse` | создать пользователя и выдать пару токенов |
| `POST` | `/api/auth/login` | `LoginRequest` | `AuthTokensResponse` | проверить credentials и выдать токены |
| `POST` | `/api/auth/refresh` | `RefreshTokenRequest` | `AuthTokensResponse` | обновить access/refresh token |
| `GET` | `/api/users/me` | - | `UserProfileDto` | профиль текущего пользователя |
| `PATCH` | `/api/users/me` | `UpdateProfileRequest` | `UserProfileDto` | обновить display name/avatar |
| `GET` | `/api/users/{userId}/profile` | - | `UserProfileDto` | публичный профиль пользователя |

### DTO

- `RegisterRequest`: `email`, `username`, `password`, `displayName`.
- `LoginRequest`: `usernameOrEmail`, `password`.
- `RefreshTokenRequest`: `refreshToken`.
- `AuthTokensResponse`: `accessToken`, `refreshToken`, `tokenType`, `expiresInSeconds`, `user`.
- `UserProfileDto`: `userId`, `email`, `username`, `displayName`, `avatar`, `roles`, `enabled`, timestamps.
- `AvatarDto`: metadata ссылки на avatar object.

### Events

Для первой версии события от auth-service можно не реализовывать. В следующей итерации полезны события:

| Event | Payload | Consumer |
|---|---|---|
| `user.created` | `EventEnvelope<UserProfileDto>` | `duel-service` для profile projection |
| `user.profile.updated` | `EventEnvelope<UserProfileDto>` | `duel-service`, `notification-service` |

## Problem Service

`problem-service` отвечает за задачи как за контент: условие, примеры, теги, сложность, лимиты, версии и test manifests. Он не отвечает за рейтинг, матчмейкинг и submissions.

### HTTP API

| Method | Path | Request | Response | Назначение |
|---|---|---|---|---|
| `GET` | `/api/problems` | query fields from `ProblemFilterRequest` | `List<ProblemSummaryDto>` | список задач архива с фильтрами |
| `GET` | `/api/problems/{problemId}` | - | `ProblemDetailsDto` | полная карточка задачи для UI |
| `POST` | `/api/problems` | `CreateProblemRequest` | `ProblemDetailsDto` | admin-create задачи |
| `PATCH` | `/api/problems/{problemId}` | `UpdateProblemRequest` | `ProblemDetailsDto` | admin-update задачи |
| `GET` | `/api/problems/tags` | - | `List<TagDto>` | список тегов |
| `POST` | `/internal/problems/select-for-duel` | `DuelProblemSelectionRequest` | `DuelProblemSelectionResponse` | внутренний подбор задачи для дуэли |

### DTO

- `ProblemFilterRequest`: `search`, `difficulties`, `statuses`, `tagIds`, `solvedByUser`, `userId`, pagination/sort fields.
- `ProblemSummaryDto`: короткая карточка для архива.
- `ProblemDetailsDto`: условие, спецификации ввода/вывода, лимиты, теги, примеры.
- `ProblemVersionDto`: версия задачи, ссылки на statement/test manifest object keys.
- `DuelProblemSelectionRequest`: preset/pool, users, difficulties, tags, excluded problems, rated flag.
- `DuelProblemSelectionResponse`: выбранная задача, версия, statement, limits, checker type, public examples, `testArchiveObjectKey`.

### Internal Rules

- Hidden tests и expected output скрытых тестов не должны уходить во frontend.
- `DuelProblemSelectionResponse.testArchiveObjectKey` предназначен для внутренних сервисов, не для клиента.
- `problem-service` может знать `userId` только как входной параметр фильтра или исключения уже решенных задач.

## Duel Service

`duel-service` — основной доменный сервис. Он отвечает за матчмейкинг, жизненный цикл дуэли, WebSocket-комнату, рейтинг, leaderboard, recent games и профильную read-model.

### HTTP API

| Method | Path | Request | Response | Назначение |
|---|---|---|---|---|
| `POST` | `/api/duels/matchmaking/tickets` | `CreateMatchmakingTicketRequest` | `MatchmakingTicketDto` | поставить пользователя в очередь |
| `DELETE` | `/api/duels/matchmaking/tickets/{ticketId}` | - | `MatchmakingTicketDto` | отменить поиск |
| `GET` | `/api/duels/{duelId}` | - | `DuelRoomStateDto` | состояние комнаты дуэли |
| `GET` | `/api/duels/profile/{userId}` | - | `UserDuelProfileDto` | профиль игрока, рейтинг, recent games |
| `GET` | `/api/duels/leaderboard?seasonId=...` | - | `List<LeaderboardEntryDto>` | leaderboard сезона |

### Admin CRUD

| Entity | Base path | DTO |
|---|---|---|
| Season | `/api/duels/seasons` | `SeasonDto` |
| Queue preset | `/api/duels/queue-presets` | `QueuePresetDto` |
| Duel problem pool | `/api/duels/problem-pools` | `DuelProblemPoolDto` |

Минимальный CRUD для каждой сущности:

| Method | Path | Response |
|---|---|---|
| `GET` | `{basePath}` | `List<Dto>` |
| `GET` | `{basePath}/{id}` | `Dto` |
| `POST` | `{basePath}` | `Dto` |
| `PATCH` | `{basePath}/{id}` | `Dto` |
| `DELETE` | `{basePath}/{id}` | `void` or deactivated `Dto` |

### WebSocket API

Recommended STOMP routes:

| Direction | Destination | Payload | Назначение |
|---|---|---|---|
| client -> server | `/app/duels/{duelId}/code` | `CodeSnapshotMessage` | отправить snapshot своего кода |
| client -> server | `/app/duels/{duelId}/presence` | lightweight presence payload | heartbeat/ready state |
| server -> client | `/topic/duels/{duelId}/state` | `DuelRoomStateDto` | состояние комнаты |
| server -> client | `/topic/duels/{duelId}/code/{userId}` | `CodeSnapshotMessage` | read-only код соперника |
| server -> client | `/topic/duels/{duelId}/finished` | `DuelFinishedEvent` | завершение дуэли |

### Kafka

| Direction | Topic | Key | Payload |
|---|---|---|---|
| consume | `submission-events.v1` | `duelId` or `submissionId` | `EventEnvelope<SubmissionEvaluatedEvent>` |
| produce | `duel-events.v1` | `duelId` | `EventEnvelope<DuelFinishedEvent>` |

### RabbitMQ

`duel-service` публикует команды уведомлений в exchange `notifications.topic`.

| Routing key | Payload | Когда |
|---|---|---|
| `user.{userId}.match-found` | `NotificationCommand` | соперник найден |
| `user.{userId}.duel-finished` | `NotificationCommand` | дуэль завершена |
| `user.{userId}.rating-changed` | `NotificationCommand` | рейтинг изменился |

### Internal Flow

1. Пользователь создает matchmaking ticket.
2. Matchmaker находит пару.
3. `duel-service` вызывает `problem-service` через `/internal/problems/select-for-duel`.
4. Создается duel room и участники получают `DuelRoomStateDto`.
5. Во время дуэли WebSocket передает `CodeSnapshotMessage`.
6. Решение отправляется в `submission-service`.
7. После `SubmissionEvaluatedEvent` сервис завершает дуэль, меняет рейтинг и публикует уведомления.

## Submission Service

`submission-service` отвечает за весь lifecycle решения: создать submission, зафиксировать immutable execution snapshot, отправить задачу в judge, принять полный `JudgeResult`, сохранить результат и сформировать публичный verdict.

### HTTP API

| Method | Path | Request | Response | Назначение |
|---|---|---|---|---|
| `POST` | `/api/submissions` | `CreateSubmissionRequest` | `SubmissionCreatedResponse` | создать practice/duel submission |
| `GET` | `/api/submissions/{submissionId}` | - | `SubmissionSummaryDto` | summary submission |
| `GET` | `/api/submissions/{submissionId}/verdict` | - | `PublicSubmissionVerdict` | публичный verdict без hidden test leakage |
| `GET` | `/api/users/{userId}/submissions` | query filters | `List<SubmissionSummaryDto>` | история решений пользователя |
| `GET` | `/internal/submissions/{submissionId}/execution-context` | - | `SubmissionExecutionContext` | immutable execution context для judge/orchestration |

### Kafka

| Direction | Topic | Key | Payload | Назначение |
|---|---|---|---|---|
| produce | `submissions` | `submissionId` | `ComputingTask` | отправить задачу в `online-judge` |
| consume | `submission-results` | `submissionId` | `JudgeResult` | получить полный результат judge |
| produce | `submission-events.v1` | `duelId` or `submissionId` | `EventEnvelope<SubmissionEvaluatedEvent>` | безопасное доменное событие для `duel-service` |

### DTO

- `CreateSubmissionRequest`: `userId`, `problemId`, `problemVersionId`, optional `duelId`, `mode`, `userCode`.
- `SubmissionCreatedResponse`: `submissionId`, `status`, `createdAt`.
- `ComputingTask`: `submissionId`, `userCode`, `testCases`, limits, optional custom checker code.
- `JudgeResult`: полный internal result от judge.
- `PublicSubmissionVerdict`: публичная модель для UI.
- `SubmissionEvaluatedEvent`: безопасное событие без input/expected output скрытых тестов.

### Data Rules

- Полный `JudgeResult` можно хранить внутри `submission-service`.
- Rated UI никогда не получает `TestCaseResult.input`, `expectedOutput`, `actualOutput` для hidden tests.
- Повторная доставка `JudgeResult` не должна повторно менять `SubmissionStatus` или публиковать duplicate domain event без idempotency check.

## Online Judge

`online-judge` — worker-сервис. Он не имеет публичного пользовательского API и не владеет доменной логикой платформы.

### Kafka

| Direction | Topic | Key | Payload |
|---|---|---|---|
| consume | `submissions` | `submissionId` | `ComputingTask` |
| produce | `submission-results` | `submissionId` | `JudgeResult` |

### Responsibilities

- Скомпилировать `UserCode`.
- Выполнить код в sandbox через `isolate`.
- Прогнать `List<TestCase>`.
- Вернуть `JudgeResult` со статусом, временем, памятью и результатами тестов.

### Boundaries

- Не меняет рейтинг.
- Не завершает дуэли.
- Не формирует публичный verdict для UI.
- Не ходит в базы `auth-service`, `problem-service`, `duel-service`.
- `ExecutionResult` остается внутренним классом `online-judge`, не контрактом между сервисами.

## Notification Service

`notification-service` отвечает за inbox уведомлений, статус прочтения и доставку live notification stream.

### RabbitMQ

| Exchange | Type | Payload |
|---|---|---|
| `notifications.topic` | topic | `NotificationCommand` |

Routing keys:

| Routing key | Смысл |
|---|---|
| `user.{userId}.match-found` | найден соперник |
| `user.{userId}.duel-finished` | дуэль завершена |
| `user.{userId}.rating-changed` | изменился рейтинг |
| `user.{userId}.practice-judged` | practice submission проверен |

### HTTP API

| Method | Path | Request | Response | Назначение |
|---|---|---|---|---|
| `GET` | `/api/notifications?userId=...` | - | `List<NotificationDto>` | inbox пользователя |
| `POST` | `/api/notifications/read` | `MarkNotificationsReadRequest` | `List<NotificationDto>` | отметить уведомления прочитанными |
| `GET` | `/api/notifications/stream` | - | live `NotificationDto` events | optional SSE/WebSocket stream |

### Data Rules

- `NotificationCommand` — команда создать уведомление.
- `NotificationDto` — read-model для клиента.
- Consumer должен использовать acknowledgements и не терять сообщение до сохранения notification в БД.

## Event And Data Rules

### Kafka Topics

| Topic | Producer | Consumer | Key | Payload |
|---|---|---|---|---|
| `submissions` | `submission-service` | `online-judge` | `submissionId` | `ComputingTask` |
| `submission-results` | `online-judge` | `submission-service` | `submissionId` | `JudgeResult` |
| `submission-events.v1` | `submission-service` | `duel-service` | `duelId` or `submissionId` | `EventEnvelope<SubmissionEvaluatedEvent>` |
| `duel-events.v1` | `duel-service` | analytics/future services | `duelId` | `EventEnvelope<DuelFinishedEvent>` |

### Envelope

Доменные события должны использовать `EventEnvelope<T>`:

- `eventId` — idempotency key.
- `eventType` — например `submission.evaluated`.
- `eventVersion` — версия payload.
- `occurredAt` — время события.
- `traceId` — корреляция запроса.
- `producer` — сервис-источник.
- `payload` — доменная модель события.

Текущие judge-сообщения `ComputingTask` и `JudgeResult` остаются без envelope для совместимости с уже готовым `online-judge`.

### Security

- `auth-service` выпускает JWT.
- Остальные сервисы валидируют JWT локально.
- Admin endpoints в `problem-service` и `duel-service` должны требовать роль `ADMIN`.
- Internal endpoints `/internal/**` не должны быть доступны напрямую из публичного gateway.

## Acceptance Checklist

- Каждый DTO, упомянутый в документе, существует в `contracts`.
- `submission-results` используется именно во множественном числе, потому что так сейчас работает `online-judge`.
- Полный `JudgeResult` не считается публичным frontend-контрактом.
- `duel-service` является единственным сервисом, который меняет рейтинг.
- `notification-service` не принимает доменные решения, а только сохраняет и доставляет уведомления.
