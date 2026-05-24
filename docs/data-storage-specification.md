# Спецификация хранения данных RivalCode

Документ описывает, какие данные хранит каждый микросервис, какие таблицы ему нужны и где проходят границы владения данными. Он дополняет `docs/microservice-interactions.md`: там описаны запросы и события, здесь - базы данных, таблицы, индексы и временные хранилища.

Базовый Java package контрактов: `com.rivalcode.contracts.*`.

## Общие правила

### Граница данных

- Каждый микросервис владеет своей БД или своим schema. Остальные сервисы не читают и не пишут в эти таблицы напрямую.
- Между БД разных сервисов не создаются foreign key. Поля `user_id`, `problem_id`, `duel_id`, `submission_id` в чужих сервисах являются ссылками по значению.
- Синхронизация между сервисами идет через HTTP internal API, Kafka domain events и RabbitMQ notification commands.
- Для доменных событий используется outbox-table в сервисе-владельце события.
- Kafka и RabbitMQ не считаются долговременным хранилищем бизнес-данных.

### Типы данных

| Логический тип | Рекомендуемый SQL type | Комментарий |
|---|---|---|
| service/user/problem/duel/submission id | `uuid` | В DTO контрактах наружу отдается как `String`, в БД лучше хранить как `uuid`. |
| enum из contracts | `varchar(64)` | Хранить строковое имя enum, не ordinal. |
| дата/время | `timestamptz` | Хранить в UTC. В Java использовать `Instant`. |
| произвольный payload | `jsonb` | Для event payload, notification payload, snapshot. |
| исходный код | `text` | Для submissions и live snapshots. |
| object storage key | `text` | Ключ объекта в MinIO/S3, не публичный URL. |

### Стандартная outbox table

Таблица нужна в сервисах, которые публикуют Kafka/RabbitMQ события из транзакции с изменением доменных данных.

| Column | Type | Required | Описание |
|---|---|---|---|
| `event_id` | `uuid` | yes | ID события, соответствует `EventEnvelope.eventId`. |
| `aggregate_type` | `varchar(64)` | yes | Например `SUBMISSION`, `DUEL`, `USER`. |
| `aggregate_id` | `uuid` | yes | ID сущности, вокруг которой произошло событие. |
| `event_type` | `varchar(128)` | yes | Например `SUBMISSION_EVALUATED`, `DUEL_FINISHED`. |
| `event_version` | `integer` | yes | Версия схемы события. |
| `trace_id` | `varchar(128)` | no | Корреляция запроса/события. |
| `producer` | `varchar(128)` | yes | Имя сервиса-публикатора. |
| `payload` | `jsonb` | yes | Сериализованный payload события. |
| `created_at` | `timestamptz` | yes | Когда событие создано. |
| `published_at` | `timestamptz` | no | Когда успешно опубликовано. |
| `publish_attempts` | `integer` | yes | Количество попыток публикации. |
| `last_error` | `text` | no | Последняя ошибка публикации. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `idx_outbox_unpublished` | `published_at, created_at` where `published_at is null` | Быстрый выбор неопубликованных событий. |
| `idx_outbox_aggregate` | `aggregate_type, aggregate_id` | Аудит событий по сущности. |

## Storage Matrix

| Component | Durable DB | Cache/ephemeral storage | Object storage |
|---|---|---|---|
| `gateway-service` | не требуется в v1 | Redis для rate limit и короткого health-cache | нет |
| `auth-service` | PostgreSQL `auth_db`, schema `auth` | Redis опционально для blacklist/access token cache | bucket `avatars` |
| `problem-service` | PostgreSQL `problem_db`, schema `problem` | Redis для cache архива задач | buckets `problem-assets`, `problem-test-archives` |
| `duel-service` | PostgreSQL `duel_db`, schema `duel` | Redis для matchmaking, presence, live code snapshots, timers | нет в v1 |
| `submission-service` | PostgreSQL `submission_db`, schema `submission` | Redis опционально для короткого status polling cache | bucket `judge-logs` опционально |
| `online-judge` | не требуется | локальные temp dirs, isolate boxes | нет |
| `notification-service` | PostgreSQL `notification_db`, schema `notification` | Redis опционально для live stream fanout | нет в v1 |

## Gateway Service

`gateway-service` не владеет доменной моделью. Он маршрутизирует запросы, проверяет общие ограничения и может отдавать диагностические DTO: `GatewayRouteDto`, `GatewayServiceStatusDto`.

### Durable Storage

Durable-БД в v1 не нужна.

Маршруты хранятся в конфигурации сервиса:

| Storage | Key/data | Назначение |
|---|---|---|
| `application.yml` или config repo | route id, path pattern, target service, target uri, enabled | Источник `GatewayRouteDto`. |
| Spring Actuator/health clients | service name, status, version, checkedAt, details | Источник `GatewayServiceStatusDto`. |

### Redis Keys

| Key pattern | Type | TTL | Назначение |
|---|---|---|---|
| `rate_limit:{routeId}:{principalOrIp}` | counter | window TTL | Ограничение частоты запросов. |
| `gateway:health:{serviceName}` | string/json | 5-30 sec | Короткий cache health/status внутренних сервисов. |
| `gateway:jwt:jwks` | string/json | 5-30 min | Cache публичных ключей auth-service, если используется JWKS endpoint. |

### Таблицы

Таблиц нет. Если позже понадобится runtime-управление маршрутами из UI, можно добавить отдельную таблицу `gateway_routes`, но в текущей версии это не требуется.

