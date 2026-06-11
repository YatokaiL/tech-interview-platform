--liquibase formatted sql

--changeset diploma:002-add-time-complexity splitStatements:true endDelimiter:;
ALTER TABLE code_analyses ADD COLUMN time_complexity VARCHAR(50);
--rollback ALTER TABLE code_analyses DROP COLUMN time_complexity;
