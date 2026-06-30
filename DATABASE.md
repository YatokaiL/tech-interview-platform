# База данных

## Архитектура

- **Один экземпляр PostgreSQL 16**, **9 логических схем** — по одной на сервис (паттерн «schema-per-service»).
- Схемы создаются init-скриптом `infra/postgres/init.sql` при первом старте контейнера.
- Внутри каждого сервиса миграции выполняет **Liquibase** (`db.changelog-master.yaml` → `changes/00X-*.sql`). Исключение: **user-service** работает с уже существующей схемой `auth_service` (свою Liquibase не имеет, таблицы создаёт auth-service).
- Hibernate во всех сервисах — `ddl-auto=validate`: схему меняет только Liquibase.
- Все идентификаторы — **UUID** (`gen_random_uuid()`), временные метки — `TIMESTAMPTZ`, по умолчанию `NOW()`.

### Соглашение про связи

Внешние ключи (`REFERENCES`) объявлены **только внутри одной схемы**. Связи между схемами — **логические**, через UUID без FK-констрейнта (классический микросервисный приём: схемы развязаны, удаление в одном сервисе не каскадирует в другой). В таблицах ниже такие «мягкие» ссылки помечены *(логическая ссылка)*.

### Сводка по схемам

| Схема | Таблиц | Назначение |
|---|---|---|
| `auth_service` | 4 | Пользователи, компании, роли, refresh-токены |
| `session_service` | 7 | Интервью-сессии, задачи, снимки кода, события |
| `execution_service` | 1 | История запусков кода в sandbox |
| `analysis_service` | 1 | История статического анализа кода |
| `review_service` | 2 | Ревью с оценками + комментарии к строкам |
| `report_service` | 1 | Метаданные сгенерированных отчётов (файлы в MinIO) |
| `subscription_service` | 3 | Тарифы, подписки компаний, платежи |
| `notification_service` | 1 | In-app уведомления |
| `analytics_service` | 1 | Event-store всех событий платформы |

---

## Схема `auth_service`

Используется **auth-service** и **user-service** (последний работает с теми же таблицами).

### `roles`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK, default `gen_random_uuid()` |
| `name` | VARCHAR(50) | **UNIQUE**, NOT NULL |
| `created_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Сид:** четыре фиксированных UUID для ролей `CANDIDATE`, `INTERVIEWER`, `HR_MANAGER`, `ADMIN`.

### `companies`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `name` | VARCHAR(255) | NOT NULL |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

### `users`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `email` | VARCHAR(255) | **UNIQUE**, NOT NULL |
| `password_hash` | VARCHAR(255) | NOT NULL (BCrypt) |
| `first_name`, `last_name` | VARCHAR(100) | nullable |
| `role_id` | UUID | NOT NULL, **FK → `roles(id)`** |
| `company_id` | UUID | **FK → `companies(id)`**, nullable (у кандидата может не быть) |
| `is_blocked` | BOOLEAN | NOT NULL, default FALSE |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Индексы:** `idx_users_email`.

### `refresh_tokens`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `user_id` | UUID | NOT NULL, **FK → `users(id)` ON DELETE CASCADE** |
| `token` | VARCHAR(500) | **UNIQUE**, NOT NULL |
| `expires_at` | TIMESTAMPTZ | NOT NULL |
| `revoked` | BOOLEAN | NOT NULL, default FALSE |
| `created_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Индексы:** `idx_refresh_tokens_user`, `idx_refresh_tokens_token`.

### Связи внутри схемы
```
roles 1 ──< users >── companies
users 1 ──< refresh_tokens (CASCADE on delete)
```

---

## Схема `session_service`

