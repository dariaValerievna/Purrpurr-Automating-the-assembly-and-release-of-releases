-- Удаляем старый constraint
ALTER TABLE releases DROP CONSTRAINT IF EXISTS releases_status_check;

-- Добавляем новый constraint со всеми статусами из ReleaseStatus enum
ALTER TABLE releases
    ADD CONSTRAINT releases_status_check
        CHECK (status IN (
                          'CREATED',
                          'BRANCH_CREATED',
                          'MERGE_REQUEST_CREATED',
                          'MERGE_REQUEST_MERGED',
                          'GITLAB_RELEASE_CREATED',
                          'COMPLETED',
                          'FAILED',
                          'IN_PROGRESS',
                          'CANCELLED'
            ));