## Auth Service

`auth-service` владеет identity-данными: пользователи, credentials, роли, refresh tokens, аватары. Он не хранит рейтинг, дуэли, submissions и solved statistics.

Рекомендуемое хранилище:

| Storage | Name |
|---|---|
| PostgreSQL database | `auth_db` |
| PostgreSQL schema | `auth` |
| Object storage bucket | `avatars` |

### `users`

Главная таблица пользователя.

| Column | Type | Required | Описание |
|---|---|---|---|
| `user_id` | `uuid` | yes, PK | ID пользователя. |
| `email` | `varchar(320)` | yes | Email для входа и связи. |
| `username` | `varchar(64)` | yes | Уникальный username. |
| `display_name` | `varchar(128)` | no | Отображаемое имя. |
| `enabled` | `boolean` | yes | Может ли пользователь входить в систему. |
| `created_at` | `timestamptz` | yes | Дата регистрации. |
| `updated_at` | `timestamptz` | yes | Дата последнего изменения профиля. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `uk_users_email_lower` | `lower(email)` unique | Login и защита от дублей с разным регистром. |
| `uk_users_username_lower` | `lower(username)` unique | Публичный username без дублей по регистру. |
| `idx_users_created_at` | `created_at` | Админские списки и аудит. |

### `credentials`

Парольные credentials пользователя.

| Column | Type | Required | Описание |
|---|---|---|---|
| `user_id` | `uuid` | yes, PK, FK `users.user_id` | Пользователь. |
| `password_hash` | `text` | yes | Hash пароля, не raw password. |
| `password_algo` | `varchar(64)` | yes | Например `bcrypt`. |
| `password_updated_at` | `timestamptz` | yes | Когда пароль был установлен или изменен. |

### `user_roles`

Роли из `UserRole`: `USER`, `ADMIN`.

| Column | Type | Required | Описание |
|---|---|---|---|
| `user_id` | `uuid` | yes, FK `users.user_id` | Пользователь. |
| `role` | `varchar(64)` | yes | Роль пользователя. |
| `created_at` | `timestamptz` | yes | Когда роль выдана. |

Primary key: `(user_id, role)`.

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `idx_user_roles_role` | `role` | Поиск пользователей по роли. |

### `refresh_tokens`

Долгоживущие refresh-токены. В БД хранится только hash токена.

| Column | Type | Required | Описание |
|---|---|---|---|
| `refresh_token_id` | `uuid` | yes, PK | ID refresh token record. |
| `user_id` | `uuid` | yes, FK `users.user_id` | Владелец токена. |
| `token_hash` | `text` | yes | Hash refresh token. |
| `created_at` | `timestamptz` | yes | Когда создан. |
| `expires_at` | `timestamptz` | yes | Когда истекает. |
| `last_used_at` | `timestamptz` | no | Последнее успешное использование. |
| `revoked_at` | `timestamptz` | no | Отзыв токена. |
| `replaced_by_token_id` | `uuid` | no | Новый refresh token при rotation. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `uk_refresh_tokens_token_hash` | `token_hash` unique | Поиск токена при refresh. |
| `idx_refresh_tokens_user_active` | `user_id, expires_at` where `revoked_at is null` | Активные токены пользователя. |

### `avatar_metadata`

Метаданные загруженных аватаров. Файл лежит в object storage bucket `avatars`.

| Column | Type | Required | Описание |
|---|---|---|---|
| `avatar_id` | `uuid` | yes, PK | ID аватара. |
| `user_id` | `uuid` | yes, FK `users.user_id` | Владелец аватара. |
| `object_key` | `text` | yes | Ключ объекта в bucket `avatars`. |
| `url` | `text` | no | Публичный или signed URL, если сервис его материализует. |
| `content_type` | `varchar(128)` | yes | MIME type. |
| `size_bytes` | `bigint` | yes | Размер файла. |
| `uploaded_at` | `timestamptz` | yes | Время загрузки. |
| `active` | `boolean` | yes | Используется ли как текущий аватар. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `uk_avatar_object_key` | `object_key` unique | Защита от дублей object key. |
| `idx_avatar_user_uploaded` | `user_id, uploaded_at desc` | История аватаров пользователя. |
| `idx_avatar_user_active` | `user_id` where `active = true` | Быстрый текущий аватар. |

### `outbox_events` (Рамиль, пока без аутбокса в auth сервисе)

Опционально для будущих событий профиля, например `USER_REGISTERED`, `PROFILE_UPDATED`. В текущем взаимодействии другие сервисы могут получать публичный профиль через HTTP, но outbox пригодится для проекций duel-service.

## Problem Service

`problem-service` владеет архивом задач, тегами, условиями, версиями задач, лимитами и ссылками на test archive. Он не хранит submissions, рейтинг и пользователей, кроме идентификаторов автора/фильтра.

Рекомендуемое хранилище:

| Storage | Name |
|---|---|
| PostgreSQL database | `problem_db` |
| PostgreSQL schema | `problem` |
| Object storage bucket | `problem-assets` |
| Object storage bucket | `problem-test-archives` |

### `problems`

Стабильная сущность задачи. Версионируемые детали лежат в `problem_versions`.

