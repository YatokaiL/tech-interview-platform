-- =============================================================
-- Interview Platform — PostgreSQL bootstrap
-- =============================================================
-- Creates one schema per microservice. Tables are managed
-- by each service via Liquibase migrations on startup.
--
-- The database `interview_platform` itself is created by the
-- POSTGRES_DB env var on the postgres container.
-- PostgreSQL 16 has gen_random_uuid() built-in.

CREATE SCHEMA IF NOT EXISTS auth_service;
CREATE SCHEMA IF NOT EXISTS subscription_service;
CREATE SCHEMA IF NOT EXISTS session_service;
CREATE SCHEMA IF NOT EXISTS execution_service;
CREATE SCHEMA IF NOT EXISTS analysis_service;
CREATE SCHEMA IF NOT EXISTS review_service;
CREATE SCHEMA IF NOT EXISTS report_service;
CREATE SCHEMA IF NOT EXISTS notification_service;
CREATE SCHEMA IF NOT EXISTS analytics_service;
