--liquibase formatted sql

--changeset diploma:002-seed-tariffs splitStatements:true endDelimiter:;
INSERT INTO tariffs (code, name, description, price_cents, currency, max_sessions_per_month, features, sort_order) VALUES
('START', 'Старт',
 'Базовый тариф для небольших команд: до 10 интервью в месяц, ручной анализ кода.',
 49000, 'RUB', 10,
 '10 интервью / мес;Запуск кода в песочнице;Статический анализ Java;PDF-отчёты',
 1),
('TEAM', 'Команда',
 'Для активно нанимающих команд: до 50 интервью, авто-ревью с LLM, JSON-экспорт.',
 149000, 'RUB', 50,
 '50 интервью / мес;Авто-ревью с LLM;Big-O оценка алгоритмов;Экспорт PDF и JSON',
 2),
('BUSINESS', 'Бизнес',
 'Для растущих компаний: до 200 интервью, расширенная аналитика, приоритетная поддержка.',
 449000, 'RUB', 200,
 '200 интервью / мес;Расширенная аналитика;Приоритетная поддержка;История за 12 месяцев',
 3),
('ENTERPRISE', 'Корпорация',
 'Безлимит для крупных компаний: индивидуальный SLA, on-prem развёртывание, интеграции.',
 1490000, 'RUB', NULL,
 'Безлимитные интервью;Индивидуальный SLA;On-prem развёртывание;Интеграция с ATS',
 4);
--rollback DELETE FROM tariffs WHERE code IN ('START','TEAM','BUSINESS','ENTERPRISE');