### `task_categories`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `name` | VARCHAR(100) | **UNIQUE**, NOT NULL |
| `description` | TEXT | nullable |
| `created_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Сид:** Алгоритмы, База данных, Системный дизайн, ООП, Concurrency.

### `tasks`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `title` | VARCHAR(255) | NOT NULL |
| `description` | TEXT | NOT NULL |
| `difficulty` | VARCHAR(50) | NOT NULL, default `'MEDIUM'`, **CHECK ∈ {EASY, MEDIUM, HARD}** |
| `language` | VARCHAR(50) | nullable |
| `initial_code`, `expected_output` | TEXT | nullable |
| `time_limit_minutes` | INTEGER | nullable |
| `company_id` | UUID | nullable *(логическая ссылка на `auth_service.companies`)* |
| `created_by` | UUID | NOT NULL *(логическая ссылка на `auth_service.users`)* |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Индексы:** `idx_tasks_company`, `idx_tasks_created_by`.

### `task_categories_link` (M:N)
| Колонка | Тип | Ограничения |
|---|---|---|
| `task_id` | UUID | **FK → `tasks(id)` ON DELETE CASCADE** |
| `category_id` | UUID | **FK → `task_categories(id)` ON DELETE CASCADE** |

**PK:** `(task_id, category_id)` — связь many-to-many.

### `sessions`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `title` | VARCHAR(255) | NOT NULL |
| `company_id` | UUID | NOT NULL *(логическая ссылка)* |
| `interviewer_id` | UUID | NOT NULL *(логическая ссылка на users)* |
| `candidate_id` | UUID | nullable *(логическая ссылка)* |
| `task_id` | UUID | **FK → `tasks(id)`**, nullable |
| `language` | VARCHAR(50) | NOT NULL |
| `status` | VARCHAR(50) | NOT NULL, default `'SCHEDULED'`, **CHECK ∈ {SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED}** |
| `scheduled_at` | TIMESTAMPTZ | NOT NULL |
| `started_at`, `completed_at` | TIMESTAMPTZ | nullable |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Индексы:** по `company_id`, `interviewer_id`, `candidate_id`, `status`.

### `session_participants`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `session_id` | UUID | NOT NULL, **FK → `sessions(id)` ON DELETE CASCADE** |
| `user_id` | UUID | NOT NULL *(логическая ссылка)* |
| `role` | VARCHAR(50) | NOT NULL, **CHECK ∈ {INTERVIEWER, CANDIDATE}** |
| `joined_at`, `left_at` | TIMESTAMPTZ | nullable |

### `code_snapshots`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `session_id` | UUID | NOT NULL, **FK → `sessions(id)` ON DELETE CASCADE** |
| `code` | TEXT | NOT NULL |
| `language` | VARCHAR(50) | NOT NULL |
| `snapshot_type` | VARCHAR(50) | NOT NULL, default `'AUTO'`, **CHECK ∈ {AUTO, MANUAL, FINAL}** |
| `created_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Индексы:** `idx_code_snapshots_session`.

### `session_events`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `session_id` | UUID | NOT NULL, **FK → `sessions(id)` ON DELETE CASCADE** |
| `event_type` | VARCHAR(100) | NOT NULL (`chat`, `code-edit`, …) |
| `payload` | JSONB | произвольная структура события |
| `user_id` | UUID | nullable *(логическая ссылка)* |
| `created_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Индексы:** `idx_session_events_session`.

### Связи внутри схемы
```
task_categories >── task_categories_link ──< tasks
                                              │
                                              └─< sessions ─< session_participants
                                                          ├─< code_snapshots
                                                          └─< session_events
(все «─<» дочерних таблиц от sessions — ON DELETE CASCADE)
```

---

## Схема `execution_service`

### `code_executions`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `session_id` | UUID | NOT NULL *(логическая ссылка на `session_service.sessions`)* |
| `user_id` | UUID | NOT NULL *(логическая ссылка)* |
| `language` | VARCHAR(50) | NOT NULL |
| `code` | TEXT | NOT NULL |
| `stdin`, `stdout`, `stderr`, `error_message` | TEXT | nullable |
| `exit_code` | INTEGER | nullable |
| `status` | VARCHAR(50) | NOT NULL, default `'PENDING'`, **CHECK ∈ {PENDING, RUNNING, SUCCESS, FAILURE, TIMEOUT, ERROR}** |
| `duration_ms` | BIGINT | nullable |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Индексы:** `(session_id, created_at DESC)`, `(user_id, created_at DESC)`.

---

## Схема `analysis_service`

### `code_analyses`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `session_id` | UUID | NOT NULL *(логическая ссылка)* |
| `user_id` | UUID | NOT NULL *(логическая ссылка)* |
| `language` | VARCHAR(50) | NOT NULL |
| `code` | TEXT | NOT NULL |
| `status` | VARCHAR(50) | NOT NULL, default `'PENDING'`, **CHECK ∈ {PENDING, RUNNING, COMPLETED, ERROR}** |
| `lines_of_code` | INTEGER | nullable |
| `cyclomatic_complexity` | INTEGER | nullable |
| `cognitive_complexity` | INTEGER | nullable |
| `method_count` | INTEGER | nullable |
| `time_complexity` | VARCHAR(50) | nullable (Big-O оценка, добавлена миграцией 002) |
| `issues` | JSONB | список замечаний Checkstyle/PMD |
| `duration_ms` | BIGINT | nullable |
| `error_message` | TEXT | nullable |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Индексы:** `(session_id, created_at DESC)`, `(user_id, created_at DESC)`.

---

## Схема `review_service`

### `reviews`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `session_id` | UUID | NOT NULL, **UNIQUE** *(одно ревью на сессию)* *(логическая ссылка)* |
| `reviewer_id` | UUID | NOT NULL *(логическая ссылка на users)* |
| `candidate_id` | UUID | nullable *(логическая ссылка)* |
| `company_id` | UUID | NOT NULL *(логическая ссылка)* |
| `score_correctness` | SMALLINT | nullable, **CHECK 1..5** |
| `score_code_quality` | SMALLINT | nullable, **CHECK 1..5** |
| `score_performance` | SMALLINT | nullable, **CHECK 1..5** |
| `score_communication` | SMALLINT | nullable, **CHECK 1..5** |
| `verdict` | VARCHAR(50) | nullable, **CHECK ∈ {HIRE, NO_HIRE, MAYBE}** |
| `summary` | TEXT | nullable |
| `status` | VARCHAR(50) | NOT NULL, default `'DRAFT'`, **CHECK ∈ {DRAFT, SUBMITTED}** |
| `submitted_at` | TIMESTAMPTZ | nullable |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Индексы:** по `session_id`, `reviewer_id`, `candidate_id`, `company_id`.

### `review_comments`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `review_id` | UUID | NOT NULL, **FK → `reviews(id)` ON DELETE CASCADE** |
| `line_number` | INTEGER | nullable (комментарий к строке или общий) |
| `text` | TEXT | NOT NULL |
| `author_id` | UUID | NOT NULL *(логическая ссылка)* |
| `created_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Индексы:** `idx_comments_review`.

