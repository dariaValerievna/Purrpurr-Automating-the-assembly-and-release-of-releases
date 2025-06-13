-- Создание таблицы для логирования активности релизов
CREATE TABLE release_activities (
                                    id BIGSERIAL PRIMARY KEY,
                                    release_id BIGINT NOT NULL,
                                    action VARCHAR(255) NOT NULL,
                                    timestamp TIMESTAMP NOT NULL,
                                    description TEXT,
                                    performed_by VARCHAR(255),
                                    status VARCHAR(50) NOT NULL,
                                    error_message TEXT,

                                    CONSTRAINT fk_release_activities_release
                                        FOREIGN KEY (release_id) REFERENCES releases(id) ON DELETE CASCADE
);

-- Индексы для быстрого поиска
CREATE INDEX idx_release_activities_release_id ON release_activities(release_id);
CREATE INDEX idx_release_activities_timestamp ON release_activities(timestamp);
CREATE INDEX idx_release_activities_status ON release_activities(status);


-- Добавление полей для GitLab интеграции в таблицу releases
ALTER TABLE releases
    ADD COLUMN gitlab_release_url VARCHAR(500),
ADD COLUMN gitlab_merge_request_url VARCHAR(500),
ADD COLUMN gitlab_tag_name VARCHAR(255),
ADD COLUMN release_notes TEXT,
ADD COLUMN created_by VARCHAR(255);

-- Обновление существующих записей (если есть)
UPDATE releases SET created_by = 'system' WHERE created_by IS NULL;
