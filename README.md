# Платформа технических интервью

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F)
![React](https://img.shields.io/badge/React-18-61DAFB)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6)
![Kafka](https://img.shields.io/badge/Kafka-KRaft-231F20)
![Docker](https://img.shields.io/badge/Docker%20Compose-ready-2496ED)

Дипломная работа (2026): информационная система для проведения онлайн-собеседований с IT-специалистами.
Интервьюер и кандидат работают в одной комнате: общаются по видеосвязи, вместе пишут код в редакторе,
запускают его в изолированной песочнице, получают статический анализ, а по итогам интервьюер оформляет
код-ревью и выгружает отчёт в PDF.

Вся система (12 микросервисов, фронтенд и инфраструктура) поднимается локально одной командой `docker compose up`.

## Возможности

- **Комната интервью**: Monaco-редактор с синхронизацией в реальном времени, курсоры участников, чат с историей.
- **Видеозвонок 1:1** на WebRTC с демонстрацией экрана; во время работы с кодом звонок сворачивается в плавающее окно.
- **Запуск кода** на Java, Python, JavaScript, Go и C++ в отдельном Docker-контейнере без сети, с лимитами по CPU, памяти и времени.
- **Статический анализ** Java-кода: цикломатическая и когнитивная сложность, грубая оценка Big-O, Checkstyle, PMD.
- **Код-ревью** по рубрике (4 критерия + вердикт HIRE / MAYBE / NO_HIRE), комментарии к строкам кода.
- **Авто-ревью**: черновик формируется по результатам запуска и анализа через локальную LLM (Ollama, qwen2.5-coder), при недоступности модели используется эвристика.
- **Отчёты** по сессии в PDF и JSON, хранятся в MinIO.
- **Банк задач** с категориями и уровнями сложности.
- **Подписки** компаний: 4 тарифа и mock-оплата через webhook.
- **Уведомления**: письма (MailHog) и колокольчик в интерфейсе.
- **Аналитика** для администратора: дашборд с графиками и журнал событий.

## Роли

| Роль | Что может |
|------|-----------|
| Кандидат | Проходит интервью: редактор, запуск кода, видеозвонок |
| Интервьюер | Проводит сессии, работает с банком задач, пишет ревью, выгружает отчёты |
| HR-менеджер | Управляет подпиской компании, планирует сессии и назначает интервьюеров |
| Администратор | Управляет пользователями, видит аналитику и журнал событий |

## Архитектура

```
                    ┌─────────────┐
   Браузер ───────► │ api-gateway │ (8080)  ◄── единая точка входа, JWT, CORS, routing
                    └──────┬──────┘
        ┌──────────────────┼─────────────────────────────┐
        ▼                  ▼                              ▼
  auth (8081)        бизнес-сервисы              collaboration (8085)
  user (8082)        session (8084)              ↕ WebSocket (код + звонок)
  subscription(8083) execution (8086)
                     analysis (8087)
                     review (8088)
                     report (8089)
                     notification (8090)
                     analytics (8091)
        │
        └──► Kafka (события) ◄── analytics + notification
  Инфраструктура: PostgreSQL · Redis · Kafka · MinIO · MailHog · Ollama
```

- Сервисы общаются синхронно по REST и асинхронно через Kafka (`user-registered`, `session-created`,
  `session-completed`, `code-executed`, `analysis-completed`, `review-completed`, `payment-received` и др.).
- PostgreSQL один, но у каждого сервиса своя схема и свои миграции (Liquibase).
- Каждый сервис сам проверяет JWT (resource server), gateway отвечает за маршрутизацию, CORS и проксирование WebSocket.
- Долгие операции (запуск кода, анализ) выполняются асинхронно, фронтенд опрашивает статус.

| Сервис | Порт | Назначение |
|--------|------|------------|
| api-gateway | 8080 | Spring Cloud Gateway: маршрутизация, CORS, WebSocket-прокси |
| auth-service | 8081 | Регистрация, вход, JWT access/refresh, refresh-токены в Redis |
| user-service | 8082 | Профили, компании, роли, блокировка, поиск |
| subscription-service | 8083 | Тарифы, подписки, mock-платежи |
| session-service | 8084 | Сессии интервью, банк задач, снимки кода, события сессии |
| collaboration-service | 8085 | WebSocket-комнаты: редактор, чат, сигналинг звонка |
| execution-service | 8086 | Запуск кода в Docker-песочнице |
| analysis-service | 8087 | JavaParser, Checkstyle, PMD, оценка сложности |
| review-service | 8088 | Ревью, комментарии, авто-ревью через Ollama |
| report-service | 8089 | PDF/JSON-отчёты, хранение в MinIO |
| notification-service | 8090 | Email и in-app уведомления по событиям Kafka |
| analytics-service | 8091 | Хранилище событий, метрики, журнал аудита |

## Стек

| Слой | Технологии |
|------|-----------|
| Backend | Java 21, Spring Boot 3.5, Spring Cloud Gateway, Spring Security (OAuth2 Resource Server), Spring Data JPA, Liquibase |
| Интеграция | Apache Kafka (KRaft), REST, WebSocket |
| Хранение | PostgreSQL 16, Redis 7, MinIO |
| Анализ и запуск кода | JavaParser, Checkstyle, PMD, docker-java |
| LLM | Ollama, qwen2.5-coder |
| Отчёты | OpenPDF |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS, shadcn/ui, Monaco Editor, TanStack Query, Zustand, Recharts, WebRTC |
| Инфраструктура | Docker, Docker Compose, nginx, MailHog |

## Запуск

Нужны Docker 24+ и Docker Compose v2. Рекомендуется выделить Docker не меньше 8 ГБ памяти.

```bash
git clone https://github.com/YatokaiL/tech-interview-platform.git
cd tech-interview-platform
docker compose up -d --build
```

При первом запуске создаются схемы БД и Kafka-топики, скачивается модель для Ollama (~1 ГБ)
и образы языков для песочницы, поэтому первый старт занимает несколько минут.

| Что | Адрес |
|-----|-------|
| Приложение | http://localhost:3000 |
| API Gateway | http://localhost:8080 |
| MailHog (письма) | http://localhost:8025 |
| MinIO Console | http://localhost:9001 (minioadmin / minioadmin123) |

**Docker Desktop на Windows.** Если execution-service не может подключиться к Docker через сокет,
включите в настройках Docker Desktop *Expose daemon on tcp://localhost:2375* и укажите в `.env`:

```
DOCKER_HOST_URI=tcp://host.docker.internal:2375
```

**Администратор.** При регистрации доступны роли «Кандидат» и «HR-менеджер». Чтобы получить
администратора, зарегистрируйте пользователя и поменяйте ему роль в БД:

```bash
docker compose exec postgres psql -U postgres -d interview_platform -c \
  "UPDATE auth_service.users SET role_id = '00000000-0000-0000-0000-000000000004' WHERE email = 'admin@example.com';"
```

## Документация

- [SPEC.md](SPEC.md) — техническое задание
- [OVERVIEW.md](OVERVIEW.md) — обзор системы и ключевые решения
- [DATABASE.md](DATABASE.md) — схемы БД всех сервисов
- [INTERFACE.md](INTERFACE.md) — описание интерфейса по ролям