### Связи внутри схемы
```
reviews 1 ──< review_comments (CASCADE on delete)
```

---

## Схема `report_service`

### `reports`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `session_id` | UUID | NOT NULL *(логическая ссылка)* |
| `review_id` | UUID | nullable *(логическая ссылка на review_service.reviews)* |
| `company_id` | UUID | NOT NULL *(логическая ссылка)* |
| `format` | VARCHAR(20) | NOT NULL, **CHECK ∈ {PDF, JSON}** |
| `object_key` | VARCHAR(500) | NOT NULL — ключ объекта в MinIO (бакет `reports`) |
| `size_bytes` | BIGINT | nullable |
| `generated_by` | UUID | NOT NULL *(логическая ссылка)* |
| `created_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Индексы:** `(session_id, created_at DESC)`, по `review_id`, по `company_id`.

> Сами файлы PDF/JSON хранятся **в MinIO**, в таблице — только метаданные.

---

## Схема `subscription_service`

### `tariffs`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `code` | VARCHAR(50) | **UNIQUE**, NOT NULL (`START`, `TEAM`, `BUSINESS`, `ENTERPRISE`) |
| `name` | VARCHAR(100) | NOT NULL |
| `description` | TEXT | nullable |
| `price_cents` | INTEGER | NOT NULL — цена в копейках |
| `currency` | VARCHAR(8) | NOT NULL, default `'RUB'` |
| `max_sessions_per_month` | INTEGER | nullable — NULL = безлимит |
| `features` | TEXT | список фич через `;` |
| `sort_order` | INTEGER | NOT NULL, default 0 |
| `active` | BOOLEAN | NOT NULL, default TRUE |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Сид:** четыре тарифа START (490 ₽ / 10), TEAM (1490 ₽ / 50), BUSINESS (4490 ₽ / 200), ENTERPRISE (14900 ₽ / ∞).

### `subscriptions`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `company_id` | UUID | NOT NULL *(логическая ссылка)* |
| `tariff_id` | UUID | NOT NULL, **FK → `tariffs(id)`** |
| `status` | VARCHAR(30) | NOT NULL, default `'PENDING'`, **CHECK ∈ {PENDING, ACTIVE, EXPIRED, CANCELLED}** |
| `period_months` | INTEGER | NOT NULL, default 1 |
| `started_at`, `ends_at` | TIMESTAMPTZ | nullable |
| `auto_renew` | BOOLEAN | NOT NULL, default FALSE |
| `created_by` | UUID | NOT NULL *(логическая ссылка на users)* |
| `created_at`, `updated_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Индексы:** `(company_id, created_at DESC)`, по `status`.