| Column | Type | Required | Описание |
|---|---|---|---|
| `problem_id` | `uuid` | yes, PK | ID задачи. |
| `slug` | `varchar(128)` | yes | Человекочитаемый уникальный slug. |
| `title` | `varchar(256)` | yes | Название задачи. |
| `difficulty` | `varchar(64)` | yes | `EASY`, `MEDIUM`, `HARD`. |
| `status` | `varchar(64)` | yes | `DRAFT`, `PUBLISHED`, `ARCHIVED`. |
| `author_user_id` | `uuid` | no | ID автора из auth-service, без FK в чужую БД. |
| `accepted_count` | `bigint` | yes | Количество accepted submissions. |
| `attempts_count` | `bigint` | yes | Количество попыток. |
| `published_at` | `timestamptz` | no | Когда задача опубликована. |
| `created_at` | `timestamptz` | yes | Когда создана. |
| `updated_at` | `timestamptz` | yes | Когда изменена. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `uk_problems_slug` | `slug` unique | Получение задачи по slug. |
| `idx_problems_status_difficulty` | `status, difficulty` | Архив задач и фильтры. |
| `idx_problems_published_at` | `published_at desc` | Сортировка опубликованных задач. |
| `idx_problems_author` | `author_user_id` | Задачи автора. |

### `problem_versions`

Версия условия, checker, лимитов и набора тестов.

| Column | Type | Required | Описание |
|---|---|---|---|
| `problem_version_id` | `uuid` | yes, PK | ID версии задачи. |
| `problem_id` | `uuid` | yes, FK `problems.problem_id` | Родительская задача. |
| `version_number` | `integer` | yes | Номер версии внутри задачи. |
| `statement` | `text` | yes | Условие задачи. |
| `input_spec` | `text` | no | Описание входных данных. |
| `output_spec` | `text` | no | Описание выходных данных. |
| `statement_object_key` | `text` | no | Ключ richer statement/assets в `problem-assets`. |
| `tests_manifest_object_key` | `text` | no | Ключ manifest в `problem-test-archives`. |
| `checker_type` | `varchar(64)` | yes | `STANDARD` или `CUSTOM`. |
| `custom_checker_object_key` | `text` | no | Код/бинарь custom checker в object storage. |
| `time_limit_ms` | `bigint` | yes | Time limit для judge. |
| `memory_limit_kb` | `bigint` | yes | Memory limit для judge. |
| `output_limit_bytes` | `bigint` | yes | Output limit. |
| `active` | `boolean` | yes | Активная версия для новых submissions. |
| `created_at` | `timestamptz` | yes | Когда версия создана. |

Ограничения:

| Constraint | Описание |
|---|---|
| `uk_problem_versions_problem_number` | unique `(problem_id, version_number)`. |
| `uk_problem_versions_one_active` | partial unique `(problem_id)` where `active = true`. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `idx_problem_versions_problem_active` | `problem_id, active` | Быстро найти активную версию. |
| `idx_problem_versions_created_at` | `created_at desc` | История версий. |

### `problem_examples`

Примеры из условия.

| Column | Type | Required | Описание |
|---|---|---|---|
| `example_id` | `uuid` | yes, PK | ID примера. |
| `problem_version_id` | `uuid` | yes, FK `problem_versions.problem_version_id` | Версия задачи. |
| `order_no` | `integer` | yes | Порядок отображения. |
| `input` | `text` | yes | Входные данные примера. |
| `expected_output` | `text` | yes | Ожидаемый вывод. |
| `explanation` | `text` | no | Пояснение. |

Ограничение: unique `(problem_version_id, order_no)`.

### `tags`

Справочник тегов.

| Column | Type | Required | Описание |
|---|---|---|---|
| `tag_id` | `uuid` | yes, PK | ID тега. |
| `name` | `varchar(64)` | yes | Название тега. |
| `color` | `varchar(32)` | no | Цвет для UI, например hex. |
| `created_at` | `timestamptz` | yes | Когда создан. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `uk_tags_name_lower` | `lower(name)` unique | Уникальность названия без учета регистра. |

### `problem_tags`

Связь многие-ко-многим между задачами и тегами.

| Column | Type | Required | Описание |
|---|---|---|---|
| `problem_id` | `uuid` | yes, FK `problems.problem_id` | Задача. |
| `tag_id` | `uuid` | yes, FK `tags.tag_id` | Тег. |

Primary key: `(problem_id, tag_id)`.

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `idx_problem_tags_tag` | `tag_id, problem_id` | Фильтр задач по тегу. |

### `test_suites`

Метаданные набора тестов. Сами тесты лежат в bucket `problem-test-archives`.

| Column | Type | Required | Описание |
|---|---|---|---|
| `test_suite_id` | `uuid` | yes, PK | ID набора тестов. |
| `problem_version_id` | `uuid` | yes, FK `problem_versions.problem_version_id` | Версия задачи. |
| `object_key` | `text` | yes | Архив тестов или manifest в object storage. |
| `checksum` | `varchar(128)` | yes | Контрольная сумма архива. |
| `visible_sample_tests_count` | `integer` | yes | Сколько тестов можно показать в public verdict. |
| `created_at` | `timestamptz` | yes | Когда загружено. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `uk_test_suites_object_key` | `object_key` unique | Защита от дублей object key. |
| `idx_test_suites_problem_version` | `problem_version_id` | Получить тесты версии. |

### `outbox_events`

Опционально для событий `PROBLEM_PUBLISHED`, `PROBLEM_UPDATED`, если дальше появятся read-models или инвалидация cache.

### Redis Keys

