-- Создание таблицы для связи релизов с задачами YouTrack
CREATE TABLE release_issues (
                                id BIGSERIAL PRIMARY KEY,
                                release_id BIGINT NOT NULL,
                                issue_id VARCHAR(255) NOT NULL,
                                title TEXT,
                                status VARCHAR(100),
                                type VARCHAR(100),
                                priority VARCHAR(100),
                                assignee VARCHAR(255),
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_release_issues_release
                                    FOREIGN KEY (release_id) REFERENCES releases(id) ON DELETE CASCADE,

                                CONSTRAINT uk_release_issues_release_issue
                                    UNIQUE (release_id, issue_id)
);

-- Индексы для оптимизации запросов
CREATE INDEX idx_release_issues_release_id ON release_issues(release_id);
CREATE INDEX idx_release_issues_issue_id ON release_issues(issue_id);
CREATE INDEX idx_release_issues_status ON release_issues(status);
CREATE INDEX idx_release_issues_assignee ON release_issues(assignee);

-- Комментарии к таблице и полям
COMMENT ON TABLE release_issues IS 'Связь релизов с задачами YouTrack';
COMMENT ON COLUMN release_issues.release_id IS 'ID релиза';
COMMENT ON COLUMN release_issues.issue_id IS 'ID задачи в YouTrack';
COMMENT ON COLUMN release_issues.title IS 'Заголовок задачи';
COMMENT ON COLUMN release_issues.status IS 'Статус задачи в YouTrack';
COMMENT ON COLUMN release_issues.type IS 'Тип задачи';
COMMENT ON COLUMN release_issues.priority IS 'Приоритет задачи';
COMMENT ON COLUMN release_issues.assignee IS 'Исполнитель задачи';

-- Добавляем поля для YouTrack интеграции в таблицу releases
ALTER TABLE releases
    ADD COLUMN name VARCHAR(500),
ADD COLUMN description TEXT;

-- Заполняем name из version для существующих записей
UPDATE releases SET name = CONCAT('Release ', version) WHERE name IS NULL;

-- Заполняем description базовым значением
UPDATE releases SET description = 'Release created via Release Manager' WHERE description IS NULL;

-- Добавляем комментарии
COMMENT ON COLUMN releases.name IS 'Название релиза';
COMMENT ON COLUMN releases.description IS 'Описание релиза';
