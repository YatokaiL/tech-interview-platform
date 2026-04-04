# Платформа технических интервью

Информационная система для автоматизации технических интервью и код-ревью.  
Дипломный проект. Запускается локально через `docker compose up`.

## Стек

| Слой | Технологии |
|------|-----------|
| Backend | Java 21, Spring Boot 3, Spring Cloud Gateway |
| БД | PostgreSQL 16 |
| Брокер | Apache Kafka 3.7 (KRaft) |
| Кэш | Redis 7 |
| Хранилище | MinIO |
| Frontend | React 18 + TypeScript, Monaco Editor, Yjs, Tailwind CSS |
| Sandbox | Docker-in-Docker |

## Требования

- Docker 24+
- Docker Compose v2

## Быстрый старт

```bash
docker compose up -d
```

Все сервисы поднимутся автоматически. Инициализация БД и Kafka-топиков выполняется при первом запуске.

## Сервисы и порты

| Сервис | Порт | Описание |
|--------|------|----------|
| PostgreSQL | 5432 | База данных |
| Redis | 6379 | Кэш, pub/sub |
| Kafka | 9092 | Брокер сообщений |
| MinIO API | 9000 | Объектное хранилище |
| MinIO Console | 9001 | Веб-интерфейс MinIO |
| MailHog SMTP | 1025 | Mock-почтовый сервер |
| MailHog UI | 8025 | Просмотр писем |
| API Gateway | 8080 | Точка входа (этап 2) |
| Frontend | 3000 | React SPA (этап 3) |

## Доступ к сервисам

- MinIO Console: http://localhost:9001 (minioadmin / minioadmin123)
- MailHog UI: http://localhost:8025

## Структура БД

6 схем PostgreSQL (один сервер, один экземпляр):

| Схема | Таблицы |
|-------|---------|
| `auth_service` | users, roles, refresh_tokens, companies |
| `subscription_service` | tariffs, subscriptions, payments |
| `session_service` | sessions, session_participants, tasks, code_snapshots, session_events, task_categories, task_categories_link |
| `review_service` | reviews, comments, code_executions, code_analysis |
| `report_service` | reports |
| `notification_service` | notifications |
| `analytics_service` | analytics_events, analytics_metrics_daily, audit_log |

## Kafka-топики

| Топик | Описание |
|-------|----------|
| `user-registered` | Новый пользователь зарегистрирован |
| `session-created` | Создана сессия собеседования |
| `session-completed` | Сессия завершена |
| `review-completed` | Код-ревью завершено |
| `payment-received` | Платёж подтверждён |
| `code-executed` | Код выполнен в sandbox |
| `notification-requested` | Запрос на отправку уведомления |
| `analytics-event` | Событие для аналитики |

## MinIO-бакеты

| Бакет | Назначение |
|-------|-----------|
| `reports` | PDF и JSON отчёты |
| `attachments` | Вложения |

## Этапы реализации

- [x] **Этап 1** — Инфраструктура: PostgreSQL, Redis, Kafka, MinIO
- [ ] **Этап 2** — auth-service + api-gateway
- [ ] **Этап 3** — user-service + frontend (базовый)
- [ ] **Этап 4** — session-service
- [ ] **Этап 5** — collaboration-service (WebSocket + Yjs)
- [ ] **Этап 6** — execution-service (Docker sandbox)
- [ ] **Этап 7** — analysis-service (JavaParser/Checkstyle/PMD)
- [ ] **Этап 8** — review-service + report-service
- [ ] **Этап 9** — subscription-service + notification-service
- [ ] **Этап 10** — analytics-service