| Key pattern | Type | TTL | Назначение |
|---|---|---|---|
| `problem:details:{problemId}` | json | 1-10 min | Cache `ProblemDetailsDto`. |
| `problem:list:{filterHash}` | json | 30-120 sec | Cache архива задач. |
| `problem:tags` | json | 5-30 min | Cache `List<TagDto>`. |

## Duel Service

`duel-service` владеет матчмейкингом, дуэлями, участниками, рейтингом, сезонами, leaderboard и WebSocket room state. Публичные данные профиля пользователя можно хранить как projection, но источник истины профиля остается в `auth-service`.

Рекомендуемое хранилище:

| Storage | Name |
|---|---|
| PostgreSQL database | `duel_db` |
| PostgreSQL schema | `duel` |
| Redis | matchmaking, presence, code snapshots, timers |

### `seasons`

Сезоны рейтинга.

| Column | Type | Required | Описание |
|---|---|---|---|
| `season_id` | `uuid` | yes, PK | ID сезона. |
| `name` | `varchar(128)` | yes | Название сезона. |
| `starts_at` | `timestamptz` | yes | Начало сезона. |
| `ends_at` | `timestamptz` | yes | Конец сезона. |
| `active` | `boolean` | yes | Активный сезон. |
| `created_at` | `timestamptz` | yes | Когда создан. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `idx_seasons_active` | `active` where `active = true` | Найти текущий сезон. |
| `idx_seasons_dates` | `starts_at, ends_at` | Проверка попадания даты в сезон. |

### `queue_presets`

Настройки очередей матчмейкинга.

| Column | Type | Required | Описание |
|---|---|---|---|
| `preset_id` | `uuid` | yes, PK | ID preset. |
| `name` | `varchar(128)` | yes | Название очереди. |
| `mode` | `varchar(64)` | yes | `RATED`, `UNRATED`, `PRACTICE`. |
| `duel_duration_seconds` | `bigint` | yes | Длительность дуэли. |
| `initial_rating_window` | `integer` | yes | Стартовый разброс рейтинга для поиска. |
| `max_rating_window` | `integer` | yes | Максимальный разброс рейтинга. |
| `active` | `boolean` | yes | Доступна ли очередь. |
| `created_at` | `timestamptz` | yes | Когда создана. |
| `updated_at` | `timestamptz` | yes | Когда изменена. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `idx_queue_presets_active_mode` | `active, mode` | Список доступных очередей. |

### `duel_problem_pools`

Пулы задач для очередей/сезонов.

| Column | Type | Required | Описание |
|---|---|---|---|
| `pool_id` | `uuid` | yes, PK | ID pool. |
| `name` | `varchar(128)` | yes | Название pool. |
| `season_id` | `uuid` | no, FK `seasons.season_id` | Сезон, если pool сезонный. |
| `preset_id` | `uuid` | no, FK `queue_presets.preset_id` | Очередь, если pool привязан к preset. |
| `active` | `boolean` | yes | Доступен ли pool. |
| `created_at` | `timestamptz` | yes | Когда создан. |

### `duel_problem_pool_items`

Состав pool. `problem_id` хранится как значение из `problem-service`, без FK в чужую БД.

| Column | Type | Required | Описание |
|---|---|---|---|
| `pool_id` | `uuid` | yes, FK `duel_problem_pools.pool_id` | Pool. |
| `problem_id` | `uuid` | yes | ID задачи из `problem-service`. |
| `created_at` | `timestamptz` | yes | Когда добавлено. |

Primary key: `(pool_id, problem_id)`.

### `matchmaking_tickets`

Тикеты поиска матча.

| Column | Type | Required | Описание |
|---|---|---|---|
| `ticket_id` | `uuid` | yes, PK | ID тикета. |
| `user_id` | `uuid` | yes | Пользователь из auth-service. |
| `preset_id` | `uuid` | yes, FK `queue_presets.preset_id` | Очередь. |
| `mode` | `varchar(64)` | yes | Режим дуэли. |
| `status` | `varchar(64)` | yes | `WAITING`, `MATCHED`, `CANCELLED`, `EXPIRED`. |
| `current_rating` | `integer` | no | Рейтинг на момент входа в очередь. |
| `created_at` | `timestamptz` | yes | Когда тикет создан. |
| `expires_at` | `timestamptz` | yes | Когда тикет истекает. |
| `matched_duel_id` | `uuid` | no | Дуэль, если тикет сматчен. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `idx_matchmaking_tickets_waiting` | `preset_id, mode, current_rating, created_at` where `status = 'WAITING'` | Поиск кандидатов для матча. |
| `idx_matchmaking_tickets_user_active` | `user_id` where `status = 'WAITING'` | Запретить несколько активных тикетов. |
| `idx_matchmaking_tickets_expires` | `expires_at` where `status = 'WAITING'` | Expire job. |

### `duels`

Комната дуэли и ее итог.