### `payments`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `subscription_id` | UUID | NOT NULL, **FK → `subscriptions(id)` ON DELETE CASCADE** |
| `amount_cents` | INTEGER | NOT NULL |
| `currency` | VARCHAR(8) | NOT NULL, default `'RUB'` |
| `status` | VARCHAR(30) | NOT NULL, default `'PENDING'`, **CHECK ∈ {PENDING, SUCCEEDED, FAILED, REFUNDED}** |
| `gateway_ref` | VARCHAR(100) | nullable — внешний ID платежа |
| `paid_at` | TIMESTAMPTZ | nullable |
| `created_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Индексы:** `idx_payments_subscription`.

### Связи внутри схемы
```
tariffs 1 ──< subscriptions 1 ──< payments (CASCADE on delete от subscriptions)
```

---

## Схема `notification_service`

### `notifications`
| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `recipient_user_id` | UUID | NOT NULL *(логическая ссылка)* |
| `recipient_email` | VARCHAR(255) | nullable |
| `type` | VARCHAR(50) | NOT NULL (`WELCOME`, `SESSION_INVITE`, `REVIEW_READY`, `PAYMENT_RECEIVED`, …) |
| `title` | VARCHAR(255) | NOT NULL |
| `message` | TEXT | NOT NULL |
| `link` | VARCHAR(500) | nullable — внутренняя ссылка для клика |
| `email_sent` | BOOLEAN | NOT NULL, default FALSE |
| `read` | BOOLEAN | NOT NULL, default FALSE |
| `created_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Индексы:** `(recipient_user_id, created_at DESC)`, `(recipient_user_id, read)`.

---

## Схема `analytics_service`

### `analytics_events`
Универсальный event-store: одна строка на каждое событие из Kafka.

| Колонка | Тип | Ограничения |
|---|---|---|
| `id` | UUID | PK |
| `event_type` | VARCHAR(64) | NOT NULL — имя топика (`user-registered`, `session-created`, …) |
| `company_id` | UUID | nullable — извлекается из payload |
| `user_id` | UUID | nullable — actor (candidate/createdBy/reviewer/…) |
| `language` | VARCHAR(40) | nullable — для срезов по языку |
| `verdict` | VARCHAR(40) | nullable — для срезов по вердиктам |
| `value_cents` | BIGINT | nullable — `amountCents` из платежей |
| `payload` | JSONB | полное тело события |
| `occurred_at` | TIMESTAMPTZ | NOT NULL — из payload или `NOW()` |
| `received_at` | TIMESTAMPTZ | NOT NULL, default NOW() |

**Индексы:** по `event_type`, `occurred_at`, `company_id`.

---

## Логические связи между схемами

Все ссылки ниже — это UUID-колонки без FK-констрейнта (схемы развязаны):

```
auth_service.users  ←──── session_service.sessions.interviewer_id
                    ←──── session_service.sessions.candidate_id
                    ←──── session_service.tasks.created_by
                    ←──── session_service.session_participants.user_id
                    ←──── session_service.session_events.user_id
                    ←──── execution_service.code_executions.user_id
                    ←──── analysis_service.code_analyses.user_id
                    ←──── review_service.reviews.reviewer_id / candidate_id
                    ←──── review_service.review_comments.author_id
                    ←──── report_service.reports.generated_by
                    ←──── subscription_service.subscriptions.created_by
                    ←──── notification_service.notifications.recipient_user_id

auth_service.companies ←── session_service.sessions.company_id
                       ←── session_service.tasks.company_id
                       ←── review_service.reviews.company_id
                       ←── report_service.reports.company_id
                       ←── subscription_service.subscriptions.company_id

session_service.sessions ←── execution_service.code_executions.session_id
                         ←── analysis_service.code_analyses.session_id
                         ←── review_service.reviews.session_id
                         ←── report_service.reports.session_id

review_service.reviews   ←── report_service.reports.review_id
```

Идентификаторы синхронизируются через события Kafka (`user-registered`, `session-created`, …), а не через хард-FK.

---

## Итого

- **9 схем**, **21 таблица**, **полный список constraint'ов** — CHECK для всех enum-полей и оценок 1..5; UNIQUE на `email`, `roles.name`, `task_categories.name`, `tariffs.code`, `reviews.session_id` (одно ревью на сессию); `ON DELETE CASCADE` внутри схем где нужно.
- **JSONB** используется для гибких полей: `session_events.payload`, `code_analyses.issues`, `analytics_events.payload`.
- **Сидированные данные:** 4 роли, 5 категорий задач, 4 тарифа.
- **Файлы** (PDF/JSON отчётов) хранятся в MinIO, в БД — только метаданные с `object_key`.
