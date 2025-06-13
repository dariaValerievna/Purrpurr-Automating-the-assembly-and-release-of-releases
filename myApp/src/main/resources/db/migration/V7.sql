-- Делаем поле releaseBranch nullable
ALTER TABLE releases ALTER COLUMN release_branch DROP NOT NULL;

-- Или если используете другой синтаксис (зависит от БД)
-- ALTER TABLE releases MODIFY COLUMN release_branch VARCHAR(255) NULL;