| Column | Type | Required | Описание |
|---|---|---|---|
| `duel_id` | `uuid` | yes, PK | ID дуэли. |
| `season_id` | `uuid` | no, FK `seasons.season_id` | Сезон rated-дуэли. |
| `preset_id` | `uuid` | no, FK `queue_presets.preset_id` | Очередь, из которой создана дуэль. |
| `problem_id` | `uuid` | yes | ID задачи из problem-service. |
| `problem_version_id` | `uuid` | yes | Версия задачи из problem-service. |
| `status` | `varchar(64)` | yes | `MATCHMAKING`, `READY`, `IN_PROGRESS`, `FINISHED`, `CANCELLED`, `EXPIRED`. |
| `mode` | `varchar(64)` | yes | `RATED`, `UNRATED`, `PRACTICE`. |
| `winner_user_id` | `uuid` | no | Победитель, если есть. |
| `started_at` | `timestamptz` | no | Когда началась. |
| `ends_at` | `timestamptz` | no | Плановое окончание. |
| `finished_at` | `timestamptz` | no | Фактическое окончание. |
| `created_at` | `timestamptz` | yes | Когда создана. |
| `updated_at` | `timestamptz` | yes | Когда изменена. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `idx_duels_status_ends` | `status, ends_at` | Завершение истекших дуэлей. |
| `idx_duels_problem` | `problem_id` | Аналитика по задачам. |
| `idx_duels_season_finished` | `season_id, finished_at desc` | История сезона. |

### `duel_participants`

Участники дуэли. Содержит snapshot публичного профиля на момент матча, чтобы история не менялась при смене username/avatar.

| Column | Type | Required | Описание |
|---|---|---|---|
| `duel_id` | `uuid` | yes, FK `duels.duel_id` | Дуэль. |
| `user_id` | `uuid` | yes | Пользователь из auth-service. |
| `username` | `varchar(64)` | no | Snapshot username. |
| `display_name` | `varchar(128)` | no | Snapshot display name. |
| `avatar_url` | `text` | no | Snapshot avatar URL. |
| `rating_before` | `integer` | no | Рейтинг до дуэли. |
| `rating_after` | `integer` | no | Рейтинг после дуэли. |
| `outcome` | `varchar(64)` | no | `WIN`, `LOSS`, `DRAW`, `CANCELLED`. |
| `accepted_submission_id` | `uuid` | no | Submission, который решил задачу. |
| `joined_at` | `timestamptz` | no | Когда пользователь вошел в комнату. |

Primary key: `(duel_id, user_id)`.

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `idx_duel_participants_user` | `user_id, duel_id` | История дуэлей пользователя. |
| `idx_duel_participants_submission` | `accepted_submission_id` | Связать accepted submission с дуэлью. |

### `rating_history`

История изменений рейтинга.

| Column | Type | Required | Описание |
|---|---|---|---|
| `rating_history_id` | `uuid` | yes, PK | ID изменения. |
| `user_id` | `uuid` | yes | Пользователь. |
| `duel_id` | `uuid` | no, FK `duels.duel_id` | Дуэль-источник изменения. |
| `season_id` | `uuid` | no, FK `seasons.season_id` | Сезон. |
| `old_rating` | `integer` | yes | Было. |
| `new_rating` | `integer` | yes | Стало. |
| `delta` | `integer` | yes | Изменение рейтинга. |
| `reason` | `varchar(64)` | yes | `DUEL_WIN`, `DUEL_LOSS`, `DUEL_DRAW`, `SEASON_RESET`, `ADMIN_ADJUSTMENT`. |
| `changed_at` | `timestamptz` | yes | Когда изменилось. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `idx_rating_history_user_time` | `user_id, changed_at desc` | История рейтинга пользователя. |
| `idx_rating_history_season` | `season_id, changed_at desc` | Аудит сезона. |

### `user_duel_profiles`

Read-model для `UserDuelProfileDto` и leaderboard. Это данные duel-domain, а не auth-domain.

| Column | Type | Required | Описание |
|---|---|---|---|
| `user_id` | `uuid` | yes, PK | Пользователь. |
| `username` | `varchar(64)` | no | Projection из auth-service. |
| `display_name` | `varchar(128)` | no | Projection из auth-service. |
| `avatar_url` | `text` | no | Projection из auth-service. |
| `rating` | `integer` | yes | Текущий рейтинг. |
| `rank` | `integer` | no | Материализованный rank, если пересчитывается job. |
| `wins` | `integer` | yes | Победы. |
| `losses` | `integer` | yes | Поражения. |
| `draws` | `integer` | yes | Ничьи. |
| `solved_problems` | `integer` | yes | Количество решенных задач в дуэлях. |
| `updated_at` | `timestamptz` | yes | Когда projection обновлена. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `idx_user_duel_profiles_rating` | `rating desc, user_id` | Leaderboard. |
| `idx_user_duel_profiles_rank` | `rank` | Быстро получить место. |

### `duel_event_log`

Аудит событий комнаты. Не заменяет outbox, а помогает восстанавливать ход дуэли.

| Column | Type | Required | Описание |
|---|---|---|---|
| `event_id` | `uuid` | yes, PK | ID события. |
| `duel_id` | `uuid` | yes, FK `duels.duel_id` | Дуэль. |
| `event_type` | `varchar(128)` | yes | Например `ROOM_CREATED`, `CODE_SNAPSHOT`, `SUBMISSION_ACCEPTED`, `DUEL_FINISHED`. |
| `payload` | `jsonb` | yes | Детали события. |
| `created_at` | `timestamptz` | yes | Когда записано. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `idx_duel_event_log_duel_time` | `duel_id, created_at` | Хронология дуэли. |

### `outbox_events`

Публикует `duel-events.v1` и notification commands:

| Event | Transport | Payload |
|---|---|---|
| `DUEL_FINISHED` | Kafka `duel-events.v1` | `EventEnvelope<DuelFinishedEvent>` |
| `MATCH_FOUND` | RabbitMQ `notifications.topic` | `NotificationCommand` |
| `DUEL_FINISHED_NOTIFICATION` | RabbitMQ `notifications.topic` | `NotificationCommand` |
| `RATING_CHANGED` | RabbitMQ `notifications.topic` | `NotificationCommand` |

### Redis Keys

| Key pattern | Type | TTL | Назначение |
|---|---|---|---|
| `matchmaking:{presetId}:{mode}` | sorted set | тикет TTL | Очередь поиска. Score может быть рейтингом или временем. |
| `matchmaking:ticket:{ticketId}` | json | тикет TTL | Быстрое состояние тикета. |
| `presence:{duelId}:{userId}` | string | 15-60 sec | Online presence в комнате. |
| `code_snapshot:{duelId}:{userId}` | json | до конца дуэли + короткий grace period | Последний `CodeSnapshotMessage`. |
| `duel_timer:{duelId}` | string | до конца дуэли | Таймер завершения. |

## Submission Service

`submission-service` владеет submissions, состоянием judge-пайплайна, полным `JudgeResult`, public verdict и domain event `SubmissionEvaluatedEvent`. Он не выполняет код сам, а отправляет `ComputingTask` в Kafka topic `submissions`.

Рекомендуемое хранилище:

| Storage | Name |
|---|---|
| PostgreSQL database | `submission_db` |
| PostgreSQL schema | `submission` |
| Object storage bucket | `judge-logs` опционально |

### `submissions`

Главная таблица попыток.

| Column | Type | Required | Описание |
|---|---|---|---|
| `submission_id` | `uuid` | yes, PK | ID submission. |
| `user_id` | `uuid` | yes | Пользователь из auth-service. |
| `problem_id` | `uuid` | yes | Задача из problem-service. |
| `problem_version_id` | `uuid` | yes | Версия задачи. |
| `duel_id` | `uuid` | no | Дуэль, если `mode = DUEL`. |
| `mode` | `varchar(64)` | yes | `DUEL` или `PRACTICE`. |
| `language` | `varchar(64)` | yes | `JAVA`, `CPP`, `PYTHON`, `RUST`, `RUBY`, `JAVASCRIPT`. |
| `source_code` | `text` | yes | Код пользователя на момент отправки. |
| `status` | `varchar(64)` | yes | `CREATED`, `QUEUED`, `JUDGING`, `JUDGED`, `FAILED`, `CANCELLED`. |
| `overall_status` | `varchar(64)` | no | Итоговый `JudgeStatus`, если уже оценено. |
| `accepted` | `boolean` | no | true только для `ACCEPTED`. |
| `created_at` | `timestamptz` | yes | Когда создано. |
| `queued_at` | `timestamptz` | no | Когда отправлено в Kafka `submissions`. |
| `judged_at` | `timestamptz` | no | Когда получен `JudgeResult`. |
| `updated_at` | `timestamptz` | yes | Когда изменено. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `idx_submissions_user_created` | `user_id, created_at desc` | История пользователя. |
| `idx_submissions_problem_created` | `problem_id, created_at desc` | История по задаче. |
| `idx_submissions_duel` | `duel_id` | Submission внутри дуэли. |
| `idx_submissions_status` | `status, created_at` | Поиск зависших/активных submissions. |
| `idx_submissions_problem_accepted` | `problem_id, user_id` where `accepted = true` | Подсчет решенных задач. |

### `execution_snapshots`

Снимок execution context на момент отправки в judge. Нужен, чтобы изменение задачи после отправки не меняло старую попытку.

| Column | Type | Required | Описание |
|---|---|---|---|
| `submission_id` | `uuid` | yes, PK, FK `submissions.submission_id` | Submission. |
| `problem_version_id` | `uuid` | yes | Версия задачи. |
| `time_limit_ms` | `bigint` | yes | Лимит времени. |
| `memory_limit_kb` | `bigint` | yes | Лимит памяти. |
| `checker_type` | `varchar(64)` | yes | `STANDARD` или `CUSTOM`. |
| `custom_checker_code` | `text` | no | Код custom checker, если передается в judge. |
| `test_archive_object_key` | `text` | no | Архив тестов в object storage. |
| `visible_sample_tests_count` | `integer` | yes | Сколько тестов можно показывать в public verdict. |
| `store_full_judge_log` | `boolean` | yes | Сохранять ли полные детали judge. |
| `rated_mode` | `boolean` | yes | Если true, наружу нельзя отдавать полный `JudgeResult`. |
| `snapshot_json` | `jsonb` | no | Полный снимок контекста, если нужно сохранить исходный DTO. |
| `created_at` | `timestamptz` | yes | Когда создан снимок. |

### `judge_results`

Полный результат от `online-judge`. Эта таблица внутренняя для `submission-service`.

| Column | Type | Required | Описание |
|---|---|---|---|
| `submission_id` | `uuid` | yes, PK, FK `submissions.submission_id` | Submission. |
| `overall_status` | `varchar(64)` | yes | `JudgeStatus`. |
| `max_time_ms` | `bigint` | no | Максимальное время по тестам. |
| `max_memory_kb` | `bigint` | no | Максимальная память по тестам. |
| `compilation_error` | `text` | no | Ошибка компиляции. |
| `raw_result_json` | `jsonb` | yes | Полный `JudgeResult`. |
| `judge_log_object_key` | `text` | no | Ключ лога в `judge-logs`, если используется. |
| `received_at` | `timestamptz` | yes | Когда получен результат. |

### `test_case_results`

Результаты отдельных тестов из `JudgeResult.testCaseResults`.

| Column | Type | Required | Описание |
|---|---|---|---|
| `test_case_result_id` | `uuid` | yes, PK | ID результата теста. |
| `submission_id` | `uuid` | yes, FK `submissions.submission_id` | Submission. |
| `order_no` | `integer` | yes | Порядок теста. |
| `status` | `varchar(64)` | yes | `JudgeStatus` теста. |
| `time_ms` | `bigint` | no | Время выполнения. |
| `memory_kb` | `bigint` | no | Память. |
| `input` | `text` | no | Внутренние данные теста. В rated UI не отдавать. |
| `actual_output` | `text` | no | Фактический вывод. В rated UI не отдавать. |
| `expected_output` | `text` | no | Ожидаемый вывод. В rated UI не отдавать. |
| `message` | `text` | no | Сообщение judge/checker. |
| `visible` | `boolean` | yes | Можно ли показывать в `PublicTestCaseResult`. |

Ограничение: unique `(submission_id, order_no)`.

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `idx_test_case_results_submission` | `submission_id, order_no` | Построить verdict. |

### `submission_public_views`

Материализованный public verdict для UI. Rated UI получает эти данные, а не полный `JudgeResult`.

| Column | Type | Required | Описание |
|---|---|---|---|
| `submission_id` | `uuid` | yes, PK, FK `submissions.submission_id` | Submission. |
| `overall_status` | `varchar(64)` | yes | Итоговый verdict. |
| `passed_tests` | `integer` | yes | Количество пройденных тестов. |
| `total_tests` | `integer` | yes | Общее количество тестов. |
| `max_time_ms` | `bigint` | no | Максимальное время. |
| `max_memory_kb` | `bigint` | no | Максимальная память. |
| `compilation_error` | `text` | no | Ошибка компиляции, если ее можно показать. |
| `visible_tests_json` | `jsonb` | no | `List<PublicTestCaseResult>`. |
| `updated_at` | `timestamptz` | yes | Когда обновлено. |

### `processed_judge_results`

Idempotency table для Kafka consumer `submission-results`.

| Column | Type | Required | Описание |
|---|---|---|---|
| `submission_id` | `uuid` | yes, PK | Submission/result key. |
| `result_event_hash` | `varchar(128)` | no | Hash payload, если нужно обнаруживать разные дубли. |
| `processed_at` | `timestamptz` | yes | Когда обработано. |

### `outbox_events`

Публикует `submission-events.v1`.

| Event | Transport | Payload |
|---|---|---|
| `SUBMISSION_EVALUATED` | Kafka `submission-events.v1` | `EventEnvelope<SubmissionEvaluatedEvent>` |

### Redis Keys

| Key pattern | Type | TTL | Назначение |
|---|---|---|---|
| `submission:summary:{submissionId}` | json | 10-60 sec | Cache `SubmissionSummaryDto`. |
| `submission:verdict:{submissionId}` | json | 10-60 sec | Cache `PublicSubmissionVerdict`. |

## Online Judge

`online-judge` не владеет пользовательскими или доменными данными. Он получает `ComputingTask` из Kafka topic `submissions`, выполняет код в sandbox и публикует `JudgeResult` в Kafka topic `submission-results`.

### Durable Storage

Durable-БД в v1 не нужна.

### Локальное временное хранилище

| Path/storage | Назначение | Очистка |
|---|---|---|
| temp work dir | Исходники, compiled artifacts, stdin/stdout/stderr одного запуска. | После каждого запуска в `Sandbox.cleanup()`. |
| `/var/local/lib/isolate` | isolate boxes. | `isolate --cleanup` после выполнения. |
| `/run/isolate/locks` | locks isolate. | Управляется isolate. |
| `/usr/local/etc/isolate` или `/etc/isolate` | config isolate. | Не runtime-data. |

### Внутренние runtime objects

Эти объекты не надо хранить в contracts и не надо сохранять в БД:

| Object | Назначение |
|---|---|
| `ExecutionResult` | Метрики одного запуска: exit code, stdout/stderr, time, memory. |
| `CompilationResult` | Результат компиляции. |
| sandbox metadata | Временные файлы isolate и metadata выполнения. |

### Таблицы

Таблиц нет. Для diagnostics позже можно добавить отдельное observability-хранилище, но бизнес-источником истины должен оставаться `submission-service`.

## Notification Service

`notification-service` владеет inbox пользователя: уведомления, статус прочтения, попытки доставки и idempotency обработки RabbitMQ commands.

Рекомендуемое хранилище:

| Storage | Name |
|---|---|
| PostgreSQL database | `notification_db` |
| PostgreSQL schema | `notification` |

### `notifications`

Основная таблица уведомлений.

| Column | Type | Required | Описание |
|---|---|---|---|
| `notification_id` | `uuid` | yes, PK | ID уведомления. Может приходить из `NotificationCommand`. |
| `user_id` | `uuid` | yes | Получатель из auth-service. |
| `type` | `varchar(64)` | yes | `MATCH_FOUND`, `DUEL_FINISHED`, `RATING_CHANGED`, `PRACTICE_JUDGED`, `PROFILE_UPDATED`, `SEASON_STARTED`, `SYSTEM`. |
| `status` | `varchar(64)` | yes | `NEW`, `READ`, `ARCHIVED`. |
| `title` | `varchar(256)` | yes | Заголовок. |
| `body` | `text` | no | Текст уведомления. |
| `payload` | `jsonb` | no | Дополнительные данные для UI navigation. |
| `created_at` | `timestamptz` | yes | Когда создано. |
| `read_at` | `timestamptz` | no | Когда прочитано. |
| `archived_at` | `timestamptz` | no | Когда архивировано. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `idx_notifications_user_created` | `user_id, created_at desc` | Inbox пользователя. |
| `idx_notifications_user_status_created` | `user_id, status, created_at desc` | Фильтр unread/read. |
| `idx_notifications_unread` | `user_id, created_at desc` where `status = 'NEW'` | Быстрый счетчик unread. |

### `notification_delivery_attempts`

История попыток доставки в live stream, email, push или другой канал.

| Column | Type | Required | Описание |
|---|---|---|---|
| `attempt_id` | `uuid` | yes, PK | ID попытки. |
| `notification_id` | `uuid` | yes, FK `notifications.notification_id` | Уведомление. |
| `channel` | `varchar(64)` | yes | Например `SSE`, `WEBSOCKET`, `EMAIL`, `PUSH`. |
| `status` | `varchar(64)` | yes | Например `SENT`, `FAILED`, `SKIPPED`. |
| `error_message` | `text` | no | Ошибка доставки. |
| `attempted_at` | `timestamptz` | yes | Когда была попытка. |

Индексы:

| Index | Columns | Назначение |
|---|---|---|
| `idx_delivery_attempts_notification` | `notification_id, attempted_at desc` | История доставки уведомления. |

### `processed_notification_commands`

Idempotency table для RabbitMQ consumer.

| Column | Type | Required | Описание |
|---|---|---|---|
| `message_id` | `varchar(256)` | yes, PK | Message id из RabbitMQ headers или вычисленный hash. |
| `notification_id` | `uuid` | no | Созданное уведомление. |
| `routing_key` | `varchar(256)` | no | Routing key, например `user.{userId}.match-found`. |
| `processed_at` | `timestamptz` | yes | Когда command обработан. |

### Redis Keys

| Key pattern | Type | TTL | Назначение |
|---|---|---|---|
| `notifications:stream:{userId}` | pub/sub или stream | короткий | Fanout live events для SSE/WebSocket nodes. |
| `notifications:unread:{userId}` | integer | 30-120 sec | Cache unread count. |

## Cross-Service References

| Field | Owner service | Где хранится как ссылка | Правило |
|---|---|---|---|
| `user_id` | `auth-service` | `duel-service`, `submission-service`, `notification-service`, `problem-service.author_user_id` | Не FK между БД. При необходимости сервис запрашивает профиль через auth-service или хранит projection. |
| `problem_id` | `problem-service` | `duel-service`, `submission-service` | Не FK между БД. Перед созданием submission/duel проверять через problem-service. |
| `problem_version_id` | `problem-service` | `duel-service`, `submission-service` | Использовать snapshot, чтобы старые submissions не менялись. |
| `duel_id` | `duel-service` | `submission-service` | Submission-service хранит ссылку, duel-service потребляет `SubmissionEvaluatedEvent`. |
| `submission_id` | `submission-service` | `duel-service.accepted_submission_id`, `online-judge` messages | Источник истины - submission-service. |
| `notification_id` | `notification-service` | producers of `NotificationCommand` могут задавать заранее | Notification-service отвечает за idempotency. |

## Миграции

- У каждого сервиса свои миграции: `src/main/resources/db/migration`.
- Рекомендуемый инструмент: Flyway или Liquibase. Важно выбрать один и использовать единообразно.
- Миграции одного сервиса не должны создавать таблицы в schema другого сервиса.
- Enum в БД хранить как `varchar`, чтобы добавление нового enum value не требовало сложной миграции PostgreSQL enum type.
- Для больших таблиц (`submissions`, `test_case_results`, `notifications`, `duel_event_log`) заранее держать индексы под основные API-запросы и jobs.

## Retention И Очистка

| Data | Retention rule |
|---|---|
| `refresh_tokens` | Удалять истекшие и revoked tokens старше заданного окна, например 30-90 дней. |
| `matchmaking_tickets` | Завершенные/истекшие можно архивировать или удалять после 7-30 дней. |
| Redis matchmaking/presence/code snapshots | TTL обязателен. Данные не должны жить бесконечно. |
| `judge_results` и `test_case_results` | Хранить по продуктовой политике. Для rated submissions не раскрывать скрытые данные наружу. |
| `notifications` | `ARCHIVED` можно удалять после длительного срока, например 180-365 дней. |
| online-judge temp dirs | Удалять после каждого запуска, плюс иметь startup-cleanup для старых директорий. |

## Минимальный порядок реализации

1. `auth-service`: `users`, `credentials`, `user_roles`, `refresh_tokens`, `avatar_metadata`.
2. `problem-service`: `problems`, `problem_versions`, `problem_examples`, `tags`, `problem_tags`, `test_suites`.
3. `submission-service`: `submissions`, `execution_snapshots`, `judge_results`, `test_case_results`, `submission_public_views`, `processed_judge_results`, `outbox_events`.
4. `duel-service`: `seasons`, `queue_presets`, `duel_problem_pools`, `duel_problem_pool_items`, `matchmaking_tickets`, `duels`, `duel_participants`, `rating_history`, `user_duel_profiles`, `duel_event_log`, `outbox_events`.
5. `notification-service`: `notifications`, `notification_delivery_attempts`, `processed_notification_commands`.
6. `gateway-service`: без БД, только config и Redis rate limit при необходимости.
7. `online-judge`: без БД, только корректная работа temp dirs и isolate cleanup.
